package com.jpenner.vibetuner.data.model.stremio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StremioManifestParsingTest {

    @Test
    fun parses_id_name_version_and_catalogs() {
        val json = """
            {
              "id": "com.linvo.cinemeta",
              "name": "Cinemeta",
              "version": "3.0.0",
              "resources": ["catalog", "meta"],
              "types": ["movie", "series"],
              "catalogs": [
                { "type": "movie", "id": "top", "name": "Popular",
                  "extra": [ { "name": "genre" }, { "name": "skip" } ] },
                { "type": "series", "id": "top", "name": "Popular Series" }
              ]
            }
        """.trimIndent()

        val manifest = parseManifest(json)!!
        assertEquals("com.linvo.cinemeta", manifest.id)
        assertEquals("Cinemeta", manifest.name)
        assertEquals(listOf("catalog", "meta"), manifest.resources)
        assertEquals(2, manifest.catalogs.size)
        assertTrue(manifest.servesCatalogs)

        val movieCatalog = manifest.catalogs[0]
        assertEquals("movie", movieCatalog.type)
        assertEquals("top", movieCatalog.id)
        assertEquals("Popular", movieCatalog.name)
        assertEquals(listOf("genre", "skip"), movieCatalog.extra.map { it.name })
    }

    @Test
    fun parses_object_form_resources() {
        val json = """
            {
              "id": "x", "name": "X", "version": "1.0.0",
              "resources": [ { "name": "catalog", "types": ["movie"] }, "meta" ],
              "catalogs": [ { "type": "movie", "id": "a", "name": "A" } ]
            }
        """.trimIndent()

        val manifest = parseManifest(json)!!
        assertEquals(listOf("catalog", "meta"), manifest.resources)
    }

    @Test
    fun returns_null_when_required_fields_missing() {
        assertNull(parseManifest("""{ "name": "No Id", "version": "1.0.0" }"""))
        assertNull(parseManifest("""{ "id": "x", "version": "1.0.0" }"""))
        assertNull(parseManifest("not json"))
    }

    @Test
    fun addon_without_catalogs_does_not_serve_catalogs() {
        val json = """{ "id": "sub", "name": "Subs", "version": "1.0.0", "resources": ["subtitles"] }"""
        val manifest = parseManifest(json)!!
        assertTrue(manifest.catalogs.isEmpty())
        assertTrue(!manifest.servesCatalogs)
    }

    @Test
    fun parses_extra_options_and_isRequired() {
        val json = """
            { "id": "x", "name": "X", "version": "1.0.0", "resources": ["catalog"],
              "catalogs": [ { "type": "movie", "id": "top", "name": "Top",
                "extra": [
                  { "name": "genre", "options": ["Action", "Comedy"], "isRequired": true },
                  { "name": "skip" } ] } ] }
        """.trimIndent()
        val catalog = parseManifest(json)!!.catalogs[0]
        val genre = catalog.extra.first { it.name == "genre" }
        assertEquals(listOf("Action", "Comedy"), genre.options)
        assertTrue(genre.isRequired)
        assertTrue(catalog.requiresExtra)
        assertEquals(listOf(genre), catalog.optionExtras)
        val skip = catalog.extra.first { it.name == "skip" }
        assertTrue(skip.options.isEmpty())
        assertTrue(!skip.isRequired)
    }

    @Test
    fun parses_legacy_extraSupported_and_catalog_genres() {
        val json = """
            { "id": "x", "name": "X", "version": "1.0.0", "resources": ["catalog"],
              "catalogs": [ { "type": "movie", "id": "top", "name": "Top",
                "extraSupported": ["genre", "skip"],
                "genres": ["Action", "Drama"] } ] }
        """.trimIndent()
        val catalog = parseManifest(json)!!.catalogs[0]
        assertEquals(listOf("genre", "skip"), catalog.extra.map { it.name })
        assertEquals(listOf("Action", "Drama"), catalog.extra.first { it.name == "genre" }.options)
    }

    @Test
    fun parses_behaviorHints_adult_and_blocks() {
        val adult = parseManifest(
            """{ "id": "x", "name": "X", "version": "1", "behaviorHints": { "adult": true },
                 "resources": ["catalog"], "catalogs": [ { "type": "movie", "id": "a", "name": "A" } ] }"""
        )!!
        assertTrue(adult.adult)
        assertTrue(adult.adultBlocked)

        val clean = parseManifest(
            """{ "id": "y", "name": "Y", "version": "1", "resources": ["catalog"],
                 "catalogs": [ { "type": "movie", "id": "a", "name": "A" } ] }"""
        )!!
        assertTrue(!clean.adult)
        assertTrue(!clean.adultBlocked)
    }

    @Test
    fun adult_types_block_addon_and_catalog() {
        val mixed = parseManifest(
            """{ "id": "x", "name": "X", "version": "1", "types": ["movie", "Porn"],
                 "resources": ["catalog"],
                 "catalogs": [ { "type": "movie", "id": "a", "name": "A" },
                               { "type": "Porn", "id": "b", "name": "B" } ] }"""
        )!!
        assertTrue(mixed.adultBlocked)                      // manifest-level types[] blocks the addon
        assertTrue(mixed.catalogs[1].adultBlocked)          // catalog-level type blocks the catalog
        assertTrue(!mixed.catalogs[0].adultBlocked)
        assertTrue(isAdultType(" XXX "))
        assertTrue(!isAdultType("movie"))
    }

    @Test
    fun new_extra_shape_round_trips_and_legacy_string_extra_reads() {
        val manifest = parseManifest(
            """{ "id": "com.x", "name": "X", "version": "1", "resources": ["catalog"],
                 "behaviorHints": { "adult": true },
                 "catalogs": [ { "type": "movie", "id": "top", "name": "Top",
                   "extra": [ { "name": "genre", "options": ["Action"], "isRequired": true } ] } ] }"""
        )!!
        val restored = addonFromJson(StremioAddon("https://x.example/manifest.json", manifest).toJson())!!
        val genre = restored.manifest.catalogs[0].extra.single()
        assertEquals("genre", genre.name)
        assertEquals(listOf("Action"), genre.options)
        assertTrue(genre.isRequired)
        assertTrue(restored.manifest.adult)

        // Old cached addon files stored extra as a bare string array — must still read.
        val legacy = org.json.JSONObject(
            """{ "manifestUrl": "https://y.example/manifest.json", "enabled": true,
                 "manifest": { "id": "y", "name": "Y", "version": "1", "resources": ["catalog"],
                   "catalogs": [ { "type": "movie", "id": "top", "name": "Top", "extra": ["genre", "skip"] } ] } }"""
        )
        val old = addonFromJson(legacy)!!
        assertEquals(listOf("genre", "skip"), old.manifest.catalogs[0].extra.map { it.name })
    }

    @Test
    fun addon_json_round_trips_through_persistence() {
        val manifest = parseManifest(
            """
            { "id": "com.x", "name": "X", "version": "1.2.3",
              "resources": ["catalog"], "types": ["movie"],
              "catalogs": [ { "type": "movie", "id": "top", "name": "Top" } ] }
            """.trimIndent()
        )!!
        val addon = StremioAddon("https://x.example/manifest.json", manifest, enabled = false)

        val restored = addonFromJson(addon.toJson())!!
        assertEquals("https://x.example/manifest.json", restored.manifestUrl)
        assertEquals("https://x.example", restored.baseUrl)
        assertEquals(false, restored.enabled)
        assertEquals("com.x", restored.id)
        assertEquals(1, restored.manifest.catalogs.size)
        assertEquals("top", restored.manifest.catalogs[0].id)
    }
}
