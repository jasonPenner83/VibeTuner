package com.jpenner.vibetuner.ui.screens.startup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.JetBrainsMonoFontFamily
import com.jpenner.vibetuner.ui.theme.SoraFontFamily
import kotlin.math.roundToInt

// Azure palette from the design handoff (azure-only per project convention).
private val Bg = Color(0xFF0D0F14)
private val Raised = Color(0xFF1D212C)
private val Line = Color(0xFF2A2F3C)
private val Txt = Color(0xFFEEF1F6)
private val Txt2 = Color(0xFFA7AFBF)
private val Txt3 = Color(0xFF6B7384)
private val Accent = Color(0xFF3D9BFF)
private val Accent2 = Color(0xFF8FC7FF)
private val AccentInk = Color(0xFF06121F)
private val Glow = Color(0x803D9BFF) // rgba(61,155,255,.5)

private val BarWidth = 573.dp // design 860px on the 1920 canvas -> x2/3 on the 1280 canvas

@Composable
fun AppStartupLoadingScreen(
    state: StartupUiState,
    modifier: Modifier = Modifier,
) {
    AerialCanvas {
        val transition = rememberInfiniteTransition(label = "startup")
        val breathe by transition.animateFloat(
            initialValue = 0.5f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
            label = "breathe",
        )
        val drift by transition.animateFloat(
            initialValue = 1f, targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Reverse),
            label = "drift",
        )
        val sweep by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
            label = "sweep",
        )
        val dot by transition.animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
            label = "dot",
        )

        Box(
            modifier = modifier.fillMaxSize().background(Bg),
            contentAlignment = Alignment.Center,
        ) {
            // Ambient azure glow that slowly drifts/breathes.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Glow.copy(alpha = 0.45f), Color.Transparent),
                        radius = 820f * drift,
                    )
                )
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // ── brand ──
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(126.dp)
                            .scale(0.9f + breathe * 0.2f)
                            .alpha(breathe)
                            .clip(RoundedCornerShape(44.dp))
                            .background(Brush.radialGradient(listOf(Glow, Color.Transparent)))
                    )
                    Box(
                        Modifier
                            .size(79.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Accent, Accent2))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "V", color = AccentInk, fontFamily = SoraFontFamily,
                            fontWeight = FontWeight.ExtraBold, fontSize = 44.sp,
                        )
                    }
                }
                Spacer(Modifier.height(17.dp))
                Text(
                    "VibeTuner", color = Txt, fontFamily = SoraFontFamily,
                    fontWeight = FontWeight.ExtraBold, fontSize = 29.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "GOOGLE TV", color = Txt3, fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp, letterSpacing = 3.sp,
                )

                Spacer(Modifier.height(52.dp))

                // ── rotating status ──
                Box(Modifier.height(69.dp), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = state.stageIndex,
                        transitionSpec = {
                            (fadeIn(tween(550)) + slideInVertically(tween(550)) { it / 4 })
                                .togetherWith(fadeOut(tween(200)))
                        },
                        label = "headline",
                    ) { idx ->
                        val s = STARTUP_STAGES[idx.coerceIn(0, STARTUP_STAGES.lastIndex)]
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                s.label, color = Accent, fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 2.sp,
                            )
                            Spacer(Modifier.height(13.dp))
                            Text(
                                s.headline, color = Txt, fontFamily = SoraFontFamily,
                                fontWeight = FontWeight.SemiBold, fontSize = 35.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                if (state.profileTotal > 0) {
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "Loading guide for ${state.profileName.orEmpty()} " +
                            "(${state.profileIndex + 1} of ${state.profileTotal})",
                        color = Txt2, fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp, textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(27.dp))

                // ── progress ──
                Column(Modifier.width(BarWidth)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Raised),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(Accent, Accent2)))
                        )
                        // Shimmer that sweeps left -> right across the bar.
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.4f)
                                .offset(x = BarWidth * (sweep * 1.8f - 0.4f))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent)
                                    )
                                )
                        )
                    }
                    Spacer(Modifier.height(15.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            STARTUP_STAGES.forEachIndexed { i, _ ->
                                val active = i == state.stageIndex
                                val done = i < state.stageIndex
                                Box(
                                    Modifier
                                        .width(if (active) 23.dp else 7.dp)
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active || done) Accent else Line)
                                )
                            }
                        }
                        Text(
                            "${(state.progress * 100).roundToInt()}%",
                            color = Txt2, fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        )
                    }
                }
            }

            // ── footer ──
            Text(
                "VibeTuner", color = Txt3, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 37.dp, bottom = 27.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 37.dp, bottom = 27.dp),
            ) {
                Box(Modifier.size(5.dp).alpha(dot).clip(RoundedCornerShape(50)).background(Accent))
                Text("Getting things ready", color = Txt3, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp)
            }
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun AppStartupLoadingScreenPreview() {
    AppStartupLoadingScreen(
        state = StartupUiState(
            progress = 0.46f,
            stageIndex = 2,
            done = false,
            profileName = "Sam",
            profileIndex = 1,
            profileTotal = 3,
        )
    )
}
