package com.jpenner.vibetuner.ui.components

import android.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.Profile
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileTile(profile: Profile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(168.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = AerialColors.Surface),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, AerialColors.Accent), shape = RoundedCornerShape(24.dp))),
            glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(AerialColors.Glow, elevation = 24.dp)),
        ) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(profile.gradient)), Alignment.Center) {
                Text(profile.initial, style = AerialTypography.displayLarge, color = AerialColors.Txt)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(profile.name, style = AerialTypography.titleMedium.copy(fontSize = 24.sp), color = AerialColors.Txt)
    }
}