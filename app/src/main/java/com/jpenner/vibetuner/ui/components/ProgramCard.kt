package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.ui.screens.guide.formatTime
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.Dimens
import com.jpenner.vibetuner.ui.theme.AerialTypography

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProgramCard(program: Program, category: Category, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val width = (program.durationMinutes * Dimens.PxPerMin).dp - 6.dp   // duration-driven

    Surface(
        onClick = onClick,
        modifier = modifier.width(width).height(82.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(11.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (program.isLive) AerialColors.Surface
            else AerialColors.Surface.copy(alpha = 0.6f),
            focusedContainerColor = AerialColors.Raised,
            contentColor = AerialColors.Txt,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            // resting: 3dp category edge on the left
            border = Border(BorderStroke(3.dp, category.color),
                shape = RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(11.dp)),
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(AerialColors.Glow, elevation = 16.dp)),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 17.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (program.isLive) { LiveDot(); Spacer(Modifier.width(8.dp)) }
                Text(program.title, style = AerialTypography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(5.dp))
            Text(formatTime(program.startMinutes), style = AerialTypography.bodySmall,
                color = AerialColors.Txt3)
        }
    }
}