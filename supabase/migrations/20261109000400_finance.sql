-- =====================================================================
-- LZ Academy · 0004 · Finances
-- Formules (packs), abonnements et paiements en DZD.
-- =====================================================================

create table public.packs (
    id              uuid primary key default gen_random_uuid(),
    name            text not null unique,
    description     text,
    price_dzd       numeric(12, 2) not null check (price_dzd >= 0),
    sessions_count  integer not null default 0 check (sessions_count >= 0),
    duration_months smallint check (duration_months is null or duration_months > 0),
    is_active       boolean not null default true,
    created_at      timestamptz not null default now()
);

create table public.subscriptions (
    id               uuid primary key default gen_random_uuid(),
    student_id       uuid not null references public.students (id) on delete cascade,
    pack_id          uuid references public.packs (id) on delete set null,
    starts_on        date not null default current_date,
    ends_on          date,
    sessions_total   integer not null check (sessions_total > 0),
    sessions_used    integer not null default 0 check (sessions_used >= 0),
    sessions_remaining integer generated always as (sessions_total - sessions_used) stored,
    price_dzd        numeric(12, 2) not null default 0 check (price_dzd >= 0),
    status           public.subscription_status not null default 'active',
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    check (sessions_used <= sessions_total),
    check (ends_on is null or ends_on >= starts_on)
);

create index subscriptions_student_idx on public.subscriptions (student_id, status);
create index subscriptions_active_idx on public.subscriptions (ends_on) where status = 'active';

create trigger subscriptions_set_updated_at
    before update on public.subscriptions
    for each row execute function public.set_updated_at();

create table public.payments (
    id              uuid primary key default gen_random_uuid(),
    student_id      uuid not null references public.students (id) on delete cascade,
    subscription_id uuid references public.subscriptions (id) on delete set null,
    amount_dzd      numeric(12, 2) not null check (amount_dzd > 0),
    method          public.payment_method not null default 'cash',
    status          public.payment_status not null default 'pending',
    reference       text,
    due_on          date,
    paid_at         timestamptz,
    recorded_by     uuid references public.profiles (id) on delete set null,
    created_at      timestamptz not null default now(),
    check (status <> 'paid' or paid_at is not null)
);

create index payments_student_idx on public.payments (student_id, created_at desc);
create index payments_status_idx on public.payments (status, due_on);
create index payments_paid_idx on public.payments (paid_at desc) where status = 'paid';
