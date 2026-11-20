-- =====================================================================
-- LZ Academy · 0001 · Socle
-- Extensions, types énumérés, profils utilisateurs (Supabase Auth),
-- paramètres de l'académie et journal d'audit.
-- =====================================================================

create schema if not exists extensions;
create extension if not exists pg_trgm with schema extensions;

-- ---------------------------------------------------------------------
-- Types énumérés
-- ---------------------------------------------------------------------
create type public.user_role          as enum ('student', 'teacher', 'admin');
create type public.attendance_status  as enum ('present', 'absent', 'late', 'justified');
create type public.payment_status     as enum ('paid', 'pending', 'overdue', 'refunded');
create type public.payment_method     as enum ('cash', 'ccp', 'baridimob', 'bank', 'card');
create type public.subscription_status as enum ('active', 'exhausted', 'expired', 'cancelled');
create type public.file_kind          as enum ('pdf', 'doc', 'slides', 'image', 'video', 'archive', 'other');
create type public.audience           as enum ('all', 'students', 'teachers', 'level', 'group');
create type public.broadcast_channel  as enum ('push', 'sms', 'email', 'push_sms');
create type public.message_status     as enum ('pending', 'sent', 'read', 'failed');
create type public.session_status     as enum ('scheduled', 'completed', 'cancelled');

-- ---------------------------------------------------------------------
-- Fonction générique updated_at
-- ---------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

-- ---------------------------------------------------------------------
-- Profils : extension de auth.users avec un rôle applicatif.
-- Le rôle est TOUJOURS 'student' à l'inscription : jamais de rôle
-- fourni par le client dans les metadata (élévation de privilèges).
-- ---------------------------------------------------------------------
create table public.profiles (
    id         uuid primary key references auth.users (id) on delete cascade,
    role       public.user_role not null default 'student',
    full_name  text not null default '',
    phone      text,
    avatar_url text,
    locale     text not null default 'fr',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create trigger profiles_set_updated_at
    before update on public.profiles
    for each row execute function public.set_updated_at();

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, full_name, phone)
    values (
        new.id,
        coalesce(new.raw_user_meta_data ->> 'full_name', ''),
        new.phone
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- Empêche un utilisateur de changer lui-même son rôle.
create or replace function public.guard_profile_role()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if new.role is distinct from old.role
       and auth.uid() is not null
       and not public.is_admin() then
        raise exception 'Le rôle ne peut pas être modifié par l''utilisateur.';
    end if;
    return new;
end;
$$;

create trigger profiles_guard_role
    before update on public.profiles
    for each row execute function public.guard_profile_role();

-- ---------------------------------------------------------------------
-- Paramètres de l'académie (ligne unique)
-- ---------------------------------------------------------------------
create table public.academy_settings (
    id               smallint primary key default 1 check (id = 1),
    name             text not null default 'LZ Academy',
    address          text,
    phone            text,
    email            text,
    timezone         text not null default 'Africa/Algiers',
    academic_year_id uuid,     -- FK ajoutée dans la migration 0002
    updated_at       timestamptz not null default now()
);

create trigger academy_settings_set_updated_at
    before update on public.academy_settings
    for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------
-- Journal d'audit (actions administratives)
-- ---------------------------------------------------------------------
create table public.audit_log (
    id         bigint generated always as identity primary key,
    actor_id   uuid references public.profiles (id) on delete set null,
    actor_role public.user_role,
    action     text not null,
    entity     text not null,
    entity_id  text,
    details    jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index audit_log_created_idx on public.audit_log (created_at desc);
create index audit_log_entity_idx  on public.audit_log (entity, entity_id);
