package com.jpenner.vibetuner.data.model.stremio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StremioAddonMetaTest {
    private fun addon(id: String, name: String, url: String) =
        StremioAddon(url, StremioManifest(id = id, name = name, version = "1.0.0"))

    @Test fun official_true_only_for_cinemeta() {
        assertTrue(addon("com.linvo.cinemeta", "Cinemeta", "https://v3-cinemeta.strem.io/manifest.json").official)
        assertFalse(addon("com.example.other", "Other", "https://x.example/manifest.json").official)
    }

    @Test fun host_is_taken_from_manifest_url() {
        assertEquals("v3-cinemeta.strem.io", addon("id", "n", "https://v3-cinemeta.strem.io/manifest.json").host)
        assertEquals("addons.example.com", addon("id", "n", "https://addons.example.com/x/manifest.json").host)
    }

    @Test fun host_falls_back_to_empty_on_garbage() {
        assertEquals("", addon("id", "n", "not a url").host)
    }

    @Test fun abbreviation_is_two_upper_chars() {
        assertEquals("CI", addon("id", "Cinemeta", "https://x/manifest.json").abbreviation)
        assertEquals("O", addon("id", "O", "https://x/manifest.json").abbreviation)
    }
}
