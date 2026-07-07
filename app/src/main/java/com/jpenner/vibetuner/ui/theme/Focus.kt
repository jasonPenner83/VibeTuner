package com.jpenner.vibetuner.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardGlow
import androidx.tv.material3.CardScale
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceGlow
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.Glow

/** D-pad focus treatment constants from the Aerial kit's "States" reference. */
object VibeFocus {
    const val FocusedScale = 1.07f
    val BorderWidth = 2.dp
    const val DurationMs = 180
    val Elevation = 16.dp
}

/**
 * Reusable focus treatment for non-Card focusable surfaces (nav items, buttons, tiles):
 * animates scale to 1.07, draws a 2dp accent border, and approximates the glow ring via a
 * colored elevation shadow, all over 180ms FastOutSlowIn.
 *
 * Glow caveat: Compose has no box-shadow spread; the glow is approximated with a colored
 * shadow (`ambientColor`/`spotColor`), which is honored on API 28+ and falls back to a
 * dark shadow on API 26–27. Visuals are tuned on-device.
 */
fun Modifier.tvFocus(
    shape: Shape = RoundedCornerShape(12.dp),
    onFocusChanged: (Boolean) -> Unit = {},
): Modifier = composed {
    val colors = LocalVibeColors.current
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) VibeFocus.FocusedScale else 1f,
        animationSpec = tween(VibeFocus.DurationMs, easing = FastOutSlowInEasing),
        label = "tvFocusScale",
    )
    this
        .onFocusChanged {
            focused = it.isFocused
            onFocusChanged(it.isFocused)
        }
        .focusable()
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .shadow(
            elevation = if (focused) VibeFocus.Elevation else 0.dp,
            shape = shape,
            ambientColor = colors.glow,
            spotColor = colors.glow,
        )
        .border(
            width = if (focused) VibeFocus.BorderWidth else 0.dp,
            color = if (focused) colors.accent else Color.Transparent,
            shape = shape,
        )
}

/** Card container colors from tokens: surface at rest, raised when focused. */
@Composable
fun vibeCardColors(): CardColors {
    val colors = LocalVibeColors.current
    return CardDefaults.colors(
        containerColor = colors.surface,
        focusedContainerColor = colors.raised,
        contentColor = colors.txt,
        focusedContentColor = colors.txt,
    )
}

/** Card border: 1dp line at rest, 2dp accent when focused (matches the kit's guide cells). */
@Composable
fun vibeCardBorder(): CardBorder {
    val colors = LocalVibeColors.current
    return CardDefaults.border(
        border = Border(
            border = BorderStroke(1.dp, colors.line),
            inset = 0.dp,
        ),
        focusedBorder = Border(
            border = BorderStroke(VibeFocus.BorderWidth, colors.accent),
            inset = 0.dp,
        ),
    )
}

/** Card scale: 1.0 at rest, 1.07 when focused. */
fun vibeCardScale(): CardScale =
    CardDefaults.scale(focusedScale = VibeFocus.FocusedScale)

/** Guide-cell scale: 1.0 at rest, 1.05 when focused (the kit uses 1.05 for guide cells). */
fun vibeCellScale(): CardScale =
    CardDefaults.scale(focusedScale = 1.05f)

/** Card glow: the kit's focus ring + drop shadow, approximated with the glow token + elevation. */
@Composable
fun vibeCardGlow(): CardGlow =
    CardDefaults.glow(
        focusedGlow = Glow(
            elevationColor = LocalVibeColors.current.glow,
            elevation = VibeFocus.Elevation,
        )
    )

/** ClickableSurface scale: 1.0 at rest, 1.07 when focused. */
@Composable
fun vibeClickableScale(): ClickableSurfaceScale =
    ClickableSurfaceDefaults.scale(focusedScale = VibeFocus.FocusedScale)

/** ClickableSurface glow: glow token color at VibeFocus.Elevation when focused. */
@Composable
fun vibeClickableGlow(): ClickableSurfaceGlow =
    ClickableSurfaceDefaults.glow(
        focusedGlow = Glow(
            elevationColor = LocalVibeColors.current.glow,
            elevation = VibeFocus.Elevation
        )
    )
