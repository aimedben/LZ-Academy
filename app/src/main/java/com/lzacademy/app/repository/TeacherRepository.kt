package com.lzacademy.app.repository

import androidx.room.withTransaction
import com.lzacademy.app.data.teachers.FeaturedDayEntity
import com.lzacademy.app.data.teachers.LIST_SEPARATOR
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.data.teachers.TeacherDao
import com.lzacademy.app.data.teachers.TeacherEntity
import com.lzacademy.app.data.teachers.TeacherMessageEntity
import com.lzacademy.app.data.teachers.toDomain
import com.lzacademy.app.models.Teacher
import com.lzacademy.app.models.TeacherMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Teacher directory, student-home rotation and local message queue.
 *
 * Rotation rules implemented here:
 * 1. only active teachers with `isFeaturedOnStudentHome = true` are eligible;
 * 2. the page of the day (default 4 teachers) is persisted the first time it is computed,
 *    so navigation, recomposition and app restarts never reshuffle the carousel;
 * 3. the page is built from the least-recently-featured teachers first (then by priority),
 *    which guarantees every eligible teacher eventually appears and never twice in one page;
 * 4. a teacher who opts out is filtered out immediately, without waiting for the next day.
 */
class TeacherRepository(private val database: LZDatabase) {

    private val dao = database.teacherDao()

    private val rotationMutex = Mutex()

    // ------------------------------------------------------------------ reads

    fun observeTeachers(): Flow<List<Teacher>> =
        dao.observeAll().map { rows -> rows.map(TeacherEntity::toDomain) }

    fun observeTeacher(id: String): Flow<Teacher?> =
        dao.observe(id).map { it?.toDomain() }

    /** The real teacher linked to a Supabase auth profile, or null when unlinked. */
    fun observeTeacherByProfile(profileId: String?): Flow<Teacher?> =
        if (profileId.isNullOrBlank()) {
            flowOf(null)
        } else {
            dao.observeByProfile(profileId).map { it?.toDomain() }
        }

    fun observeMessages(teacherId: String): Flow<List<TeacherMessage>> =
        dao.observeMessages(teacherId).map { rows -> rows.map { it.toDomain() } }

    fun observeMessageCount(teacherId: String): Flow<Int> = dao.observeMessageCount(teacherId)

    /**
     * Persists the teachers mirrored from Supabase (called by the sync). Server values are
     * the source of truth — no local seeding exists anymore.
     */
    suspend fun upsertMirrorTeachers(teachers: List<TeacherEntity>) = dao.upsertMirror(teachers)

    /** The student-home selection. Stable for the whole epoch day. */
    fun observeFeatured(
        pageSize: Int = FEATURED_PAGE_SIZE,
        today: () -> LocalDate = { LocalDate.now() },
    ): Flow<List<Teacher>> =
        dao.observeAll()
            .map { rows ->
                val all = rows.map(TeacherEntity::toDomain)
                val eligible = all.filter { it.isFeaturedOnStudentHome }
                if (eligible.isEmpty()) return@map emptyList()

                val epochDay = today().toEpochDay()
                val pageIds = rotationMutex.withLock { ensureDayPage(eligible, epochDay, pageSize) }
                val byId = all.associateBy(Teacher::id)
                pageIds.mapNotNull { id -> byId[id]?.takeIf { it.isFeaturedOnStudentHome } }
            }
            .distinctUntilChanged()

    // ------------------------------------------------------------------ settings

    /** Profile visibility on the student home. Independent from message availability. */
    suspend fun setShowcaseVisibility(teacherId: String, value: Boolean) =
        dao.updateShowcaseVisibility(teacherId, value)

    /** Message availability. Independent from profile visibility. */
    suspend fun setAcceptsMessages(teacherId: String, value: Boolean) =
        dao.updateAcceptsMessages(teacherId, value)

    // ------------------------------------------------------------------ messages

    /**
     * Queues a message locally. Returns [SendMessageResult] so the UI can explain exactly
     * why nothing was written (teacher unavailable, empty body, daily quota reached).
     *
     * Anti-spam rule: at most [MAX_MESSAGES_PER_DAY] messages per teacher per calendar day.
     * Storage rule: messages older than [MESSAGE_RETENTION_DAYS] days are purged on every
     * write, which keeps the local database bounded week after week.
     */
    suspend fun sendMessage(teacherId: String, body: String): SendMessageResult {
        // Weekly cleanup first — even a refused message keeps the store tidy.
        purgeOldMessages()
        if (body.isBlank()) return SendMessageResult.Empty
        val teacher = dao.byId(teacherId) ?: return SendMessageResult.Unavailable
        if (!teacher.acceptsStudentMessages) return SendMessageResult.Unavailable
        val dayStart = LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (dao.countMessagesSince(teacherId, dayStart) >= MAX_MESSAGES_PER_DAY) {
            return SendMessageResult.DailyLimitReached
        }
        dao.insertMessage(
            TeacherMessageEntity(
                teacherId = teacherId,
                body = body.trim(),
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
        return SendMessageResult.Sent
    }

    /** Deletes messages older than the weekly retention window. */
    suspend fun purgeOldMessages() {
        val cutoff = System.currentTimeMillis() - MESSAGE_RETENTION_DAYS * DAY_MILLIS
        dao.deleteMessagesBefore(cutoff)
    }

    // ------------------------------------------------------------------ rotation

    private suspend fun ensureDayPage(
        eligible: List<Teacher>,
        epochDay: Long,
        pageSize: Int,
    ): List<String> {
        val byId = eligible.associateBy(Teacher::id)
        val stored = dao.dayPage(epochDay)?.teacherIds
            ?.split(LIST_SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()

        // Keep the order of the persisted day, but drop teachers who opted out (or became
        // inactive) immediately — no need to wait for the next rotation day.
        val kept = stored.mapNotNull { id -> id.takeIf(byId::containsKey) }.distinct()
        val target = minOf(pageSize, eligible.size)
        if (kept.size >= target) return kept

        // Refill with the least recently featured eligible teachers, preserving the page size
        // (the same "purge then complete" rule as the Supabase rotation).
        val added = eligible
            .asSequence()
            .filter { it.id !in kept }
            .sortedWith(fairnessOrder)
            .take(target - kept.size)
            .map(Teacher::id)
            .toList()
        val ids = kept + added
        if (ids.isEmpty()) return emptyList()

        database.withTransaction {
            dao.insertDayPage(
                FeaturedDayEntity(epochDay = epochDay, teacherIds = ids.joinToString(LIST_SEPARATOR)),
            )
            if (added.isNotEmpty()) dao.markFeatured(added, epochDay)
        }
        return ids
    }

    private val fairnessOrder = compareBy<Teacher> { it.lastFeaturedAt?.toEpochDay() ?: Long.MIN_VALUE }
        .thenByDescending { it.featuredRotationPriority }
        .thenBy { it.fullName }

    companion object {
        const val FEATURED_PAGE_SIZE = 4

        /** Anti-spam: max messages sent to one teacher per calendar day. */
        const val MAX_MESSAGES_PER_DAY = 3

        /** Retention: messages older than this many days are deleted (weekly cleanup). */
        const val MESSAGE_RETENTION_DAYS = 7L

        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}

/** Outcome of a student → teacher message write. */
enum class SendMessageResult {
    /** Stored locally, waiting to be delivered. */
    Sent,

    /** Nothing written: the message body was blank. */
    Empty,

    /** Nothing written: the teacher no longer accepts messages. */
    Unavailable,

    /** Nothing written: the student already sent 3 messages to this teacher today. */
    DailyLimitReached,
}
