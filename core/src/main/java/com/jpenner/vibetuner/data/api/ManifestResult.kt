package com.jpenner.vibetuner.data.api

import com.jpenner.vibetuner.data.model.stremio.StremioAddon
import com.jpenner.vibetuner.data.model.stremio.StremioManifest

/** Result of validating a pasted manifest URL, surfaced live under the Add-Ons paste field. */
sealed interface ManifestResult {
    data object Loading : ManifestResult
    data class Ok(val name: String, val catalogCount: Int, val addon: StremioAddon) : ManifestResult
    data class Invalid(val reason: String) : ManifestResult
}

/** Pure mapping from a (maybe-parsed) manifest to a result. Network fetch lives in AddonRepository.validate. */
fun manifestResultOf(manifestUrl: String, manifest: StremioManifest?): ManifestResult =
    if (manifest == null) {
        ManifestResult.Invalid("Couldn't load a valid manifest from that URL.")
    } else {
        ManifestResult.Ok(
            name = manifest.name,
            catalogCount = manifest.catalogs.size,
            addon = StremioAddon(manifestUrl = manifestUrl, manifest = manifest, enabled = true),
        )
    }
