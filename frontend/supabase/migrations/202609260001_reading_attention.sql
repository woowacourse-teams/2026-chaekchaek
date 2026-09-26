create table if not exists public.stranger_attention_events (
  id text primary key,
  user_id text not null references public.stranger_participants(id) on delete cascade,
  target text not null check (char_length(target) between 1 and 150),
  event_type text not null check (event_type in ('view', 'dwell')),
  duration_ms integer not null default 0 check (duration_ms between 0 and 300000),
  created_at timestamptz not null default now(),
  constraint stranger_attention_event_duration check (
    (event_type = 'view' and duration_ms = 0) or
    (event_type = 'dwell' and duration_ms > 0)
  )
);

create index if not exists stranger_attention_events_user_id_idx
  on public.stranger_attention_events (user_id);

create index if not exists stranger_attention_events_created_at_idx
  on public.stranger_attention_events (created_at, id);

alter table public.stranger_attention_events enable row level security;
revoke all on public.stranger_attention_events from anon, authenticated;
grant select, insert, update, delete on public.stranger_attention_events to service_role;
