-- =====================================================================
-- LZ Academy · 0011 · Correction de la récursion RLS
-- Les policies students_select et enrollments_select se référençaient
-- mutuellement, ce que PostgreSQL refuse (infinite recursion).
-- Les contrôles croisés passent désormais par des fonctions
-- SECURITY DEFINER : elles lisent les tables sans réappliquer les RLS.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Helpers de contrôle d'accès
-- ---------------------------------------------------------------------
create or replace function public.is_teacher_of_student(p_student_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
          from public.enrollments e
          join public.groups g on g.id = e.group_id
          join public.teachers t on t.id = g.teacher_id
         where e.student_id = p_student_id
           and e.left_on is null
           and t.profile_id = auth.uid()
    );
$$;

create or replace function public.is_student_self(p_student_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
          from public.students s
         where s.id = p_student_id
           and s.profile_id = auth.uid()
    );
$$;

create or replace function public.is_teacher_of_group(p_group_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
          from public.groups g
          join public.teachers t on t.id = g.teacher_id
         where g.id = p_group_id
           and t.profile_id = auth.uid()
    );
$$;

revoke execute on function public.is_teacher_of_student(uuid) from public, anon;
revoke execute on function public.is_student_self(uuid)      from public, anon;
revoke execute on function public.is_teacher_of_group(uuid)  from public, anon;
grant  execute on function public.is_teacher_of_student(uuid) to authenticated;
grant  execute on function public.is_student_self(uuid)      to authenticated;
grant  execute on function public.is_teacher_of_group(uuid)  to authenticated;

-- ---------------------------------------------------------------------
-- Policies sans référence croisée
-- ---------------------------------------------------------------------
drop policy if exists students_select on public.students;
create policy students_select on public.students
    for select to authenticated
    using (
        profile_id = (select auth.uid())
        or public.is_admin()
        or public.is_teacher_of_student(id)
    );

drop policy if exists enrollments_select on public.enrollments;
create policy enrollments_select on public.enrollments
    for select to authenticated
    using (
        public.is_admin()
        or public.is_student_self(student_id)
        or public.is_teacher_of_group(group_id)
    );
