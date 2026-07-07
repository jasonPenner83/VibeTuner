package com.jpenner.vibetuner.ui.settings

import androidx.compose.ui.graphics.Color
import com.jpenner.vibetuner.data.model.ProfileType
import com.jpenner.vibetuner.data.model.Rating

/** One row in the PROFILES list. [summary] is Profile.summary(); [hasPin]
 *  draws the lock glyph. */
data class ProfileRow(
    val id: String,
    val name: String,
    val initial: String,
    val gradient: List<Color>,
    val summary: String,
    val type: ProfileType,
    val restricted: Boolean,
    val hasPin: Boolean,
)

/** The EDIT pane for the selected profile. */
data class ProfileEdit(
    val id: String,
    val name: String,
    val initial: String,
    val gradient: List<Color>,
    val type: ProfileType,
    val maxRating: Rating,
    val allowedTypes: Set<String>,       // empty = all types allowed
    val availableTypes: List<String>,    // union of installed add-ons' manifest types
    val requirePin: Boolean,
    val hasPin: Boolean,
    val adultContent: Boolean,
) {
    val allowedCount: Int get() = if (allowedTypes.isEmpty()) availableTypes.size else allowedTypes.size
    val totalTypes: Int get() = availableTypes.size
}

/** Drives PinEntryDialog. The mode switches copy + the number of stages. */
sealed interface PinPrompt {
    val profileId: String

    /** Verify to unlock a restricted profile (picker or edit). [thenEnableAdult]
     *  chains the adult-content unlock that triggered the verification. */
    data class Unlock(
        override val profileId: String,
        val name: String,
        val thenEnableAdult: Boolean = false,
    ) : PinPrompt

    /** Set or change: enter a new PIN, then confirm it. [thenEnableAdult]
     *  chains the adult-content unlock that triggered the PIN setup. */
    data class SetPin(
        override val profileId: String,
        val name: String,
        val confirming: Boolean,
        val thenEnableAdult: Boolean = false,
    ) : PinPrompt
}

data class ProfileManagerUiState(
    val rows: List<ProfileRow> = emptyList(),
    val selectedId: String? = null,
    val editing: ProfileEdit? = null,
    val prompt: PinPrompt? = null,
    val pinError: Boolean = false,
    val confirmingDelete: Boolean = false,
) {
    val restrictedCount: Int get() = rows.count { it.restricted }
}
