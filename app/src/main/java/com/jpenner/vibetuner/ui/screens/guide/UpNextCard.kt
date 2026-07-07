package com.jpenner.vibetuner.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.ui.theme.AerialTypography
import androidx.tv.material3.Text
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.screens.guide.formatTime

/** Compact, non-focusable "what's on next" card. */
@Composable
fun UpNextCard(program: Program?, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(AerialColors.Surface.copy(alpha = 0.55f))
            .border(1.dp, AerialColors.Line.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (program == null) {
            Text("\u2014", color = AerialColors.Txt3); return@Column
        }
        Text(formatTime(program.startMinutes), style = AerialTypography.labelSmall,
            color = AerialColors.Txt3, fontSize = 13.sp)
        Spacer(Modifier.height(7.dp))
        Text(program.title, style = AerialTypography.titleMedium.copy(fontSize = 17.sp),
            color = AerialColors.Txt, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text("${program.durationMinutes} min", color = AerialColors.Txt3, fontSize = 13.sp)
    }
}