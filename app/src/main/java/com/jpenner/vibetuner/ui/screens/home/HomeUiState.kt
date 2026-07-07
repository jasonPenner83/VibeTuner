package com.jpenner.vibetuner.ui.screens.home

import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.data.model.ContentItem

/** A titled horizontal row of tiles on the home screen. */
data class Rail(
    val id: String,
    val title: String,
    val items: List<ContentItem>,
)

/** Everything HomeScreen renders, in one immutable snapshot. */
data class HomeUiState(
    val featuredChannel: Channel? = null,
    val featuredProgram: Program? = null,   // what's airing on the hero channel
    val rails: List<Rail> = emptyList(),
    val clock: String = "",
    val isLoading: Boolean = true,
)