package com.lzacademy.app

import com.lzacademy.app.data.settings.ADMIN_ALERTS
import com.lzacademy.app.data.settings.ADMIN_AUTO_SYNC
import com.lzacademy.app.data.settings.STUDENT_NOTIFICATIONS
import com.lzacademy.app.data.settings.SettingsSection
import com.lzacademy.app.data.settings.notificationSwitchesFor
import com.lzacademy.app.models.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every switch shown in Settings must have a real effect, so this catalogue is pinned here:
 * the ids the screens read back, and the defaults a fresh install starts from.
 */
class NotificationSettingsTest {

    @Test
    fun `l eleve a un seul interrupteur local actif par defaut`() {
        val switches = notificationSwitchesFor(UserRole.Student)
        assertEquals(1, switches.size)
        assertEquals(STUDENT_NOTIFICATIONS, switches.single().id)
        assertTrue(switches.single().default)
    }

    @Test
    fun `l admin a les alertes et la synchronisation automatique`() {
        val ids = notificationSwitchesFor(UserRole.Admin).map { it.id }
        assertEquals(listOf(ADMIN_ALERTS, ADMIN_AUTO_SYNC), ids)
    }

    @Test
    fun `seule la synchronisation est rangee dans la section donnees`() {
        val admin = notificationSwitchesFor(UserRole.Admin).associateBy { it.id }
        assertEquals(SettingsSection.Notifications, admin.getValue(ADMIN_ALERTS).section)
        assertEquals(SettingsSection.Data, admin.getValue(ADMIN_AUTO_SYNC).section)
    }

    @Test
    fun `l enseignant n a aucun interrupteur local`() {
        // His two settings are server columns of public.teachers, not local preferences.
        assertTrue(notificationSwitchesFor(UserRole.Teacher).isEmpty())
    }

    @Test
    fun `aucun identifiant n est vide ni duplique`() {
        val ids = UserRole.entries.flatMap { notificationSwitchesFor(it).map { s -> s.id } }
        assertFalse(ids.any { it.isBlank() })
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `chaque interrupteur est decrit`() {
        UserRole.entries.flatMap { notificationSwitchesFor(it) }.forEach { switch ->
            assertTrue(switch.title.isNotBlank())
            assertTrue(switch.description.isNotBlank())
        }
    }
}
