package com.jpenner.vibetuner.phone.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.phone.ui.components.PhoneCard
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors

/**
 * Touch replacement for the TV player's D-pad channel switcher (long-press
 * Enter -> ChannelSwitcherOverlay in ui/screens/player/PlayerOverlays.kt): a
 * tap-to-open bottom sheet listing every channel's now-playing program.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSwitcherSheet(
    channels: List<Channel>,
    nowMinutes: Int,
    current: Channel?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    val currentIndex = channels.indexOfFirst { it.id == current?.id }
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) listState.scrollToItem(currentIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = PhoneColors.Raised,
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(channels, key = { it.id }) { channel ->
                val now = channel.nowPlaying(nowMinutes)
                val isCurrent = channel.id == current?.id
                PhoneCard(
                    onClick = { onPick(channel.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = if (isCurrent) BorderStroke(2.dp, PhoneColors.Accent) else BorderStroke(1.dp, PhoneColors.Line),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(channel.number, color = PhoneColors.Txt3, fontSize = 12.sp)
                        Column(Modifier.weight(1f)) {
                            Text(channel.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis, color = PhoneColors.Txt)
                            Text(
                                now?.title ?: "No programme information", fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, color = PhoneColors.Txt2,
                            )
                        }
                    }
                }
            }
        }
    }
}
