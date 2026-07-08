package com.jpenner.vibetuner.ui.screens.lineup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.jpenner.vibetuner.ui.components.LiveBadge
import com.jpenner.vibetuner.ui.screens.guide.formatTime
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/**
 * One slot on the day timeline: a right-aligned time rail, the connecting spine
 * with its status dot, and a focusable program card. Aired slots dim to 42%;
 * the OnNow card takes the accent border, a LIVE pill and the live progress
 * fill. Browse-only: the card is focusable so D-pad scrolling works, but
 * clicking does nothing.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LineupTimelineRow(
    slot: LineupSlot,
    isNext: Boolean,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val p = slot.program
    val onNow = slot.status == SlotStatus.OnNow
    val aired = slot.status == SlotStatus.Aired

    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).alpha(if (aired) 0.42f else 1f)) {
        // ── time rail ──
        Column(
            Modifier.width(104.dp).padding(end = 18.dp, top = 5.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                formatTime(p.startMinutes),
                style = AerialTypography.titleMedium.copy(fontSize = 16.sp),
                fontWeight = FontWeight.Bold,
                color = when {
                    onNow -> AerialColors.Accent
                    aired -> AerialColors.Txt3
                    else -> AerialColors.Txt2
                },
            )
            if (onNow) {
                Spacer(Modifier.height(6.dp))
                Text("● NOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AerialColors.Accent)
            }
        }

        // ── spine (line + dot) ──
        Box(Modifier.width(46.dp).fillMaxHeight()) {
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxHeight().width(2.dp)
                    .background(if (onNow) AerialColors.Accent else AerialColors.Line.copy(alpha = if (aired) 0.5f else 1f)),
            )
            StatusDot(slot.status, Modifier.align(Alignment.TopCenter).padding(top = 9.dp))
        }

        // ── program card ──
        Surface(
            onClick = {},
            modifier = Modifier
                .weight(1f)
                .let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (onNow) AerialColors.Raised else AerialColors.Surface,
                focusedContainerColor = AerialColors.Raised,
                contentColor = AerialColors.Txt,
                focusedContentColor = AerialColors.Txt,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
            border = ClickableSurfaceDefaults.border(
                border = Border(
                    BorderStroke(
                        if (onNow) 2.dp else 1.dp,
                        if (onNow) AerialColors.Accent else AerialColors.Line,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ),
                focusedBorder = Border(BorderStroke(3.dp, AerialColors.Accent), shape = RoundedCornerShape(14.dp)),
            ),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (onNow) LiveBadge()
                    Text(
                        p.title,
                        style = AerialTypography.titleMedium.copy(fontSize = 19.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isNext) UpNextTag()
                }
                Spacer(Modifier.height(7.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(durationLabel(p.durationMinutes), style = AerialTypography.labelSmall, color = AerialColors.Txt3)
                    if (p.rating.isNotBlank()) RatingChip(p.rating)
                    val sub = p.episodeTitle?.takeIf { it.isNotBlank() } ?: p.mediaType
                    if (sub.isNotBlank()) {
                        Text(
                            sub, style = AerialTypography.labelSmall, color = AerialColors.Txt2,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (onNow) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        Box(
                            Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.13f)),
                        ) {
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth(slot.progress.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(4.dp)).background(AerialColors.Accent),
                            )
                        }
                        Text(
                            durationLabel(slot.minutesLeft) + " left",
                            style = AerialTypography.labelSmall,
                            color = AerialColors.Accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: SlotStatus, modifier: Modifier = Modifier) {
    when (status) {
        SlotStatus.OnNow -> Box(modifier.size(16.dp).clip(CircleShape).background(AerialColors.Accent))
        SlotStatus.Aired -> Box(
            modifier.size(12.dp).clip(CircleShape).background(AerialColors.Bg)
                .border(2.dp, AerialColors.Line, CircleShape),
        )
        SlotStatus.Upcoming -> Box(
            modifier.size(12.dp).clip(CircleShape).background(AerialColors.Bg)
                .border(2.dp, AerialColors.Txt3, CircleShape),
        )
    }
}

@Composable
private fun UpNextTag() {
    Box(
        Modifier.border(1.dp, AerialColors.Line, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text("UP NEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, color = AerialColors.Txt3)
    }
}

@Composable
private fun RatingChip(label: String) {
    Box(
        Modifier.border(1.dp, AerialColors.Line, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 1.dp),
    ) {
        Text(label, fontSize = 11.sp, color = AerialColors.Txt2)
    }
}
