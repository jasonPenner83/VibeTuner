package com.jpenner.vibetuner.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogRuntimeParsingTest {

    private fun catalogBody(metaJson: String) = """{ "metas": [ $metaJson ] }"""

    @Test fun parses_plain_minute_runtimes() {
        val items = parseCatalogMetas(
            catalogBody("""{ "id": "tt1", "name": "A", "type": "movie", "runtime": "148 min" }"""),
            "movie")
        assertEquals(148f, items.single().durationMinutes, 0.001f)
    }

    @Test fun parses_hour_minute_runtimes() {
        val items = parseCatalogMetas(
            catalogBody("""{ "id": "tt1", "name": "A", "type": "movie", "runtime": "1h 22min" }"""),
            "movie")
        assertEquals(82f, items.single().durationMinutes, 0.001f)
    }

    @Test fun missing_runtime_is_the_zero_unknown_sentinel_not_a_default() {
        val movie = parseCatalogMetas(
            catalogBody("""{ "id": "tt1", "name": "A", "type": "movie" }"""), "movie")
        val series = parseCatalogMetas(
            catalogBody("""{ "id": "tt2", "name": "B", "type": "series" }"""), "series")
        assertEquals(0f, movie.single().durationMinutes, 0.001f)
        assertEquals(0f, series.single().durationMinutes, 0.001f)
    }

    @Test fun meta_runtime_parses_when_present_and_is_null_when_absent() {
        assertEquals(
            63f,
            parseMetaRuntimeMinutes("""{ "meta": { "id": "tt1", "runtime": "63 min" } }""")!!,
            0.001f)
        assertNull(parseMetaRuntimeMinutes("""{ "meta": { "id": "tt1" } }"""))
        assertNull(parseMetaRuntimeMinutes("not json"))
    }
}
