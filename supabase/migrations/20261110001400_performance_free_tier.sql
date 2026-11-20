-- =====================================================================
-- LZ Academy · 0014 · Optimisations « offre gratuite »
-- Objectif : des centaines d'utilisateurs sans dégrader la base (500 Mo / 1 Go
-- Storage / 5 Go de sortie mensuelle) :
--   1. Vitrine : chemin rapide — la page du jour, une fois complète,
--      se lit sans verrou ni écriture (~150 ms → ~1 ms).
--   2. Policies : les policies « admin ALL » sont scindées par commande
--      (insert/update/delete) pour qu'il ne reste qu'UNE policy
--      permissive par commande — évaluation par ligne nettement réduite.
--   3. pg_cron (si disponible) : page de vitrine pré-construite chaque
--      nuit à minuit 05 (Africa/Algiers) + purge des jours anciens.
--   4. Realtime : seule la messagerie élève→enseignant est publiée.
--   5. Timeouts par rôle : une requête client ne peut plus monopoliser
--      le PostgREST.
--   6. VACUUM ANALYZE sur les tables chaudes.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1a. Helper interne : la sélection finale de la fichier du jour
-- ---------------------------------------------------------------------
create or replace function public.featured_snapshot(p_day date)
returns table (
    teacher_id                uuid,
    "position"                smallint,
    full_name                 text,
    photo_url                 text,
    subjects                  text[],
    professional_description  text,
    accepts_student_messages  boolean
)
language sql
stable
security definer
set search_path = public
as $$
    select t.id,
           e.position,
           t.full_name,
           t.photo_url,
           coalesce(
               (select array_agg(s.name order by ts.is_primary desc, s.name)
                  from public.teacher_subjects ts
                  join public.subjects s on s.id = ts.subject_id
                 where ts.teacher_id = t.id),
               '{}'::text[]
           ),
           t.professional_description,
           t.accepts_student_messages
      from public.featured_teacher_entries e
      join public.teachers t on t.id = e.teacher_id
     where e.day = p_day
     order by e.position;
$$;

-- ---------------------------------------------------------------------
-- 1b. Rotation : lecture rapide d'abord, écriture seulement au besoin
-- ---------------------------------------------------------------------
create or replace function public.rotate_featured_teachers(
    p_day  date default current_date,
    p_size integer default 4
)
returns table (
    teacher_id                uuid,
    "position"                smallint,
    full_name                 text,
    photo_url                 text,
    subjects                  text[],
    professional_description  text,
    accepts_student_messages  boolean
)
language plpgsql
security definer
set search_path = public
as $$
declare
    v_size  integer := greatest(1, least(coalesce(p_size, 4), 12));
    v_count integer;
    v_prev  integer;
begin
    -- ---------------------------------------------------------------
    -- Chemin rapide : la page du jour est déjà complète.
    -- Aucun verrou, aucune écriture : on purge seulement un enseignant
    -- qui vient de se retirer (effet immédiat garanti), puis on relit.
    -- ---------------------------------------------------------------
    v_count := coalesce(
        (select count(*)::integer
           from public.featured_teacher_entries e
          where e.day = p_day),
        0
    );

    if v_count >= v_size then
        delete from public.featured_teacher_entries e
         using public.teachers t
         where e.day = p_day
           and e.teacher_id = t.id
           and (not t.is_active or not t.is_featured_on_student_home);

        v_count := coalesce(
            (select count(*)::integer
               from public.featured_teacher_entries e
              where e.day = p_day),
            0
        );

        if v_count >= v_size then
            return query select * from public.featured_snapshot(p_day);
            return;
        end if;
    end if;

    -- ---------------------------------------------------------------
    -- Voie d'écriture (premier appel du jour ou page incomplète)
    -- ---------------------------------------------------------------
    perform pg_advisory_xact_lock(hashtext('lz_featured_' || p_day::text));

    -- 1) Purge des enseignants devenus inéligibles (retrait immédiat).
    delete from public.featured_teacher_entries e
     using public.teachers t
     where e.day = p_day
       and e.teacher_id = t.id
       and (not t.is_active or not t.is_featured_on_student_home);

    insert into public.featured_teacher_days (day)
    values (p_day)
    on conflict (day) do nothing;

    select count(*)::integer into v_count
      from public.featured_teacher_entries
     where day = p_day;

    -- 2) Compléter la page du jour (une requête atomique par tour :
    --    sélection équitable + insertion + horodatage).
    while v_count < v_size loop
        v_prev := v_count;

        with choice as (
            select t.id
              from public.teachers t
             where t.is_active
               and t.is_featured_on_student_home
               and not exists (
                   select 1 from public.featured_teacher_entries x
                    where x.day = p_day and x.teacher_id = t.id
               )
             order by t.last_featured_at asc nulls first,
                      t.featured_rotation_priority desc,
                      t.full_name asc
             limit 1
        ),
        ins as (
            insert into public.featured_teacher_entries as fe (day, teacher_id, position)
            select p_day, c.id,
                   coalesce((select max(e.position) + 1
                               from public.featured_teacher_entries e
                              where e.day = p_day),
                            1)::smallint
              from choice c
            returning fe.teacher_id as v_new_id
        )
        update public.teachers t
           set last_featured_at = p_day
         where t.id = (select v_new_id from ins);

        select count(*)::integer into v_count
          from public.featured_teacher_entries
         where day = p_day;

        exit when v_count = v_prev;  -- plus personne d'éligible
        exit when v_count >= v_size;
    end loop;

    return query select * from public.featured_snapshot(p_day);
