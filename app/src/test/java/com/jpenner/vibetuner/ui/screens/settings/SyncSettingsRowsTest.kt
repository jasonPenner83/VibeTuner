package com.jpenner.vibetuner.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSettingsRowsTest {

    @Test
    fun `unconfigured shows a single info row`() {
        val rows = syncRows(SyncSettingsState(configured = false, email = null, lastSyncMs = null, error = null))
        assertEquals(listOf("sync_status"), rows.map { it.key })
    }

    @Test
    fun `signed out offers google sign-in`() {
        val rows = syncRows(SyncSettingsState(configured = true, email = null, lastSyncMs = null, error = null))
        assertEquals(listOf("sync_status", "sync_signin"), rows.map { it.key })
    }

    @Test
    fun `signed in shows account, last sync, sync now and sign out`() {
        val rows = syncRows(
            SyncSettingsState(configured = true, email = "j@x.com", lastSyncMs = 1_720_000_000_000, error = null)
        )
        assertEquals(listOf("sync_status", "sync_last", "sync_now", "sync_signout"), rows.map { it.key })
        assertTrue(rows.first { it.key == "sync_status" }.sub.contains("j@x.com"))
    }

    @Test
    fun `error is surfaced on the last-sync row`() {
        val rows = syncRows(
            SyncSettingsState(configured = true, email = "j@x.com", lastSyncMs = null, error = "boom")
        )
        assertTrue(rows.first { it.key == "sync_last" }.sub.contains("boom"))
    }
}
