package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuideCacheTest {

    private fun ch(
        key: String,
        name: String = "N",
        sorting: String = "RANDOM",
        marathon: Int? = null,
        order: Int = 0,
        enabled: Boolean = true,
        category: Category = Category.DEFAULT,
    ) = Channel(
        id = key, name = name, abbreviation = "N", description = "",
        number = "100", category = category, sortingRule = sorting,
        marathonLimit = marathon, orderIndex = order, sourceKey = key, enabled = enabled,
    )

    @Test fun signature_is_stable_for_identical_lineups() {
        val a = listOf(ch("k1"), ch("k2"))
        val b = listOf(ch("k1"), ch("k2"))
        assertEquals(lineupSignature(a), lineupSignature(b))
    }

    @Test fun signature_changes_on_rename() {
        assertNotEquals(
            lineupSignature(listOf(ch("k1", name = "Old"))),
            lineupSignature(listOf(ch("k1", name = "New"))),
        )
    }

    @Test fun signature_changes_on_sorting_rule() {
        assertNotEquals(
            lineupSignature(listOf(ch("k1", sorting = "RANDOM"))),
            lineupSignature(listOf(ch("k1", sorting = "CHRONOLOGICAL"))),
        )
    }

    @Test fun signature_changes_on_enabled_flag() {
        assertNotEquals(
            lineupSignature(listOf(ch("k1", enabled = true))),
            lineupSignature(listOf(ch("k1", enabled = false))),
        )
    }

    @Test fun signature_changes_on_category() {
        assertNotEquals(
            lineupSignature(listOf(ch("k1", category = Category.DEFAULT))),
            lineupSignature(listOf(ch("k1", category = Category.Horror))),
        )
    }

    @Test fun signature_changes_on_source_key() {
        assertNotEquals(lineupSignature(listOf(ch("k1"))), lineupSignature(listOf(ch("k2"))))
    }

    @Test fun signature_changes_on_marathon_limit() {
        assertNotEquals(lineupSignature(listOf(ch("k1", marathon = null))), lineupSignature(listOf(ch("k1", marathon = 3))))
    }

    @Test fun signature_changes_on_order_index() {
        assertNotEquals(lineupSignature(listOf(ch("k1", order = 0))), lineupSignature(listOf(ch("k1", order = 1))))
    }

    @Test fun cache_returns_value_only_for_matching_key() {
        val cache = GuideCache()
        val channels = listOf(ch("k1"))
        cache.put("key-A", channels)
        assertEquals(channels, cache.get("key-A"))
        assertNull(cache.get("key-B"))
    }

    @Test fun clear_forgets_the_held_entry() {
        val cache = GuideCache()
        cache.put("key-A", listOf(ch("k1")))
        cache.clear()
        assertNull(cache.get("key-A"))
    }
}
