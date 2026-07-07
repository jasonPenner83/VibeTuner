package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.jpenner.vibetuner.ui.theme.AerialTypography
import androidx.tv.material3.Text
import com.jpenner.vibetuner.data.model.ContentItem
import com.jpenner.vibetuner.ui.theme.AerialColors

@Composable
fun ContentRail(
    title: String,
    items: List<ContentItem>,
    onItemClick: (ContentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(title, style = AerialTypography.headlineMedium.copy(fontSize = 24.sp), color = AerialColors.Txt)
        Spacer(Modifier.height(16.dp))
        // TvLazyRow keeps focus + restores scroll position between rails
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            items(items, key = { it.id }) { item ->
                ContentCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}
