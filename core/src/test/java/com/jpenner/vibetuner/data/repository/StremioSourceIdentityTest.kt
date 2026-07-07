package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.stremio.StremioCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StremioSourceIdentityTest {
    private val catalog = StremioCatalog("movie", "top", "Top")

    @Test fun base_identity_is_unchanged_three_part() {
        assertEquals("stremio:a:movie:top", stremioSourceKey("a", catalog))
        assertEquals("a|movie|top", stremioSourceValue("a", catalog))
        assertEquals(StremioSource("a", "movie", "top"), parseStremioSource("a|movie|top"))
    }

    @Test fun option_identity_appends_extra_equals_option() {
        assertEquals("stremio:a:movie:top:genre=Action", stremioSourceKey("a", catalog, "genre", "Action"))
        assertEquals("a|movie|top|genre=Action", stremioSourceValue("a", catalog, "genre", "Action"))
        assertEquals(
            StremioSource("a", "movie", "top", "genre", "Action"),
            parseStremioSource("a|movie|top|genre=Action"),
        )
    }

    @Test fun option_values_may_contain_equals() {
        assertEquals(
            StremioSource("a", "movie", "top", "genre", "Sci=Fi"),
            parseStremioSource("a|movie|top|genre=Sci=Fi"),
        )
    }

    @Test fun malformed_sources_parse_to_null() {
        assertNull(parseStremioSource("a|movie"))
        assertNull(parseStremioSource("a||top"))
        assertNull(parseStremioSource("a|movie|top|no-equals"))
        assertNull(parseStremioSource("a|movie|top|=Action"))
        assertNull(parseStremioSource("a|movie|top|genre="))
        assertNull(parseStremioSource("a|movie|top|genre=Action|extra"))
    }
}
