-- =====================================================================
-- LZ Academy · 0007 · Sécurité
-- RLS sur toutes les tables + bucket Storage privé « materials ».
-- Principe : chaque rôle ne voit que ce qui le concerne ; l'enseignant
-- reste seul maître de la réception des messages.
-- =====================================================================

alter table public.profiles                enable row level security;
alter table public.academy_settings        enable row level security;
alter table public.audit_log               enable row level security;
alter table public.academic_years          enable row level security;
alter table public.levels                  enable row level security;
alter table public.subjects                enable row level security;
alter table public.teachers                enable row level security;
alter table public.teacher_subjects        enable row level security;
alter table public.teacher_levels          enable row level security;
alter table public.students                enable row level security;
alter table public.rooms                   enable row level security;
alter table public.groups                  enable row level security;
alter table public.group_schedule_slots    enable row level security;
alter table public.enrollments             enable row level security;
alter table public.sessions                enable row level security;
alter table public.attendance              enable row level security;
alter table public.materials               enable row level security;
alter table public.packs                   enable row level security;
alter table public.subscriptions           enable row level security;
alter table public.payments                enable row level security;
alter table public.announcements           enable row level security;
alter table public.announcement_reads      enable row level security;
alter table public.broadcasts              enable row level security;
alter table public.teacher_messages        enable row level security;
alter table public.featured_teacher_days   enable row level security;
alter table public.featured_teacher_entries enable row level security;

-- ---------------------------------------------------------------------
-- Profils
-- ---------------------------------------------------------------------
create policy profiles_select_own on public.profiles
    for select to authenticated
    using (id = auth.uid() or public.is_admin());

create policy profiles_update_own on public.profiles
    for update to authenticated
    using (id = auth.uid())
    with check (id = auth.uid());

create policy profiles_admin_all on public.profiles
    for all to authenticated
    using (public.is_admin())
    with check (public.is_admin());

-- ---------------------------------------------------------------------
-- Paramètres de l'académie / années / niveaux / matières / salles / packs
-- ---------------------------------------------------------------------
create policy academy_settings_read on public.academy_settings
    for select to anon, authenticated using (true);

create policy academy_settings_admin_write on public.academy_settings
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy academic_years_read on public.academic_years
    for select to anon, authenticated using (true);

create policy academic_years_admin_write on public.academic_years
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy levels_read on public.levels
    for select to anon, authenticated using (true);

create policy levels_admin_write on public.levels
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy subjects_read on public.subjects
    for select to anon, authenticated using (true);

create policy subjects_admin_write on public.subjects
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy rooms_read on public.rooms
    for select to anon, authenticated using (true);

create policy rooms_admin_write on public.rooms
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy packs_read on public.packs
    for select to anon, authenticated using (true);

create policy packs_admin_write on public.packs
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- ---------------------------------------------------------------------
-- Enseignants
-- ---------------------------------------------------------------------
create policy teachers_read on public.teachers
    for select to anon, authenticated using (true);

create policy teachers_update_own on public.teachers
    for update to authenticated
    using (profile_id = auth.uid())
    with check (profile_id = auth.uid());

create policy teachers_admin_write on public.teachers
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- ---------------------------------------------------------------------
-- Matières / niveaux des enseignants
-- ---------------------------------------------------------------------
create policy teacher_subjects_read on public.teacher_subjects
    for select to anon, authenticated using (true);

create policy teacher_subjects_owner_write on public.teacher_subjects
    for all to authenticated
    using (exists (
        select 1 from public.teachers t
         where t.id = teacher_id and t.profile_id = auth.uid()
    ))
    with check (exists (
        select 1 from public.teachers t
         where t.id = teacher_id and t.profile_id = auth.uid()
    ));

create policy teacher_subjects_admin_write on public.teacher_subjects
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy teacher_levels_read on public.teacher_levels
    for select to anon, authenticated using (true);

create policy teacher_levels_owner_write on public.teacher_levels
    for all to authenticated
    using (exists (
        select 1 from public.teachers t
         where t.id = teacher_id and t.profile_id = auth.uid()
    ))
    with check (exists (
        select 1 from public.teachers t
         where t.id = teacher_id and t.profile_id = auth.uid()
    ));

create policy teacher_levels_admin_write on public.teacher_levels
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- ---------------------------------------------------------------------
-- Élèves
-- ---------------------------------------------------------------------
create policy students_select on public.students
    for select to authenticated
    using (
        profile_id = auth.uid()
        or public.is_admin()
        or exists (
            select 1
              from public.enrollments e
              join public.groups g on g.id = e.group_id
              join public.teachers t on t.id = g.teacher_id
             where e.student_id = students.id
               and e.left_on is null
               and t.profile_id = auth.uid()
        )
    );

