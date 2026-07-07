package com.jpenner.vibetuner.phone.ui.screens.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.ChannelType
import com.jpenner.vibetuner.phone.ui.components.PhoneCard
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors
import com.jpenner.vibetuner.ui.screens.guide.GuideUiState

/** Type/Genre filter chips above the channel list — same filters as the TV Guide,
 *  tap opens a full-screen picker instead of the TV's focus-trapped dialog. */
@Composable
fun GuideFilterRow(
    state: GuideUiState,
    onOpenTypePicker: () -> Unit,
    onOpenGenrePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(label = "Type", valueLabel = state.typeFilter?.label ?: "All", onClick = onOpenTypePicker)
        FilterChip(label = "Genre", valueLabel = state.genreFilter?.label ?: "All", onClick = onOpenGenrePicker)
    }
}

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

@Composable
private fun FilterChip(label: String, valueLabel: String, onClick: () -> Unit) {
    PhoneCard(onClick = onClick, shape = RoundedCornerShape(8.dp)) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), Alignment.Center) {
            Text(
                "$label: $valueLabel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = PhoneColors.Txt2,
            )
        }
    }
}

@Composable
private fun FilterPickerDialog(
    title: String,
    optionLabels: List<String>,
    selectedLabel: String,
    onSelect: (index: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler { onDismiss() }
    Box(Modifier.fillMaxSize().background(Color(0xB8060709)), Alignment.Center) {
        PhoneCard(onClick = {}, modifier = Modifier.width(300.dp), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = PhoneColors.Txt)
                Spacer(Modifier.height(14.dp))
                optionLabels.forEachIndexed { index, optionLabel ->
                    PickerRow(
                        label = optionLabel,
                        selected = optionLabel == selectedLabel,
                        onClick = { onSelect(index) },
                    )
                    if (index != optionLabels.lastIndex) Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun PickerRow(label: String, selected: Boolean, onClick: () -> Unit) {
    PhoneCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(if (selected) PhoneColors.Accent else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = if (selected) PhoneColors.AccentInk else PhoneColors.Txt2,
            )
        }
    }
}
