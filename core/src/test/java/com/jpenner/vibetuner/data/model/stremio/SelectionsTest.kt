package com.jpenner.vibetuner.data.model.stremio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionsTest {
    private val manifest = parseManifest(
        """{ "id": "com.x", "name": "X", "version": "1", "resources": ["catalog"],
             "catalogs": [ { "type": "movie", "id": "top", "name": "Top",
               "extra": [ { "name": "genre", "options": ["Action", "Comedy"] } ] } ] }"""
    )!!

    @Test fun selection_key_format_is_type_id_extra() {
        assertEquals("movie/top/genre", selectionKey("movie", "top", "genre"))
    }

    @Test fun selectedOptions_reads_by_key() {
        val addon = StremioAddon("https://x/manifest.json", manifest,
            selections = mapOf("movie/top/genre" to listOf("Action")))
        assertEquals(listOf("Action"), addon.selectedOptions(manifest.catalogs[0], "genre"))
        assertTrue(addon.selectedOptions(manifest.catalogs[0], "search").isEmpty())
    }

    @Test fun selections_round_trip_through_addon_json() {
        val addon = StremioAddon("https://x/manifest.json", manifest,
            selections = mapOf("movie/top/genre" to listOf("Action", "Comedy")))
        val restored = addonFromJson(addon.toJson())!!
        assertEquals(mapOf("movie/top/genre" to listOf("Action", "Comedy")), restored.selections)
    }

    @Test fun prune_drops_vanished_options_and_unknown_keys() {
        val selections = mapOf(
            "movie/top/genre" to listOf("Action", "Gone"),
            "movie/removed/genre" to listOf("Action"),
        )
        assertEquals(mapOf("movie/top/genre" to listOf("Action")), pruneSelections(selections, manifest))
    }

    @Test fun prune_drops_entries_left_empty() {
        val selections = mapOf("movie/top/genre" to listOf("Gone"))
        assertTrue(pruneSelections(selections, manifest).isEmpty())
    }

    @Test fun withAllOptions_selects_every_option() {
        val addon = StremioAddon("https://x/manifest.json", manifest)
        val updated = addon.withAllOptions(manifest.catalogs[0], true)
        assertEquals(mapOf("movie/top/genre" to listOf("Action", "Comedy")), updated.selections)
    }

    @Test fun withAllOptions_clear_removes_entries() {
        val addon = StremioAddon("https://x/manifest.json", manifest,
            selections = mapOf("movie/top/genre" to listOf("Action")))
        assertTrue(addon.withAllOptions(manifest.catalogs[0], false).selections.isEmpty())
    }

    @Test fun withAllOptions_preserves_other_keys() {
        val addon = StremioAddon("https://x/manifest.json", manifest,
            selections = mapOf("series/other/genre" to listOf("Drama")))
        val updated = addon.withAllOptions(manifest.catalogs[0], true)
        assertEquals(listOf("Drama"), updated.selections["series/other/genre"])
    }
}
