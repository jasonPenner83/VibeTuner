package com.jpenner.vibetuner.ui.settings

import androidx.compose.ui.graphics.Color
import com.jpenner.vibetuner.data.model.CatalogSource
import com.jpenner.vibetuner.data.repository.SubChannelToggle

/** One row in the LINEUP list. [sourceSub] disambiguates colliding names ("Cinemeta · Series"). */
data class LineupRow(
    val sourceKey: String,
    val number: String,
    val name: String,
    val sourceSub: String,
    val categoryColor: Color,
    val enabled: Boolean,
)

/** The EDIT pane for the selected channel. [source] backs the read-only SOURCE card. */
data class ChannelEdit(
    val sourceKey: String,
    val number: String,
    val name: String,
    val source: CatalogSource?,
    val categoryLabel: String,
    val categoryColor: Color,
    val marathon: Boolean,
    val marathonLimit: Int?,
    val enabled: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val subChannels: List<SubChannelToggle> = emptyList(),
) {
    val marathonLimitLabel: String
        get() = when (marathonLimit) { null -> "None"; 1 -> "1 ep"; else -> "$marathonLimit eps" }
}

/** Manual guide rebuild progress (the Rebuild Guide header action). */
sealed interface RebuildState {
    object Idle : RebuildState
    data class Running(val done: Int, val total: Int) : RebuildState
    data class Done(val channels: Int) : RebuildState
}

data class ChannelManagerUiState(
    val rows: List<LineupRow> = emptyList(),
    val selectedKey: String? = null,
    val editing: ChannelEdit? = null,
    val rebuild: RebuildState = RebuildState.Idle,
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}
