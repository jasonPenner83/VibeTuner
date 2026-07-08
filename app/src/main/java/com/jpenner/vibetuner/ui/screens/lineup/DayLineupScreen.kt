package com.jpenner.vibetuner.ui.screens.lineup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography
import com.jpenner.vibetuner.ui.theme.Dimens

/** Full-screen host for the Guide entry point ("View full schedule"). */
@Composable
fun DayLineupScreen(
    channelId: String,
    onBack: () -> Unit,
    viewModel: DayLineupViewModel,
) {
    BackHandler { onBack() }
    AerialCanvas {
        Box(Modifier.fillMaxSize().background(AerialColors.Bg)) {
            DayLineupContent(channelId = channelId, viewModel = viewModel)
        }
    }
}

/**
 * The day timeline itself — channel header, "earlier" affordance, the slot
 * list, and the D-pad legend. Shared by [DayLineupScreen] and the player's
 * Schedule overlay. Reloads on entry (the VM is activity-scoped) and opens
 * scrolled + focused on the live slot.
 */
@Composable
fun DayLineupContent(
    channelId: String,
    viewModel: DayLineupViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(channelId) { viewModel.load(channelId) }

    val listState = rememberLazyListState()
    val liveFocus = remember { FocusRequester() }

    // Open on what's live (or next up): scroll there, then land D-pad focus on it.
    LaunchedEffect(state.isLoading, state.channel?.id) {
        if (!state.isLoading && state.slots.isNotEmpty()) {
            listState.scrollToItem(state.focusIndex)
            runCatching { liveFocus.requestFocus() }
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = Dimens.SafeArea)) {
        LineupHeader(
            channel = state.channel,
            dateLabel = state.dateLabel,
            clock = state.clock,
            modifier = Modifier.padding(top = 28.dp, bottom = 20.dp),
        )

        if (!state.isLoading && state.slots.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "No schedule available",
                    style = AerialTypography.titleMedium,
                    color = AerialColors.Txt3,
                )
            }
        } else {
            if (state.earlierLabel.isNotEmpty()) EarlierDivider(state.earlierLabel)
            val nextIndex = state.slots.indexOfFirst { it.status == SlotStatus.Upcoming }
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 30.dp),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(
                    state.slots,
                    key = { _, s -> "${s.program.id}-${s.program.startMinutes}" },
                ) { index, slot ->
                    LineupTimelineRow(
                        slot = slot,
                        isNext = index == nextIndex,
                        focusRequester = if (index == state.focusIndex) liveFocus else null,
                    )
                }
            }
        }

        DPadLegend(Modifier.padding(bottom = 18.dp))
    }
}

@Composable
private fun LineupHeader(
    channel: Channel?,
    dateLabel: String,
    clock: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(AerialColors.Surface)
                    .border(1.dp, AerialColors.Line, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    channel?.abbreviation ?: "",
                    style = AerialTypography.titleMedium.copy(fontSize = 17.sp),
                    fontWeight = FontWeight.Bold,
                    color = AerialColors.Txt2,
                )
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Text(
                        channel?.name ?: "",
                        style = AerialTypography.titleMedium.copy(fontSize = 26.sp),
                        fontWeight = FontWeight.Bold,
                        color = AerialColors.Txt,
                    )
                    channel?.category?.let { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(cat.color))
                            Text(cat.label.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cat.color)
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    "CH ${channel?.number ?: ""} · Full-day schedule",
                    style = AerialTypography.labelSmall,
                    color = AerialColors.Txt3,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(dateLabel, style = AerialTypography.labelSmall, color = AerialColors.Txt2, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(4.dp))
            Row {
                Text("Now ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AerialColors.Txt)
                Text(clock, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AerialColors.Accent)
            }
        }
    }
}

@Composable
private fun EarlierDivider(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("▲ $label", fontSize = 12.sp, color = AerialColors.Txt3)
        Box(Modifier.weight(1f).height(1.dp).background(AerialColors.Line.copy(alpha = 0.6f)))
    }
}

@Composable
private fun DPadLegend(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        LegendItem("▲▼", "Browse the day")
        LegendItem("BACK", "Return")
    }
}

@Composable
private fun LegendItem(key: String, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(key, fontSize = 12.sp, color = AerialColors.Txt2)
        Text(label, fontSize = 12.sp, color = AerialColors.Txt3)
    }
}
