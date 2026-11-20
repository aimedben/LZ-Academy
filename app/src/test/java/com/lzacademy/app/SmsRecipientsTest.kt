package com.lzacademy.app

import com.lzacademy.app.data.sms.SmsRecipients
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Selection rules of the admin SMS screen, pinned on the Room mirror shape:
 * only active students, normalised phones, honest level labels and search.
 */
class SmsRecipientsTest {

    private fun student(
        matricule: String,
        name: String,
        phone: String?,
        level: String? = "3AM",
        active: Boolean = true,
    ) = SyncedStudentEntity(
        remoteId = "r_$matricule",
        matricule = matricule,
        fullName = name,
        phone = phone,
        levelName = level,
        avatarPath = null,
        isActive = active,
        profileId = null,
        createdAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `ne garde que les eleves actifs et normalise les numeros`() {
        val rows = listOf(
            student("LZ-1", "Amine Bensalem", "0555 12 34 56"),
            student("LZ-2", "Inactif Test", "0555000000", active = false),
            student("LZ-3", "Sans Numero", null, level = null),
        )
        val out = SmsRecipients.fromStudents(rows)
        assertEquals(listOf("LZ-1", "LZ-3"), out.map { it.matricule })
        assertEquals("0555123456", out[0].phone)
        assertTrue(out[0].canReceive)
        assertNull(out[1].phone)
        assertEquals("Non affecté", out[1].levelName)
    }

    @Test
    fun `un miroir vide ne produit aucun destinataire`() {
        val out = SmsRecipients.fromStudents(emptyList())
        assertTrue(out.isEmpty())
        assertTrue(SmsRecipients.levels(out).isEmpty())
    }

    @Test
    fun `filtre par recherche et par niveau`() {
        val out = SmsRecipients.fromStudents(
            listOf(
                student("LZ-1", "Amine Bensalem", "0555123456", "3AM"),
                student("LZ-2", "Nadia Cherif", "0666778899", "2AS"),
            ),
        )
        assertEquals(listOf("LZ-1"), SmsRecipients.filter(out, "amine", null).map { it.matricule })
        assertEquals(listOf("LZ-2"), SmsRecipients.filter(out, "0666", null).map { it.matricule })
        assertEquals(listOf("LZ-2"), SmsRecipients.filter(out, "", "2AS").map { it.matricule })
        assertEquals(2, SmsRecipients.filter(out, "", null).size)
        assertEquals(listOf("2AS", "3AM"), SmsRecipients.levels(out))
    }
}
