package com.jpenner.vibetuner.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

object Dimens {
    val SafeArea   = 56.dp     // overscan padding
    val RailGap    = 20.dp
    val RowHeight  = 94.dp     // EPG channel row
    val ChannelCol = 320.dp    // left channel cell
    val CardRadius = 12.dp
    val DialogRadius = 24.dp
    const val PxPerMin = 6f    // 30 min == 180dp on the timeline
    val DesignCanvasWidth = 1920f
    const val DesignScale = 1f
}

object Focus {
    const val CardScale = 1.07f
    const val CellScale = 1.05f
    val BorderWidth = 2.dp
    val GlowRing    = 5.dp
    val Elevation   = 16.dp
    val Anim = tween<Float>(durationMillis = 180, easing = FastOutSlowInEasing)
}