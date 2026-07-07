package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/**
 * TV text box: D-pad focus highlights it like any other row; OK/Enter opens the
 * on-screen keyboard. IME Done or Back closes it and fires [onDone]. This is the
 * default text input for the app — don't use raw OutlinedTextField in TV screens.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    onDone: (String) -> Unit = {},
) {
    var editing by remember { mutableStateOf(false) }
    var returnFocus by remember { mutableStateOf(false) }
    val containerFocus = remember { FocusRequester() }
    val fieldFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    fun finishEditing() {
        if (!editing) return
        editing = false
        returnFocus = true
        keyboard?.hide()
        onDone(value)
    }

    if (editing) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = modifier.focusRequester(fieldFocus).onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyUp && e.key == Key.Back) { finishEditing(); true } else false
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { finishEditing() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = AerialColors.Accent),
        )
        LaunchedEffect(Unit) { fieldFocus.requestFocus(); keyboard?.show() }
    } else {
        Surface(
            onClick = { editing = true },
            modifier = modifier.focusRequester(containerFocus),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = AerialColors.Bg, focusedContainerColor = AerialColors.Raised,
                contentColor = AerialColors.Txt),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(10.dp))),
        ) {
            Text(
                if (value.isNotBlank()) value else placeholder,
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                style = AerialTypography.titleMedium.copy(fontSize = 15.sp),
                color = if (value.isNotBlank()) AerialColors.Txt else AerialColors.Txt3,
                maxLines = 1,
            )
        }
        if (returnFocus) LaunchedEffect(Unit) {
            runCatching { containerFocus.requestFocus() }
            returnFocus = false
        }
    }
}
