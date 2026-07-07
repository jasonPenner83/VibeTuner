package com.jpenner.vibetuner.data.model

// One settings row, polymorphic on the control it shows on the right.
sealed interface SettingControl {
    data class Toggle(val on: Boolean) : SettingControl
    data class Slider(val fraction: Float) : SettingControl   // 0f..1f
    data class Value(val text: String, val danger: Boolean = false) : SettingControl   // select / nav
    data class Info(val text: String) : SettingControl
}

/** One row in a settings pane. [key] identifies it for edits/activation;
 *  [control] selects which widget the renderer paints on the right. */
data class SettingItem(
    val key: String,
    val label: String,
    val sub: String,
    val control: SettingControl,
)
