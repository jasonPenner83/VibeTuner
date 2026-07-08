# 🛸 VibeTuner

VibeTuner is a linear, IPTV/EPG-style Android app built with Jetpack Compose. It turns
**Stremio addon catalogs** into continuous, rolling 24-hour "channels", then resolves
playable streams through those same Stremio addons.

> **Status:** MVP — running and verified on a physical Google TV device. A phone build
> shares the same `:core` data/domain layer and is in active development.

## Features

- **EPG grid** with remote-friendly navigation: an "ALL" master view, wrap-around scrolling,
  and dynamic high-contrast category labels.
- **Channels from Stremio catalogs** — genre/category channels built from configured addon
  manifests, toggleable and customizable per profile via the in-app Channel Manager.
- **Real episodic schedules** — TV channels pull real episode metadata (titles, season/episode
  numbers, air dates) and fill a 24-hour timeline; sorting disciplines include Popular,
  Trending, Random (daily-seed shuffle) and Chronological (marathon).
- **Daily freshness with caching** — a per-day disk cache keeps lineups stable for 24h and
  avoids hammering catalog sources.
- **Multi-profile support** — separate profiles with their own addon configuration and
  channel customization; optional cross-device sync via a self-hosted Supabase project.
- **Stremio stream resolution** — two-pass selection that weights resolution/codec and skips
  toxic/broken links, with emulator-aware decoder preferences.
- **Graceful offline behavior** — if a catalog source is unreachable the guide shows a clear
  banner and empty-states instead of silently failing; genre lanes fall back to a local
  content matrix so the guide is never blank.

## Tech Stack

- Kotlin, Jetpack Compose + **androidx.tv** Material3 (TV), Compose Material3 (phone)
- OkHttp + `org.json` (data layer), kotlinx.coroutines, kotlinx.serialization
- Media3 / ExoPlayer (playback)
- Supabase (optional, self-hosted: auth + profile/channel sync)
- Local JSON file persistence (`vibetuner_channels.json`) + DataStore for settings
- Gradle wrapper **9.4.1**, JDK **21**, `minSdk 26`, `compileSdk 37`

## Project Layout

```
core/src/main/java/com/jpenner/vibetuner/     # shared data/domain layer (no androidx.tv)
├── data/
│   ├── api/                         # StremioCatalogDataSource, StremioMetaDataSource,
│   │                                 # StremioResolver, manifest/catalog models
│   ├── cache/HarvestCache.kt        # daily disk cache for harvested pools
│   ├── model/                       # Channel, Program, CategoryConfig, Profile, ...
│   ├── repository/                  # ChannelRepository, ProfileRepository, AddonRepository, ...
│   ├── settings/                    # DataStoreSettingsRepository
│   └── sync/                        # Supabase-backed cross-device profile/channel sync
└── ui/screens/                      # shared ViewModels + UiState (framework-agnostic)

app/src/main/java/com/jpenner/vibetuner/       # Android TV app
├── MainActivity.kt                  # entry point, app-screen routing, startup load/sync
└── ui/
    ├── screens/guide/               # GuideScreen + EPG components
    ├── settings/                    # Channel Manager, Add-Ons, Remote Setup
    └── screens/player/              # PlayerScreen + ExoPlayer (rememberPlayer) + chrome

app-phone/src/main/java/com/jpenner/vibetuner/phone/  # Android phone app (shares :core)
```

## Building

Requires the Android SDK and JDK 21.

```bash
# Unit tests
./gradlew testDebugUnitTest

# Build the debug APK (TV app)
./gradlew :app:assembleDebug

# Install to a connected device
./gradlew :app:installDebug
# (For a wireless-adb device whose serial Gradle can't match, install directly:
#  adb -t 1 install -r app/build/outputs/apk/debug/app-debug.apk)
```

`local.properties` (your SDK path + Supabase/Google keys) is git-ignored — Android Studio
generates the SDK path on first open; add the other keys yourself (see below).

## Configuration

- **Stremio add-ons** — configure manifest URLs in the in-app **Add-Ons** screen; the resolver
  queries them in order for both catalog listing and stream resolution.
- **Cross-device sync (optional)** — the app ships with sync disabled; it works fully
  offline/local. To enable it you bring your own Supabase project and Google OAuth
  credentials — see below.

## Self-hosting sync (optional)

Sync keeps profiles, add-on configuration, and channel customizations identical across
devices signed into the same Google account. All sync logic runs on-device (`core/data/sync/`);
the backend is just a Supabase Postgres table behind row-level security, so "hosting" it
means creating a free Supabase project and pointing your build at it.

### 1. Create the Supabase project

1. Create a project at [supabase.com](https://supabase.com) (the free tier is fine — note
   free projects pause after a week of inactivity).
2. Apply the schema in `supabase/migrations/20260704_sync.sql`: paste it into the dashboard's
   **SQL Editor** and run it, or use the Supabase CLI (`supabase link`, then `supabase db push`).
   This creates the `sync_docs` and `harvest_pools` tables, the server-side `updated_at`
   trigger, and RLS policies so each signed-in user can only touch their own rows.

### 2. Set up Google OAuth

The apps sign in with the classic Google Sign-In ID-token flow, which needs **two** OAuth
clients in one Google Cloud project ([console.cloud.google.com](https://console.cloud.google.com)
→ **APIs & Services → Credentials**):

1. Configure the **OAuth consent screen** (External, add yourself as a test user while
   the app is unpublished).
2. Create a **Web application** OAuth client ID. No redirect URIs are needed — the ID token
   is exchanged directly with Supabase. Its client ID is your `google.webClientId`.
3. Create an **Android** OAuth client ID for each app you build, using the applicationId
   (`com.jpenner.vibetuner` for TV, `com.jpenner.vibetuner.phone` for phone) and the SHA-1
   of your signing certificate (`./gradlew signingReport`, or `keytool -list -v -keystore
   ~/.android/debug.keystore` for debug builds). Without a matching Android client,
   Google Sign-In fails on device with a `DEVELOPER_ERROR` status.

### 3. Connect Google to Supabase

In the Supabase dashboard, **Authentication → Sign In / Providers → Google**: enable the
provider and add the **web** client ID from step 2 to the client IDs field. No client
secret is required for the native ID-token flow.

### 4. Point your build at it

Add to `local.properties` (git-ignored):

```properties
supabase.url=https://<your-project-ref>.supabase.co
supabase.anonKey=<your anon/publishable key>
google.webClientId=<web client id>.apps.googleusercontent.com
```

Rebuild, then sign in with Google from the in-app Settings screen on each device. If any
key is missing the sync code no-ops silently and the app stays local-only.

Notes for operators: the anon key is safe to embed in a client (RLS enforces per-user
isolation), and `harvest_pools` rows are day-scoped but never cleaned up automatically —
if you run this long-term, add a scheduled delete (e.g. `pg_cron`) for rows older than a
couple of days.

## Roadmap

- Expanded channel-creation tooling
- Phone app UI parity with the TV app
- Player hardware-decoding polish
