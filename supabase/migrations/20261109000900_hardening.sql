-- =====================================================================
-- LZ Academy · 0009 · Durcissement & performance
-- · extension pg_trgm déplacée hors du schéma exposé ;
-- · search_path figé sur la fonction updated_at ;
-- · privilèges EXECUTE resserrés sur les fonctions internes ;
-- · index couvrants sur les clés étrangères signalées par l'advisor.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Extension hors du schéma public
-- (les index GIN existants restent valides : ils référencent l'OID de
--  l'opérateur, pas son nom).
-- ---------------------------------------------------------------------
create schema if not exists extensions;
alter extension pg_trgm set schema extensions;

-- ---------------------------------------------------------------------
-- search_path immuable sur le trigger générique
-- ---------------------------------------------------------------------
alter function public.set_updated_at() set search_path = public;

-- ---------------------------------------------------------------------
-- Fonctions internes : jamais exposées via /rest/v1/rpc
-- ---------------------------------------------------------------------
revoke execute on function public.set_updated_at()        from public, anon, authenticated;
revoke execute on function public.handle_new_user()       from public, anon, authenticated;
revoke execute on function public.guard_profile_role()    from public, anon, authenticated;
revoke execute on function public.guard_teacher_settings() from public, anon, authenticated;

-- Helpers RLS : nécessaires aux utilisateurs connectés, inutiles à anon.
revoke execute on function public.is_admin()          from public, anon;
revoke execute on function public.current_student_id() from public, anon;
revoke execute on function public.current_teacher_id() from public, anon;
grant  execute on function public.is_admin()          to authenticated;
grant  execute on function public.current_student_id() to authenticated;
grant  execute on function public.current_teacher_id() to authenticated;

-- Rotation de la vitrine : volontairement publique (accueil élève).
revoke execute on function public.rotate_featured_teachers(date, integer) from public;
grant  execute on function public.rotate_featured_teachers(date, integer) to anon, authenticated;

-- Messagerie : réservée aux utilisateurs connectés.
revoke execute on function public.send_teacher_message(uuid, text) from public, anon;
grant  execute on function public.send_teacher_message(uuid, text) to authenticated;

-- ---------------------------------------------------------------------
-- Index des clés étrangères (évite les scans lors des suppressions
-- en cascade et des jointures inverses).
-- ---------------------------------------------------------------------
create index if not exists academy_settings_year_idx     on public.academy_settings (academic_year_id);
create index if not exists announcement_reads_user_idx   on public.announcement_reads (user_id);
create index if not exists announcements_level_idx       on public.announcements (level_id);
create index if not exists announcements_group_idx       on public.announcements (group_id);
create index if not exists announcements_publisher_idx   on public.announcements (published_by);
create index if not exists attendance_recorder_idx       on public.attendance (recorded_by);
create index if not exists audit_log_actor_idx           on public.audit_log (actor_id);
create index if not exists broadcasts_level_idx          on public.broadcasts (level_id);
create index if not exists broadcasts_group_idx          on public.broadcasts (group_id);
create index if not exists broadcasts_sender_idx         on public.broadcasts (sent_by);
create index if not exists group_slots_room_idx          on public.group_schedule_slots (room_id);
create index if not exists groups_year_idx               on public.groups (academic_year_id);
create index if not exists groups_room_idx               on public.groups (default_room_id);
create index if not exists materials_uploader_idx        on public.materials (uploaded_by);
create index if not exists payments_subscription_idx     on public.payments (subscription_id);
create index if not exists payments_recorder_idx         on public.payments (recorded_by);
create index if not exists sessions_room_idx             on public.sessions (room_id);
create index if not exists sessions_subject_idx          on public.sessions (subject_id);
create index if not exists subscriptions_pack_idx        on public.subscriptions (pack_id);
