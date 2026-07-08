package com.jpenner.vibetuner.phone.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors
import com.jpenner.vibetuner.ui.screens.guide.GuideViewModel

@Composable
fun GuideScreen(
    onWatch: (channelId: String) -> Unit,
    onOpenLineup: (channelId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: GuideViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }   // re-entering picks up profile/lineup changes

    var showTypePicker by remember { mutableStateOf(false) }
    var showGenrePicker by remember { mutableStateOf(false) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }

    val listState = rememberLazyListState()
    // Scroll to the tuned channel's row whenever it resolves to a real index
    // (e.g. right after returning from Player). -1 (no tuned channel, or it's
    // filtered out) is a no-op.
    val focusIndex = state.visibleChannels.indexOfFirst { it.id == state.focusChannelId }
    LaunchedEffect(focusIndex) {
        if (focusIndex >= 0) listState.scrollToItem(focusIndex)
    }

    Box(Modifier.fillMaxSize().background(PhoneColors.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Guide", style = MaterialTheme.typography.headlineMedium, color = PhoneColors.Txt)
                Row {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = PhoneColors.Txt2)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PhoneColors.Txt2)
                    }
                }
            }

            GuideFilterRow(
                state = state,
                onOpenTypePicker = { showTypePicker = true },
                onOpenGenrePicker = { showGenrePicker = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (!state.isLoading && state.channels.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No channels yet", color = PhoneColors.Txt2)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.visibleChannels, key = { it.id }) { channel ->
                        ChannelRow(
                            channel = channel,
                            nowMinutes = state.nowMinutes,
                            onClick = { onWatch(channel.id) },
                            onOpenMenu = { selectedChannel = channel },
                            isCurrent = channel.id == state.focusChannelId,
                        )
                    }
                }
            }
        }

        if (showTypePicker) {
            GuideTypeFilterDialog(
                state = state,
                onTypeSelected = viewModel::onTypeSelected,
                onDismiss = { showTypePicker = false },
            )
        }
        if (showGenrePicker) {
            GuideGenreFilterDialog(
                state = state,
                onGenreSelected = viewModel::onGenreSelected,
                onDismiss = { showGenrePicker = false },
            )
        }

        selectedChannel?.let { channel ->
            ChannelContextSheet(
                channel = channel,
                items = listOf(
                    ChannelMenuItem("Watch now") { onWatch(channel.id); selectedChannel = null },
                    ChannelMenuItem("View full schedule") {
                        onOpenLineup(channel.id)
                        selectedChannel = null
                    },
                    ChannelMenuItem("Add to favourites") {
                        viewModel.toggleFavourite(channel.id)
                        selectedChannel = null
                    },
                ),
                onDismiss = { selectedChannel = null },
            )
        }
    }
}
