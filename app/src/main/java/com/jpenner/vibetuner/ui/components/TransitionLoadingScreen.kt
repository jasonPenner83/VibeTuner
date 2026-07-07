package com.jpenner.vibetuner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.jpenner.vibetuner.data.api.StremioResolver
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.JetBrainsMonoFontFamily
import com.jpenner.vibetuner.ui.theme.SoraFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// Azure palette from the design handoff (azure-only per project convention).
private val Bg = Color(0xFF0D0F14)
private val Surface = Color(0xFF151821)
private val Raised = Color(0xFF1D212C)
private val Line = Color(0xFF2A2F3C)
private val Txt = Color(0xFFEEF1F6)
private val Txt2 = Color(0xFFA7AFBF)
private val Txt3 = Color(0xFF6B7384)
private val Accent = Color(0xFF3D9BFF)
private val Accent2 = Color(0xFF8FC7FF)
private val AccentInk = Color(0xFF06121F)
private val Glow = Color(0x803D9BFF)        // rgba(61,155,255,.5)
private val Live = Color(0xFFFF5D5D)
private val Scrim = Color(0xFF090B0F)        // rgba(9,11,15,x)

/** The stages the tune-in progresses through — mirrors the design's headline rotation. */
private val TUNE_STAGES = listOf(
    "TUNING" to "Tuning in",
    "CONNECTING" to "Connecting to the live stream",
    "BUFFERING" to "Buffering live video",
    "STARTING" to "Starting playback",
)

private val clockFormat = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Channel-transition splash shown while a channel is being tuned (Guide/Player → resolve → Player).
 *
 * The show's fanart sits quietly behind layered scrims (the "backdrop"); the foreground is the
 * Aerial channel lockup + a real-progress tune-in bar driven by [StremioResolver]. Signature is
 * unchanged so MainActivity's LOADING route needs no rewiring.
 */
