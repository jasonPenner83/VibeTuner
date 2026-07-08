package com.jpenner.vibetuner.phone.ui.screens.lineup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors
import com.jpenner.vibetuner.ui.screens.lineup.DayLineupViewModel
import com.jpenner.vibetuner.ui.screens.lineup.SlotStatus

/** Full-screen host for the Guide entry point ("View full schedule"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayLineupScreen(
    channelId: String,
    onBack: () -> Unit,
    viewModel: DayLineupViewModel,
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsState()
    Scaffold(
        containerColor = PhoneColors.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.channel?.name ?: "", fontWeight = FontWeight.SemiBold)
                        Text(
                            "CH ${state.channel?.number ?: ""} · Today · ${state.dateLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PhoneColors.Txt3,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PhoneColors.Txt2)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PhoneColors.Bg,
                    titleContentColor = PhoneColors.Txt,
                ),
            )
        },
    ) { insets ->
        DayLineupContent(
            channelId = channelId,
            viewModel = viewModel,
            modifier = Modifier.padding(insets),
        )
    }
}

/**
 * The day timeline: "earlier / NOW" strip over the slot list. Shared by
 * [DayLineupScreen] and [DayLineupSheet]. Reloads on entry (the VM is
 * activity-scoped) and opens scrolled to the live slot.
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
    LaunchedEffect(state.isLoading, state.channel?.id) {
        if (!state.isLoading && state.slots.isNotEmpty()) listState.scrollToItem(state.focusIndex)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (state.earlierLabel.isEmpty()) "" else "▲ ${state.earlierLabel}",
                fontSize = 11.sp, color = PhoneColors.Txt3,
            )
            Text(
                "NOW ${state.clock}",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PhoneColors.Accent,
            )
        }

        if (!state.isLoading && state.slots.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No schedule available", color = PhoneColors.Txt2)
            }
        } else {
            val nextIndex = state.slots.indexOfFirst { it.status == SlotStatus.Upcoming }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(
                    state.slots,
                    key = { _, s -> "${s.program.id}-${s.program.startMinutes}" },
                ) { index, slot ->
                    LineupTimelineRow(slot = slot, isNext = index == nextIndex)
                }
            }
        }
    }
}

/** Full-height player sheet ("Schedule" button): same content over playback. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayLineupSheet(
    channelId: String,
    viewModel: DayLineupViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PhoneColors.Bg,
    ) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    state.channel?.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = PhoneColors.Txt,
                )
                Text(
                    "CH ${state.channel?.number ?: ""} · Today · ${state.dateLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PhoneColors.Txt3,
                )
            }
            Spacer(Modifier.height(8.dp))
            DayLineupContent(
                channelId = channelId,
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
