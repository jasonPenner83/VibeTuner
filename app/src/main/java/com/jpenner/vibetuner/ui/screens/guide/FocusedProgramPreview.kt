package com.jpenner.vibetuner.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.ui.components.AerialButton
import com.jpenner.vibetuner.ui.components.LiveBadge
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/** The big preview strip above the grid, bound to the focused cell. */
@Composable
fun FocusedProgramPreview(
    program: Program?,
    channel: Channel?,
    onWatch: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Nothing focused yet (still loading) — render an empty spacer.
    if (program == null || channel == null) {
        Spacer(modifier); return
    }

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(34.dp)) {
        // 16:9 backdrop. AsyncImage falls back to the surface tint while loading.
        Box(
            Modifier.width(454.dp).fillMaxHeight()
                .clip(RoundedCornerShape(14.dp)).background(AerialColors.Surface),
        ) {
            AsyncImage(
                model = program.backdropUrl,           // see note: add to Program
                contentDescription = program.title,
                modifier = Modifier.fillMaxWidth(),
            )
            if (program.isLive) LiveBadge(Modifier.align(Alignment.TopStart).padding(16.dp))
            Text(
                "CH " + channel.number + " \u00B7 " + channel.name,
                style = AerialTypography.labelSmall,
                color = AerialColors.Txt2,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(timeRange(program), style = AerialTypography.bodyLarge, color = AerialColors.Accent)
                RatingChip(program.rating)
                Text(program.mediaType, style = AerialTypography.bodyLarge, color = AerialColors.Txt2)
            }
            Spacer(Modifier.height(13.dp))
            Text(
                program.title,
                style = AerialTypography.displayLarge.copy(fontSize = 48.sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                color= AerialColors.Txt
            )
            Spacer(Modifier.height(13.dp))
            Text(
                program.description,
                style = AerialTypography.bodyLarge.copy(fontSize = 20.sp),
                color = AerialColors.Txt2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AerialButton("Watch Live", onWatch, filled = true)
                AerialButton("More Info", onInfo)
            }
        }
    }
}

@Composable
private fun RatingChip(label: String) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp))
            .background(AerialColors.Surface).padding(horizontal = 9.dp, vertical = 2.dp),
    ) {
        Text(label, style = AerialTypography.labelSmall, color = AerialColors.Txt2)
    }
}