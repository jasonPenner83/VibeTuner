# Handoff: Aerial TV — Android TV UI Redesign

## Overview
A complete visual redesign for a **live / linear-TV Android TV app** ("10-foot" leanback UI). Covers the program guide (EPG), program detail, home browse, playback, channel switching, settings, sign-in, modals, and loading/focus states. Target platform: **Android TV with Kotlin + Jetpack Compose (Compose for TV / `androidx.tv.material3`)**. Design intent: dark, cinematic, premium, with a single vivid accent and a strong, consistent D-pad focus treatment.

## About the Design Files
The file in this bundle (`Android TV.dc.html`) is a **design reference created in HTML** — an interactive prototype showing intended look, layout, and behavior. **It is not production code to copy.** The task is to **recreate these screens in the existing Android app using Jetpack Compose** and the app's established patterns (theme, navigation, data/repository layer, focus management). Where the app already has components (cards, dialogs, lists), reuse them and restyle to match; only build new composables where none exist.

The HTML uses a left sidebar + theme swatches **purely as a prototype viewer/navigator** — that chrome is NOT part of the product. Only the content inside the 1920×1080 "TV frame" is the design. Each product screen is marked in the HTML with a `data-screen-label` attribute.

## Fidelity
**High-fidelity (hifi).** Final colors, typography, spacing, radii, and focus behavior are specified below as exact values. Recreate pixel-accurately at a **1920×1080 design canvas** (standard Android TV `1080p`; values below are in design px ≈ dp at this canvas — scale proportionally for other densities). Imagery is shown as striped placeholders; wire these to the app's real artwork/poster/backdrop sources.

## Global Layout Rules
- **Design canvas:** 1920 × 1080. All px values below are at this canvas.
- **Overscan / safe area:** 56 px horizontal padding on full screens. Keep all interactive content inside the safe area.
- **Top app bar (Home/Guide):** 84 px tall. Left: wordmark "Aerial" + logo; primary nav tabs (Home · Guide · On Demand · Recordings) with a 3 px accent underline on the active tab. Right: search circle (42 px), clock (tabular numerals), profile avatar (42 px rounded square).
- **Type:** Two families only.
  - **Sora** (Google Font, weights 300–800) — all UI text and headings.
  - **JetBrains Mono** (weights 400–600) — timecodes, channel numbers, metadata labels, token/spec text.
- **Everything is dark.** No light mode in scope.

## Theme System (3 directions)
Implement as a selectable theme; **"Midnight Azure" is the default**. All three share the same structure — only the token values change. Use these as your Compose color scheme / `MaterialTheme` tokens.

| Token (semantic) | Midnight Azure (default) | Onyx Ember | Slate Aurora |
|---|---|---|---|
| `bg` (app background) | `#0D0F14` | `#100E0C` | `#0A1013` |
| `surface` (cards, bars) | `#151821` | `#1A1714` | `#11191E` |
| `raised` (focused/elevated) | `#1D212C` | `#241F1A` | `#19242A` |
| `line` (hairlines, borders) | `#2A2F3C` | `#332B24` | `#26343D` |
| `txt` (primary text) | `#EEF1F6` | `#F3EFE9` | `#EAF2F3` |
| `txt2` (secondary text) | `#A7AFBF` | `#B6ADA3` | `#9FB0B3` |
| `txt3` (muted/labels) | `#6B7384` | `#7A7066` | `#647479` |
| `accent` | `#3D9BFF` | `#FF9D3C` | `#34E0D0` |
| `accent2` (gradient pair) | `#8FC7FF` | `#FFC27A` | `#8AF3EA` |
| `accent-ink` (text on accent) | `#06121F` | `#1A0F02` | `#042321` |
| `glow` (focus ring) | `rgba(61,155,255,.50)` | `rgba(255,157,60,.42)` | `rgba(52,224,208,.40)` |
| `danger` / live | `#FF5D5D` | `#FF5D5D` | `#FF6B6B` |
| `success` | `#46D18B` | `#46D18B` | `#46D18B` |
| `warn` | `#FFB454` | `#FFB454` | `#FFB454` |

Category accent dots (used on guide rows / badges): News `#5B8DEF`, Sport `#46D18B`, Movie `#B07CFF`, Drama `#FF8A5C`, Documentary `#34D0C0`, Kids `#FFB454`, Music `#FF7AC0`, Entertainment = `accent`.

