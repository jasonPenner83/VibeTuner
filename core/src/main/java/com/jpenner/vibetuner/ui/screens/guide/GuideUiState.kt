package com.jpenner.vibetuner.ui.screens.guide

import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.ChannelType
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.data.model.type

/** Everything GuideScreen needs to render, in one immutable snapshot. */
data class GuideUiState(
    val channels: List<Channel> = emptyList(),
    val gridStartMinutes: Int = 0,     // left edge of the timeline
    val nowMinutes: Int = 0,           // drives the now-line position
    val clock: String = "",            // formatted top-bar clock
    val typeFilter: ChannelType? = null,   // null == "All Types"
    val genreFilter: Category? = null,     // null == "All Genres"
    val focused: FocusedCell = FocusedCell(),
    val isLoading: Boolean = true,
    // The channel currently playing in the Player, if any — the Guide's list
    // scrolls to and focuses this row on every entry (see NowNextList/GuideScreen).
    val focusChannelId: String? = null,
) {
    val visibleChannels: List<Channel>
        get() = channels.filter {
            (typeFilter == null || it.type == typeFilter) &&
            (genreFilter == null || it.category == genreFilter)
        }

    /** Types with at least one loaded channel, in declaration order. ADULT only
     *  appears here when the active profile actually has adult channels loaded. */
    val availableTypes: List<ChannelType>
        get() = ChannelType.entries.filter { t -> channels.any { it.type == t } }

    /** Genres present among channels matching [typeFilter] (or all channels if
     *  [typeFilter] is null), in Category's declared order. */
    val availableGenres: List<Category>
        get() {
            val scoped = if (typeFilter == null) channels else channels.filter { it.type == typeFilter }
            return Category.entries.filter { g -> scoped.any { it.category == g } }
        }

    /** Sets the Type filter and clears Genre if it's no longer among [availableGenres]
     *  under the new Type — avoids landing on an empty guide with a stuck genre. */
    fun withTypeFilter(type: ChannelType?): GuideUiState {
        val next = copy(typeFilter = type, focused = FocusedCell())
        return if (genreFilter == null || next.availableGenres.contains(genreFilter)) next
        else next.copy(genreFilter = null)
    }

    fun withGenreFilter(genre: Category?): GuideUiState =
        copy(genreFilter = genre, focused = FocusedCell())

    val focusedChannel: Channel?
        get() = visibleChannels.getOrNull(focused.channel)

    // The Now/Next list surfaces the *now-playing* program per channel (not
    // programs[0]), so the preview must resolve the same one — the focused
    // program index is meaningless in this layout.
    val focusedProgram: Program?
        get() = focusedChannel?.nowPlaying(nowMinutes)
}

/** Which guide cell currently holds D-pad focus. */
data class FocusedCell(val channel: Int = 0, val program: Int = 0)
