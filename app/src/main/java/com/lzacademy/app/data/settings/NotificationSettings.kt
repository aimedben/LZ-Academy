package com.lzacademy.app.data.settings

import com.lzacademy.app.models.UserRole

/**
 * Grouping of the switches inside the Settings screen.
 */
enum class SettingsSection {
    /** Alerts and reminders. */
    Notifications,

    /** Data transfer and synchronisation. */
    Data,
}

/**
 * One switch of the Settings screen. Every switch here has a REAL effect: either a local
 * preference the app reads back, or a server column the app already writes. Nothing decorative
 * — the project rule is "real data only".
 */
data class NotificationSwitch(
    val id: String,
    val title: String,
    val description: String,
    val default: Boolean,
    val section: SettingsSection = SettingsSection.Notifications,
)

/**
 * Local switches shown to each role, in display order.
 *
 * The teacher has none: his two settings ("recevoir les messages", "afficher mon profil sur
 * l'accueil des élèves") are server columns of `public.teachers`, rendered from the teacher row
 * itself rather than from a local preference.
 */
fun notificationSwitchesFor(role: UserRole): List<NotificationSwitch> = when (role) {
    UserRole.Student -> listOf(
        NotificationSwitch(
            id = STUDENT_NOTIFICATIONS,
            title = "Notifications de l'académie",
            description = "Afficher la pastille des annonces non lues sur le tableau de bord.",
            default = true,
        ),
    )

    UserRole.Teacher -> emptyList()

    UserRole.Admin -> listOf(
        NotificationSwitch(
            id = ADMIN_ALERTS,
            title = "Alertes de gestion",
            description = "Afficher la section « Alertes » du tableau de bord (abonnements épuisés, paiements).",
            default = true,
        ),
        NotificationSwitch(
            id = ADMIN_AUTO_SYNC,
            title = "Synchronisation automatique",
            description = "Récupérer les données de l'académie à l'ouverture. Désactivée, seuls les rafraîchissements demandés passent.",
            default = true,
            section = SettingsSection.Data,
        ),
    )
}

/** Key of the student's notification badge preference. */
const val STUDENT_NOTIFICATIONS = "student.notifications"

/** Key of the admin dashboard alerts preference. */
const val ADMIN_ALERTS = "admin.alerts"

/** Key of the admin automatic-sync preference. */
const val ADMIN_AUTO_SYNC = "admin.autoSync"
