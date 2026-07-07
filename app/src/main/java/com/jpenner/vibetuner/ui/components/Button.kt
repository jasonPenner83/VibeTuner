package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AerialButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = (if (enabled) modifier else modifier.alpha(0.5f)).height(54.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (filled) AerialColors.Accent else AerialColors.Surface,
            contentColor   = if (filled) AerialColors.AccentInk else AerialColors.Txt,
            focusedContainerColor = if (filled) AerialColors.Accent else AerialColors.Raised,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            border = if (filled) Border.None
            else Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(12.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(12.dp)),
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(AerialColors.Glow, elevation = 12.dp)),
    ) {
        Row(
            Modifier.padding(horizontal = 26.dp).fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(22.dp)) }
            Text(label, style = AerialTypography.titleMedium)
        }
    }
}