package com.jpenner.vibetuner.ui.screens.guide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.ui.components.LiveDot
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography
import com.jpenner.vibetuner.ui.screens.guide.formatTime

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NowPlayingCard(
    program: Program?,
    nowMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface, focusedContainerColor = AerialColors.Surface,
            contentColor = AerialColors.Txt, focusedContentColor = AerialColors.Txt),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(14.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(14.dp))),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(AerialColors.Glow, elevation = 16.dp)),
    ) {
        if (program == null) {
            Box(Modifier.fillMaxSize().padding(20.dp), Alignment.CenterStart) {
                Text("No programme information", color = AerialColors.Txt)
            }
            return@Surface
        }

        // remaining + progress straight from the model helpers
        val remaining = program.endMinutes - nowMinutes
        val progress = program.progressAt(nowMinutes)

        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center) {

            // title row + "X min left" pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(); Spacer(Modifier.width(9.dp))
                Text(program.title, style = AerialTypography.titleMedium.copy(fontSize = 20.sp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                RemainingPill(remaining)
            }
            Spacer(Modifier.height(9.dp))

            // start --- progress --- end
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(formatTime(program.startMinutes), style = AerialTypography.labelSmall,
                    color = AerialColors.Txt3, fontSize = 13.sp)
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(AerialColors.Line)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(3.dp))
                        .background(AerialColors.Accent))
                }
                Text(formatTime(program.endMinutes), style = AerialTypography.labelSmall,
                    color = AerialColors.Txt3, fontSize = 13.sp)
            }
            Spacer(Modifier.height(11.dp))
            Text(program.description, color = AerialColors.Txt2, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RemainingPill(minutes: Int) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp))
            .background(AerialColors.Success.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text("$minutes min left", style = AerialTypography.labelSmall,
            color = AerialColors.Success, fontSize = 12.sp)
    }
}