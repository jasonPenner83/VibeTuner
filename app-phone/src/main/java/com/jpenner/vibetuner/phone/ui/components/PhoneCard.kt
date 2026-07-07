package com.jpenner.vibetuner.phone.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors
import com.jpenner.vibetuner.phone.ui.theme.pressScale
import com.jpenner.vibetuner.phone.ui.theme.rememberPressInteractionSource

/**
 * Touch equivalent of the TV kit's focus-scaling `Surface`/`Card` (see
 * `ui/theme/Focus.kt` in :app): no focus ring, just a brief scale-down + ripple
 * on tap. Same surface/border colors as the TV Aerial kit.
 */
@Composable
fun PhoneCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val interactionSource = rememberPressInteractionSource()
    Card(
        onClick = onClick,
        modifier = modifier.pressScale(interactionSource),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = PhoneColors.Surface,
            contentColor = PhoneColors.Txt,
        ),
        border = BorderStroke(1.dp, PhoneColors.Line),
        interactionSource = interactionSource,
    ) {
        Box(Modifier.padding(contentPadding)) { content() }
    }
}

/** [PhoneCard] variant with a long-press action (e.g. a row's context menu). */
@Composable
fun PhoneCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val interactionSource = rememberPressInteractionSource()
    Card(
        modifier = modifier
            .pressScale(interactionSource)
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = PhoneColors.Surface,
            contentColor = PhoneColors.Txt,
        ),
        border = BorderStroke(1.dp, PhoneColors.Line),
    ) {
        Box(Modifier.padding(contentPadding)) { content() }
    }
}
