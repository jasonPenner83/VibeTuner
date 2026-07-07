package com.jpenner.vibetuner.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/** One action in the channel context menu. */
data class ChannelMenuItem(
    val label: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContextSideSheet(
    channel: Channel,
    items: List<ChannelMenuItem>,
    onDismiss: () -> Unit,
) {
    val firstItemFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItemFocus.requestFocus() }
    BackHandler { onDismiss() }

    Box(
        Modifier.fillMaxSize().background(Color(0xB8060709)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Surface(
            modifier = Modifier
                .width(380.dp)
                .fillMaxHeight()
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup(),
            shape = RoundedCornerShape(0.dp),
            colors = SurfaceDefaults.colors(AerialColors.Surface),
        ) {
            Row(Modifier.fillMaxSize()) {
                // Left divider line (design: 4px accent-line on the panel's leading edge).
                Box(Modifier.width(2.dp).fillMaxHeight().background(AerialColors.Line))

                Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 32.dp)) {
                    Text(
                        channel.name,
                        style = AerialTypography.titleMedium.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        color = AerialColors.Txt,
                    )
                    Spacer(Modifier.height(24.dp))

                    items.forEachIndexed { index, item ->
                        val rowModifier =
                            if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier
                        MenuRow(label = item.label, onClick = item.onClick, modifier = rowModifier)
                        if (index != items.lastIndex) Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MenuRowPreview() {
    AerialCanvas {
        Box(
            modifier = Modifier
                .background(AerialColors.Bg)
                .padding(16.dp)
        ) {
            MenuRow(
                label = "Add to Favorites",
                onClick = {}
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ContextSideSheetPreview() {
    val sampleChannel = Channel(
        id = "hbo",
        name = "HBO HD",
        abbreviation = "HBO",
        description = "Home Box Office",
        number = "101",
        category = Category.Movies,
    )
    val sampleItems = listOf(
        ChannelMenuItem("Add to Favorites") {},
        ChannelMenuItem("View Schedule") {},
        ChannelMenuItem("Channel Settings") {},
    )
    AerialCanvas {
        ContextSideSheet(
            channel = sampleChannel,
            items = sampleItems,
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MenuRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface,
            contentColor = AerialColors.Txt,
            focusedContainerColor = AerialColors.Raised,
            focusedContentColor = AerialColors.Txt,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(2.dp, AerialColors.Line), shape = RoundedCornerShape(12.dp)),
            focusedBorder = Border(BorderStroke(3.dp, AerialColors.Accent), shape = RoundedCornerShape(12.dp)),
        ),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Leading icon placeholder (design shows an accent block) — swap for a real Icon later.
            Box(Modifier.size(width = 26.dp, height = 18.dp)
                .background(AerialColors.Accent, RoundedCornerShape(5.dp)))
            Text(label, style = AerialTypography.titleMedium.copy(fontSize = 16.sp))
        }
    }
}