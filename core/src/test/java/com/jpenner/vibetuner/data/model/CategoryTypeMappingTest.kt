package com.jpenner.vibetuner.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryTypeMappingTest {

    @Test fun maps_core_stremio_types() {
        assertEquals(Category.Movies, Category.forStremioType("movie"))
        assertEquals(Category.Series, Category.forStremioType("series"))
        assertEquals(Category.Series, Category.forStremioType("tv"))
        assertEquals(Category.Series, Category.forStremioType("channel"))
        assertEquals(Category.Animation, Category.forStremioType("anime"))
        assertEquals(Category.Documentary, Category.forStremioType("docs"))
    }

    @Test fun types_matching_a_genre_label_map_directly() {
        assertEquals(Category.News, Category.forStremioType("News"))
        assertEquals(Category.Sports, Category.forStremioType("sports"))
        assertEquals(Category.Music, Category.forStremioType("music"))
        assertEquals(Category.Kids, Category.forStremioType("kids"))
        assertEquals(Category.Action, Category.forStremioType("action"))
    }

    @Test fun unknown_or_blank_types_fall_back_to_default() {
        assertEquals(Category.DEFAULT, Category.forStremioType("events"))
        assertEquals(Category.DEFAULT, Category.forStremioType(""))
    }

    @Test fun fromLabelOrNull_is_strict() {
        assertEquals(Category.Horror, Category.fromLabelOrNull("horror"))
        assertNull(Category.fromLabelOrNull("not-a-genre"))
    }
}
