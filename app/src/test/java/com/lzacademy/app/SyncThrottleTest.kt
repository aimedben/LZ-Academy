package com.lzacademy.app

import com.lzacademy.app.repository.SupabaseSyncRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Throttle rule of the automatic sync: 60 min, never when the mirror is empty. */
class SyncThrottleTest {

    @Test
    fun `saute la sync si moins de soixante minutes et miroir present`() {
        assertFalse(SupabaseSyncRepository.shouldSync(0L, 59 * 60 * 1000L, true))
    }

    @Test
    fun `synchronise apres soixante minutes`() {
        assertTrue(SupabaseSyncRepository.shouldSync(0L, 60 * 60 * 1000L, true))
    }

    @Test
    fun `synchronise toujours si le miroir est vide`() {
        assertTrue(SupabaseSyncRepository.shouldSync(0L, 1L, false))
    }

    @Test
    fun `la fenetre de throttle est bien d une heure`() {
        assertEquals(60 * 60 * 1000L, SupabaseSyncRepository.MIN_SYNC_INTERVAL_MS)
    }

    @Test
    fun `ne synchronise jamais quand la synchronisation automatique est desactivee`() {
        // Admin turned the switch off: even a stale mirror and an empty mirror stay untouched.
        assertFalse(SupabaseSyncRepository.shouldSync(0L, 10 * 60 * 60 * 1000L, true, autoSyncEnabled = false))
        assertFalse(SupabaseSyncRepository.shouldSync(0L, 10 * 60 * 60 * 1000L, false, autoSyncEnabled = false))
    }
}
