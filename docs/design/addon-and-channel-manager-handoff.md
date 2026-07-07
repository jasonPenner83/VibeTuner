# VibeTuner — Design Handoff: Add-on Manager & Channel Manager

Both are **Android TV (10-foot / D-pad) screens**. Landscape only, fully focus-navigable (no touch), dark theme. Current layout for both is a **two-column split**: a list on the left, a detail/edit or "installed" panel on the right.

Source of truth for this doc:
- `app/src/main/java/com/jpenner/vibetuner/ui/settings/AddonsScreen.kt`
- `app/src/main/java/com/jpenner/vibetuner/ui/settings/ChannelManagerScreen.kt`
- `app/src/main/java/com/jpenner/vibetuner/ui/settings/ChannelManagerViewModel.kt`
- `app/src/main/java/com/jpenner/vibetuner/ui/components/EmptyLineupScreen.kt`
- `app/src/main/java/com/jpenner/vibetuner/data/model/Channel.kt`
- `app/src/main/java/com/jpenner/vibetuner/data/model/Category.kt`
- `app/src/main/java/com/jpenner/vibetuner/data/model/stremio/StremioAddon.kt`, `StremioManifest.kt`
- `app/src/main/java/com/jpenner/vibetuner/data/repository/AddonRepository.kt`
- `app/src/main/java/com/jpenner/vibetuner/ui/theme/Color.kt`

---

## Shared design system (Aerial palette)

From `ui/theme/Color.kt` (`AerialColors`):

| Token | Hex | Use |
|---|---|---|
| Bg | `#0D0F14` | screen background |
| Surface | `#151821` | list rows / cards |
| Raised | `#1D212C` | elevated cards |
| Line | `#2A2F3C` | dividers/borders |
| Txt | `#EEF1F6` | primary text |
| Txt2 | `#A7AFBF` | secondary text |
| Txt3 | `#6B7384` | muted/help text |
| Accent | `#3D9BFF` | primary accent (blue) |
| Accent2 | `#8FC7FF` | accent highlight |
| Live | `#FF5D5D` | live / destructive |
| Success | `#46D18B` | on / enabled |
| Warn | `#FFB454` | warnings |

> **Note:** the *current* screens are hardcoded to an older green scheme (bg `#0B0E14`, accent `#00FF88`, focus `#1E4D2C`). The Aerial palette above is the intended direction (redesign in progress) — **design toward Aerial, not the green.**

Focus behavior is the key TV interaction: every row/button needs a clear **focused state** (currently a brighter container fill). Design distinct **default / focused / selected / disabled** states for all interactive elements.

---

## 1. Add-on Manager (`AddonsScreen.kt`)

**Purpose:** Users paste a Stremio manifest URL; each add-on's catalogs become channels in the guide. Add-ons are stored **per profile**.

### Layout — two columns
- **Left (weight 1) — "Add New":** heading, help text, URL text field, primary "Link Add-on" button.
- **Right (weight 1.2) — "Installed Add-ons":** scrollable list of installed add-on rows, or an empty state.

### Fields / data

**Add-new form:**

| Field | Type | Notes |
|---|---|---|
| Manifest URL | single-line text input | Placeholder: `Manifest URL (e.g. https://.../manifest.json)`. App auto-appends `/manifest.json` if missing. |
| Link button | button | Label toggles `Link Add-on` → `Linking…` while fetching (async, needs a loading state). |

Section help text (verbatim): *"Paste a Stremio manifest URL to add its catalogs as channels. Each catalog becomes one channel on the guide. Add-ons are saved per profile."*

**Installed add-on row** — data available per add-on (`StremioAddon` / `StremioManifest`):

| Field | Type | Shown today | Also available |
|---|---|---|---|
| name | String | ✅ title (bold; gray when disabled) | |
| catalog count | Int | ✅ `"N catalogs · On/Off"` subtitle | |
| enabled | Boolean | ✅ drives On/Off + Enable/Disable button | |
| logo | String? (URL) | ❌ not shown | **available** — a logo/icon slot would enrich the row |
| description | String | ❌ | available |
| version | String | ❌ | available |
| types | List<String> (e.g. movie, series) | ❌ | available — could show as chips |
| catalogs[].name / type | list | ❌ | available — could expand to list the actual catalogs (each = one channel) |

