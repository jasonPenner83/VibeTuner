package com.jpenner.vibetuner.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
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
import com.jpenner.vibetuner.ui.components.*
import com.jpenner.vibetuner.ui.theme.Dimens.DesignCanvasWidth
import com.jpenner.vibetuner.ui.theme.Dimens
import java.time.LocalTime

private const val ENTER_LONG_PRESS_MS = 500L

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel?,
    program: Program?,
    streamUrl: String?,
    channels: List<Channel>,
    onExit: () -> Unit,
    onZap: (String) -> Unit,
    onOpenGuide: () -> Unit,
    // Fired once when the video has buffered its first frame — the tune-in
    // overlay keys off this to lift itself.
    onFirstFrameReady: () -> Unit = {},
    // While false the player is covered by the tune-in overlay: it holds focus
    // but swallows D-pad input so nothing happens behind the overlay.
    interactive: Boolean = true,
    viewModel: PlayerViewModel = viewModel(),
) {
    if (streamUrl.isNullOrBlank()) {
        StreamUnavailable(onBack = onExit)
        return
    }


    val state by viewModel.state.collectAsState()

    // Map the 1920-px design canvas onto the real screen width.
    val base = LocalDensity.current
    val screenWidthPx = base.density * LocalConfiguration.current.screenWidthDp
    val designDensity = Density(
        density = screenWidthPx / DesignCanvasWidth,
        fontScale = base.fontScale,
    )

    val focusRequester = remember { FocusRequester() }
    val keyScope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressHandled by remember { mutableStateOf(false) }
    val topBarFocusRequester = remember { FocusRequester() }
    val chromeFocusRequester = remember { FocusRequester() }
    val switcherFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.chromeFocused) {
        if (state.chromeFocused) runCatching { chromeFocusRequester.requestFocus() }
    }
    LaunchedEffect(state.switcherOpen) {
        if (state.switcherOpen) runCatching { switcherFocusRequester.requestFocus() }
    }
    LaunchedEffect(state.chromeFocused, state.switcherOpen) {
        if (!state.chromeFocused && !state.switcherOpen) {
            runCatching { focusRequester.requestFocus() }
        }
    }
    val player = rememberPlayer(
        streamUrl = streamUrl,
        program = program,
        onBuffering = viewModel::setBuffering,
        onError = viewModel::setError,
        onReady = onFirstFrameReady,
    )

    LaunchedEffect(channel, program) { viewModel.open(channel, program) }

    // Hold the screen awake only while video is actually playing (or briefly
    // rebuffering); paused/idle/ended lets the screensaver protect the panel.
    var keepScreenOn by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        fun refresh() {
            keepScreenOn = isActivelyPlaying(player.playWhenReady, player.playbackState)
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

    // Back exits the player at baseline. Closing the switcher/chrome is handled
    // directly in onPreviewKeyEvent below, not here: once either overlay holds
    // real Compose focus on a child Surface, the system BackHandler dispatch
    // becomes unreliable (observed on-device: it fires only every other press,
    // presumably because a focused tv-material3 Surface interferes with
    // Android's Down/Up back-tracking) — handling it as a plain key event,
    // like every other D-pad key here, sidesteps that entirely.
    BackHandler { onExit() }
    CompositionLocalProvider(LocalDensity provides designDensity) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            // State-conditional key handling: baseline zaps channels directly,
            // Enter/DirectionCenter distinguishes short press (overlay) from
            // long press (channel switcher), and default focus navigation
            // takes over once an overlay holds focus.
            .onPreviewKeyEvent { event ->
                // Close the switcher/chrome directly, ahead of the !interactive
                // gate, so Back can still cancel the tune-in overlay. See the
                // BackHandler comment above for why this isn't done there.
                if (event.key == Key.Back) {
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    return@onPreviewKeyEvent when {
                        state.switcherOpen -> { viewModel.closeSwitcher(); true }
                        state.chromeFocused -> { viewModel.closeChrome(); true }
                        else -> false // baseline: let BackHandler exit
                    }
                }

                // Covered by the tune-in overlay: hold focus but swallow all input.
                if (!interactive) return@onPreviewKeyEvent event.type == KeyEventType.KeyDown

                val isBaseline = !state.chromeFocused && !state.switcherOpen
                val isEnter = event.key == Key.DirectionCenter || event.key == Key.Enter

                // Enter/DirectionCenter at baseline: short press opens the standard
                // overlay, a >=500ms hold opens the channel switcher instead. The
                // KeyUp that completes the press is consumed whenever we're the ones
                // tracking it (longPressJob != null), not just while still baseline —
                // openSwitcher() already flips isBaseline false before that KeyUp
                // arrives, and letting it fall through would "click" whatever the
                // switcher just focused (re-tuning the current channel).
                if (isEnter) {
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (isBaseline && event.nativeKeyEvent.repeatCount == 0) {
                                longPressHandled = false
                                longPressJob = keyScope.launch {
                                    delay(ENTER_LONG_PRESS_MS)
                                    longPressHandled = true
                                    viewModel.openSwitcher()
                                }
                                return@onPreviewKeyEvent true
                            }
                            // Auto-repeat KeyDowns for a press we're already tracking:
                            // swallow them so they don't fall through to showControls()
                            // and flash the chrome mid-hold. Don't restart the timer.
                            if (longPressJob != null) return@onPreviewKeyEvent true
                        }
                        KeyEventType.KeyUp -> {
                            if (longPressJob != null) {
                                longPressJob?.cancel()
                                longPressJob = null
                                if (!longPressHandled) viewModel.openChrome()
                                return@onPreviewKeyEvent true
                            }
                        }
                        else -> {}
                    }
                }

                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                // Never reveal the standard overlay while the switcher is open — the
                // two are visually independent and would overlap at the bottom of
                // the screen (e.g. Left/Right navigating the switcher's channel row
                // would otherwise re-trigger controlsVisible on every keypress).
                if (!state.switcherOpen) viewModel.showControls()
                when (event.key) {
                    Key.DirectionUp, Key.DirectionDown -> when {
                        isBaseline -> {
                            val delta = if (event.key == Key.DirectionUp) -1 else 1
                            adjacentChannelId(channels, state.channel?.id, delta)?.let(onZap)
                            true
                        }
                        state.switcherOpen -> true // swallow: keep focus on the channel row
                        else -> false // chrome-focused: default focus navigation takes over
                    }

                    else -> false
                }
            },
    ) {
        // 1) Video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { it.keepScreenOn = keepScreenOn },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize(),
        )

        // 2) Auto-hiding chrome (fades as one layer)
        AnimatedVisibility(visible = state.controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            PlayerChrome(
                state = state,
                onBack = onExit,
                onPlayPause = {
                    if (player.playWhenReady) player.pause() else player.play()
                    viewModel.showControls()
                },
                onSeek = { /* scrubbing is display-only for now */ },
                onOpenGuide = onOpenGuide,
                topBarFocusRequester = topBarFocusRequester,
                chromeFocusRequester = chromeFocusRequester,
            )
        }

        // 3) Centre play indicator while paused
        AnimatedVisibility(
            visible = state.controlsVisible && !state.isPlaying,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(), exit = fadeOut(),
        ) { CenterPlayBadge(isPlaying = state.isPlaying) }

        // 4) Channel switcher overlay (Up/Down)
        AnimatedVisibility(
            visible = state.switcherOpen,
            modifier = Modifier.align(Alignment.BottomStart),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            ChannelSwitcherOverlay(
                current = state.channel,
                channels = channels,
                nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute },
                onPick = onZap,
                onDismiss = viewModel::closeSwitcher,
                focusRequester = switcherFocusRequester,
            )
        }

        // 5) Buffering / error states
        if (state.isBuffering) BufferingSpinner(Modifier.align(Alignment.Center))
        state.error?.let {
            StatusBanner(
                "Playback error", it, StatusKind.Error,
                Modifier.align(Alignment.TopCenter).padding(top = 40.dp),
            )
        }
    }
}
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/**
 * True while playback is actively holding the screen: user wants playback
 * ([playWhenReady]) and the player is READY or merely rebuffering. IDLE
 * (error) and ENDED never hold the screen, even with playWhenReady set.
 */