create policy students_admin_write on public.students
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- ---------------------------------------------------------------------
-- Groupes / créneaux / inscriptions
-- ---------------------------------------------------------------------
create policy groups_read on public.groups
    for select to authenticated using (true);

create policy groups_admin_write on public.groups
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy group_slots_read on public.group_schedule_slots
    for select to authenticated using (true);

create policy group_slots_admin_write on public.group_schedule_slots
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy enrollments_select on public.enrollments
    for select to authenticated
    using (
        public.is_admin()
        or exists (
            select 1 from public.students s
             where s.id = enrollments.student_id and s.profile_id = auth.uid()
        )
        or exists (
            select 1
              from public.groups g
              join public.teachers t on t.id = g.teacher_id
             where g.id = enrollments.group_id and t.profile_id = auth.uid()
        )
    );

create policy enrollments_admin_write on public.enrollments
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- ---------------------------------------------------------------------
-- Séances
-- ---------------------------------------------------------------------
create policy sessions_select on public.sessions
    for select to authenticated
    using (
        public.is_admin()
        or exists (
            select 1
              from public.groups g
              join public.teachers t on t.id = g.teacher_id
             where g.id = sessions.group_id and t.profile_id = auth.uid()
        )
        or exists (
            select 1
              from public.enrollments e
              join public.students s on s.id = e.student_id
             where e.group_id = sessions.group_id
               and e.left_on is null
               and s.profile_id = auth.uid()
        )
    );

create policy sessions_teacher_write on public.sessions
    for all to authenticated
    using (
        public.is_admin()
        or exists (
            select 1
              from public.groups g
              join public.teachers t on t.id = g.teacher_id
             where g.id = sessions.group_id and t.profile_id = auth.uid()
        )
    )
    with check (
        public.is_admin()
        or exists (
            select 1
              from public.groups g
              join public.teachers t on t.id = g.teacher_id
             where g.id = sessions.group_id and t.profile_id = auth.uid()
        )
    );

-- ---------------------------------------------------------------------
-- Présences
-- ---------------------------------------------------------------------
create policy attendance_select on public.attendance
    for select to authenticated
    using (
        public.is_admin()
        or exists (
            select 1 from public.students s
             where s.id = attendance.student_id and s.profile_id = auth.uid()
        )
        or exists (
            select 1
              from public.sessions ss
              join public.groups g on g.id = ss.group_id
              join public.teachers t on t.id = g.teacher_id
             where ss.id = attendance.session_id and t.profile_id = auth.uid()
        )
    );

create policy attendance_teacher_write on public.attendance
    for all to authenticated
    using (
        public.is_admin()
        or exists (
            select 1
              from public.sessions ss
              join public.groups g on g.id = ss.group_id
              join public.teachers t on t.id = g.teacher_id
             where ss.id = attendance.session_id and t.profile_id = auth.uid()
        )
    )
    with check (
        public.is_admin()
        or exists (
            select 1
              from public.sessions ss
              join public.groups g on g.id = ss.group_id
              join public.teachers t on t.id = g.teacher_id
             where ss.id = attendance.session_id and t.profile_id = auth.uid()
        )
    );

-- ---------------------------------------------------------------------
-- Supports de cours
-- ---------------------------------------------------------------------
create policy materials_select on public.materials
    for select to authenticated
    using (
        public.is_admin()
        or uploaded_by = auth.uid()
        or (
            is_published
            and (
                group_id is null
                or exists (
                    select 1
                      from public.enrollments e
                      join public.students s on s.id = e.student_id
                     where e.group_id = materials.group_id
                       and e.left_on is null
                       and s.profile_id = auth.uid()
                )
                or exists (
                    select 1
                      from public.groups g
                      join public.teachers t on t.id = g.teacher_id
                     where g.id = materials.group_id and t.profile_id = auth.uid()
                )
            )
        )
    );

create policy materials_owner_write on public.materials
    for all to authenticated
    using (uploaded_by = auth.uid() or public.is_admin())
    with check (uploaded_by = auth.uid() or public.is_admin());

-- ---------------------------------------------------------------------
-- Abonnements & paiements
-- ---------------------------------------------------------------------
create policy subscriptions_select on public.subscriptions
    for select to authenticated
    using (
        public.is_admin()
        or exists (
            select 1 from public.students s
             where s.id = subscriptions.student_id and s.profile_id = auth.uid()
        )
    );

create policy subscriptions_admin_write on public.subscriptions
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy payments_select on public.payments
    for select to authenticated
    using (
        public.is_admin()
        or exists (
            select 1 from public.students s
             where s.id = payments.student_id and s.profile_id = auth.uid()
        )
    );

