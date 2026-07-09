package com.jpenner.vibetuner.ui.screens.guide

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.ui.components.ChannelRowHeader
import com.jpenner.vibetuner.ui.theme.Dimens
import com.jpenner.vibetuner.ui.components.ContextSideSheet
import com.jpenner.vibetuner.ui.components.ChannelMenuItem
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester

/**
 * One channel as a Now / Next row. No timeline, no horizontal scroll:
 * a fixed header, a focusable "Now Playing" card that fills the middle,
 * and a compact "Up Next" card on the right.
 */
@Composable
fun NowNextRow(
    channel: Channel,
    nowMinutes: Int,
    onClick: (programId: String) -> Unit,
    onFocused: () -> Unit,
    onOpenMenu: () -> Unit,
    isFavourite: Boolean = false,
    requestFocusOnAppear: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val now = channel.nowPlaying(nowMinutes)
    val next = channel.nextUp(nowMinutes)

    val nowFocus = remember { FocusRequester() }

    // Fires once when this row first appears already matching the tuned
    // channel (e.g. scrolled into view by NowNextList) — not on later
    // recompositions, since the key value doesn't change afterward.
    LaunchedEffect(requestFocusOnAppear) {
        if (requestFocusOnAppear) nowFocus.requestFocus()
    }

    Row(
        modifier
            .fillMaxWidth()
            .height(Dimens.RowHeight)
            .focusGroup()
            .focusProperties {onEnter = {nowFocus.requestFocus()}},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        ChannelRowHeader(
            channel = channel,
            onClick = onOpenMenu,
            isFavourite = isFavourite,
            modifier = Modifier.width(248.dp))

        // The focusable element of the row -> opens detail, drives nothing else.
        NowPlayingCard(
            program = now,
            nowMinutes = nowMinutes,
            onClick = { now?.let { onClick(it.id) } },
            modifier = Modifier
                .weight(1f)
                .focusRequester(nowFocus)
                .onFocusChanged { if (it.isFocused) onFocused() },
        )

        UpNextCard(program = next, modifier = Modifier.width(300.dp))
    }
}