-- Profile sync documents: opaque JSONB, last-push-wins via server-set updated_at.
create table public.sync_docs (
  user_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
  profile_id text not null,
  kind       text not null check (kind in ('profile', 'addons', 'overrides')),
  payload    jsonb not null,
  deleted    boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key (user_id, profile_id, kind)
);

-- updated_at is server-authoritative: stamped on every insert/update,
-- regardless of what the client sends.
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end $$;

create trigger sync_docs_touch
  before insert or update on public.sync_docs
  for each row execute function public.set_updated_at();

alter table public.sync_docs enable row level security;

create policy "own docs select" on public.sync_docs
  for select using (user_id = auth.uid());
create policy "own docs insert" on public.sync_docs
  for insert with check (user_id = auth.uid());
create policy "own docs update" on public.sync_docs
  for update using (user_id = auth.uid()) with check (user_id = auth.uid());
-- No delete policy: tombstones only.

-- Day-scoped harvest pools: immutable per day, first-writer-wins.
create table public.harvest_pools (
  user_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
  profile_id text not null,
  day        bigint not null,          -- epoch day in America/Chicago
  source_key text not null,            -- HarvestCache key: "<channelId>:<SORTING_RULE>"
  payload    jsonb not null,           -- JSON array of RawMediaItem
  created_at timestamptz not null default now(),
  primary key (user_id, profile_id, day, source_key)
);

alter table public.harvest_pools enable row level security;

create policy "own pools all" on public.harvest_pools
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());
