package com.jpenner.vibetuner.ui.screens.guide

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.jpenner.vibetuner.data.model.Channel

/**
 * Drop-in replacement for GuideGrid. Same GuideUiState / GuideViewModel,
 * but a plain vertical list of Now/Next rows — no shared scroll, no now-line.
 */
@Composable
fun NowNextList(
    state: GuideUiState,
    onProgramFocused: (channel: Int, program: Int) -> Unit,
    onProgramClick: (programId: String) -> Unit,
    onChannelMenu: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val channels = state.visibleChannels
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
            NowNextRow(
                channel = channel,
                nowMinutes = state.nowMinutes,
                onClick = onProgramClick,
                onOpenMenu = { onChannelMenu(channel)},
                // Only the channel row is focusable here; the preview resolves the
                // now-playing program itself, so the program index is unused.
                onFocused = { onProgramFocused(index, 0) },
            )
        }
    }
}