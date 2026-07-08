package com.jpenner.vibetuner.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MarathonProgressStoreTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun store(name: String = "progress.json") = MarathonProgressStore(File(tmp.root, name))

    @Test
    fun `unknown channel starts every show at zero`() {
        assertTrue(store().startPointersFor("p1", "ch1", 100L).isEmpty())
    }

    @Test
    fun `same day rebuild returns the day's start pointers`() {
        val s = store()
        s.save("p1", "ch1", 100L, startPointers = mapOf("A" to 2), endPointers = mapOf("A" to 5))
        assertEquals(mapOf("A" to 2), s.startPointersFor("p1", "ch1", 100L))
    }

    @Test
    fun `a later day resumes from the previous day's end pointers`() {
        val s = store()
        s.save("p1", "ch1", 100L, startPointers = mapOf("A" to 2), endPointers = mapOf("A" to 5))
        assertEquals(mapOf("A" to 5), s.startPointersFor("p1", "ch1", 101L))
        assertEquals(mapOf("A" to 5), s.startPointersFor("p1", "ch1", 107L)) // skipped days don't advance
    }

    @Test
    fun `progress survives a new instance over the same file`() {
        val f = File(tmp.root, "persist.json")
        MarathonProgressStore(f).save("p1", "ch1", 100L, mapOf("A" to 1), mapOf("A" to 3))
        assertEquals(mapOf("A" to 1), MarathonProgressStore(f).startPointersFor("p1", "ch1", 100L))
    }

    @Test
    fun `profiles and channels are isolated`() {
        val s = store()
        s.save("p1", "ch1", 100L, mapOf("A" to 1), mapOf("A" to 2))
        s.save("p1", "ch2", 100L, mapOf("A" to 7), mapOf("A" to 8))
        s.save("p2", "ch1", 100L, mapOf("A" to 4), mapOf("A" to 6))
        assertEquals(mapOf("A" to 1), s.startPointersFor("p1", "ch1", 100L))
        assertEquals(mapOf("A" to 7), s.startPointersFor("p1", "ch2", 100L))
        assertEquals(mapOf("A" to 4), s.startPointersFor("p2", "ch1", 100L))
    }
}
