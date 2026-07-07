package com.jpenner.vibetuner.ui.screens.guide

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.Dimens

/**
 * The vertical accent line marking the current time. Its x position is
 * now * PxPerMin (absolute, from 00:00), shifted left by the shared scroll offset
 * so it stays glued to real time and lines up with cells while the timeline pans.
 */
@Composable
fun NowLine(nowMinutes: Int, scroll: ScrollState, modifier: Modifier = Modifier) {
    val xDp = (nowMinutes * Dimens.PxPerMin).dp
    Box(
        modifier
            .offset { IntOffset(x = xDp.roundToPx() - scroll.value, y = 0) }
            .width(12.dp),                       // room for the dot cap
    ) {
        Box(Modifier.align(Alignment.TopCenter).fillMaxHeight().width(2.dp).background(AerialColors.Accent))
        Box(Modifier.align(Alignment.TopCenter).size(12.dp).clip(CircleShape).background(AerialColors.Accent))
    }
}