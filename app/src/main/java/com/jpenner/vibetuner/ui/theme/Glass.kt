package com.jpenner.vibetuner.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Diagonal-stripe placeholder texture, matching the design kit's artwork placeholders
 * (`repeating-linear-gradient(135deg, rgba(255,255,255,.035) 0 2px, transparent 2px 13px)`).
 * Drawn behind content, so it shows through only where there is no artwork.
 */
fun Modifier.diagonalStripes(color: Color = Color.White.copy(alpha = 0.035f)): Modifier =
    drawBehind {
        val step = 13.dp.toPx()
        val w = 2.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x + size.height, size.height), w)
            x += step
        }
    }