## D-pad Focus Spec (the single most important interaction)
Every focusable element (guide cell, card, button, list row, profile, channel tile) animates to a focused state on D-pad navigation:
- **scale:** 1.00 → **1.07** (cards/tiles) or **1.05** (guide cells), `transformOrigin: center`
- **border:** 2 dp solid `accent`
- **glow ring:** 5 dp ring at 50% `accent` (`glow` token) — i.e. a `0 0 0 5px glow` outer ring
- **elevation:** ~16 dp drop shadow (`0 14–16px 34–36px -8px rgba(0,0,0,.7)`)
- **background:** swaps from `surface`/transparent to `raised`
- **z-order:** focused element rises above neighbors
- **motion:** **180 ms**, easing **FastOutSlowIn**
- Only **one** element is focused at a time. Default focus on screen entry: the now-playing/primary item.
- In Compose: use `Modifier.onFocusChanged` + `animateFloatAsState`/`graphicsLayer` for scale, or the TV Material `Border`/`Glow`/`scale` indication APIs (`androidx.tv.material3` `ClickableSurfaceDefaults`).

---

## Screens / Views

### 1. EPG / Program Guide  *(data-screen-label="EPG")*  — primary screen
**Purpose:** Browse what's on across channels and time; focus a program to preview it, OK to watch/record.
**Layout (top → bottom):**
- **Top app bar** (84 px) — "Guide" tab active.
- **Focused-program preview** (284 px tall, 56 px side padding): left = 454 px wide 16:9 backdrop (radius 14, `surface` w/ diagonal-stripe placeholder; LIVE badge top-left; "CH n · Name" bottom-left). Right = metadata column: time range (mono, accent) · duration · rating chip · category; **title 48 px / 700**; description 20 px `txt2` (max 880 px); action row — **Watch Live** (filled `accent`, `accent-ink` text, radius 11, 4px `glow` ring), **Record** + **More Info** (outline buttons, `surface`/`line`).
- **Guide grid:**
  - **Time header** (50 px): left 320 px = "All Channels ▾" filter; right = time labels every **180 px** = **30 min** (i.e. `pxPerMin = 6`). Bottom hairline.
  - **Channel rows**, each **94 px** tall:
    - Left **320 px** channel cell: channel number (mono, 42 px wide, `txt3`) · 62 px rounded-square logo (radius 12, 2-letter abbreviation) · name (18 px/600) + category label (13 px, category color).
    - Right = program cells laid in a horizontal row, **width = durationMinutes × 6 px** (minus 6 px gap), radius 11. Resting cell: `surface` (or 60%-opacity if not live) with a 3 px **left border in category color**; live cell slightly brighter. Cell content: title (18 px/600, ellipsis) + start time (14 px mono `txt3`); live cell shows a small pulsing red dot before the title.
  - **"Now" line:** a 2 px vertical `accent` line with a 12 px dot cap at top, positioned at `320 + (nowMinutes − gridStartMinutes) × 6` px, above all cells, with a `glow` shadow.
**Interaction:** focusing a cell updates the preview strip above and applies the focus spec. Vertical scroll through channels; horizontal scroll through time.

### 2. Home / Browse  *(data-screen-label="Home")*
**Purpose:** Landing screen — featured live hero + content rails.
**Layout:** Full-bleed **hero** (620 px) = 16:9 backdrop placeholder with a left-to-right `bg`→transparent gradient and a bottom `bg`→transparent gradient; top app bar overlaid ("Home" active). Hero content bottom-left (max 840 px): LIVE badge + "CH n · Name"; **title 66 px / 800**; rating chip · time (mono accent) · category; description 21 px; actions = **Watch Live** (filled accent), **More Info** (translucent outline), **+** (54 px square). Below: vertical stack of **rails** (28–30 px gap), each = section title (24 px/700) + horizontal row of **286 px** cards. Card = 160 px 16:9 thumbnail (radius 12, stripe placeholder, optional LIVE badge top-left + mono badge bottom-right) + title (16 px/600) + subtitle (13 px `txt3`). Rails: "On Now", "Live Channels", "Your Recordings".

