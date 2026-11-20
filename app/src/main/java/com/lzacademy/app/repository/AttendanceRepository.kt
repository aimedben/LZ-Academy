package com.lzacademy.app.repository

import androidx.room.withTransaction
import com.lzacademy.app.data.attendance.AttendanceDao
import com.lzacademy.app.data.attendance.AttendanceEntity
import com.lzacademy.app.data.attendance.SubscriptionEntity
import com.lzacademy.app.data.attendance.toDomain
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.models.AttendanceOutcome
import com.lzacademy.app.models.AttendanceRecord
import com.lzacademy.app.models.AttendanceStatus
import com.lzacademy.app.models.AttendanceUndo
import com.lzacademy.app.models.RenewalResult
import com.lzacademy.app.models.Student
import com.lzacademy.app.models.StudentRosterEntry
import com.lzacademy.app.models.Subscription
import com.lzacademy.app.models.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Attendance and subscription rules, all in one place.
 *
 * 1. `Present` and `Late` consume one session; `Absent` and `Justified` never do.
 * 2. A presence is refused when the pack is empty: the student must renew first.
 * 3. The unique (student, day, session) key makes the action idempotent: tapping twice or
 *    reopening the screen never consumes two sessions.
 * 4. Cancelling a consuming mark, or switching it to Absent, refunds the session.
 * 5. Renewal resets the counter to the pack size, in a single transaction.
 *
 * The roster comes ONLY from the Supabase mirror (`synced_students`): no demo student exists.
 * The local `subscriptions` table is the session counter ledger, hydrated from the mirror
 * ([hydrateSubscriptions]) the first time a student is seen — never overwritten afterwards.
 */
