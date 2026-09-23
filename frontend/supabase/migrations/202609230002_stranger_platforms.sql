alter table public.stranger_participants
  add column if not exists source text not null default 'unknown',
  add column if not exists device text not null default 'other';

alter table public.stranger_participants
  add constraint stranger_participants_source_length check (char_length(source) between 1 and 64),
  add constraint stranger_participants_device_value check (device in ('iPhone', 'iPad', 'Android', 'PC', 'other'));
