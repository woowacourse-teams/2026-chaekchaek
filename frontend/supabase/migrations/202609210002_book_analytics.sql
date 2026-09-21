create table if not exists public.experiment_book_events (
  id text primary key,
  book_id text not null,
  user_id text not null,
  event_type text not null check (event_type in ('view', 'dwell')),
  duration_ms integer not null default 0 check (duration_ms between 0 and 300000),
  created_at timestamptz not null default now()
);

create index if not exists experiment_book_events_book_id_idx
  on public.experiment_book_events (book_id);

create index if not exists experiment_book_events_created_at_idx
  on public.experiment_book_events (created_at);

alter table public.experiment_book_events enable row level security;
revoke all on public.experiment_book_events from anon, authenticated;
grant select, insert, update, delete on public.experiment_book_events to service_role;
