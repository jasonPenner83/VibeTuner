package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.stremio.StremioAddon
import com.jpenner.vibetuner.data.model.stremio.StremioCatalog
import com.jpenner.vibetuner.data.model.stremio.StremioExtra
import com.jpenner.vibetuner.data.model.stremio.StremioManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogExpansionTest {
    private fun addon(
        id: String = "com.x",
        catalogs: List<StremioCatalog>,
        adult: Boolean = false,
        types: List<String> = listOf("movie"),
        enabled: Boolean = true,
        selections: Map<String, List<String>> = emptyMap(),
    ) = StremioAddon(
        "https://x/manifest.json",
        StremioManifest(id, "X", "1", types = types, adult = adult,
            resources = listOf("catalog"), catalogs = catalogs),
        enabled = enabled, selections = selections,
    )

    private val genre = StremioExtra("genre", options = listOf("Action", "Comedy"))

    @Test fun base_catalog_becomes_one_spec() {
        val specs = expandCatalogSpecs(listOf(addon(catalogs = listOf(StremioCatalog("movie", "top", "Top")))))
        assertEquals(1, specs.size)
        assertEquals(null, specs[0].option)
    }

    @Test fun selected_options_add_specs_after_the_base() {
        val a = addon(
            catalogs = listOf(StremioCatalog("movie", "top", "Top", extra = listOf(genre))),
            selections = mapOf("movie/top/genre" to listOf("Action")),
        )
        val specs = expandCatalogSpecs(listOf(a))
        assertEquals(listOf(null, "Action"), specs.map { it.option })
        assertEquals("genre", specs[1].extraName)
    }

    @Test fun unselected_and_unknown_options_do_not_expand() {
        val a = addon(
            catalogs = listOf(StremioCatalog("movie", "top", "Top", extra = listOf(genre))),
            selections = mapOf("movie/top/genre" to listOf("NotAnOption")),
        )
        assertEquals(1, expandCatalogSpecs(listOf(a)).size)
    }

    @Test fun required_extra_suppresses_base_but_keeps_options() {
        val required = StremioExtra("genre", isRequired = true, options = listOf("Action", "Comedy"))
        val a = addon(
            catalogs = listOf(StremioCatalog("movie", "by-genre", "By Genre", extra = listOf(required))),
            selections = mapOf("movie/by-genre/genre" to listOf("Comedy")),
        )
        val specs = expandCatalogSpecs(listOf(a))
        assertEquals(listOf("Comedy"), specs.map { it.option })
    }

    @Test fun adult_manifest_blocks_all_and_adult_catalog_blocks_itself() {
        val blockedAddon = addon(adult = true, catalogs = listOf(StremioCatalog("movie", "top", "Top")))
        assertTrue(expandCatalogSpecs(listOf(blockedAddon)).isEmpty())

        val mixed = addon(catalogs = listOf(
            StremioCatalog("movie", "top", "Top"),
            StremioCatalog("Porn", "x", "X"),
        ))
        assertEquals(listOf("top"), expandCatalogSpecs(listOf(mixed)).map { it.catalog.id })

        val adultTyped = addon(types = listOf("movie", "xxx"),
            catalogs = listOf(StremioCatalog("movie", "top", "Top")))
        assertTrue(expandCatalogSpecs(listOf(adultTyped)).isEmpty())
    }

    @Test fun disabled_addons_expand_to_nothing() {
        val a = addon(enabled = false, catalogs = listOf(StremioCatalog("movie", "top", "Top")))
        assertTrue(expandCatalogSpecs(listOf(a)).isEmpty())
    }

    @Test fun optionToggles_reflect_current_selection() {
        val a = addon(
            catalogs = listOf(StremioCatalog("movie", "top", "Top", extra = listOf(genre))),
            selections = mapOf("movie/top/genre" to listOf("Comedy")),
        )
        val toggles = optionToggles(a, a.manifest.catalogs[0])
        assertEquals(
            listOf(
                SubChannelToggle("genre", "Action", selected = false),
                SubChannelToggle("genre", "Comedy", selected = true),
            ),
            toggles,
        )
    }

    @Test fun allowAdult_lifts_addon_and_catalog_blocks() {
        val blockedAddon = addon(adult = true, catalogs = listOf(StremioCatalog("movie", "top", "Top")))
        assertEquals(1, expandCatalogSpecs(listOf(blockedAddon), allowAdult = true).size)

        val mixed = addon(catalogs = listOf(
            StremioCatalog("movie", "top", "Top"),
            StremioCatalog("Porn", "x", "X"),
        ))
        assertEquals(listOf("top", "x"), expandCatalogSpecs(listOf(mixed), allowAdult = true).map { it.catalog.id })

        val adultTyped = addon(types = listOf("movie", "xxx"),
            catalogs = listOf(StremioCatalog("movie", "top", "Top")))
        assertEquals(1, expandCatalogSpecs(listOf(adultTyped), allowAdult = true).size)
    }
}
