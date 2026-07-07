package com.jpenner.vibetuner.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Aerial design-system color tokens. Token names mirror the design kit's CSS vars.
 * Phase 1 ships [MidnightAzure] only. Ember / Aurora values are recorded below for the
 * (deferred) theme switcher:
 *   Onyx Ember : bg=#100E0C surface=#1A1714 raised=#241F1A line=#332B24 txt=#F3EFE9
 *                txt2=#B6ADA3 txt3=#7A7066 accent=#FF9D3C accent2=#FFC27A accentInk=#1A0F02
 *                glow=#6BFF9D3C danger=#FF5D5D success=#46D18B warn=#FFB454
 *   Slate Aurora: bg=#0A1013 surface=#11191E raised=#19242A line=#26343D txt=#EAF2F3
 *                txt2=#9FB0B3 txt3=#647479 accent=#34E0D0 accent2=#8AF3EA accentInk=#042321
 *                glow=#6634E0D0 danger=#FF6B6B success=#46D18B warn=#FFB454
 */
@Immutable
data class VibeColors(
    val bg: Color,
    val surface: Color,
    val raised: Color,
    val line: Color,
    val txt: Color,
    val txt2: Color,
    val txt3: Color,
    val accent: Color,
    val accent2: Color,
    val accentInk: Color,
    val glow: Color,
    val danger: Color,
    val success: Color,
    val warn: Color,
)

val MidnightAzure = VibeColors(
    bg = Color(0xFF0D0F14),
    surface = Color(0xFF151821),
    raised = Color(0xFF1D212C),
    line = Color(0xFF2A2F3C),
    txt = Color(0xFFEEF1F6),
    txt2 = Color(0xFFA7AFBF),
    txt3 = Color(0xFF6B7384),
    accent = Color(0xFF3D9BFF),
    accent2 = Color(0xFF8FC7FF),
    accentInk = Color(0xFF06121F),
    glow = Color(0x803D9BFF), // rgba(61,155,255,.5)
    danger = Color(0xFFFF5D5D),
    success = Color(0xFF46D18B),
    warn = Color(0xFFFFB454),
)

val LocalVibeColors = staticCompositionLocalOf { MidnightAzure }
