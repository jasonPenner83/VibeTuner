package com.jpenner.vibetuner.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSourceTest {
    private fun src(type: String, count: Int?) = CatalogSource(
        addonId = "com.linvo.cinemeta", addonName = "Cinemeta", addonAbbrev = "CI",
        type = type, catalogId = "top", catalogName = "Popular", itemCount = count,
    )

    @Test fun type_label_maps_series_and_movie() {
        assertEquals("Series", src("series", 0).typeLabel)
        assertEquals("Movie", src("movie", 0).typeLabel)
    }

    @Test fun library_label_formats_by_type_with_grouping() {
        assertEquals("1,850 titles", src("movie", 1850).libraryLabel)
        assertEquals("1,240 shows", src("series", 1240).libraryLabel)
    }

    @Test fun library_label_is_em_dash_when_count_unknown() {
        assertEquals("—", src("movie", null).libraryLabel)
    }

    @Test fun source_key_matches_stremio_scheme() {
        assertEquals("stremio:com.linvo.cinemeta:movie:top", src("movie", 5).sourceKey)
    }

    @Test fun option_source_key_and_label() {
        val s = src("movie", null).copy(extraName = "genre", option = "Action")
        assertEquals("stremio:com.linvo.cinemeta:movie:top:genre=Action", s.sourceKey)
        assertEquals("Popular · Action", s.catalogLabel)
        assertEquals("Popular", src("movie", null).catalogLabel)
    }
}
