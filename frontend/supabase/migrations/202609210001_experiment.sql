create table if not exists public.experiment_books (
  id text primary key,
  title text not null check (char_length(title) between 1 and 120),
  author text not null check (char_length(author) between 1 and 80),
  genre text not null check (genre in ('철학', '고전소설', '현대소설', '에세이', '서브컬쳐', '시집')),
  cover_url text,
  product_url text,
  created_at timestamptz not null default now()
);

create table if not exists public.experiment_reflections (
  id text primary key,
  book_id text not null,
  user_id text not null,
  nickname text not null,
  body text not null check (char_length(body) between 1 and 3000),
  created_at timestamptz not null default now()
);

create table if not exists public.experiment_replies (
  id text primary key,
  reflection_id text not null,
  user_id text not null,
  nickname text not null,
  body text not null check (char_length(body) between 1 and 1000),
  created_at timestamptz not null default now()
);

create table if not exists public.experiment_likes (
  reflection_id text not null,
  user_id text not null,
  created_at timestamptz not null default now(),
  primary key (reflection_id, user_id)
);

alter table public.experiment_books enable row level security;
alter table public.experiment_reflections enable row level security;
alter table public.experiment_replies enable row level security;
alter table public.experiment_likes enable row level security;

revoke all on public.experiment_books from anon, authenticated;
revoke all on public.experiment_reflections from anon, authenticated;
revoke all on public.experiment_replies from anon, authenticated;
revoke all on public.experiment_likes from anon, authenticated;

grant usage on schema public to service_role;
grant select, insert, update, delete on public.experiment_books to service_role;
grant select, insert, update, delete on public.experiment_reflections to service_role;
grant select, insert, update, delete on public.experiment_replies to service_role;
grant select, insert, update, delete on public.experiment_likes to service_role;
