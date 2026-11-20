-- =====================================================================
-- LZ Academy · 0005 · Vie scolaire & communication
-- Annonces, accusés de lecture, diffusions (SMS/Push/Email),
-- messages élève → enseignant et rotation quotidienne de la vitrine.
-- =====================================================================

create table public.announcements (
    id           uuid primary key default gen_random_uuid(),
    title        text not null,
    body         text not null,
    audience     public.audience not null default 'all',
    level_id     uuid references public.levels (id) on delete cascade,
    group_id     uuid references public.groups (id) on delete cascade,
    is_pinned    boolean not null default false,
    published_by uuid references public.profiles (id) on delete set null,
    published_at timestamptz not null default now(),
    expires_at   timestamptz,
    created_at   timestamptz not null default now(),
    check (
        (audience = 'level' and level_id is not null)
        or (audience = 'group' and group_id is not null)
        or audience not in ('level', 'group')
    )
);

create index announcements_published_idx on public.announcements (published_at desc);
create index announcements_audience_idx on public.announcements (audience, level_id, group_id);

create table public.announcement_reads (
    announcement_id uuid not null references public.announcements (id) on delete cascade,
    user_id         uuid not null references public.profiles (id) on delete cascade,
    read_at         timestamptz not null default now(),
    primary key (announcement_id, user_id)
);

create table public.broadcasts (
    id               uuid primary key default gen_random_uuid(),
    title            text not null,
    body             text not null,
    audience         public.audience not null default 'all',
    channel          public.broadcast_channel not null default 'push',
    level_id         uuid references public.levels (id) on delete set null,
    group_id         uuid references public.groups (id) on delete set null,
    recipients_count integer not null default 0 check (recipients_count >= 0),
    delivered_count  integer not null default 0 check (delivered_count >= 0),
    sent_by          uuid references public.profiles (id) on delete set null,
    sent_at          timestamptz not null default now(),
    created_at       timestamptz not null default now(),
    check (delivered_count <= recipients_count)
);

create index broadcasts_sent_idx on public.broadcasts (sent_at desc);

-- ---------------------------------------------------------------------
-- Messagerie élève → enseignant.
-- La réception reste un choix de l'enseignant ; l'envoi passe par la RPC
-- send_teacher_message() qui refuse si le professeur est hors ligne.
-- ---------------------------------------------------------------------
create table public.teacher_messages (
    id         uuid primary key default gen_random_uuid(),
    student_id uuid not null references public.students (id) on delete cascade,
    teacher_id uuid not null references public.teachers (id) on delete cascade,
    body       text not null check (char_length(btrim(body)) between 1 and 4000),
    status     public.message_status not null default 'pending',
    created_at timestamptz not null default now(),
    sent_at    timestamptz,
    read_at    timestamptz
);

create index teacher_messages_teacher_idx on public.teacher_messages (teacher_id, created_at desc);
create index teacher_messages_student_idx on public.teacher_messages (student_id, created_at desc);

-- ---------------------------------------------------------------------
-- Rotation quotidienne de la vitrine « Nos enseignants ».
-- La page du jour est persistée : stable toute la journée, y compris
-- après redémarrage ; les enseignants retirés sont purgés à la lecture.
-- ---------------------------------------------------------------------
create table public.featured_teacher_days (
    day        date primary key,
    created_at timestamptz not null default now()
);

create table public.featured_teacher_entries (
    day        date not null references public.featured_teacher_days (day) on delete cascade,
    teacher_id uuid not null references public.teachers (id) on delete cascade,
    position   smallint not null check (position between 1 and 20),
    primary key (day, teacher_id),
    unique (day, position)
);

create index featured_entries_teacher_idx on public.featured_teacher_entries (teacher_id);
