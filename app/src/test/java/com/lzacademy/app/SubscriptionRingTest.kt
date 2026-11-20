package com.lzacademy.app

import com.lzacademy.app.components.RingSegments
import com.lzacademy.app.components.ringSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The subscription ring of the student profile: one arc per session of the real pack
 * (4 sessions today). A session is consumed when the teacher's roll-call decrements
 * the local counter, so `remaining` is what the counter holds.
 */
class SubscriptionRingTest {

    @Test
    fun `quatre seances dont deux faites donnent deux segments pleins`() {
        assertEquals(RingSegments(total = 4, filled = 2, empty = 2, hasSegments = true), ringSegments(remaining = 2, total = 4))
    }

    @Test
    fun `remaining negatif ou superieur au total est borne`() {
        assertEquals(RingSegments(4, 4, 0, true), ringSegments(remaining = -3, total = 4))
        assertEquals(RingSegments(4, 0, 4, true), ringSegments(remaining = 9, total = 4))
    }

    @Test
    fun `total nul ou negatif ne dessine aucun segment`() {
        assertEquals(RingSegments(0, 0, 0, false), ringSegments(remaining = 0, total = 0))
        assertFalse(ringSegments(remaining = 2, total = -1).hasSegments)
    }

    @Test
    fun `au dela de douze seances le total est plafonne a douze`() {
        assertEquals(12, ringSegments(remaining = 20, total = 20).total)
    }
}
