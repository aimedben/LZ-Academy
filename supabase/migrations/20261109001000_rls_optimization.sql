-- =====================================================================
-- LZ Academy · 0010 · Optimisation RLS & RPC vitrine
-- 1. Les policies évaluent auth.uid() une seule fois par requête
--    ((select auth.uid()) au lieu de auth.uid() par ligne).
-- 2. La rotation de la vitrine n'est plus exposée directement aux
--    clients : seule get_featured_teachers() est publique et travaille
--    toujours sur la date du jour.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Réécriture mécanique des expressions RLS
-- ---------------------------------------------------------------------
do $$
declare
    r       record;
    v_using text;
    v_check text;
begin
    for r in
        select schemaname, tablename, policyname, qual, with_check
          from pg_policies
         where schemaname = 'public'
           and (qual like '%auth.uid()%' or with_check like '%auth.uid()%')
           and coalesce(qual, '') not like '%(select auth.uid())%'
           and coalesce(with_check, '') not like '%(select auth.uid())%'
    loop
        v_using := case
            when r.qual is null then null
            else replace(r.qual, 'auth.uid()', '(select auth.uid())')
        end;
        v_check := case
            when r.with_check is null then null
            else replace(r.with_check, 'auth.uid()', '(select auth.uid())')
        end;

        execute format(
            'alter policy %I on %I.%I%s%s',
            r.policyname,
            r.schemaname,
            r.tablename,
            case when v_using is null then '' else format(' using (%s)', v_using) end,
            case when v_check is null then '' else format(' with check (%s)', v_check) end
        );
    end loop;
end;
$$;

-- ---------------------------------------------------------------------
-- 2. RPC publique en lecture : vitrine du jour uniquement
-- ---------------------------------------------------------------------
create or replace function public.get_featured_teachers(p_size integer default 4)
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
begin
    return query
    select *
      from public.rotate_featured_teachers(
          current_date,
          greatest(1, least(coalesce(p_size, 4), 12))
      );
end;
$$;

revoke execute on function public.rotate_featured_teachers(date, integer) from public, anon, authenticated;
revoke execute on function public.get_featured_teachers(integer) from public;
grant  execute on function public.get_featured_teachers(integer) to anon, authenticated;
