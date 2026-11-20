package com.lzacademy.app.data.attendance

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lzacademy.app.models.AttendanceRecord
import com.lzacademy.app.models.AttendanceStatus
import com.lzacademy.app.models.Student
import com.lzacademy.app.models.Subscription
import com.lzacademy.app.models.SubscriptionStatus
import java.time.LocalDate

/** A student of the academy. Linked to a local account through [UserEntity.linkedStudentId]. */
@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val initials: String,
    val levelLabel: String,
    val groupLabel: String,
    val email: String,
    val isActive: Boolean,
)

internal fun StudentEntity.toDomain(): Student = Student(
    id = id,
    fullName = fullName,
    initials = initials,
    levelLabel = levelLabel,
    groupLabel = groupLabel,
    email = email,
    isActive = isActive,
)

/**
 * The current pack of a student. [remainingSessions] is the only mutable counter: each accepted
 * presence decrements it inside a transaction, renewal resets it to [totalSessions].
 */
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val studentId: String,
    val planLabel: String,
    val totalSessions: Int,
    val remainingSessions: Int,
    val status: String,
    val periodStartEpochDay: Long,
    val lastRenewedEpochDay: Long?,
    val renewalCount: Int,
)

internal fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    studentId = studentId,
    planLabel = planLabel,
    totalSessions = totalSessions,
    remainingSessions = remainingSessions,
    status = runCatching { SubscriptionStatus.valueOf(status) }
        .getOrDefault(SubscriptionStatus.Active),
    periodStart = LocalDate.ofEpochDay(periodStartEpochDay),
    lastRenewedAt = lastRenewedEpochDay?.let(LocalDate::ofEpochDay),
    renewalCount = renewalCount,
)

/**
 * One attendance mark. The unique index on (student, day, session) is the idempotency key: the
 * same student can never be counted twice for the same session, even if the button is tapped
 * repeatedly or the screen is reopened.
 */
@Entity(
    tableName = "attendance",
    indices = [Index(value = ["studentId", "sessionDateEpochDay", "sessionLabel"], unique = true)],
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val sessionLabel: String,
    val groupLabel: String,
    val status: String,
    val consumedSession: Boolean,
    val sessionDateEpochDay: Long,
    val recordedAtMillis: Long,
)

internal fun AttendanceEntity.toDomain(): AttendanceRecord = AttendanceRecord(
    id = id,
    studentId = studentId,
    sessionLabel = sessionLabel,
    groupLabel = groupLabel,
    status = runCatching { AttendanceStatus.valueOf(status) }.getOrDefault(AttendanceStatus.Present),
    consumedSession = consumedSession,
    date = LocalDate.ofEpochDay(sessionDateEpochDay),
    recordedAtMillis = recordedAtMillis,
)
