package com.jpenner.vibetuner.ui.screens.player

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.jpenner.vibetuner.data.model.Program

/**
 * Builds a fully-configured ExoPlayer tied to the composition + lifecycle:
 * pauses in onStop, releases when the screen leaves composition.
 *
 * Carries the legacy player's proven playback setup — decoder fallback,
 * extension renderers, an HTTP data source with the Android-TV user agent and
 * generous timeouts, looping playback, and the linear "live" time-shift seek
 * (resume mid-program based on real-world elapsed time since [Program.startTimeMillis]).
 */
@OptIn(UnstableApi::class)
@Composable
fun rememberPlayer(
    streamUrl: String,
    program: Program?,
    onBuffering: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onReady: () -> Unit = {},
): ExoPlayer {
    val context = LocalContext.current

    val player = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setPreferredAudioLanguage("en"))
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30000, 60000, 2500, 5000)
            .build()

        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ALL
            }
    }

    // Surface buffering / error and run the one-shot live time-shift seek.
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            private var seeked = false
            private var readyFired = false

            override fun onPlayerError(error: PlaybackException) {
                val cause = error.cause
                val msg = "Playback Error: ${error.errorCodeName} (${error.errorCode})\n" +
                    "Cause: ${cause?.message ?: "Unknown"}"
                Log.e("VibeTuner Player", msg, error)
                onError(msg)
            }

            override fun onPlaybackStateChanged(state: Int) {
                onBuffering(state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    if (!seeked) {
                        seeked = true
                        val durationMs = player.duration
                        if (program != null && durationMs > 0) {
                            val elapsed = System.currentTimeMillis() - program.startTimeMillis
                            if (elapsed > 0) player.seekTo(elapsed % durationMs)
                        }
                    }
                    // First frame is buffered and decodable — let the tune-in overlay lift.
                    if (!readyFired) {
                        readyFired = true
                        onReady()
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // (Re)point the player whenever the stream URL changes.
    LaunchedEffect(streamUrl) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("VibeTuner/1.0 (Android TV)")
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(streamUrl))

        onError(null)
        player.setMediaSource(mediaSource)
        player.prepare()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }
    return player
}