### 3. Program Info / Detail  *(data-screen-label="Program Info")*
**Purpose:** Full detail for a program with primary actions.
**Layout:** Top 660 px = key-art backdrop (placeholder) with left `bg` gradient + bottom `bg` gradient. Top-left "← Back to Guide". Content block at left 56 px, top ~250 px: LIVE NOW badge + "CH n · Name"; **title 78 px / 800** (max 1100 px); meta row (19 px) = rating chip · time (mono accent) · duration · category · ★ rating (in `warn`); description 23 px (max 980 px); action row = **Watch Live** (filled accent), **Record Series** (outline), **+** and **⤴ share** (58 px squares). Below, three metadata columns (64 px gap), each a mono uppercase label (`txt3`) + value: **CAST**, **UP NEXT** (next title + time), **AUDIO** (5.1 / stereo / subtitles).

### 4. Channel Switcher (mini-guide)  *(data-screen-label="Channel Switcher")*
**Purpose:** Flip channels while watching without leaving playback.
**Layout:** Full-bleed video (placeholder) behind. Bottom overlay (430 px) with a `bg`→transparent top gradient. Header row: "CHANNELS" (mono, `txt2`) + hint "▲▼ Browse · OK Watch · ⌫ Close" (mono `txt3`). Horizontal strip of **300 px** channel cards (radius 14, 18 px padding): logo (54 px) + "CH n" (mono) + name; now-playing line with red dot; "Next · …" line. **Active/focused card:** `accent` border, `raised` bg, scale 1.04, glow ring (focus spec).

### 5. Playback Overlay  *(data-screen-label="Playback")*
**Purpose:** Transport controls over live video.
**Layout:** Full-bleed video placeholder. **Top bar** (gradient): ← back · LIVE badge + "CH 7 · Apex Sports" + **title 30 px** · clock right. **Center:** 108 px circular play/pause button in `accent` with a 7 px glow ring (focused state). **Bottom controls** (gradient, 300 px): scrubber row = elapsed `-42:18` (mono) · progress track (7 px, 78% filled in accent, white knob w/ glow, red live-edge dot at far right) · "LIVE" (red). Button row: left cluster = restart (⟲), +30s, CH ▲▼ (60 px squares); right cluster = "CC Subtitles", "♪ Audio", "▤ Guide" (56 px pill buttons, `surface`/`line`).

### 6. Sign-In / Profile Picker  *(data-screen-label="Sign-In")*
**Purpose:** Choose who's watching on launch.
**Layout:** Centered on a radial `surface`→`bg` background. Logo + "Aerial"; **"Who's watching?" 56 px / 800**; row of profile tiles (48 px gap): **168 px** rounded-square (radius 24) gradient avatar with initial (64 px), name (24 px) below; **active profile** = `accent` border + 6 px glow + scale 1.05. After profiles: dashed **+ Add Profile** tile. Below: "⚙ Manage Profiles" outline pill. (Profiles in mock: Jordan, Sam, Avery, Kids.)

### 7. Confirm & Error Modals  *(data-screen-label="Modals")*
**Purpose:** Destructive confirmation + system messaging family.
**Layout:** Dimmed scrim (`rgba(6,7,10,.72)`) over the app.
- **Confirm dialog** (centered, 680 px, `raised`, radius 24, 48 px padding, heavy shadow): circular danger icon (74 px, `danger` on 14%-tint), **title 36 px/700** ("Delete this recording?"), body 20 px `txt2`, two equal buttons — **Cancel** (`surface`/`line`) and **Delete** (filled `danger`, white, focus glow). Primary/destructive action takes focus.
- **Error banner** (top center): `raised` card, 5 px left `danger` border, "!" icon, bold title + body, dismiss ✕.
- **Success toast** (bottom-right): `raised` card, 5 px left `success` border, ✓ icon, title + subtitle. Auto-dismiss in product.

### 8. Loading & Focus States (spec sheet)  *(data-screen-label="States")*
**Purpose:** Reference screen — **this is your implementation cheat-sheet**, not a product screen. Documents: skeleton/shimmer loaders (rail + cards, `surface`→`line` shimmer sweep, 1.4 s), a buffering spinner ("720p → 4K"), the **focus anatomy** (rest vs focused card with the exact values from the Focus Spec above), the color-token swatches, and the **type scale**. Use it to drive your theme + reusable focus modifier.