class AttendanceRepository(
    private val database: LZDatabase,
) {    private val dao: AttendanceDao = database.attendanceDao()
    private val syncDao = database.syncDao()

    companion object {
        const val DEFAULT_SESSION_LABEL = "Séance du jour"
        const val DEFAULT_GROUP_LABEL = "Séance du jour"
    }

    /** Real students from the mirror (active only), keyed by matricule. */
    val students: Flow<List<Student>> =
        syncDao.observeStudents().map { rows -> rows.filter { it.isActive }.map { it.toDomainStudent() } }

    val subscriptions: Flow<Map<String, Subscription>> =
        dao.observeSubscriptions().map { rows ->
            rows.associate { it.studentId to it.toDomain() }
        }

    fun observeSubscription(studentId: String): Flow<Subscription?> =
        dao.observeSubscription(studentId).map { it?.toDomain() }

    fun observeDay(date: LocalDate, sessionLabel: String): Flow<List<AttendanceRecord>> =
        dao.observeDay(date.toEpochDay(), sessionLabel).map { rows -> rows.map(AttendanceEntity::toDomain) }

    fun observeHistory(studentId: String, limit: Int = 20): Flow<List<AttendanceRecord>> =
        dao.observeHistory(studentId, limit).map { rows -> rows.map(AttendanceEntity::toDomain) }

    /**
     * Copies every mirrored subscription into the local counter ledger. `IGNORE` semantics:
     * a counter already being consumed locally is never reset by a re-hydration.
     */
    suspend fun hydrateSubscriptions() {
        val synced = syncDao.observeSubscriptions().first()
        if (synced.isEmpty()) return
        val today = LocalDate.now().toEpochDay()
        dao.seedSubscriptions(
            synced.map { sub ->
                SubscriptionEntity(
                    studentId = sub.studentMatricule,
                    planLabel = "Abonnement",
                    totalSessions = sub.sessionsTotal,
                    remainingSessions = sub.sessionsRemaining,
                    status = if (sub.sessionsRemaining <= 0) {
                        SubscriptionStatus.Exhausted.name
                    } else {
                        SubscriptionStatus.Active.name
                    },
                    periodStartEpochDay = today,
                    lastRenewedEpochDay = null,
                    renewalCount = 0,
                )
            },
        )
        // A pack that changed size in the mirror (the old 48-session one replaced by the real
        // 4-session one) makes the local counter obsolete: the mirror wins. An unchanged pack
        // keeps the local count, so a roll-call already recorded is never lost.
        synced.forEach { sub ->
            val local = dao.subscription(sub.studentMatricule) ?: return@forEach
            val aligned = realignedCounter(
                localTotal = local.totalSessions,
                localRemaining = local.remainingSessions,
                remoteTotal = sub.sessionsTotal,
                remoteRemaining = sub.sessionsRemaining,
            ) ?: return@forEach
            val (total, remaining) = aligned
            val status = if (remaining <= 0) SubscriptionStatus.Exhausted.name else SubscriptionStatus.Active.name
            dao.alignPack(sub.studentMatricule, remaining, total, status)
        }
    }

    /**
     * Students joined with their pack and the mark of the day, ready for the roll-call UI.
     *
     * When [groupId] is provided, the roster is restricted to the students with an ACTIVE
     * enrollment in that exact group (enrollments.left_on IS NULL) — the teacher's "faire
     * l'appel" therefore lists only the pupils of the group they tapped.
     */
    fun observeRoster(
        date: LocalDate,
        sessionLabel: String,
        groupLabel: String = DEFAULT_GROUP_LABEL,
        groupId: String? = null,
    ): Flow<List<StudentRosterEntry>> {
        val activeRows = syncDao.observeStudents()
            .map { rows -> rows.filter { it.isActive } }
        val memberIds: Flow<Set<String>?> = if (groupId == null) {
            kotlinx.coroutines.flow.flowOf(null)
        } else {
            syncDao.observeEnrollments().map { rows ->
                rows.filter { it.groupId == groupId && it.leftOn == null }
                    .map { it.studentId }
                    .toSet()
            }
        }
        return combine(
            activeRows,
            subscriptions,
            observeDay(date, sessionLabel),
            memberIds,
        ) { rows, subscriptions, records, members ->
            val byStudent = records.associateBy { it.studentId }
            rows
                .filter { members == null || it.remoteId in members }
                .map { it.toDomainStudent() }
                .filter { groupLabel == DEFAULT_GROUP_LABEL || it.groupLabel == groupLabel }
                .mapNotNull { student ->
                    val subscription = subscriptions[student.id] ?: return@mapNotNull null
                    StudentRosterEntry(student, subscription, byStudent[student.id])
                }
        }
    }

    suspend fun record(
        studentId: String,
        date: LocalDate,
        sessionLabel: String,
        status: AttendanceStatus,
        groupLabel: String = DEFAULT_GROUP_LABEL,
        nowMillis: Long = System.currentTimeMillis(),
    ): AttendanceOutcome {
        return database.withTransaction {
            val stored = dao.subscription(studentId) ?: return@withTransaction AttendanceOutcome.NoSubscription
            val existing = dao.byKey(studentId, date.toEpochDay(), sessionLabel)
            val consumes = status.consumesSession

            if (existing != null) {
                if (existing.status == status.name) {
                    return@withTransaction AttendanceOutcome.AlreadyRecorded
                }
                val delta = (if (consumes) 1 else 0) - (if (existing.consumedSession) 1 else 0)
                if (delta > 0 && stored.remainingSessions <= 0) {
                    return@withTransaction AttendanceOutcome.Exhausted(stored.toDomain())
                }
                val remaining = (stored.remainingSessions - delta).coerceIn(0, stored.totalSessions)
                val updated = existing.copy(
                    status = status.name,
                    consumedSession = consumes,
                    recordedAtMillis = nowMillis,
                )
                dao.updateAttendance(updated)
                dao.updateRemaining(studentId, remaining, statusOf(remaining))
                val subscription = stored.copy(
                    remainingSessions = remaining,
                    status = statusOf(remaining),
                ).toDomain()
                AttendanceOutcome.Recorded(updated.toDomain(), subscription)
            } else {
                if (consumes && stored.remainingSessions <= 0) {
                    return@withTransaction AttendanceOutcome.Exhausted(stored.toDomain())
                }
                val remaining = if (consumes) {
                    (stored.remainingSessions - 1).coerceAtLeast(0)
                } else {
                    stored.remainingSessions
                }
                val entity = AttendanceEntity(
                    studentId = studentId,
                    sessionLabel = sessionLabel,
                    groupLabel = groupLabel,
                    status = status.name,
                    consumedSession = consumes,
                    sessionDateEpochDay = date.toEpochDay(),
                    recordedAtMillis = nowMillis,
                )
                val id = dao.insertAttendance(entity)
                if (id == -1L) {
                    // Lost the race against another tap: the other transaction already decremented.
                    return@withTransaction AttendanceOutcome.AlreadyRecorded
                }
                if (consumes) {
                    dao.updateRemaining(studentId, remaining, statusOf(remaining))
                }
                val subscription = stored.copy(
                    remainingSessions = remaining,
                    status = statusOf(remaining),
                ).toDomain()
                AttendanceOutcome.Recorded(entity.copy(id = id).toDomain(), subscription)
            }
        }
    }

    /** Cancel a mark. A consumed session is refunded, capped at the pack size. */
    suspend fun undo(recordId: Long): AttendanceUndo {
        return database.withTransaction {
            val record = dao.byId(recordId) ?: return@withTransaction AttendanceUndo.NotFound
            dao.deleteAttendance(recordId)
            val subscription = dao.subscription(record.studentId)
            val refunded = if (record.consumedSession && subscription != null) {
                val remaining = (subscription.remainingSessions + 1).coerceAtMost(subscription.totalSessions)
                dao.updateRemaining(record.studentId, remaining, statusOf(remaining))
                subscription.copy(remainingSessions = remaining, status = statusOf(remaining)).toDomain()
            } else {
                subscription?.toDomain()
            }
            AttendanceUndo.Refunded(refunded)
        }
    }

    /** Renew the pack: full counter, active status, renewal date and counter updated. */
    suspend fun renew(
        studentId: String,
        sessions: Int? = null,
        today: LocalDate = LocalDate.now(),
    ): RenewalResult {
        return database.withTransaction {
            val stored = dao.subscription(studentId) ?: return@withTransaction RenewalResult.NotFound
            val total = (sessions ?: stored.totalSessions).coerceAtLeast(1)
            dao.renew(
                studentId = studentId,
                remaining = total,
                total = total,
                status = SubscriptionStatus.Active.name,
                renewedAtEpochDay = today.toEpochDay(),
            )
            RenewalResult.Renewed(
                stored.copy(
                    totalSessions = total,
                    remainingSessions = total,
                    status = SubscriptionStatus.Active.name,
                    lastRenewedEpochDay = today.toEpochDay(),
                    renewalCount = stored.renewalCount + 1,
                ).toDomain(),
            )
        }
    }

    /** Test/diagnostic helper. */
    suspend fun subscriptionOf(studentId: String): Subscription? =
        dao.subscription(studentId)?.toDomain()

    private fun statusOf(remaining: Int): String =
        if (remaining <= 0) SubscriptionStatus.Exhausted.name else SubscriptionStatus.Active.name
}

/** Real mirror row → roll-call student. The id IS the matricule (LZ-…). */
private fun SyncedStudentEntity.toDomainStudent(): Student = Student(
    id = matricule,
    fullName = fullName,
    initials = fullName
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "—" },
    levelLabel = levelName ?: "",
    groupLabel = levelName ?: "",
    email = phone ?: "",
    isActive = isActive,
)

private val AttendanceStatus.consumesSession: Boolean
    get() = this == AttendanceStatus.Present || this == AttendanceStatus.Late

/**
 * Compares the local session counter with the mirrored subscription.
 *
 * A different pack size in the mirror means the student's pack changed (the academy moved
 * from a 48-session pack to the real 4-session one): the local counter follows the mirror.
 * The same pack never realigns, so a roll-call already recorded stays consumed.
 *
 * Returns `total to remaining` when the counter must be rewritten, `null` when it stands.
 */
fun realignedCounter(
    localTotal: Int,
    localRemaining: Int,
    remoteTotal: Int,
    remoteRemaining: Int,
): Pair<Int, Int>? {
    if (remoteTotal <= 0 || remoteTotal == localTotal) return null
    return remoteTotal to remoteRemaining.coerceIn(0, remoteTotal)
}