create policy payments_admin_write on public.payments
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- ---------------------------------------------------------------------
-- Annonces
-- ---------------------------------------------------------------------
create policy announcements_select on public.announcements
    for select to authenticated
    using (
        public.is_admin()
        or (
            published_at <= now()
            and (expires_at is null or expires_at > now())
            and (
                audience = 'all'
                or (audience = 'students' and public.current_student_id() is not null)
                or (audience = 'teachers' and public.current_teacher_id() is not null)
                or (audience = 'level' and (
                    exists (
                        select 1 from public.students s
                         where s.profile_id = auth.uid() and s.level_id = announcements.level_id
                    )
                    or exists (
                        select 1
                          from public.teacher_levels tl
                          join public.teachers t on t.id = tl.teacher_id
                         where tl.level_id = announcements.level_id and t.profile_id = auth.uid()
                    )
                ))
                or (audience = 'group' and (
                    exists (
                        select 1
                          from public.enrollments e
                          join public.students s on s.id = e.student_id
                         where e.group_id = announcements.group_id
                           and e.left_on is null
                           and s.profile_id = auth.uid()
                    )
                    or exists (
                        select 1
                          from public.groups g
                          join public.teachers t on t.id = g.teacher_id
                         where g.id = announcements.group_id and t.profile_id = auth.uid()
                    )
                ))
            )
        )
    );

create policy announcements_admin_write on public.announcements
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy announcement_reads_select on public.announcement_reads
    for select to authenticated
    using (user_id = auth.uid() or public.is_admin());

create policy announcement_reads_own_write on public.announcement_reads
    for insert to authenticated
    with check (user_id = auth.uid());

create policy announcement_reads_own_update on public.announcement_reads
    for update to authenticated
    using (user_id = auth.uid()) with check (user_id = auth.uid());

create policy announcement_reads_own_delete on public.announcement_reads
    for delete to authenticated
    using (user_id = auth.uid());

-- ---------------------------------------------------------------------
-- Diffusions : administration uniquement
-- ---------------------------------------------------------------------
create policy broadcasts_admin_all on public.broadcasts
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- ---------------------------------------------------------------------
-- Messages élève → enseignant
-- Insertion exclusivement via la RPC send_teacher_message().
-- ---------------------------------------------------------------------
create policy teacher_messages_student_select on public.teacher_messages
    for select to authenticated
    using (exists (
        select 1 from public.students s
         where s.id = teacher_messages.student_id and s.profile_id = auth.uid()
    ));

create policy teacher_messages_teacher_select on public.teacher_messages
    for select to authenticated
    using (exists (
        select 1 from public.teachers t
         where t.id = teacher_messages.teacher_id and t.profile_id = auth.uid()
    ));

create policy teacher_messages_teacher_update on public.teacher_messages
    for update to authenticated
    using (exists (
        select 1 from public.teachers t
         where t.id = teacher_messages.teacher_id and t.profile_id = auth.uid()
    ))
    with check (exists (
        select 1 from public.teachers t
         where t.id = teacher_messages.teacher_id and t.profile_id = auth.uid()
    ));

-- ---------------------------------------------------------------------
-- Rotation de la vitrine : lecture publique, écriture via RPC only
-- ---------------------------------------------------------------------
create policy featured_days_read on public.featured_teacher_days
    for select to anon, authenticated using (true);

create policy featured_entries_read on public.featured_teacher_entries
    for select to anon, authenticated using (true);

-- ---------------------------------------------------------------------
-- Journal d'audit : lecture administration
-- ---------------------------------------------------------------------
create policy audit_log_admin_read on public.audit_log
    for select to authenticated using (public.is_admin());

-- ---------------------------------------------------------------------
-- Privilèges (RLS reste la barrière réelle)
-- ---------------------------------------------------------------------
grant usage on schema public to anon, authenticated;
grant select on all tables in schema public to anon;
grant select, insert, update, delete on all tables in schema public to authenticated;
grant usage, select on all sequences in schema public to anon, authenticated;

-- ---------------------------------------------------------------------
-- Storage : bucket privé « materials »
-- Chemin attendu : {user_id}/{group_id}/{fichier}
-- ---------------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('materials', 'materials', false)
on conflict (id) do nothing;

create policy materials_storage_read on storage.objects
    for select to authenticated
    using (bucket_id = 'materials');

create policy materials_storage_insert_own on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'materials'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy materials_storage_update_own on storage.objects
    for update to authenticated
    using (
        bucket_id = 'materials'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy materials_storage_delete_own on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'materials'
        and (storage.foldername(name))[1] = auth.uid()::text
    );
