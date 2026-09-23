create table if not exists public.stranger_participants (
  id text primary key,
  nickname text not null,
  reading_status text check (reading_status in ('read', 'unread')),
  composer_started boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.stranger_page_views (
  user_id text not null references public.stranger_participants(id),
  page_number integer not null check (page_number between 1 and 6),
  created_at timestamptz not null default now(),
  primary key (user_id, page_number)
);

create table if not exists public.stranger_reflections (
  id text primary key,
  user_id text not null references public.stranger_participants(id),
  nickname text not null,
  body text not null check (char_length(body) between 1 and 3000),
  created_at timestamptz not null default now()
);

create table if not exists public.stranger_replies (
  id text primary key,
  reflection_id text not null references public.stranger_reflections(id),
  user_id text not null references public.stranger_participants(id),
  nickname text not null,
  body text not null check (char_length(body) between 1 and 1000),
  created_at timestamptz not null default now()
);

create table if not exists public.stranger_likes (
  reflection_id text not null references public.stranger_reflections(id),
  user_id text not null references public.stranger_participants(id),
  primary key (reflection_id, user_id)
);

alter table public.stranger_participants enable row level security;
alter table public.stranger_page_views enable row level security;
alter table public.stranger_reflections enable row level security;
alter table public.stranger_replies enable row level security;
alter table public.stranger_likes enable row level security;

revoke all on public.stranger_participants, public.stranger_page_views, public.stranger_reflections, public.stranger_replies, public.stranger_likes from anon, authenticated;
grant usage on schema public to service_role;
grant select, insert, update, delete on public.stranger_participants, public.stranger_page_views, public.stranger_reflections, public.stranger_replies, public.stranger_likes to service_role;
