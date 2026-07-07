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
import com.jpenner.vibetuner.data.model.SettingControl
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsRow(label: String, sub: String, control: SettingControl, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface, focusedContainerColor = AerialColors.Raised),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(12.dp))),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(label, style = AerialTypography.titleMedium.copy(fontSize = 22.sp), color = AerialColors.Txt)
                Text(sub, fontSize = 16.sp, color = AerialColors.Txt2)
            }
            when (control) {
                is SettingControl.Toggle -> AerialSwitch(control.on)
                is SettingControl.Slider -> AerialSlider(control.fraction, Modifier.width(240.dp))
                is SettingControl.Value  -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(control.text, color = if (control.danger) AerialColors.Live else AerialColors.Txt)
                    Text(" \u203A", color = AerialColors.Txt)
                }
                is SettingControl.Info   -> Text(control.text, color = AerialColors.Txt2)
            }
        }
    }
}