@Composable
fun TransitionLoadingScreen(
    channel: Channel?,
    program: Program?,
    troubleshootLogs: MutableList<String>,
    ready: Boolean,
    onResolved: (String?) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Instantiates its own resolver safely using the local compose lifecycle context
    val resolver = remember(context) { StremioResolver(context) }

    var progress by remember { mutableFloatStateOf(0f) }
    val stageIndex by remember {
        derivedStateOf { (progress * TUNE_STAGES.size).toInt().coerceIn(0, TUNE_STAGES.lastIndex) }
    }
    // Guards onFinished so the ready path and the failure/stall timers can't double-fire.
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    // Phase 1 — resolve the stream URL, then hand off to the player behind us.
    LaunchedEffect(program) {
        if (program == null) {
            onResolved(null)
            finish()
            return@LaunchedEffect
        }

        troubleshootLogs.clear()
        troubleshootLogs.add("🔍 Initializing Resolver Engine...")

        // Creep to the "buffering" stage while the resolver works.
        val creep = launch {
            animate(0f, 0.72f, animationSpec = tween(5000, easing = LinearOutSlowInEasing)) { v, _ ->
                progress = v
            }
        }
        val result = resolver.resolveStreamUrl(program) { status -> troubleshootLogs.add(status) }
        creep.cancel()
        onResolved(result) // player mounts behind us and buffers (or shows Unavailable)

        if (result == null) {
            troubleshootLogs.add("❌ No stream found in current addon stack.")
            animate(progress, 1f, animationSpec = tween(400, easing = LinearEasing)) { v, _ -> progress = v }
            delay(1600)
            finish()
        } else {
            troubleshootLogs.add("✅ Link Locked! Buffering video…")
            // Hold at the buffering stage (the shimmer conveys activity) until the player
            // reports its first frame via [ready]; bail after a stall so we never hang.
            delay(15_000)
            finish()
        }
    }

    // Phase 2 — the video buffered its first frame: fill the bar and lift the overlay.
    LaunchedEffect(ready) {
        if (ready && !finished) {
            animate(progress, 1f, animationSpec = tween(400, easing = LinearEasing)) { v, _ -> progress = v }
            delay(400)
            finish()
        }
    }

    AerialCanvas {
        val infinite = rememberInfiniteTransition(label = "tune")
        val breathe by infinite.animateFloat(
            0.45f, 0.9f,
            infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "breathe",
        )
        val drift by infinite.animateFloat(
            1.02f, 1.1f,
            infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "drift",
        )
        val sweep by infinite.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "sweep",
        )
        val pulse by infinite.animateFloat(
            0.5f, 1f,
            infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse), label = "pulse",
        )

        // One-shot "zap in" entrance for the channel lockup.
        val zap = remember { Animatable(0.92f) }
        LaunchedEffect(Unit) { zap.animateTo(1f, tween(600)) }

        val now = remember { LocalTime.now() }
        val nowMinutes = now.hour * 60 + now.minute
        val nextUp = remember(channel, nowMinutes) {
            channel?.programs?.firstOrNull { it.startMinutes > nowMinutes }
        }
        val catColor = channel?.category?.color ?: Accent

        Box(modifier.fillMaxSize().background(Bg)) {

            // ── LAYER 1: fanart backdrop (drifts slowly), quieted by scrims ──
            Box(Modifier.fillMaxSize().background(Surface))
            val art = program?.backdropUrl ?: program?.posterUrl
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().scale(drift),
                )
            }

            // ── LAYER 2: veil + category glow + legibility scrims ──
            Box(Modifier.fillMaxSize().background(Scrim.copy(alpha = 0.5f)))
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(catColor.copy(alpha = 0.2f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(1500f, 240f),
                        radius = 1300f,
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0.22f to Scrim.copy(alpha = 0.94f),
                        0.52f to Scrim.copy(alpha = 0.6f),
                        1f to Scrim.copy(alpha = 0.32f),
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Scrim.copy(alpha = 0.85f),
                        0.2f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to Bg,
                    )
                )
            )

            // ── TOP BAR ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(top = 44.dp, start = 56.dp, end = 56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface.copy(alpha = 0.72f))
                            .border(1.dp, Line, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text("←", color = Txt2, fontSize = 20.sp) }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "NOW TUNING", color = Accent, fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 3.sp,
                        )
                        Text(
                            "Changing channel…", color = Txt3,
                            fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp,
                        )
                    }
                }
                Text(
                    now.format(clockFormat), color = Txt2, fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                )
            }

            // ── MAIN CONTENT ──
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 96.dp)
                    .width(1180.dp),
            ) {
                // channel identity lockup
                Row(
                    Modifier.scale(zap.value).alpha(zap.value),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(182.dp)
                                .scale(breathe + 0.15f)
                                .alpha(breathe)
                                .background(Brush.radialGradient(listOf(Glow, Color.Transparent)))
                        )
                        Box(
                            Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Brush.linearGradient(listOf(Accent, Accent2))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "CH", color = AccentInk.copy(alpha = 0.7f),
                                    fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp, letterSpacing = 2.sp,
                                )
                                Text(
                                    channel?.number ?: "–", color = AccentInk,
                                    fontFamily = SoraFontFamily, fontWeight = FontWeight.ExtraBold,
                                    fontSize = 74.sp, letterSpacing = (-2).sp,
                                )
                            }
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (program?.isLive == true) {
                                Row(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Live)
                                        .padding(horizontal = 11.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    Box(Modifier.size(7.dp).alpha(pulse).clip(RoundedCornerShape(50)).background(Color.White))
                                    Text(
                                        "LIVE", color = Color.White, fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp, letterSpacing = 0.6.sp,
                                    )
                                }
                            }
                            channel?.category?.let {
                                Text(
                                    it.label.uppercase(), color = catColor,
                                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.3.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            channel?.name ?: "Tuning…", color = Txt, fontFamily = SoraFontFamily,
                            fontWeight = FontWeight.ExtraBold, fontSize = 50.sp, letterSpacing = (-1.2).sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "1080p · Live · HDR", color = Txt2,
                            fontFamily = JetBrainsMonoFontFamily, fontSize = 18.sp,
                        )
                    }
                }

                // program now / next
                Spacer(Modifier.height(44.dp))
                Column(Modifier.width(820.dp)) {
                    Text(
                        program?.title ?: "", color = Txt, fontFamily = SoraFontFamily,
                        fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        program?.rating?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it, color = Txt2, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Line, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                        program?.displayTimeSlot?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it, color = Accent, fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                            )
                        }
                        program?.episodeTitle?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = Txt3, fontSize = 16.sp)
                        }
                    }
                    if (nextUp != null) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "UP NEXT", color = Txt2, fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                            )
                            Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(Txt3))
                            Text(
                                nextUp.title, color = Txt3, fontFamily = JetBrainsMonoFontFamily, fontSize = 15.sp,
                            )
                        }
                    }
                }

                // progress
                Spacer(Modifier.height(52.dp))
                Column(Modifier.width(960.dp)) {
                    // animated stage headline
                    Box(Modifier.height(34.dp), contentAlignment = Alignment.CenterStart) {
                        AnimatedContent(
                            targetState = stageIndex,
                            transitionSpec = {
                                (fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 })
                                    .togetherWith(fadeOut(tween(200)))
                            },
                            label = "stage",
                        ) { idx ->
                            val (tag, line) = TUNE_STAGES[idx]
                            val headline = if (idx == 0 && channel != null) "Tuning to channel ${channel.number}" else line
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Box(Modifier.size(11.dp).clip(RoundedCornerShape(50)).background(Accent))
                                Text(
                                    headline, color = Txt, fontFamily = SoraFontFamily,
                                    fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.3).sp,
                                )
                                Text(
                                    tag, color = Txt3, fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 2.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    // determinate bar with sweeping shimmer
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Raised)
                            .border(1.dp, Line, RoundedCornerShape(6.dp)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.horizontalGradient(listOf(Accent, Accent2)))
                        )
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.38f)
                                .offset(x = 960.dp * (sweep * 1.8f - 0.4f))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent)
                                    )
                                )
                        )
                    }
                    Spacer(Modifier.height(24.dp))

                    // stage steps + percent
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            TUNE_STAGES.forEachIndexed { i, (tag, _) ->
                                val done = i < stageIndex
                                val active = i == stageIndex
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (done) Accent else Color.Transparent)
                                            .border(
                                                2.dp,
                                                if (active) Accent else if (done) Color.Transparent else Line,
                                                RoundedCornerShape(50),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            if (done) "✓" else "${i + 1}",
                                            color = if (done) AccentInk else if (active) Accent else Txt3,
                                            fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                        )
                                    }
                                    Text(
                                        tag,
                                        color = if (done || active) Txt else Txt3,
                                        fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp, letterSpacing = 0.5.sp,
                                    )
                                }
                            }
                        }
                        Text(
                            "${(progress * 100).roundToInt()}%", color = Txt,
                            fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp,
                        )
                    }
                }
            }

            // ── FOOTER ──
            Text(
                "VibeTuner", color = Txt3, fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp,
                letterSpacing = 0.4.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 56.dp, bottom = 40.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 56.dp, bottom = 40.dp),
            ) {
                Text(
                    "▲▼  Switch channel", color = Txt3,
                    fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.size(8.dp).alpha(pulse).clip(RoundedCornerShape(50)).background(Accent))
                    Text(
                        "Establishing stream", color = Txt3,
                        fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
