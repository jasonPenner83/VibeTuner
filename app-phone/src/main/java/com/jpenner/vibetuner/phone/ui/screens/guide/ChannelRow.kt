package com.jpenner.vibetuner.phone.ui.screens.guide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.phone.ui.components.PhoneCard
import com.jpenner.vibetuner.phone.ui.components.PhoneLiveDot
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors
import com.jpenner.vibetuner.ui.screens.guide.formatTime

/**
 * Touch counterpart of the TV app's NowNextRow (ui/screens/guide/NowNextRow.kt):
 * same channel identity + now-playing card, but the "Up Next" card is dropped —
 * that context now lives in the tap-triggered bottom sheet instead (see
 * GuideScreen's ChannelContextSheet), which suits a narrow phone width better
 * than a fixed three-column row.
 */
@Composable
fun ChannelRow(
    channel: Channel,
    nowMinutes: Int,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit,
    isCurrent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val now = channel.nowPlaying(nowMinutes)

    PhoneCard(
        onClick = onClick,
        onLongClick = onOpenMenu,
        modifier = modifier.fillMaxWidth(),
        border = if (isCurrent) BorderStroke(2.dp, PhoneColors.Accent) else BorderStroke(1.dp, PhoneColors.Line),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            // channel identity
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    channel.number, modifier = Modifier.width(30.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    color = PhoneColors.Txt,
                )
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(PhoneColors.Bg),
                    Alignment.Center,
                ) {
                    Text(channel.abbreviation, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PhoneColors.Txt2)
                }
                Column(Modifier.weight(1f)) {
                    Text(channel.name, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, color = PhoneColors.Txt)
                    Text(channel.category.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = channel.category.color)
                }
            }
            Spacer(Modifier.height(10.dp))

            // now playing
            if (now == null) {
                Text("No programme information", color = PhoneColors.Txt2, fontSize = 13.sp)
            } else {
                val remaining = now.endMinutes - nowMinutes
                val progress = now.progressAt(nowMinutes)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PhoneLiveDot(); Spacer(Modifier.width(8.dp))
                    Text(now.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = PhoneColors.Txt)
                    Text("${remaining}m left", fontSize = 11.sp, color = PhoneColors.Success)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(formatTime(now.startMinutes), fontSize = 11.sp, color = PhoneColors.Txt3)
                    Box(
                        Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(PhoneColors.Line),
                    ) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(2.dp)).background(PhoneColors.Accent),
                        )
                    }
                    Text(formatTime(now.endMinutes), fontSize = 11.sp, color = PhoneColors.Txt3)
                }
            }
        }
    }
}
