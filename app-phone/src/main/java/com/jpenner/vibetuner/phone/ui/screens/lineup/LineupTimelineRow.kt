package com.jpenner.vibetuner.phone.ui.screens.lineup

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors
import com.jpenner.vibetuner.ui.screens.guide.formatTime
import com.jpenner.vibetuner.ui.screens.lineup.LineupSlot
import com.jpenner.vibetuner.ui.screens.lineup.SlotStatus
import com.jpenner.vibetuner.ui.screens.lineup.durationLabel

/**
 * Touch counterpart of the TV app's LineupTimelineRow (ui/screens/lineup in :app):
 * compact metrics, no focus model, not clickable — the day lineup is browse-only.
 */
@Composable
fun LineupTimelineRow(
    slot: LineupSlot,
    isNext: Boolean,
    modifier: Modifier = Modifier,
) {
    val p = slot.program
    val onNow = slot.status == SlotStatus.OnNow
    val aired = slot.status == SlotStatus.Aired

    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).alpha(if (aired) 0.42f else 1f)) {
        // ── time rail ── ("7:00" over "PM")
        Column(
            Modifier.width(58.dp).padding(end = 12.dp, top = 4.dp),
            horizontalAlignment = Alignment.End,
        ) {
            val timeColor = when {
                onNow -> PhoneColors.Accent
                aired -> PhoneColors.Txt3
                else -> PhoneColors.Txt2
            }
            Text(
                formatTime(p.startMinutes).substringBeforeLast(' '),
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = timeColor,
            )
            Text(
                formatTime(p.startMinutes).substringAfterLast(' '),
                fontSize = 10.sp, color = timeColor.copy(alpha = 0.7f),
            )
        }

        // ── spine (line + dot) ──
        Box(Modifier.width(26.dp).fillMaxHeight()) {
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxHeight().width(2.dp)
                    .background(if (onNow) PhoneColors.Accent else PhoneColors.Line.copy(alpha = if (aired) 0.5f else 1f)),
            )
            StatusDot(slot.status, Modifier.align(Alignment.TopCenter).padding(top = 7.dp))
        }

        // ── program card ──
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (onNow) PhoneColors.Raised else PhoneColors.Surface)
                .border(
                    if (onNow) 2.dp else 1.dp,
                    if (onNow) PhoneColors.Accent else PhoneColors.Line,
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 15.dp, vertical = 13.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (onNow) {
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp)).background(PhoneColors.Live)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        p.title,
                        fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = PhoneColors.Txt,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isNext) {
                        Box(
                            Modifier.border(1.dp, PhoneColors.Line, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text("NEXT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PhoneColors.Txt3)
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(durationLabel(p.durationMinutes), fontSize = 11.sp, color = PhoneColors.Txt3)
                    if (p.rating.isNotBlank()) {
                        Box(
                            Modifier.border(1.dp, PhoneColors.Line, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(p.rating, fontSize = 9.5.sp, color = PhoneColors.Txt2)
                        }
                    }
                }
                if (onNow) {
                    Spacer(Modifier.height(11.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.13f)),
                        ) {
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth(slot.progress.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(3.dp)).background(PhoneColors.Accent),
                            )
                        }
                        Text(
                            durationLabel(slot.minutesLeft) + " left",
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = PhoneColors.Accent,
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
        SlotStatus.OnNow -> Box(modifier.size(14.dp).clip(CircleShape).background(PhoneColors.Accent))
        SlotStatus.Aired -> Box(
            modifier.size(10.dp).clip(CircleShape).background(PhoneColors.Bg)
                .border(2.dp, PhoneColors.Line, CircleShape),
        )
        SlotStatus.Upcoming -> Box(
            modifier.size(10.dp).clip(CircleShape).background(PhoneColors.Bg)
                .border(2.dp, PhoneColors.Txt3, CircleShape),
        )
    }
}
