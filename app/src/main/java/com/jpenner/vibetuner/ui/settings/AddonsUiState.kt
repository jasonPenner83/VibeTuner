package com.jpenner.vibetuner.ui.settings

import com.jpenner.vibetuner.data.api.ManifestResult
import com.jpenner.vibetuner.data.model.stremio.StremioAddon

data class AddonsUiState(
    val addons: List<StremioAddon> = emptyList(),
    val allowAdult: Boolean = false,    // active profile's effective adult unlock
    val sheet: AddSheetState? = null,   // non-null while the paste sheet is open
) {
    val enabledCount: Int get() = addons.count { it.enabled }
    val catalogCount: Int get() = addons
        .filter { it.enabled && (allowAdult || !it.manifest.adultBlocked) }
        .sumOf { a -> a.manifest.catalogs.count { allowAdult || !it.adultBlocked } }
}

/** The Add-manifest sheet: current field text + live validation. */
data class AddSheetState(
    val url: String = "",
    val result: ManifestResult? = null,
) {
    val canAdd: Boolean get() = result is ManifestResult.Ok
}
