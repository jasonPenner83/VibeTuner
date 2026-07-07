package com.jpenner.vibetuner.phone.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.data.api.StremioResolver
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors

/**
 * Phone-appropriate stand-in for the TV app's animated tune-in overlay
 * (ui/components/TransitionLoadingScreen.kt in :app): same [StremioResolver]
 * call underneath, just a plain spinner instead of the 10-foot channel-lockup
 * animation — that piece of "same styling" wasn't worth porting for an MVP.
 */
@Composable
fun ResolvingScreen(
    channel: Channel?,
    program: Program?,
    onResolved: (String?) -> Unit,
) {
    val context = LocalContext.current
    val resolver = remember(context) { StremioResolver(context) }
    var status by remember { mutableStateOf("Tuning in…") }

    LaunchedEffect(program) {
        if (program == null) {
            onResolved(null)
            return@LaunchedEffect
        }
        val result = resolver.resolveStreamUrl(program) { s -> status = s }
        onResolved(result)
    }

    Box(Modifier.fillMaxSize().background(PhoneColors.Bg), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PhoneColors.Accent)
            Spacer(Modifier.height(16.dp))
            Text(channel?.name ?: "Tuning…", style = MaterialTheme.typography.titleMedium, color = PhoneColors.Txt)
            Text(status, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp), color = PhoneColors.Txt2)
        }
    }
}
