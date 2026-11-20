-- =====================================================================
-- LZ Academy · 0006 · Fonctions métier, RPC et vues
-- =====================================================================

-- ---------------------------------------------------------------------
-- Helpers d'identité (utilisés par les policies RLS)
-- ---------------------------------------------------------------------
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.profiles p
         where p.id = auth.uid() and p.role = 'admin'
    );
$$;

create or replace function public.current_student_id()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
    select s.id from public.students s
     where s.profile_id = auth.uid() and s.is_active
     limit 1;
$$;

create or replace function public.current_teacher_id()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
    select t.id from public.teachers t
     where t.profile_id = auth.uid() and t.is_active
     limit 1;
$$;

-- ---------------------------------------------------------------------
-- Garde-fou : les deux réglages enseignants sont indépendants.
-- L'admin gère la visibilité ; il ne peut jamais forcer la messagerie.
-- ---------------------------------------------------------------------
create or replace function public.guard_teacher_settings()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    v_owner boolean;
begin
    if auth.uid() is null then
        return new;   -- service role / éditeur SQL
    end if;

    v_owner := exists (
        select 1 from public.teachers t
         where t.id = new.id and t.profile_id = auth.uid()
    );
    if v_owner then
        return new;
    end if;

    if not public.is_admin() then
        raise exception 'Modification non autorisée.';
    end if;

    if new.accepts_student_messages is distinct from old.accepts_student_messages then
        raise exception 'La réception des messages reste contrôlée par l''enseignant.';
    end if;

    return new;
end;
$$;

drop trigger if exists teachers_guard_settings on public.teachers;
create trigger teachers_guard_settings
    before update on public.teachers
    for each row execute function public.guard_teacher_settings();

-- ---------------------------------------------------------------------
-- Rotation quotidienne de la vitrine « Nos enseignants »
-- · page persistée et stable pour la journée ;
-- · sélection équitable : jamais mis en avant d'abord, puis priorité ;
-- · les enseignants retirés sont purgés dès la lecture.
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
    v_size        integer := greatest(1, least(coalesce(p_size, 4), 12));
    v_count       integer;
    v_next        integer;
    v_teacher_id  uuid;
begin
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

    -- 2) Compléter la page du jour.
    while v_count < v_size loop
        select coalesce(max(e.position), 0) + 1 into v_next
          from public.featured_teacher_entries e
         where e.day = p_day;

        select t.id into v_teacher_id
          from public.teachers t
         where t.is_active
           and t.is_featured_on_student_home
           and not exists (
               select 1 from public.featured_teacher_entries e
                where e.day = p_day and e.teacher_id = t.id
           )
         order by t.last_featured_at asc nulls first,
                  t.featured_rotation_priority desc,
                  t.full_name asc
         limit 1;

        exit when v_teacher_id is null;

        insert into public.featured_teacher_entries (day, teacher_id, position)
        values (p_day, v_teacher_id, v_next::smallint);

        update public.teachers
           set last_featured_at = p_day
         where id = v_teacher_id;

        v_count := v_count + 1;
    end loop;

    return query
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
end;
$$;

-- ---------------------------------------------------------------------
-- Envoi d'un message élève → enseignant (refusé si hors ligne).
-- ---------------------------------------------------------------------
create or replace function public.send_teacher_message(
    p_teacher_id uuid,
    p_body       text
)
returns public.teacher_messages
language plpgsql
security definer
set search_path = public
as $$
declare
    v_student_id uuid;
    v_row        public.teacher_messages;
begin
    v_student_id := public.current_student_id();
    if v_student_id is null then
        raise exception 'Profil élève introuvable pour cet utilisateur.';
    end if;

    if not exists (
        select 1 from public.teachers t
         where t.id = p_teacher_id
           and t.is_active
           and t.accepts_student_messages
    ) then
        raise exception 'Cet enseignant n''est actuellement pas disponible pour recevoir des messages.';
    end if;

    insert into public.teacher_messages (student_id, teacher_id, body)
    values (v_student_id, p_teacher_id, btrim(p_body))
    returning * into v_row;

    return v_row;
