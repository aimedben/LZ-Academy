package com.lzacademy.app

import com.lzacademy.app.repository.realignedCounter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The local counter ledger must never keep an obsolete pack size: the real pack is
 * 4 sessions (381 rows in `public.subscriptions` say so), while older local rows hold 48.
 * When the pack size changes, the mirror wins; otherwise the local consumption stands.
 */
class SubscriptionRealignTest {

    @Test
    fun `un ancien pack de 48 est ramene au pack reel de 4`() {
        assertEquals(
            4 to 4,
            realignedCounter(localTotal = 48, localRemaining = 36, remoteTotal = 4, remoteRemaining = 4),
        )
    }

    @Test
    fun `un pack inchange conserve le compteur local deja consomme`() {
        assertNull(realignedCounter(localTotal = 4, localRemaining = 2, remoteTotal = 4, remoteRemaining = 4))
    }

    @Test
    fun `un nouveau pack remplace l ancien`() {
        assertEquals(
            8 to 8,
            realignedCounter(localTotal = 4, localRemaining = 0, remoteTotal = 8, remoteRemaining = 8),
        )
    }

    @Test
    fun `un pack inconnu en base ne detruit pas le compteur local`() {
        assertNull(realignedCounter(localTotal = 4, localRemaining = 2, remoteTotal = 0, remoteRemaining = 0))
    }
}
