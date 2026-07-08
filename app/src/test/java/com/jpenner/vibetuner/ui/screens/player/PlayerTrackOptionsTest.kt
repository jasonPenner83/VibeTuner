package com.jpenner.vibetuner.ui.screens.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTrackOptionsTest {

    // One format per group: multi-language groups trip TrackGroup's
    // android.util.Log validation on the JVM.
    private fun audioGroup(
        lang: String? = null,
        label: String? = null,
        channels: Int = 2,
        selected: Boolean = false,
    ): Tracks.Group {
        val format = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_AAC)
            .setLanguage(lang)
            .setLabel(label)
            .setChannelCount(channels)
            .build()
        return Tracks.Group(
            TrackGroup(format), false,
            intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(selected),
        )
    }

    private fun textGroup(lang: String? = null, selected: Boolean = false): Tracks.Group {
        val format = Format.Builder()
            .setSampleMimeType(MimeTypes.TEXT_VTT)
            .setLanguage(lang)
            .build()
        return Tracks.Group(
            TrackGroup(format), false,
            intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(selected),
        )
    }

    private fun videoGroup(): Tracks.Group {
        val format = Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).build()
        return Tracks.Group(
            TrackGroup(format), false,
            intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(true),
        )
    }

    @Test
    fun `audio options use language display name and channel layout`() {
        val tracks = Tracks(listOf(audioGroup(lang = "en", channels = 6, selected = true)))
        val opts = audioOptions(tracks)
        assertEquals(1, opts.size)
        assertEquals("English · 5.1", opts[0].label)
        assertTrue(opts[0].selected)
    }

    @Test
    fun `audio options prefer the format label when present`() {
        val tracks = Tracks(listOf(audioGroup(label = "Director Commentary", channels = 2)))
        assertEquals("Director Commentary · Stereo", audioOptions(tracks)[0].label)
    }

    @Test
    fun `audio options fall back to Track n when metadata is missing`() {
        val tracks = Tracks(listOf(
            audioGroup(channels = Format.NO_VALUE),
            audioGroup(channels = Format.NO_VALUE),
        ))
        val opts = audioOptions(tracks)
        assertEquals(listOf("Track 1", "Track 2"), opts.map { it.label })
    }

    @Test
    fun `option ids index the full group list including video groups`() {
        val tracks = Tracks(listOf(videoGroup(), audioGroup(lang = "en"), audioGroup(lang = "fr")))
        val opts = audioOptions(tracks)
        assertEquals(listOf("1:0", "2:0"), opts.map { it.id })
    }

    @Test
    fun `subtitle options start with Off, selected when no text track is on`() {
        val tracks = Tracks(listOf(textGroup(lang = "en", selected = false)))
        val opts = subtitleOptions(tracks)
        assertEquals(TRACK_OFF_ID, opts[0].id)
        assertEquals("Off", opts[0].label)
        assertTrue(opts[0].selected)
        assertEquals("English", opts[1].label)
        assertFalse(opts[1].selected)
    }

    @Test
    fun `subtitle Off is unselected when a text track is active`() {
        val tracks = Tracks(listOf(textGroup(lang = "en", selected = true)))
        val opts = subtitleOptions(tracks)
        assertFalse(opts[0].selected)
        assertTrue(opts[1].selected)
    }

    @Test
    fun `no subtitle tracks yields just the Off entry`() {
        val opts = subtitleOptions(Tracks(listOf(videoGroup(), audioGroup())))
        assertEquals(1, opts.size)
        assertEquals(TRACK_OFF_ID, opts[0].id)
        assertTrue(opts[0].selected)
    }
}