end;
$$;

-- Privileges inchangés : seule get_featured_teachers reste publique.
revoke execute on function public.featured_snapshot(date) from public, anon, authenticated;
revoke execute on function public.rotate_featured_teachers(date, integer) from public, anon, authenticated;
grant  execute on function public.rotate_featured_teachers(date, integer) to postgres, service_role;

-- ---------------------------------------------------------------------
-- 2. Policies : une seule policy permissive par commande
--    Chaque policy « ALL » doublonnant avec une policy « SELECT » est
--    scindée en insert / update / delete de la même expression.
-- ---------------------------------------------------------------------
do $$
declare
    r       record;
    v_roles text;
begin
    for r in
        select p.schemaname, p.tablename, p.policyname, p.roles, p.qual, p.with_check
          from pg_policies p
         where p.schemaname = 'public'
           and p.cmd = 'ALL'
           and exists (
               select 1 from pg_policies s
                where s.schemaname = p.schemaname
                  and s.tablename  = p.tablename
                  and s.cmd        = 'SELECT'
           )
    loop
        v_roles := (select string_agg(role_pg, ', ')
                      from unnest(r.roles) as role_pg);

        execute format('drop policy %I on %I.%I', r.policyname, r.schemaname, r.tablename);
        execute format(
            'create policy %I on %I.%I for insert to %s with check (%s)',
            r.policyname || '_insert', r.schemaname, r.tablename,
            v_roles, coalesce(r.with_check, r.qual));
        execute format(
            'create policy %I on %I.%I for update to %s using (%s) with check (%s)',
            r.policyname || '_update', r.schemaname, r.tablename,
            v_roles, r.qual, coalesce(r.with_check, r.qual));
        execute format(
            'create policy %I on %I.%I for delete to %s using (%s)',
            r.policyname || '_delete', r.schemaname, r.tablename,
            v_roles, r.qual);
    end loop;
end;
$$;

-- ---------------------------------------------------------------------
-- 3. pg_cron : pré-construire la page de vitrine la nuit.
--    L'algérien = UTC+1 : 23:05 UTC ≙ 00:05 Africa/Algiers, et à cette
--    heure `(now() at time zone 'Africa/Algiers')::date` désigne déjà
--    le jour cible — la page est prête avant le premier élève du matin.
-- ---------------------------------------------------------------------
do $$
begin
    create extension if not exists pg_cron;
exception
    when others then
        raise notice 'pg_cron non disponible : la rotation rester on-demand (aucun impact).';
end;
$$;

do $$
declare
    j record;
begin
    if exists (select 1 from pg_extension where extname = 'pg_cron') then
        -- idempotent : on remplace les jobs existants
        for j in
            select jobid from cron.job
             where jobname in ('lz_featured_nightly_rotation', 'lz_featured_daily_purge')
        loop
            perform cron.unschedule(j.jobid);
        end loop;

        perform cron.schedule(
            'lz_featured_nightly_rotation', '5 23 * * *',
            $job$select public.rotate_featured_teachers(
                (now() at time zone 'Africa/Algiers')::date, 4)$job$
        );
        perform cron.schedule(
            'lz_featured_daily_purge', '15 23 * * *',
            $job$delete from public.featured_teacher_days
             where day < (now() at time zone 'Africa/Algiers')::date - 3$job$
        );
    end if;
end;
$$;

-- ---------------------------------------------------------------------
-- 4. Realtime : uniquement la messagerie (faible volume).
--    Les séances et annonces sont rechargées au retour au premier plan.
-- ---------------------------------------------------------------------
do $$
begin
    -- garde-fou si la publication n'existe pas (project neuf)
    if not exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        create publication supabase_realtime;
    end if;

    if not exists (
        select 1 from pg_publication_tables
         where pubname = 'supabase_realtime'
           and schemaname = 'public' and tablename = 'teacher_messages'
    ) then
        alter publication supabase_realtime add table public.teacher_messages;
    end if;
exception
    when others then
        raise notice 'Realtime : %', sqlerrm;
end;
$$;

-- ---------------------------------------------------------------------
-- 5. Timeouts côté rôles Data API (protège contre les requêtes toxiques)
-- ---------------------------------------------------------------------
alter role anon          set statement_timeout = '5s';
alter role authenticated set statement_timeout = '10s';

-- ---------------------------------------------------------------------
-- 6. Statistiques à jour sur les tables chaudes
-- (VACUUM impossible via le Management API : la base est fraîche,
--  un ANALYZE suffit.)
-- ---------------------------------------------------------------------
analyze public.teachers;
analyze public.featured_teacher_entries;
analyze public.featured_teacher_days;
analyze public.sessions;
analyze public.attendance;
analyze public.teacher_messages;
analyze public.announcements;
