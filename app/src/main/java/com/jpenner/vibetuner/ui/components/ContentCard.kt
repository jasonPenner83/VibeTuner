package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.jpenner.vibetuner.data.model.ContentItem
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentCard(item: ContentItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.width(286.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = AerialColors.Surface),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(12.dp))),
            glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(AerialColors.Glow, elevation = 16.dp)),
        ) {
            Box {
                AsyncImage(model = item.thumbnailUrl, contentDescription = item.title,
                    modifier = Modifier.fillMaxSize())
                if (item.isLive) LiveBadge(Modifier.align(Alignment.TopStart).padding(10.dp))
            }
        }
        Spacer(Modifier.height(11.dp))
        Text(item.title, style = AerialTypography.titleMedium.copy(fontSize = 16.sp), color = AerialColors.Txt,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.subtitle, fontSize = 13.sp, color = AerialColors.Txt2, maxLines = 1)
    }
}