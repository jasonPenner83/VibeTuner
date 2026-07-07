package com.jpenner.vibetuner.data.model.stremio

import com.jpenner.vibetuner.data.repository.expandCatalogSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SeedRequiredSelectionsTest {
    private val requiredGenre = StremioExtra("genre", isRequired = true, options = listOf("Amateur", "Anal"))
    private val requiredSkip = StremioExtra("skip", isRequired = true)          // no options
    private val optionalGenre = StremioExtra("genre", options = listOf("Action", "Comedy"))

    private fun addon(
        catalogs: List<StremioCatalog>,
        selections: Map<String, List<String>> = emptyMap(),
    ) = StremioAddon(
        "https://x/manifest.json",
        StremioManifest("com.x", "X", "1", types = listOf("movie"),
            resources = listOf("catalog"), catalogs = catalogs),
        selections = selections,
    )

    @Test fun seeds_first_option_of_unselected_required_extra() {
        val a = addon(listOf(StremioCatalog("movie", "cat", "Cat", extra = listOf(requiredGenre, requiredSkip))))
        val seeded = a.seedRequiredSelections()
        assertEquals(mapOf("movie/cat/genre" to listOf("Amateur")), seeded.selections)
    }

    @Test fun never_touches_existing_selections() {
        val a = addon(
            listOf(StremioCatalog("movie", "cat", "Cat", extra = listOf(requiredGenre))),
            selections = mapOf("movie/cat/genre" to listOf("Anal")),
        )
        assertEquals(mapOf("movie/cat/genre" to listOf("Anal")), a.seedRequiredSelections().selections)
    }

    @Test fun ignores_optional_extras_and_is_identity_when_nothing_to_seed() {
        val a = addon(listOf(StremioCatalog("movie", "top", "Top", extra = listOf(optionalGenre))))
        assertSame(a, a.seedRequiredSelections())
    }

    @Test fun is_idempotent() {
        val once = addon(listOf(StremioCatalog("movie", "cat", "Cat", extra = listOf(requiredGenre))))
            .seedRequiredSelections()
        assertSame(once, once.seedRequiredSelections())
    }

    @Test fun seeded_required_extra_catalog_expands_to_one_option_spec() {
        val seeded = addon(listOf(StremioCatalog("movie", "cat", "Cat", extra = listOf(requiredGenre))))
            .seedRequiredSelections()
        val specs = expandCatalogSpecs(listOf(seeded))
        assertEquals(listOf("Amateur"), specs.map { it.option })
        assertEquals(listOf("genre"), specs.map { it.extraName })
    }
}
