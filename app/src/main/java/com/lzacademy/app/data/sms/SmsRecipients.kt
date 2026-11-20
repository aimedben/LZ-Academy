package com.lzacademy.app.data.sms

import com.lzacademy.app.data.supabase.SyncedStudentEntity

/**
 * One selectable SMS recipient, derived from the offline Room mirror of public.students.
 * [phone] is already normalised; null means "no usable number" and the row is not selectable.
 */
data class SmsRecipient(
    val matricule: String,
    val fullName: String,
    val phone: String?,
    val levelName: String,
) {
    val canReceive: Boolean get() = phone != null
}

/** Selection rules for the admin SMS composer — pure JVM, no Android dependency. */
object SmsRecipients {

    const val UNASSIGNED_LEVEL = "Non affecté"

    /** Active students only, phones normalised, sorted by name. */
    fun fromStudents(students: List<SyncedStudentEntity>): List<SmsRecipient> =
        students
            .asSequence()
            .filter { it.isActive }
            .map {
                SmsRecipient(
                    matricule = it.matricule,
                    fullName = it.fullName,
                    phone = SmsComposer.normalizePhone(it.phone),
                    levelName = it.levelName?.trim()?.takeIf(String::isNotEmpty) ?: UNASSIGNED_LEVEL,
                )
            }
            .sortedBy { it.fullName.lowercase() }
            .toList()

    /**
     * Search matches name, matricule and phone. A query made only of digits and
     * separators ("0555 12") is compared against the normalised phone.
     */
    fun filter(recipients: List<SmsRecipient>, query: String, level: String?): List<SmsRecipient> {
        val q = query.trim().lowercase()
        val qDigits = q.filter(Char::isDigit)
        return recipients.filter { r ->
            val sameLevel = level == null || r.levelName == level
            val matches = q.isEmpty() ||
                r.fullName.lowercase().contains(q) ||
                r.matricule.lowercase().contains(q) ||
                (qDigits.length >= 3 && r.phone?.contains(qDigits) == true)
            sameLevel && matches
        }
    }

    /** Distinct levels, alphabetical, "Non affecté" last. */
    fun levels(recipients: List<SmsRecipient>): List<String> =
        recipients
            .map { it.levelName }
            .distinct()
            .sortedWith(compareBy({ it == UNASSIGNED_LEVEL }, { it }))
}
