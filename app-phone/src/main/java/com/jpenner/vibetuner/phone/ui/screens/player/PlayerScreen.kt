package com.jpenner.vibetuner.phone.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors
import com.jpenner.vibetuner.ui.screens.player.PlayerViewModel
import com.jpenner.vibetuner.ui.screens.player.rememberPlayer

/**
 * Touch counterpart of the TV app's PlayerScreen (ui/screens/player/PlayerScreen.kt
 * in :app). Reuses [PlayerViewModel]/[rememberPlayer] as-is; the whole D-pad
 * `onPreviewKeyEvent` input model (channel-zap on Up/Down, Enter short/long-press)
 * has no touch equivalent and is replaced outright: tap the video to toggle chrome,
 * on-screen play/pause, and a bottom sheet (via [switcherOpen]) for channel-switching
 * instead of in-place D-pad zapping.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel?,
    program: Program?,
    streamUrl: String?,
    channels: List<Channel>,
    onExit: () -> Unit,
    onZap: (String) -> Unit,
    viewModel: PlayerViewModel = viewModel(),
) {
    if (streamUrl.isNullOrBlank()) {
        StreamUnavailable(onBack = onExit)
        return
    }

    val state by viewModel.state.collectAsState()

    val player = rememberPlayer(
        streamUrl = streamUrl,
        program = program,
        onBuffering = viewModel::setBuffering,
        onError = viewModel::setError,
    )

    LaunchedEffect(channel, program) { viewModel.open(channel, program) }

    var keepScreenOn by remember { mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(player) {
        fun refresh() {
            keepScreenOn = player.playWhenReady &&
                (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING)
            viewModel.setPlaying(player.playWhenReady)
        }
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = refresh()
            override fun onPlaybackStateChanged(playbackState: Int) = refresh()
        }
        player.addListener(listener)
        refresh()
        onDispose { player.removeListener(listener) }
    }

    BackHandler { onExit() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // 1) Video surface — tap toggles chrome.
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { it.keepScreenOn = keepScreenOn },
            onRelease = { it.player = null },
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = viewModel::showControls,
                ),
        )

        AnimatedVisibility(visible = state.controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            PlayerChrome(
                channel = state.channel,
                program = state.program,
                isPlaying = state.isPlaying,
                progress = state.progress,
                onBack = onExit,
                onPlayPause = {
                    if (player.playWhenReady) player.pause() else player.play()
                    viewModel.showControls()
                },
                onOpenSwitcher = viewModel::openSwitcher,
            )
        }

        if (state.isBuffering) {
            CircularProgressIndicator(color = PhoneColors.Accent, modifier = Modifier.align(Alignment.Center))
        }
        state.error?.let {
            Text(
                it, color = PhoneColors.Live,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 16.dp, end = 16.dp),
            )
        }

        if (state.switcherOpen) {
            val now = remember { java.time.LocalTime.now() }
            ChannelSwitcherSheet(
                channels = channels,
                nowMinutes = now.hour * 60 + now.minute,
                onPick = { id -> viewModel.closeSwitcher(); onZap(id) },
                onDismiss = viewModel::closeSwitcher,
            )
        }
    }
}

@Composable
private fun PlayerChrome(
    channel: Channel?,
    program: Program?,
    isPlaying: Boolean,
    progress: Float,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onOpenSwitcher: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.6f),
                0.25f to Color.Transparent,
                0.7f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.7f),
            ),
        ),
    ) {
        // Top bar: back + channel identity + switcher.
        Row(
            Modifier.fillMaxWidth().align(Alignment.TopStart).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    channel?.name ?: "", color = Color.White, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    program?.title ?: "", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onOpenSwitcher) {
                Icon(Icons.Default.List, contentDescription = "Switch channel", tint = Color.White)
            }
        }

        // Bottom transport: play/pause + progress.
        Column(Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(16.dp)) {
            Box(
                Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f)),
            ) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp)).background(PhoneColors.Accent),
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
            IconButton(onClick = onPlayPause, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

@Composable
private fun StreamUnavailable(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Stream Unavailable", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text(
                "No playable URL found in your addon stack.", color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
            androidx.compose.material3.Button(onClick = onBack) {
                Text("Return to Guide")
            }
        }
    }
}
