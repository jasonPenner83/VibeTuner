package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography
import com.jpenner.vibetuner.ui.theme.Dimens

enum class TopTab(val label: String) { Home("Home"), Guide("Guide") }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    selected: TopTab,
    onSelect: (TopTab) -> Unit,
    clock: String,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().height(84.dp).padding(horizontal = Dimens.SafeArea),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(38.dp)) {
            Logo()
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                TopTab.entries.forEach { tab -> NavTab(tab.label, tab == selected) { onSelect(tab) } }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
//            SearchButton()
            Text(clock, style = AerialTypography.titleLarge, color = AerialColors.Txt)
            IconChipButton(Icons.Default.Settings, onSettings)
            IconChipButton(Icons.Default.AccountCircle, onProfile)
        }
    }
}

// NavTab is a focusable Surface (so the D-pad can select it); it paints a
// 3dp accent underline when active and a focus ring when focused.
//@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavTab(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = AerialColors.Raised,
            contentColor = if (active) AerialColors.Txt else AerialColors.Txt2,
            focusedContentColor = AerialColors.Txt,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(10.dp))),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label)
            if (active) Box(Modifier.padding(top = 6.dp).height(3.dp).width(28.dp)
                .background(AerialColors.Accent, RoundedCornerShape(3.dp)))
        }
    }
}
