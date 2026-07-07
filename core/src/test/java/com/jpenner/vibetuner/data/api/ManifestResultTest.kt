package com.jpenner.vibetuner.data.api

import com.jpenner.vibetuner.data.model.stremio.StremioCatalog
import com.jpenner.vibetuner.data.model.stremio.StremioManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestResultTest {
    @Test fun ok_carries_name_and_catalog_count() {
        val manifest = StremioManifest(
            id = "com.linvo.cinemeta", name = "Cinemeta", version = "3.0.0",
            resources = listOf("catalog"),
            catalogs = listOf(StremioCatalog("movie", "top", "Popular"), StremioCatalog("series", "top", "Popular")),
        )
        val result = manifestResultOf("https://x/manifest.json", manifest)
        assertTrue(result is ManifestResult.Ok)
        result as ManifestResult.Ok
        assertEquals("Cinemeta", result.name)
        assertEquals(2, result.catalogCount)
        assertEquals("https://x/manifest.json", result.addon.manifestUrl)
    }

    @Test fun null_manifest_is_invalid() {
        val result = manifestResultOf("https://x/manifest.json", null)
        assertTrue(result is ManifestResult.Invalid)
    }
}
