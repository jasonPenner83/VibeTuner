package com.jpenner.vibetuner.data.api

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogPathTest {
    @Test fun bare_catalog_path() {
        assertEquals("/catalog/movie/top.json", catalogPath("movie", "top", extra = null, skip = 0))
    }

    @Test fun skip_only_keeps_legacy_shape() {
        assertEquals("/catalog/movie/top/skip=100.json", catalogPath("movie", "top", null, skip = 100))
    }

    @Test fun extra_is_url_encoded_and_combined_with_skip() {
        assertEquals(
            "/catalog/movie/top/genre=Sci-Fi%20%26%20Fantasy.json",
            catalogPath("movie", "top", "genre" to "Sci-Fi & Fantasy", skip = 0),
        )
        assertEquals(
            "/catalog/movie/top/genre=Action&skip=100.json",
            catalogPath("movie", "top", "genre" to "Action", skip = 100),
        )
    }
}
