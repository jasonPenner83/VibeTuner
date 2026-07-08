package com.jpenner.vibetuner.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpenner.vibetuner.ui.components.TopBar
import com.jpenner.vibetuner.ui.components.TopTab
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.Dimens
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.ui.components.ContextSideSheet
import com.jpenner.vibetuner.ui.components.ChannelMenuItem

import androidx.compose.ui.unit.dp


@Composable
fun GuideScreen(
    onWatch: (channelId: String) -> Unit,
    onOpenInfo: (programId: String) -> Unit,
    onOpenLineup: (channelId: String) -> Unit,
    onNavigate: (TopTab) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAddons: () -> Unit,
    viewModel: GuideViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }   // re-entering picks up profile/lineup changes

    var showTypePicker by remember { mutableStateOf(false) }
    var showGenrePicker by remember { mutableStateOf(false) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }

    AerialCanvas {
        Box(Modifier.fillMaxSize().background(AerialColors.Bg)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    selected = TopTab.Guide,
                    onSelect = onNavigate,
                    clock = state.clock,
                    onSettings = onOpenSettings,
                    onProfile = onOpenProfile,
                )

                if (!state.isLoading && state.channels.isEmpty()) {
                    com.jpenner.vibetuner.ui.components.EmptyLineupScreen(
                        onOpenAddons = onOpenAddons,
                        modifier = Modifier.weight(1f),
                    )
                    return@Column
                }

                // Detail of whichever cell currently holds focus.
                FocusedProgramPreview(
                    program = state.focusedProgram,
                    channel = state.focusedChannel,
                    onWatch = { state.focusedChannel?.let { onWatch(it.id) } },
                    onInfo = { state.focusedProgram?.let { onOpenInfo(it.id) } },
                    modifier = Modifier
                        .height(284.dp)
                        .padding(horizontal = Dimens.SafeArea, vertical = 6.dp),
                )

                GuideFilterRow(
                    state = state,
                    onOpenTypePicker = { showTypePicker = true },
                    onOpenGenrePicker = { showGenrePicker = true },
                    modifier = Modifier.padding(horizontal = Dimens.SafeArea, vertical = 4.dp),
                )

                NowNextList(
                    state = state,
                    onProgramFocused = viewModel::onProgramFocused,
                    onProgramClick = onOpenInfo,
                    onChannelMenu = { selectedChannel = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Dimens.SafeArea, vertical = 16.dp),
                )
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
                ContextSideSheet(
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
                        ChannelMenuItem("Channel info") { selectedChannel = null /* TODO */ },
                        ChannelMenuItem("Hide channel") { selectedChannel = null /* TODO */ },
                    ),
                    onDismiss = { selectedChannel = null },
                )
            }

        }
    }
}