internal fun isActivelyPlaying(playWhenReady: Boolean, playbackState: Int): Boolean =
    playWhenReady &&
        (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING)

/**
 * Adjacent channel id for D-pad Up/Down zapping, wrapping around the ends of
 * [channels]. Returns null if [currentId] isn't found or the list is empty.
 */
internal fun adjacentChannelId(channels: List<Channel>, currentId: String?, delta: Int): String? {
    if (channels.isEmpty()) return null
    val idx = channels.indexOfFirst { it.id == currentId }
    if (idx == -1) return null
    return channels[(idx + delta).mod(channels.size)].id
}

/** The three-zone chrome: top info bar, scrubber + transport (PlaybackControls). */
@Composable
private fun PlayerChrome(
    state: PlayerUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenGuide: () -> Unit,
    topBarFocusRequester: FocusRequester,
    chromeFocusRequester: FocusRequester,
) {
    Box(Modifier.fillMaxSize()) {
        PlayerTopBar(
            channel = state.channel, program = state.program, clock = state.clock,
            onBack = onBack, modifier = Modifier.align(Alignment.TopStart),
            backFocusRequester = topBarFocusRequester,
            downFocusRequester = chromeFocusRequester,
        )
        PlaybackControls(
            state = PlaybackState(
                isPlaying = state.isPlaying,
                progress = state.progress,
                behindLiveLabel = state.behindLiveLabel,
                atLiveEdge = state.atLiveEdge,
            ),
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onRestart = { onSeek(0f) },
            onJumpForward = { onSeek((state.progress + 0.02f).coerceAtMost(1f)) },
            onOpenGuide = onOpenGuide,
            firstControlFocusRequester = chromeFocusRequester,
            upFocusRequester = topBarFocusRequester,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Fallback shown when no playable URL was resolved. */
@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
private fun StreamUnavailable(onBack: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.tv.material3.Text(
                "Stream Unavailable", color = Color.White, fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            androidx.tv.material3.Text(
                "No playable URL found in your addon stack.",
                color = Color.Gray, fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(32.dp))
            androidx.tv.material3.Button(onClick = onBack) {
                androidx.tv.material3.Text("Return to Guide")
            }
        }
    }
}
