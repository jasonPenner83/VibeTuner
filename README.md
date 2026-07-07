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
  channel customization, synced across devices via Supabase.
- **Stremio stream resolution** — two-pass selection that weights resolution/codec and skips
  toxic/broken links, with emulator-aware decoder preferences.
- **Graceful offline behavior** — if a catalog source is unreachable the guide shows a clear
  banner and empty-states instead of silently failing; genre lanes fall back to a local
  content matrix so the guide is never blank.

## Tech Stack

- Kotlin, Jetpack Compose + **androidx.tv** Material3 (TV), Compose Material3 (phone)
- OkHttp + `org.json` (data layer), kotlinx.coroutines, kotlinx.serialization
- Media3 / ExoPlayer (playback)
- Supabase (auth + profile/channel sync)
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
    ├── epg/                         # TvGuideScreen + EPG components
    ├── settings/                    # Channel Manager, Add-Ons, Remote Setup
    └── screens/player/              # PlayerScreen + ExoPlayer (rememberPlayer) + chrome

app-phone/src/main/java/com/jpenner/vibetuner/phone/  # Android phone app (shares :core)
```

Design specs, implementation plans, and progress notes live in `docs/superpowers/`.

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
- **Supabase sync** — set `supabase.url` and `supabase.anonKey` in `local.properties` to enable
  cross-device profile/channel sync; without them, sync is silently skipped and the app works
  fully offline/local.
- **Google Sign-In** — set `google.webClientId` in `local.properties` to enable Google
  authentication for sync.

## Roadmap

- Expanded channel-creation tooling
- Phone app UI parity with the TV app
- Player hardware-decoding polish
