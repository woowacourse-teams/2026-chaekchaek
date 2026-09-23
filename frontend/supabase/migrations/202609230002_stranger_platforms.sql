alter table public.stranger_participants
  add column if not exists source text not null default 'unknown',
  add column if not exists device text not null default 'other';

do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conrelid = 'public.stranger_participants'::regclass
      and conname = 'stranger_participants_source_length'
  ) then
    alter table public.stranger_participants
      add constraint stranger_participants_source_length check (char_length(source) between 1 and 64);
  end if;
  if not exists (
    select 1 from pg_constraint
    where conrelid = 'public.stranger_participants'::regclass
      and conname = 'stranger_participants_device_value'
  ) then
    alter table public.stranger_participants
      add constraint stranger_participants_device_value check (device in ('iPhone', 'iPad', 'Android', 'PC', 'other'));
  end if;
end $$;
