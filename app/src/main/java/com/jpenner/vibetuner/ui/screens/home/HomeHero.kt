package com.jpenner.vibetuner.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.ui.components.AerialButton
import com.jpenner.vibetuner.ui.components.IconChipButton
import com.jpenner.vibetuner.ui.components.LiveBadge
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.Dimens
import com.jpenner.vibetuner.ui.screens.guide.timeRange
import com.jpenner.vibetuner.ui.theme.AerialTypography

/**
 * Full-bleed featured backdrop with a left-to-right and bottom scrim so the
 * copy stays legible, plus the primary live actions. Sits as item 0 of the
 * home column; bind [program] to the focused tile to make it immersive.
 */
@Composable
fun HomeHero(
    channel: Channel?,
    program: Program?,
    onWatch: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Box(modifier.fillMaxWidth().height(620.dp)) {
        // 16:9 backdrop
        AsyncImage(
            model = program?.backdropUrl,
            contentDescription = program?.title,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxSize()
        )
        // left + bottom gradients -> bg, painted over the artwork
        Box(
            Modifier.fillMaxWidth().drawWithContent {
                drawContent()
                drawRect(Brush.horizontalGradient(
                    0f to AerialColors.Bg, 0.55f to Color.Transparent, startX = 0f, endX = size.width))
                drawRect(Brush.verticalGradient(
                    0.45f to Color.Transparent, 1f to AerialColors.Bg))
            },
        )

        Column(
            Modifier.align(Alignment.BottomStart)
                .padding(start = Dimens.SafeArea, bottom = 44.dp, end = Dimens.SafeArea)
                .widthIn(max = 840.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (program?.isLive == true) LiveBadge()
                Text(
                    "CH " + (channel?.number ?: "") + "  \u00B7  " + (channel?.name ?: ""),
                    style = AerialTypography.labelSmall, color = AerialColors.Txt2,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                program?.title.orEmpty(),
                style = AerialTypography.displayLarge.copy(fontSize = 66.sp), color = AerialColors.Txt,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                program?.let { RatingChip(it.rating) }
                Text(program?.let { timeRange(it) }.orEmpty(),
                    style = AerialTypography.labelSmall, color = AerialColors.Accent)
                Text(channel?.category?.label.orEmpty(),
                    style = AerialTypography.bodyLarge, color = AerialColors.Txt2)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                program?.description.orEmpty(),
                style = AerialTypography.bodyLarge.copy(fontSize = 21.sp),
                color = AerialColors.Txt2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(26.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AerialButton("Watch Live", onWatch, icon = Icons.Default.PlayArrow, filled = true)
                AerialButton("More Info", onInfo, icon = Icons.Default.Info)
                IconChipButton(Icons.Default.Add, onClick = { /* add to My List */ })
            }
        }
    }
}

@Composable
private fun RatingChip(label: String) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp))
            .background(AerialColors.Surface).padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(label, style = AerialTypography.labelSmall, color = AerialColors.Txt)
    }
}