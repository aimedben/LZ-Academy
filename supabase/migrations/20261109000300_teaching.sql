-- =====================================================================
-- LZ Academy · 0003 · Pédagogie
-- Séances datées, feuilles d'appel et documents partagés.
-- =====================================================================

create table public.sessions (
    id         uuid primary key default gen_random_uuid(),
    group_id   uuid not null references public.groups (id) on delete cascade,
    teacher_id uuid references public.teachers (id) on delete set null,
    subject_id uuid references public.subjects (id) on delete set null,
    room_id    uuid references public.rooms (id) on delete set null,
    starts_at  timestamptz not null,
    ends_at    timestamptz not null,
    status     public.session_status not null default 'scheduled',
    topic      text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (ends_at > starts_at),
    unique (group_id, starts_at)
);

create index sessions_starts_idx on public.sessions (starts_at);
create index sessions_teacher_idx on public.sessions (teacher_id, starts_at);
create index sessions_group_idx on public.sessions (group_id, starts_at);

create trigger sessions_set_updated_at
    before update on public.sessions
    for each row execute function public.set_updated_at();

create table public.attendance (
    id          uuid primary key default gen_random_uuid(),
    session_id  uuid not null references public.sessions (id) on delete cascade,
    student_id  uuid not null references public.students (id) on delete cascade,
    status      public.attendance_status not null default 'present',
    note        text,
    recorded_by uuid references public.profiles (id) on delete set null,
    recorded_at timestamptz not null default now(),
    unique (session_id, student_id)
);

create index attendance_student_idx on public.attendance (student_id, recorded_at desc);
create index attendance_session_idx on public.attendance (session_id);

-- ---------------------------------------------------------------------
-- Supports de cours : métadonnées + chemin dans Supabase Storage
-- (bucket privé « materials »). Les BLOB ne vivent pas en base.
-- ---------------------------------------------------------------------
create table public.materials (
    id           uuid primary key default gen_random_uuid(),
    title        text not null,
    description  text,
    kind         public.file_kind not null default 'pdf',
    storage_path text not null unique,
    size_bytes   bigint check (size_bytes >= 0),
    subject_id   uuid references public.subjects (id) on delete set null,
    group_id     uuid references public.groups (id) on delete cascade,
    uploaded_by  uuid references public.profiles (id) on delete set null,
    is_published boolean not null default true,
    published_at timestamptz not null default now(),
    created_at   timestamptz not null default now()
);

create index materials_group_idx on public.materials (group_id, published_at desc);
create index materials_subject_idx on public.materials (subject_id);
