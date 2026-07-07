package com.jpenner.vibetuner.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.ui.components.ChannelLogo
import com.jpenner.vibetuner.ui.components.ChannelSwitcherCard
import com.jpenner.vibetuner.ui.components.IconChipButton
import com.jpenner.vibetuner.ui.components.LiveDot
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography
import com.jpenner.vibetuner.ui.theme.Dimens

/** Top info bar: back chip + channel identity + now-playing title + clock. */
@Composable
fun PlayerTopBar(
    channel: Channel?,
    program: Program?,
    clock: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent),
                ),
            )
            .padding(horizontal = Dimens.SafeArea, vertical = 28.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconChipButton(
                Icons.AutoMirrored.Filled.ArrowBack, onBack,
                modifier = Modifier
                    .let { if (backFocusRequester != null) it.focusRequester(backFocusRequester) else it }
                    .let { if (downFocusRequester != null) it.focusProperties { down = downFocusRequester } else it },
            )

            if (channel != null) {
                ChannelLogo(channel, size = 54.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        "CH ${channel.number}  ·  ${channel.name}",
                        style = AerialTypography.labelSmall,
                        color = AerialColors.Txt,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LiveDot()
                        Text(
                            program?.title ?: "Live",
                            style = AerialTypography.titleMedium,
                            maxLines = 1,
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            Text(clock, style = AerialTypography.titleMedium, color = AerialColors.Txt2)
        }
    }
}

/** Up/Down channel switcher: a focusable row of channel cards pinned bottom. */
@Composable
fun ChannelSwitcherOverlay(
    current: Channel?,
    channels: List<Channel>,
    nowMinutes: Int,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    // Fall back to the current channel alone if the caller has no full list yet.
    val list = channels.ifEmpty { listOfNotNull(current) }
    Column(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                ),
            )
            .padding(horizontal = Dimens.SafeArea, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "CHANNELS",
            color = AerialColors.Txt,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(list, key = { it.id }) { ch ->
            ChannelSwitcherCard(
                    channel = ch,
                    nowMinutes = nowMinutes,
                    onClick = { onPick(ch.id) },
                    modifier = if (ch.id == current?.id && focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else Modifier,
                )
            }
        }
    }
}

/** Centre play/pause indicator shown while paused. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CenterPlayBadge(isPlaying: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(96.dp)
            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = AerialColors.Txt,
            modifier = Modifier.size(48.dp),
        )
    }
}

/** Buffering spinner. */
@Composable
fun BufferingSpinner(modifier: Modifier = Modifier) {
    androidx.compose.material3.CircularProgressIndicator(
        color = AerialColors.Accent,
        modifier = modifier.size(56.dp),
    )
}
