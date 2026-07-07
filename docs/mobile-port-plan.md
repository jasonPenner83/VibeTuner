# VibeTuner — Android Phone Port Plan

_Goal: ship an Android **phone** app alongside the existing Android **TV** app, with a single shared data + domain + ViewModel layer and two form-factor–specific UI layers._

_Last updated: 2026-07-06_

---

## 1. Where the code stands today

The repo is already in good shape for this. The `data/` layer has **zero** dependency on `androidx.tv` or leanback — repositories are plain `Context`-based classes, and every ViewModel depends only on repositories and Kotlin `Flow`. The TV coupling is confined to the Compose UI: **37 of the ~40 UI files import `androidx.tv`** (`tv.material3`, `tv.foundation`), plus D-pad focus code (`focusRestorer`, `FocusRequester`, `focusProperties`).

| Concern | Location | Portable as-is? |
|---|---|---|
| Models | `data/model/**` | ✅ Yes |
| Repositories (Channel, Profile, Addon, Settings…) | `data/repository/**`, `data/settings/**` | ✅ Yes |
| Trakt / Stremio / API | `data/api/**`, `data/auth/**` | ✅ Yes |
| Supabase sync engine | `data/sync/**` | ✅ Yes |
| Room / DataStore storage | `data/storage/**`, `data/cache/**` | ✅ Yes |
| ViewModels + UiState | `ui/screens/*/…ViewModel.kt`, `…UiState.kt` | ✅ Yes (UI-framework-agnostic `androidx.lifecycle.ViewModel`) |
| Player engine (media3 ExoPlayer) | `ui/screens/player/rememberPlayer.kt` | ⚠️ Mostly — split engine from TV overlays |
| Design tokens (Color, Dimens, Fonts, Glass) | `ui/theme/**` | ⚠️ Mostly — `Type.kt` uses `tv.material3.Typography` |
| Screens (`@Composable`) | `ui/screens/**`, `ui/components/**` | ❌ No — reimplement for touch |
| Navigation | hand-rolled `when(currentScreen)` enum in `MainActivity` | ❌ No — TV/enum, rebuild for phone |

**Bottom line:** the "logic" is already effectively shared; it just lives in the `:app` module. The work is (a) lifting it into a shared module and (b) writing a new touch UI on top of it.

---

## 2. Target architecture (module split)

```
:core        ← shared. No androidx.tv, no androidx.compose.material3.tv.
             data/**, domain models, all ViewModels + UiState,
             player engine, design tokens (colors/dimens/fonts).
             Depends on: lifecycle-viewmodel, coroutines, room,
             datastore, media3, supabase, retrofit/okhttp, moshi.

:tv          ← app module (existing UI). applicationId .tv (or shared).
             androidx.tv.material3 + D-pad UI. Depends on :core.
             minSdk 26. Leanback launcher.

:mobile      ← new app module. Touch UI with androidx.compose.material3.
             Depends on :core. minSdk 26. Portrait. Standard launcher.
```

Dependency direction is strictly one-way: `:tv` → `:core` and `:mobile` → `:core`, never the reverse (per `CLAUDE.md` §Architecture). `:core` must not import any UI-framework or form-factor code beyond Compose runtime + the design-token primitives.

> **Note on ViewModels:** they currently sit under `ui/screens/**` but only touch repositories and `Flow`. They move into `:core` (e.g. `core/presentation/**`). No logic change — only package + module. This is the single highest-leverage move: both UIs then bind to identical state.

> **Note on wiring:** there's no DI framework (no Hilt/Koin). Dependencies are instantiated manually in `MainActivity` via `by lazy` + `viewModelFactory`. Keep this pattern but move the construction into a small `core` service-locator/`AppContainer` so both apps build the same graph the same way. (Optional: adopt Hilt later — out of scope for the port.)

---

## 3. What has to be rebuilt for phone

The UI is the real work. For each TV screen there's a phone equivalent that binds to the **same ViewModel/UiState** but renders for touch:

