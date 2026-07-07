package com.jpenner.vibetuner.phone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.data.model.Profile
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors

/** Touch counterpart of the TV app's ProfileTile — tap instead of D-pad focus. */
@Composable
fun PhoneProfileTile(profile: Profile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PhoneCard(onClick = onClick, modifier = Modifier.size(112.dp), shape = RoundedCornerShape(20.dp)) {
            Box(
                Modifier.size(112.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(profile.gradient)),
                Alignment.Center,
            ) {
                Text(profile.initial, style = MaterialTheme.typography.displayLarge, color = PhoneColors.Txt)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(profile.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp), color = PhoneColors.Txt)
    }
}