end;
$$;

-- ---------------------------------------------------------------------
-- Vues de lecture (security_invoker : les RLS de l'appelant s'appliquent)
-- ---------------------------------------------------------------------
create or replace view public.v_teacher_directory
with (security_invoker = true) as
select
    t.id,
    t.full_name,
    t.photo_url,
    t.professional_description,
    coalesce(
        (select array_agg(s.name order by ts.is_primary desc, s.name)
           from public.teacher_subjects ts
           join public.subjects s on s.id = ts.subject_id
          where ts.teacher_id = t.id),
        '{}'::text[]
    ) as subjects,
    coalesce(
        (select array_agg(l.name order by l.order_index)
           from public.teacher_levels tl
           join public.levels l on l.id = tl.level_id
          where tl.teacher_id = t.id),
        '{}'::text[]
    ) as levels,
    t.years_of_experience,
    t.is_featured_on_student_home,
    t.accepts_student_messages,
    t.featured_rotation_priority,
    t.last_featured_at,
    t.is_active,
    (select count(*) from public.groups g where g.teacher_id = t.id) as groups_count
from public.teachers t;

create or replace view public.v_showcase_stats
with (security_invoker = true) as
select
    count(*) filter (where t.is_featured_on_student_home)      as featured_count,
    count(*) filter (where not t.is_featured_on_student_home)  as opted_out_count,
    count(*) filter (where t.accepts_student_messages)         as available_count,
    count(*) filter (where not t.accepts_student_messages)     as offline_count,
    count(*)                                                   as total_count
from public.teachers t
where t.is_active;

create or replace view public.v_monthly_revenue
with (security_invoker = true) as
select
    date_trunc('month', coalesce(p.paid_at, p.created_at))::date as month,
    sum(p.amount_dzd)                                            as revenue_dzd,
    count(*)                                                     as payments_count
from public.payments p
where p.status = 'paid'
group by 1
order by 1 desc;

create or replace view public.v_attendance_summary
with (security_invoker = true) as
select
    s.id                                                                   as student_id,
    s.matricule,
    s.full_name,
    l.name                                                                 as level_name,
    count(a.id)                                                            as sessions_recorded,
    count(a.id) filter (where a.status = 'present')                        as present_count,
    count(a.id) filter (where a.status in ('absent', 'justified'))         as absent_count,
    count(a.id) filter (where a.status = 'late')                           as late_count,
    round(
        100.0 * count(a.id) filter (where a.status in ('present', 'late'))
        / nullif(count(a.id), 0), 1
    )                                                                      as attendance_rate
from public.students s
left join public.levels l on l.id = s.level_id
left join public.attendance a on a.student_id = s.id
group by s.id, s.matricule, s.full_name, l.name;

create or replace view public.v_student_overview
with (security_invoker = true) as
select
    s.id,
    s.matricule,
    s.full_name,
    s.level_id,
    l.name as level_name,
    s.is_active,
    (select count(*) from public.enrollments e
      where e.student_id = s.id and e.left_on is null)              as groups_count,
    coalesce((
        select sub.sessions_remaining
          from public.subscriptions sub
         where sub.student_id = s.id and sub.status = 'active'
         order by sub.created_at desc
         limit 1
    ), 0)                                                           as sessions_remaining,
    coalesce((
        select sum(p.amount_dzd)
          from public.payments p
         where p.student_id = s.id and p.status = 'overdue'
    ), 0)                                                           as overdue_amount_dzd
from public.students s
left join public.levels l on l.id = s.level_id;

grant execute on function public.rotate_featured_teachers(date, integer) to anon, authenticated;
grant execute on function public.send_teacher_message(uuid, text) to authenticated;
