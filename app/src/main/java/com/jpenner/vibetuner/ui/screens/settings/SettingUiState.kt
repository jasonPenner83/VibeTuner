package com.jpenner.vibetuner.ui.screens.settings

import com.jpenner.vibetuner.data.model.SettingItem

/** One entry in the left rail. [id] keys the section; [summary] is the
 *  mono sub-label shown under the title. */
data class SettingsSection(
    val id: String,
    val title: String,
    val summary: String,
)

/** Immutable snapshot the screen renders from. */
data class SettingsUiState(
    val sections: List<SettingsSection> = emptyList(),
    val selectedId: String = "display",
    val rows: List<SettingItem> = emptyList(),
) {
    val selected: SettingsSection?
        get() = sections.firstOrNull { it.id == selectedId }
}