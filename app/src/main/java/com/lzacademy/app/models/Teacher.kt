package com.lzacademy.app.models

import java.time.LocalDate

/**
 * A teacher of the academy, as exposed to the UI.
 *
 * [isFeaturedOnStudentHome] and [acceptsStudentMessages] are two **independent** settings:
 * the first controls the student-home showcase, the second controls whether a student may
 * start a conversation. Never merge the two in the UI.
 */
data class Teacher(
    val id: String,
    val fullName: String,
    val photoUrl: String? = null,
    val subjects: List<String> = emptyList(),
    val professionalDescription: String = "",
    val isFeaturedOnStudentHome: Boolean = false,
    val acceptsStudentMessages: Boolean = false,
    val featuredRotationPriority: Int = 0,
    val lastFeaturedAt: LocalDate? = null,
    val levels: List<String> = emptyList(),
    val yearsOfExperience: Int = 0,
    val groups: Int = 0,
    val isActive: Boolean = true,
) {
    /** "KL" for "M. Karim Lebbah" — the honorific is ignored. */
    val initials: String
        get() = fullName
            .replace(Regex("^(M\\.|MM\\.|Mme|Mlle)\\s+"), "")
            .split(' ', '-')
            .mapNotNull { it.firstOrNull()?.takeIf(Char::isLetter)?.uppercaseChar() }
            .take(2)
            .joinToString("")

    val primarySubject: String get() = subjects.firstOrNull() ?: "Enseignement"

    val subjectLabel: String get() = subjects.joinToString(" • ")

    val availabilityLabel: String get() = if (acceptsStudentMessages) "Disponible" else "Hors ligne"

    val showcaseLabel: String get() = if (isFeaturedOnStudentHome) "Sur l'accueil élèves" else "Retiré de l'accueil"

    val levelsLabel: String get() = levels.joinToString(" • ")

    val experienceLabel: String
        get() = when (yearsOfExperience) {
            0 -> "Nouveau dans l'équipe"
            1 -> "1 an d'expérience"
            else -> "$yearsOfExperience ans d'expérience"
        }
}

/** State of a message queued from the student app towards a teacher. */
enum class TeacherMessageStatus(val label: String) {
    Pending("En attente d'envoi"),
}

/**
 * A message written by a student. It is stored on the device only: this build has no network
 * messaging backend, so a message is never reported as "delivered" — only "queued locally".
 */
data class TeacherMessage(
    val id: Long,
    val teacherId: String,
    val body: String,
    val createdAtMillis: Long,
    val status: TeacherMessageStatus = TeacherMessageStatus.Pending,
)
