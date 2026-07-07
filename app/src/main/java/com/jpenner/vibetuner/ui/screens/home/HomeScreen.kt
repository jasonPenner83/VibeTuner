package com.jpenner.vibetuner.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpenner.vibetuner.ui.components.TopTab
import com.jpenner.vibetuner.ui.components.ContentRail
import com.jpenner.vibetuner.data.model.ContentItem
import com.jpenner.vibetuner.data.model.PlaybackTarget
import com.jpenner.vibetuner.ui.components.TopBar
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.Dimens

@Composable
fun HomeScreen(
    onNavigate: (TopTab) -> Unit,
    onOpenTarget: (PlaybackTarget) -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onOpenAddons: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }   // re-entering picks up profile/lineup changes

    // D-pad focus bridge between the overlaid TopBar and the scrolling content.
    val contentFocus = remember { FocusRequester() }
    val topBarFocus = remember { FocusRequester() }

    // Netflix-style: land focus on the content (hero "Watch Live") once loaded.
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) runCatching { contentFocus.requestFocus() }
    }

    AerialCanvas {
    Box(Modifier.fillMaxSize().background(AerialColors.Bg)) {
        if (!state.isLoading && state.rails.isEmpty() && state.featuredChannel == null) {
            com.jpenner.vibetuner.ui.components.EmptyLineupScreen(onOpenAddons = onOpenAddons)
        } else {
            // The whole screen scrolls as one column; the hero is the first item
            // so it parallaxes away as the rails come up.
            LazyColumn(
                modifier = Modifier
                    .focusRequester(contentFocus)
                    .focusRestorer()
                    .focusProperties { up = topBarFocus },
                verticalArrangement = Arrangement.spacedBy(Dimens.RailGap + 8.dp),
                contentPadding = PaddingValues(bottom = Dimens.SafeArea),
            ) {
                item {
                    HomeHero(
                        channel = state.featuredChannel,
                        program = state.featuredProgram,
                        onWatch = { state.featuredChannel?.let { onOpenTarget(PlaybackTarget.WatchChannel(it.id)) } },
                        onInfo  = { state.featuredProgram?.let { onOpenTarget(PlaybackTarget.ProgramDetail(it.id)) } },
                    )
                }
                items(state.rails, key = { it.id }) { rail ->
                    ContentRail(
                        title = rail.title,
                        items = rail.items,
                        onItemClick = { item -> onOpenTarget(item.target) },
                        modifier = Modifier.padding(horizontal = Dimens.SafeArea),
                    )
                }
            }
        }

        // Top bar overlays the hero; its own scrim keeps the tabs legible.
        TopBar(
            selected = TopTab.Home,
            onSelect = onNavigate,
            clock = state.clock,
            onSettings = onSettings,
            onProfile = onProfile,
            modifier = Modifier
                .focusRequester(topBarFocus)
                .focusGroup()
                .focusProperties { down = contentFocus },
        )
    }
    }
}