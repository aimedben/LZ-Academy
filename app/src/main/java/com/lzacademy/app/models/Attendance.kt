package com.lzacademy.app.models

import java.time.LocalDate

/** A student of the academy, as stored locally. */
data class Student(
    val id: String,
    val fullName: String,
    val initials: String,
    val levelLabel: String,
    val groupLabel: String,
    val email: String,
    val isActive: Boolean = true,
) {
    val firstName: String
        get() = fullName.trim().split(" ").firstOrNull().orEmpty().ifBlank { fullName }
}

enum class SubscriptionStatus(val label: String) {
    Active("Actif"),
    Exhausted("Épuisé"),
    Expired("Expiré"),
}

/**
 * The pack of sessions bought by a student. [remainingSessions] is decremented by the attendance
 * repository on every accepted presence and reset by a renewal.
 */
data class Subscription(
    val studentId: String,
    val planLabel: String,
    val totalSessions: Int,
    val remainingSessions: Int,
    val status: SubscriptionStatus,
    val periodStart: LocalDate,
    val lastRenewedAt: LocalDate? = null,
    val renewalCount: Int = 0,
) {
    val isExhausted: Boolean get() = remainingSessions <= 0

    /** "Actif" / "Épuisé", derived from the counter so the label can never lie. */
    val statusLabel: String
        get() = if (isExhausted) SubscriptionStatus.Exhausted.label else status.label

    val progress: Float
        get() = if (totalSessions <= 0) 0f else remainingSessions.toFloat() / totalSessions
}

/** One attendance mark, uniquely identified by (student, session day, session label). */
data class AttendanceRecord(
    val id: Long,
    val studentId: String,
    val sessionLabel: String,
    val groupLabel: String,
    val status: AttendanceStatus,
    val consumedSession: Boolean,
    val date: LocalDate,
    val recordedAtMillis: Long,
)

/** A student plus the state of the day: what is left and what was already marked. */
data class StudentRosterEntry(
    val student: Student,
    val subscription: Subscription,
    val record: AttendanceRecord? = null,
) {
    val isMarked: Boolean get() = record != null
}

/** Everything the roll-call screen needs for one session. */
data class AttendanceDay(
    val date: LocalDate,
    val sessionLabel: String,
    val groupLabel: String,
    val roster: List<StudentRosterEntry>,
) {
    val markedCount: Int get() = roster.count { it.isMarked }
    val remaining: Int get() = roster.size - markedCount
}

sealed interface AttendanceOutcome {
    /** The mark was stored; [subscription] is the state after the decrement. */
    data class Recorded(val record: AttendanceRecord, val subscription: Subscription) : AttendanceOutcome

    /** The exact same mark already exists: nothing was decremented twice. */
    data object AlreadyRecorded : AttendanceOutcome

    /** The student exists but has no subscription at all. */
    data object NoSubscription : AttendanceOutcome

    /** A consuming mark was refused because the pack is empty; renewal is required. */
    data class Exhausted(val subscription: Subscription) : AttendanceOutcome

    data object NotFound : AttendanceOutcome
}

sealed interface AttendanceUndo {
    data class Refunded(val subscription: Subscription?) : AttendanceUndo
    data object NotFound : AttendanceUndo
}

sealed interface RenewalResult {
    data class Renewed(val subscription: Subscription) : RenewalResult
    data object NotFound : RenewalResult
}
