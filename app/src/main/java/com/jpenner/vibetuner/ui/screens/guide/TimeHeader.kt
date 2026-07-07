package com.jpenner.vibetuner.ui.screens.guide

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jpenner.vibetuner.ui.theme.AerialTypography
import androidx.tv.material3.Text
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.Dimens
import com.jpenner.vibetuner.ui.screens.guide.*

private const val SLOT_MINUTES = 30
private const val SLOT_COUNT = 48                       // the full day in half-hours
private val SLOT_WIDTH = (SLOT_MINUTES * Dimens.PxPerMin).dp   // == 180dp

// The ruler is laid from absolute 00:00 and shares [scroll] with the lanes, so a
// slot's x (i * SLOT_WIDTH == minute * PxPerMin) lines up with a cell at the same minute.
@Composable
fun TimeHeader(scroll: ScrollState, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        // Filter chip sits above the fixed channel column.
        Box(Modifier.width(Dimens.ChannelCol).padding(bottom = 10.dp)) {
            Text("All Channels  \u25BE", style = AerialTypography.titleMedium)
        }
        Row(Modifier.weight(1f).horizontalScroll(scroll)) {
            repeat(SLOT_COUNT) { i ->
                Text(
                    formatTime(i * SLOT_MINUTES),
                    modifier = Modifier.width(SLOT_WIDTH).padding(bottom = 10.dp),
                    style = AerialTypography.labelSmall,
                    color = AerialColors.Txt2,
                )
            }
        }
    }
}