-- =====================================================================
-- LZ Academy · 0002 · Personnes & structure académique
-- Années scolaires, niveaux, matières, enseignants, élèves, salles,
-- groupes, créneaux hebdomadaires et inscriptions.
-- =====================================================================

create table public.academic_years (
    id         uuid primary key default gen_random_uuid(),
    label      text not null unique,
    starts_on  date not null,
    ends_on    date not null,
    is_current boolean not null default false,
    created_at timestamptz not null default now(),
    check (ends_on > starts_on)
);

create unique index academic_years_one_current
    on public.academic_years ((is_current)) where is_current;

create table public.levels (
    id          uuid primary key default gen_random_uuid(),
    code        text not null unique,
    name        text not null,
    cycle       text not null default 'Secondaire'
                check (cycle in ('Primaire', 'Moyen', 'Secondaire')),
    order_index smallint not null default 0
);

create table public.subjects (
    id     uuid primary key default gen_random_uuid(),
    code   text not null unique,
    name   text not null unique,
    accent text not null default 'green'
           check (accent in ('green', 'blue', 'violet', 'orange'))
);

-- ---------------------------------------------------------------------
-- Enseignants — champs de la vitrine élève + préférences indépendantes
-- ---------------------------------------------------------------------
create table public.teachers (
    id                        uuid primary key default gen_random_uuid(),
    profile_id                uuid unique references public.profiles (id) on delete set null,
    full_name                 text not null,
    photo_url                 text,
    professional_description  text not null default '',
    years_of_experience       smallint not null default 0 check (years_of_experience >= 0),
    is_featured_on_student_home boolean not null default false,
    accepts_student_messages  boolean not null default false,
    featured_rotation_priority smallint not null default 0,
    last_featured_at          date,
    is_active                 boolean not null default true,
    created_at                timestamptz not null default now(),
    updated_at                timestamptz not null default now()
);

create index teachers_showcase_idx
    on public.teachers (is_featured_on_student_home) where is_featured_on_student_home;

create index teachers_rotation_idx
    on public.teachers (last_featured_at, featured_rotation_priority desc, full_name)
    where is_featured_on_student_home and is_active;

create index teachers_name_trgm_idx
    on public.teachers using gin (full_name extensions.gin_trgm_ops);

create trigger teachers_set_updated_at
    before update on public.teachers
    for each row execute function public.set_updated_at();

create table public.teacher_subjects (
    teacher_id uuid not null references public.teachers (id) on delete cascade,
    subject_id uuid not null references public.subjects (id) on delete cascade,
    is_primary boolean not null default false,
    primary key (teacher_id, subject_id)
);

create index teacher_subjects_subject_idx on public.teacher_subjects (subject_id);

create table public.teacher_levels (
    teacher_id uuid not null references public.teachers (id) on delete cascade,
    level_id   uuid not null references public.levels (id) on delete cascade,
    primary key (teacher_id, level_id)
);

create index teacher_levels_level_idx on public.teacher_levels (level_id);

-- ---------------------------------------------------------------------
-- Élèves (peuvent exister sans compte Auth, créés par l'administration)
-- ---------------------------------------------------------------------
create table public.students (
    id             uuid primary key default gen_random_uuid(),
    profile_id     uuid unique references public.profiles (id) on delete set null,
    matricule      text not null unique,
    full_name      text not null,
    level_id       uuid references public.levels (id) on delete set null,
    phone          text,
    guardian_name  text,
    guardian_phone text,
    enrolled_on    date not null default current_date,
    is_active      boolean not null default true,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

create index students_name_trgm_idx on public.students using gin (full_name extensions.gin_trgm_ops);
create index students_level_idx on public.students (level_id);
create index students_active_idx on public.students (is_active) where is_active;

create trigger students_set_updated_at
    before update on public.students
    for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------
-- Salles
-- ---------------------------------------------------------------------
create table public.rooms (
    id            uuid primary key default gen_random_uuid(),
    name          text not null unique,
    building      text not null default 'Bâtiment principal',
    capacity      smallint not null default 20 check (capacity > 0),
    is_active     boolean not null default true
);

-- ---------------------------------------------------------------------
-- Groupes & créneaux hebdomadaires
-- ---------------------------------------------------------------------
create table public.groups (
    id               uuid primary key default gen_random_uuid(),
    name             text not null,
    level_id         uuid references public.levels (id) on delete set null,
    teacher_id       uuid references public.teachers (id) on delete set null,
    default_room_id  uuid references public.rooms (id) on delete set null,
    academic_year_id uuid references public.academic_years (id) on delete set null,
    capacity         smallint not null default 20 check (capacity > 0),
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    unique (name, academic_year_id)
);

create index groups_teacher_idx on public.groups (teacher_id);
create index groups_level_idx on public.groups (level_id);

create trigger groups_set_updated_at
    before update on public.groups
    for each row execute function public.set_updated_at();

create table public.group_schedule_slots (
    id        uuid primary key default gen_random_uuid(),
    group_id  uuid not null references public.groups (id) on delete cascade,
    weekday   smallint not null check (weekday between 1 and 7),
    starts_at time not null,
    ends_at   time not null,
    room_id   uuid references public.rooms (id) on delete set null,
    check (ends_at > starts_at)
);

create index group_slots_group_idx on public.group_schedule_slots (group_id);

create table public.enrollments (
    id         uuid primary key default gen_random_uuid(),
    student_id uuid not null references public.students (id) on delete cascade,
    group_id   uuid not null references public.groups (id) on delete cascade,
    joined_on  date not null default current_date,
    left_on    date,
    created_at timestamptz not null default now(),
    unique (student_id, group_id)
);

create index enrollments_group_idx on public.enrollments (group_id) where left_on is null;
create index enrollments_student_idx on public.enrollments (student_id);

-- Rattachement de l'année courante aux paramètres de l'académie.
alter table public.academy_settings
    add constraint academy_settings_academic_year_fk
    foreign key (academic_year_id) references public.academic_years (id) on delete set null;
