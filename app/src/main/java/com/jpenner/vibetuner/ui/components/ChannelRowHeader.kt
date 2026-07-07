package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.Dimens
import com.jpenner.vibetuner.ui.theme.AerialTypography

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelRowHeader(
    channel: Channel,
    onClick: ()-> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Bg, focusedContainerColor = AerialColors.Surface,
            contentColor = AerialColors.Txt, focusedContentColor = AerialColors.Txt),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(
//            border = Border(BorderStroke(0.dp, AerialColors.Line), shape = RoundedCornerShape(14.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(14.dp))),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(AerialColors.Glow, elevation = 16.dp)),
        )
    {
        Row(
            Modifier.width(Dimens.ChannelCol).height(Dimens.RowHeight).padding(end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                channel.number, modifier = Modifier.width(42.dp),
                style = AerialTypography.labelSmall.copy(fontSize = 16.sp),
                color = AerialColors.Txt
            )
            // logo chip — swap Box for Coil AsyncImage when a real logo URL exists
            Surface(
                shape = RoundedCornerShape(12.dp),
                colors = SurfaceDefaults.colors(AerialColors.Surface),
                border = Border(BorderStroke(1.dp, channel.category.color)),
                modifier = Modifier.size(62.dp)
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        channel.abbreviation,
                        fontWeight = FontWeight.Bold,
                        color = AerialColors.Txt2
                    )
                }
            }
            Column {
                Text(
                    channel.name,
                    style = AerialTypography.titleMedium,
                    maxLines = 1,
                    color = AerialColors.Txt
                )
                Text(
                    channel.category.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = channel.category.color
                )
            }
        }
    }
}

