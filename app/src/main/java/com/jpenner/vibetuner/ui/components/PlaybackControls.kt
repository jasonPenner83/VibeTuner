package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.components.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import com.jpenner.vibetuner.ui.theme.AerialTypography

data class PlaybackState(
    val isPlaying: Boolean,
    val progress: Float,       // 0f..1f
    val behindLiveLabel: String,   // "-42:18"
    val atLiveEdge: Boolean,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaybackControls(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onRestart: () -> Unit,
    onJumpForward: () -> Unit,
    onOpenGuide: () -> Unit = {},
    onOpenSubtitles: () -> Unit = {},
    onOpenAudio: () -> Unit = {},
    onOpenInfo: () -> Unit = {},
    onOpenSchedule: () -> Unit = {},
    firstControlFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 56.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp)) {
        // scrubber: elapsed | track(78%) + knob + live-edge dot | LIVE
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(state.behindLiveLabel, style = AerialTypography.labelSmall, color = AerialColors.Txt2)
            ScrubBar(progress = state.progress, atLiveEdge = state.atLiveEdge,
                onSeek = onSeek, modifier = Modifier.weight(1f))
            Text("LIVE", color = AerialColors.Live)
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ControlButton(
                    Icons.Default.Replay, onRestart,
                    modifier = Modifier
                        .let { if (firstControlFocusRequester != null) it.focusRequester(firstControlFocusRequester) else it }
                        .let { if (upFocusRequester != null) it.focusProperties { up = upFocusRequester } else it },
                )
                ControlButton(Icons.Default.Forward30, onJumpForward)
                ControlButton(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onPlayPause,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PillButton("CC Subtitles", onOpenSubtitles)
                PillButton("Audio", onOpenAudio)
                PillButton("Info", onOpenInfo)
                PillButton("Schedule", onOpenSchedule)
                PillButton("Guide", onOpenGuide)
            }
        }
    }
}