- **Material library swap** — `androidx.tv.material3` → `androidx.compose.material3`. Different components (Card, Button, Surface, Text, Tab). Not a find-replace; APIs differ.
- **Remove D-pad/focus machinery** — delete `focusRestorer`, `FocusRequester` bridges, `focusProperties{ up = … }`, `onKeyEvent` handlers. Replace with `clickable`, scroll, and gesture handling.
- **Navigation** — replace the `when(currentScreen)` enum state machine with a phone-appropriate pattern: Navigation-Compose `NavHost` + a bottom navigation bar (Home / Guide / Settings). The enum transition *logic* is a useful reference for the graph.
- **Layout reflow** — rails become touch-scrollable `LazyRow` carousels; the parallax hero adapts to a narrow portrait viewport; settings become a scrolling list, not focus columns. Import the **"Home Screen – Mobile"** design as the reference for spacing, hero treatment, and rail density.
- **Player** — split `rememberPlayer` into a shared media3 engine (in `:core`) and a phone controls overlay: tap-to-toggle controls, a draggable scrub bar, pinch/rotate for fullscreen — replacing the D-pad `PlayerOverlays`.
- **Manifest** — new `:mobile` manifest: drop `leanback`/`LEANBACK_LAUNCHER`, set `touchscreen required="true"`, portrait (or unlocked) orientation instead of the TV's `screenOrientation="landscape"`, standard `LAUNCHER` category, phone icon (no `tv_banner`).
- **Theme** — share `Color.kt`, `Dimens.kt` (add phone density values), `Fonts.kt`, `Glass.kt`; give `:mobile` its own `Type.kt` built on `material3.Typography` instead of `tv.material3.Typography`.

Screens to port: Home, Guide, Program Info/Detail, Player, Profile Picker, Settings (+ Addons, Channel Manager, Profile Manager), Startup/Auth.

---

## 4. Phased execution

1. **Create `:core`, move the non-UI code.** New module; move `data/**`, models, and the design-token files. Update `settings.gradle.kts` and dependencies. `:tv` (renamed from `:app`) depends on `:core`. **Build green before touching UI** — this is a pure move + rewire.
2. **Move ViewModels + UiState into `:core`.** Repackage under `core/presentation/**`; fix imports in `:tv`. Build green. Now both apps can share state.
3. **Extract the player engine** into `:core`; leave TV overlays in `:tv`.
4. **Scaffold `:mobile`.** New app module, manifest, `MainActivity`, theme (`Type.kt`), Navigation-Compose + bottom nav. Wire the `AppContainer` graph.
5. **Port screens Home → Guide → Detail → Player → Settings**, one at a time, each binding to the existing ViewModel. Use the imported mobile design as the visual spec. Small, reviewable PRs per screen (per `CLAUDE.md` §9 — no large multi-file refactors in one go).
6. **Phone player controls** (touch overlay).
7. **Verification** — run the shared unit tests against `:core` unchanged; add phone UI/instrumentation smoke tests; manual pass on a phone/emulator in portrait; confirm `:tv` still builds and runs unregressed.

---

## 5. Risks & watch-items

- **ViewModel move is the risk-bearing step** — it's a large cross-file repackage. Do it in isolation, verify the TV app is behaviorally identical before proceeding (per `CLAUDE.md` §9 on inconsistent refactoring).
- **`compose.adaptive` deps are already declared but unused** — you can lean on `WindowSizeClass` inside `:mobile` for tablet/foldable layouts later, but don't use it to fork TV vs phone (that's what the module split is for).
- **Version currency** — `compileSdk 37` and the media3/compose BOM versions should be re-verified current before adding the new module (per `CLAUDE.md` §Dependencies); don't let the new module drift to different versions than `:tv`.
- **applicationId** — decide whether phone + TV ship as one listing (same `applicationId`, `<uses-feature leanback required=false>` already present) or two separate Play listings. Affects sync/account identity.

---

## 6. Blocker: importing the design reference

The mobile design lives behind the **Claude Design** connector (`https://api.anthropic.com/v1/design/mcp`, auth via `/design-login`). That connector is **not connected to this session**, and the shared link (`…?file=Home+Screen+-+Mobile.dc.html`) redirects to login — so I could not import "Home Screen – Mobile" here.

**To unblock:** connect the Claude Design connector (or paste the exported `Home Screen – Mobile.dc.html`), and I can pull the exact layout, spacing, colors, and hero treatment into the `:mobile` theme and the Home screen port in step 5.
