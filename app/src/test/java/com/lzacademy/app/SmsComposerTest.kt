package com.lzacademy.app

import com.lzacademy.app.data.sms.SmsComposer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phone normalisation, multi-recipient URI and batching for the admin SMS screen.
 * Pure JVM: no Android dependency, so the behaviour is pinned without a device.
 */
class SmsComposerTest {

    @Test
    fun `normalise espaces points tirets et parentheses`() {
        assertEquals("0555123456", SmsComposer.normalizePhone("0555 12.34-56"))
        assertEquals("0555123456", SmsComposer.normalizePhone("(0) 555 12 34 56"))
    }

    @Test
    fun `conserve l indicatif international`() {
        assertEquals("+213555123456", SmsComposer.normalizePhone("+213 555 12 34 56"))
        assertEquals("+213555123456", SmsComposer.normalizePhone(" +213 555 12 34 56 "))
    }

    @Test
    fun `refuse un numero trop court ou vide`() {
        assertNull(SmsComposer.normalizePhone(null))
        assertNull(SmsComposer.normalizePhone(""))
        assertNull(SmsComposer.normalizePhone("12"))
        assertNull(SmsComposer.normalizePhone("abc"))
        assertNull(SmsComposer.normalizePhone("+"))
    }

    @Test
    fun `construit l uri smsto dans l ordre recu`() {
        assertEquals("smsto:", SmsComposer.smsUri(emptyList()))
        assertEquals("smsto:0555123456", SmsComposer.smsUri(listOf("0555123456")))
        assertEquals(
            "smsto:0555123456;+213666778899",
            SmsComposer.smsUri(listOf("0555123456", "+213666778899")),
        )
    }

    @Test
    fun `decoupe en lots de vingt`() {
        val phones = (1..45).map { "05550000" + it.toString().padStart(2, '0') }
        val lots = SmsComposer.batches(phones)
        assertEquals(listOf(20, 20, 5), lots.map { it.size })
        assertEquals(phones, lots.flatten())
        assertEquals(emptyList<List<String>>(), SmsComposer.batches(emptyList()))
        assertEquals(1, SmsComposer.batches(phones.take(20)).size)
    }
}
