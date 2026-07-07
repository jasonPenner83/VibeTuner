package com.jpenner.vibetuner.ui.screens.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.ChannelType
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/** Filter row above the Guide's program list: Type and Genre dropdowns today,
 *  Favourites planned as a third button in a follow-up pass.
 *
 *  Renders only the dropdown buttons — the picker dialogs it opens are true
 *  screen-level overlays owned by the caller (see [GuideScreen]), so they can
 *  stack on top of the ENTIRE screen instead of this row's local layout slot. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GuideFilterRow(
    state: GuideUiState,
    onOpenTypePicker: () -> Unit,
    onOpenGenrePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterDropdownButton(
            label = "Type",
            valueLabel = state.typeFilter?.label ?: "All",
            onClick = onOpenTypePicker,
        )
        FilterDropdownButton(
            label = "Genre",
            valueLabel = state.genreFilter?.label ?: "All",
            onClick = onOpenGenrePicker,
        )
    }
}

/** Type picker overlay — call as a direct sibling of the screen's root Column
 *  inside its outer Box so it covers the true full screen. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GuideTypeFilterDialog(
    state: GuideUiState,
    onTypeSelected: (ChannelType?) -> Unit,
    onDismiss: () -> Unit,
) {
    FilterPickerDialog(
        title = "Type",
        optionLabels = listOf("All") + state.availableTypes.map { it.label },
        selectedLabel = state.typeFilter?.label ?: "All",
        onSelect = { index ->
            onTypeSelected(if (index == 0) null else state.availableTypes[index - 1])
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

/** Genre picker overlay — call as a direct sibling of the screen's root Column
 *  inside its outer Box so it covers the true full screen. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GuideGenreFilterDialog(
    state: GuideUiState,
    onGenreSelected: (Category?) -> Unit,
    onDismiss: () -> Unit,
) {
    FilterPickerDialog(
        title = "Genre",
        optionLabels = listOf("All") + state.availableGenres.map { it.label },
        selectedLabel = state.genreFilter?.label ?: "All",
        onSelect = { index ->
            onGenreSelected(if (index == 0) null else state.availableGenres[index - 1])
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterDropdownButton(label: String, valueLabel: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Bg,
            contentColor = AerialColors.Txt3,
            focusedContainerColor = AerialColors.Raised,
            focusedContentColor = AerialColors.Txt,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(8.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(8.dp))),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), Alignment.Center) {
            Text("$label: $valueLabel", style = AerialTypography.labelSmall.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Full-screen scrim + centered, focus-trapped picker — same pattern as
 *  AerialConfirmDialog (VibeDialog.kt); this app has no anchored-Popup pattern. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterPickerDialog(
    title: String,
    optionLabels: List<String>,
    selectedLabel: String,
    onSelect: (index: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedIndex = optionLabels.indexOf(selectedLabel).coerceAtLeast(0)
    val selectedFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { selectedFocus.requestFocus() }
    BackHandler { onDismiss() }

    Box(Modifier.fillMaxSize().background(Color(0xB8060709)), Alignment.Center) {
        Surface(
            modifier = Modifier.width(420.dp)
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup(),
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(AerialColors.Raised),
            border = Border(BorderStroke(1.dp, AerialColors.Line)),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(title, style = AerialTypography.titleMedium.copy(fontSize = 20.sp))
                Spacer(Modifier.height(16.dp))
                optionLabels.forEachIndexed { index, optionLabel ->
                    val rowModifier = if (index == selectedIndex) Modifier.focusRequester(selectedFocus) else Modifier
                    PickerRow(
                        label = optionLabel,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        modifier = rowModifier,
                    )
                    if (index != optionLabels.lastIndex) Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PickerRow(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AerialColors.Accent else Color.Transparent,
            contentColor = if (selected) AerialColors.AccentInk else AerialColors.Txt2,
            focusedContainerColor = if (selected) AerialColors.Accent else AerialColors.Bg,
            focusedContentColor = if (selected) AerialColors.AccentInk else AerialColors.Txt,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(10.dp))),
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(label, style = AerialTypography.titleMedium.copy(fontSize = 15.sp))
        }
    }
}