---

## Interactions & Behavior
- **Navigation:** D-pad up/down/left/right moves focus; OK/Enter activates. Back returns up the stack. Map to Compose for TV focus + your `NavHost`.
- **Guide:** focusing a cell updates the preview; vertical = channels, horizontal = timeline. Keep the "now" line pinned to current time.
- **Focus animation:** 180 ms / FastOutSlowIn on scale, border, glow, elevation (see Focus Spec).
- **Live indicators:** small red dot pulses (opacity 0.55↔1, ~1.4 s loop) on LIVE badges and live cells.
- **Playback:** for live content the scrubber shows time-behind-live and a live-edge marker; restart and +30s seek within the buffer.
- **Skeletons:** shimmer sweep while artwork/data loads; spinner for buffering.
- **Modals:** scrim dims background; destructive action is visually primary and pre-focused; toasts/banners are non-blocking.

## State Management
- `currentScreen` / nav back-stack
- `selectedTheme` (azure | ember | aurora) → drives MaterialTheme
- `focusedProgram { channelIndex, programIndex }` → drives guide preview
- `activeProfile`
- `settingsSection` + per-row values (toggles, selects, slider percentages)
- `switcherChannelIndex`
- `nowMinutes` / `gridStartMinutes` (guide timeline; `pxPerMin = 6`)
- Playback: `isPlaying`, `position`, `isAtLiveEdge`, buffer length
- Data fetching: channel lineup + EPG schedule (per channel: title, startMinutes, durationMinutes, category, rating, live flag, description), profiles, recordings.

## Design Tokens (summary)
- **Colors:** see Theme table above (default = Midnight Azure).
- **Type scale (Sora):** Display 64/800 · Headline 40/700 · Title 22/600 · Body 17/400. Mono = JetBrains Mono for time/numbers/labels.
- **Radius:** card/cell 11–12 · logo 12 · pill button 13 · dialog/avatar 24.
- **Spacing:** safe area 56 · rail gap 20 · grid time slot 180 (=30 min) · channel row height 94 · channel cell width 320.
- **Shadow / elevation:** focus 16 dp (`0 14–16px 34px -8px rgba(0,0,0,.7)`); dialog `0 40px 110px -30px rgba(0,0,0,.85)`.
- **Focus ring:** 5 dp at 50% accent (`glow`).
- **Motion:** focus 180 ms FastOutSlowIn · shimmer 1.4 s · live-dot pulse 1.4 s.

## Assets
No bitmap assets are included — all imagery (hero backdrops, key art, posters, channel logos, video frames) is shown as **diagonal-stripe placeholders** with mono captions ("BACKDROP 16:9", "KEY ART", "VIDEO · LIVE STREAM"). Wire these to the app's existing artwork/EPG image sources. Icons in the mock are Unicode glyphs (▶ ❚❚ ● ＋ ⌕ ★ ✓ ✕ ⟲ ▲▼) — replace with the app's existing icon set (e.g. Material Icons / vector drawables). No third-party/branded assets are used; "Aerial" and all channel/program names are fictional placeholders — swap for real branding.

## Files
- `Aerial TV — Design Reference.html` — the full interactive design reference (all 9 screens), **self-contained (works offline, no build step)**. Open it in any browser; use the left rail to switch screens and the theme swatches to preview the three color directions. Inspect element styles in DevTools for exact values; each product screen is tagged with a `data-screen-label` attribute. **This file is a visual spec — do not copy its HTML/CSS into the app; reimplement the screens in Jetpack Compose using the app's existing patterns.**
- `README.md` — this document.

## How to use this with Claude Code
1. Place this `design_handoff_androidtv/` folder inside (or alongside) the app repo.
2. From the repo, start Claude Code and prompt, e.g.:
   > Read `design_handoff_androidtv/README.md` and open `Aerial TV — Design Reference.html` for the visual spec. Redesign our EPG, Program Info, and Settings screens to match this direction, using our existing Compose-for-TV components, theme, and navigation. Start by mapping each design screen to our current screens and proposing a token/theme update, then implement screen by screen.
3. Have it work **screen by screen**, reusing existing composables and only adding new ones where needed. Start with the theme/token layer (the table above), then the reusable D-pad focus modifier (Focus Spec), then the screens.