**Row actions:** Enable/Disable toggle, Remove (🗑 — currently an emoji; needs a real destructive icon, uses `Live` red on focus).

### States to design
- **Empty installed list:** centered text *"No add-ons configured."*
- **Loading:** button shows `Linking…`.
- **Success:** toast `Added <name>` / `Removed <name>`.
- **Error:** toast — main message *"Couldn't load a valid manifest from that URL."* (bad/unreachable URL, invalid manifest, network timeout). Consider an inline error state on the field rather than a toast.
- First-run add-ons are auto-seeded with **Cinemeta**, so a truly empty state is rare but must exist.

---

## 2. Channel Manager (`ChannelManagerScreen.kt`)

**Purpose:** Reorder, rename, categorize, and configure the channels derived from enabled add-on catalogs.

### Layout — two columns
- **Left (weight 1) — "Channel Lineup":** scrollable list of channel rows.
- **Right (weight 1.2) — "Edit" panel:** controls for the selected channel.

### Fields / data

**Lineup row** (per `Channel`):

| Field | Type | Shown today | Also available |
|---|---|---|---|
| number | String | ✅ channel number (gray) | |
| name | String | ✅ (white; gray when disabled) | |
| enabled | Boolean | ✅ On/Off label | |
| logoUrl | String? | ❌ | **available** — logo slot recommended |
| category | Category | ❌ in row | available (has a color — could show a color dot/chip) |
| abbreviation | String | ❌ | available |
| description | String | ❌ | available |

**Edit panel** — controls for the selected channel:

| Control | Type | Values |
|---|---|---|
| Name | text input + "Save name" button | free text |
| Category | cycling stepper `< Label >` | one of the Category enum (see below); each has a color |
| Mode | cycling stepper `< Random / Marathon >` | Random or Marathon (`CHRONOLOGICAL`) |
| Marathon limit | cycling stepper (only when Mode = Marathon) | None (full show), 1, 2, 3, 4, 5 per show |
| Enabled | toggle button | ON / OFF |
| Move Up / Move Down | two buttons side by side | reorders lineup |

> The steppers are currently `< prev value next >` cycling buttons because of D-pad input. Design a TV-friendly stepper / segmented pattern — this is a good candidate for a design improvement.

**Category enum** (label + assigned color — 18 genres + 2 special):

| Label | Color | | Label | Color |
|---|---|---|---|---|
| Movies | `#B07CFF` | | Documentary | `#34D0C0` |
| Kids | `#FFB454` | | Romance | `#FF6FA5` |
| Sports | `#46D18B` | | Animation | `#4AC0E0` |
| Music | `#FF7AC0` | | Crime | `#6E7687` |
| News | `#5B8DEF` | | Fantasy | `#9B6DFF` |
| Sci-Fi | `#7C9CFF` | | Adventure | `#4CC38A` |
| Action | `#FF6B5C` | | Mystery | `#5C6BC0` |
| Comedy | `#FFD24A` | | Featured (special) | `#FFC24A` |
| Drama | `#FF8A5C` | | For You (special) | `#3D9BFF` |
| Horror | `#B23A48` | | Thriller | `#8E7CFF` |

### States to design
- **Empty lineup:** dedicated screen (`EmptyLineupScreen`) — heading *"No channels yet"*, body *"Channels come from Stremio add-on catalogs. Open Settings → Add-Ons and paste a manifest URL (for example Cinemeta) to fill your guide."*, and an **"Open Add-Ons"** button. (This links the two screens — the empty channel state routes to the Add-on manager.)
- **No selection:** right panel shows *"Select a channel"*.
- **Loading:** lineup should not flash the empty state while loading (recent fix).
- Toast on rename: *"Renamed"*.

---

## Cross-screen relationships worth noting for design
- **Add-ons → Channels is causal:** an add-on's catalogs *are* the channels. The empty-channel state deep-links to Add-ons. Consider showing this relationship visually (e.g. "3 add-ons → 12 channels").
- Both screens live under **Settings**; consistent header / back treatment matters.
- Everything is **D-pad driven** — no drag-to-reorder; reordering is Move Up / Move Down buttons.
