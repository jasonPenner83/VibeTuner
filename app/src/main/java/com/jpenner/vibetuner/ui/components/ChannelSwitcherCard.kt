package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelSwitcherCard(
    channel: Channel,
    nowMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = channel.nowPlaying(nowMinutes)
    val next = channel.nextUp(nowMinutes)

    Surface(
        onClick = onClick,
        modifier = modifier.width(300.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface.copy(alpha = 0.72f),
            focusedContainerColor = AerialColors.Raised),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(14.dp))),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(AerialColors.Glow, elevation = 16.dp)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ChannelLogo(channel, size = 54.dp)
                Column {
                    Text("CH " + channel.number, style = AerialTypography.labelSmall, color = AerialColors.Txt)
                    Text(channel.name, style = AerialTypography.titleMedium, maxLines = 1)
                }
            }
            Spacer(Modifier.height(14.dp))
            // only show the live row when something is actually airing
            if (now != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveDot()
                    Text(now.title, fontSize = 16.sp, maxLines = 1)
                }
            }
            next?.let {
                Text("Next \u00B7 " + it.title, fontSize = 14.sp, color = AerialColors.Txt, maxLines = 1)
            }
        }
    }
}

