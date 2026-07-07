package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.ChannelOverride
import com.jpenner.vibetuner.data.model.SourceType
import com.jpenner.vibetuner.data.model.stremio.StremioAddon
import com.jpenner.vibetuner.data.model.stremio.StremioCatalog
import com.jpenner.vibetuner.data.model.stremio.StremioExtra
import com.jpenner.vibetuner.data.model.stremio.StremioManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LineupBuilderTest {
    private fun addon(id: String, name: String, catalogs: List<StremioCatalog>) =
        StremioAddon("https://$id/manifest.json",
            StremioManifest(id, name, "1.0.0", resources = listOf("catalog"), catalogs = catalogs))

    private val cinemeta = addon("com.linvo.cinemeta", "Cinemeta", listOf(
        StremioCatalog("movie", "top", "Popular Movies"),
        StremioCatalog("series", "top", "Popular Series"),
    ))
    private fun specs(a: StremioAddon) = expandCatalogSpecs(listOf(a))

    @Test fun builds_default_channels_from_catalogs() {
        val lineup = buildLineup(specs(cinemeta), emptyMap())
        assertEquals(2, lineup.size)
        val movie = lineup.first { it.sourceKey == "stremio:com.linvo.cinemeta:movie:top" }
        assertEquals("Popular Movies", movie.name)
        assertEquals(SourceType.STREMIO_CATALOG, movie.sourceType)
        assertEquals("stremio:com.linvo.cinemeta:movie:top", movie.id) // deterministic id
        assertEquals("RANDOM", movie.sortingRule)
        assertEquals(Category.Movies, movie.category)
        // series catalog now defaults from its type, not Movies:
        assertEquals(Category.Series,
            lineup.first { it.sourceKey == "stremio:com.linvo.cinemeta:series:top" }.category)
    }

    @Test fun applies_overrides_by_source_key() {
        val key = "stremio:com.linvo.cinemeta:series:top"
        val overrides = mapOf(key to ChannelOverride(
            name = "Binge TV", category = "Horror", mode = "CHRONOLOGICAL",
            marathonLimit = 3, enabled = false, orderIndex = 0))
        val lineup = buildLineup(specs(cinemeta), overrides)
        val tv = lineup.first { it.sourceKey == key }
        assertEquals("Binge TV", tv.name)
        assertEquals(Category.Horror, tv.category)
        assertEquals("CHRONOLOGICAL", tv.sortingRule)
        assertEquals(3, tv.marathonLimit)
        assertFalse(tv.enabled)
    }

    @Test fun orders_by_override_orderIndex_then_assigns_numbers() {
        val movieKey = "stremio:com.linvo.cinemeta:movie:top"
        val seriesKey = "stremio:com.linvo.cinemeta:series:top"
        val overrides = mapOf(
            movieKey to ChannelOverride(orderIndex = 1),
            seriesKey to ChannelOverride(orderIndex = 0),
        )
        val lineup = buildLineup(specs(cinemeta), overrides)
        assertEquals(seriesKey, lineup[0].sourceKey)
        assertEquals("100", lineup[0].number)
        assertEquals(movieKey, lineup[1].sourceKey)
        assertEquals("110", lineup[1].number)
    }

    @Test fun reorder_swaps_neighbor() {
        val order = listOf("a", "b", "c")
        assertEquals(listOf("a", "c", "b"), reorderedSourceKeys(order, "b", up = false))
        assertEquals(listOf("b", "a", "c"), reorderedSourceKeys(order, "b", up = true))
        assertEquals(order, reorderedSourceKeys(order, "a", up = true)) // no-op at edge
    }

    @Test fun populates_catalog_source_provenance() {
        val movie = buildLineup(specs(cinemeta), emptyMap())
            .first { it.sourceKey == "stremio:com.linvo.cinemeta:movie:top" }
        val src = requireNotNull(movie.source)
        assertEquals("com.linvo.cinemeta", src.addonId)
        assertEquals("Cinemeta", src.addonName)
        assertEquals("movie", src.type)
        assertEquals("top", src.catalogId)
        assertEquals("Popular Movies", src.catalogName)
        assertEquals("stremio:com.linvo.cinemeta:movie:top", src.sourceKey)
        assertNull(src.itemCount)
    }

    @Test fun option_channels_default_name_and_category_from_option() {
        val a = StremioAddon("https://x/manifest.json",
            StremioManifest("com.x", "X", "1", resources = listOf("catalog"), catalogs = listOf(
                StremioCatalog("movie", "top", "Top",
                    extra = listOf(StremioExtra("genre", options = listOf("Horror", "Weird")))))),
            selections = mapOf("movie/top/genre" to listOf("Horror", "Weird")))
        val lineup = buildLineup(expandCatalogSpecs(listOf(a)), emptyMap())
        assertEquals(3, lineup.size)

        val horror = lineup.first { it.sourceKey == "stremio:com.x:movie:top:genre=Horror" }
        assertEquals("Horror", horror.name)
        assertEquals(Category.Horror, horror.category)          // option matches a genre label
        assertEquals("com.x|movie|top|genre=Horror", horror.sourceValue)
        assertEquals("Horror", horror.source?.option)

        val weird = lineup.first { it.sourceKey == "stremio:com.x:movie:top:genre=Weird" }
        assertEquals(Category.Movies, weird.category)           // falls back to the catalog type
    }

    @Test fun overrides_still_win_over_type_defaults() {
        val overrides = mapOf("stremio:com.linvo.cinemeta:series:top" to ChannelOverride(category = "Kids"))
        val lineup = buildLineup(specs(cinemeta), overrides)
        assertEquals(Category.Kids,
            lineup.first { it.sourceKey == "stremio:com.linvo.cinemeta:series:top" }.category)
    }
}
