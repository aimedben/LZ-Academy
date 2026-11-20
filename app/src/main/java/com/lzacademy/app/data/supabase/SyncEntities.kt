package com.lzacademy.app.data.supabase

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Mirror of the academy Supabase database, cached offline in Room. Written by
 * [SupabaseSyncRepository] after every successful sign-in, read by the admin and
 * profile screens so the app keeps working without network between syncs.
 */
@Entity(
    tableName = "synced_students",
    indices = [Index(value = ["matricule"], unique = true)],
)
data class SyncedStudentEntity(
    /** Supabase uuid (public.students.id). */
    @PrimaryKey val remoteId: String,
    val matricule: String,
    val fullName: String,
    val phone: String?,
    val levelName: String?,
    val avatarPath: String?,
    val isActive: Boolean,
    /** Supabase profile uuid; equals the signed-in user id for the account's own student. */
    val profileId: String?,
    /** ISO timestamp of the row creation (real enrollment growth chart). */
    val createdAt: String,
)

@Entity(
    tableName = "synced_subscriptions",
    indices = [Index(value = ["studentMatricule"], unique = true)],
)
data class SyncedSubscriptionEntity(
    /** Supabase uuid (public.subscriptions.id), used by the admin renewal PATCH. */
    @PrimaryKey val remoteId: String,
    val studentMatricule: String,
    val sessionsTotal: Int,
    val sessionsUsed: Int,
    val sessionsRemaining: Int,
    val status: String,
) {
    val isExhausted: Boolean get() = sessionsRemaining <= 0
}

// ----------------------------------------------------------------------------
// Extended mirror (v4): one entity per additional Supabase table read by the
// dashboards. Joins are done in Kotlin at read time (small tables).
// ----------------------------------------------------------------------------

/** Mirror of public.teachers, subjects flattened as a CSV. */
@Entity(tableName = "synced_teachers")
data class SyncedTeacherEntity(
    @PrimaryKey val remoteId: String,
    val fullName: String,
    val photoUrl: String?,
    val description: String?,
    val yearsOfExperience: Int,
    val isFeatured: Boolean,
    val acceptsMessages: Boolean,
    val rotationPriority: Int,
    /** Epoch day of public.teachers.last_featured_at, or null. */
    val lastFeaturedEpochDay: Long?,
    val isActive: Boolean,
    /** Supabase profile uuid; equals the signed-in user id for the account's own teacher. */
    val profileId: String?,
    /** Subject names joined with the LIST_SEPARATOR used by the teacher directory. */
    val subjects: String,
)

/** Mirror of public.subjects. */
@Entity(tableName = "synced_subjects")
data class SyncedSubjectEntity(
    @PrimaryKey val remoteId: String,
    val code: String?,
    val name: String,
)

/** Mirror of public.groups, level name flattened. */
@Entity(tableName = "synced_groups")
data class SyncedGroupEntity(
    @PrimaryKey val remoteId: String,
    val name: String,
    val levelName: String?,
    val teacherId: String?,
    val capacity: Int,
)

/** Mirror of public.payments — real revenue numbers. */
@Entity(tableName = "synced_payments")
data class SyncedPaymentEntity(
    @PrimaryKey val remoteId: String,
    val studentId: String,
    val amountDzd: Double,
    val method: String,
    val status: String,
    val reference: String?,
    /** ISO date "yyyy-MM-dd" or null. */
    val dueOn: String?,
    val paidAtIso: String?,
    val createdAtIso: String,
)

/** Mirror of public.announcements. */
@Entity(tableName = "synced_announcements")
data class SyncedAnnouncementEntity(
    @PrimaryKey val remoteId: String,
    val title: String,
    val body: String,
    val audience: String,
    val levelId: String?,
    val isPinned: Boolean,
    val publishedAtIso: String?,
    val expiresAtIso: String?,
    val createdAtIso: String,
)

/** Mirror of public.materials. */
@Entity(tableName = "synced_materials")
data class SyncedMaterialEntity(
    @PrimaryKey val remoteId: String,
    val title: String,
    val description: String?,
    val kind: String,
    val storagePath: String?,
    val sizeBytes: Long,
    val subjectId: String?,
    val groupName: String?,
    val isPublished: Boolean,
    val publishedAtIso: String?,
)

/** Mirror of public.group_schedule_slots, times as "HH:mm[:ss]". */
@Entity(tableName = "synced_schedule_slots")
data class SyncedSlotEntity(
    @PrimaryKey val remoteId: String,
    val groupId: String,
    val groupName: String?,
    val weekday: Int,
    val start: String,
    val end: String,
    val roomName: String?,
)

/** Mirror of public.sessions, names flattened. */
@Entity(tableName = "synced_sessions")
data class SyncedSessionEntity(
    @PrimaryKey val remoteId: String,
    val groupId: String,
    val groupName: String?,
    val teacherId: String?,
    val teacherName: String?,
    val subjectName: String?,
    val roomName: String?,
    val startsAtIso: String,
    val endsAtIso: String,
    val status: String,
    val topic: String?,
)

/** Mirror of public.attendance. */
@Entity(tableName = "synced_attendance")
data class SyncedAttendanceEntity(
    @PrimaryKey val remoteId: String,
    val sessionId: String,
    val studentId: String,
    val status: String,
    val recordedAtIso: String,
)

/** Mirror of public.enrollments: which student belongs to which group. */
@Entity(tableName = "synced_enrollments")
data class SyncedEnrollmentEntity(
    @PrimaryKey val remoteId: String,
    val studentId: String,
    val groupId: String,
    /** ISO date "yyyy-MM-dd" or null. */
    val joinedOn: String?,
    val leftOn: String?,
)

@Dao
interface SyncDao {

    @Upsert
    suspend fun upsertStudents(rows: List<SyncedStudentEntity>)

    @Upsert
    suspend fun upsertSubscriptions(rows: List<SyncedSubscriptionEntity>)

    @Query("SELECT * FROM synced_students ORDER BY matricule")
    fun observeStudents(): Flow<List<SyncedStudentEntity>>

    @Query("SELECT * FROM synced_subscriptions")
    fun observeSubscriptions(): Flow<List<SyncedSubscriptionEntity>>

    @Query("SELECT * FROM synced_students WHERE matricule = :matricule LIMIT 1")
    suspend fun studentByMatricule(matricule: String): SyncedStudentEntity?

    @Query("SELECT * FROM synced_students WHERE profileId = :profileId LIMIT 1")
    suspend fun studentByProfile(profileId: String): SyncedStudentEntity?

    @Query("SELECT * FROM synced_subscriptions WHERE studentMatricule = :matricule LIMIT 1")
    suspend fun subscriptionByMatricule(matricule: String): SyncedSubscriptionEntity?

    @Query("SELECT COUNT(*) FROM synced_students")
    suspend fun studentCount(): Int

    @Query(
        "UPDATE synced_subscriptions SET sessionsUsed = 0, sessionsRemaining = sessionsTotal, " +
            "status = 'active' WHERE remoteId = :remoteId",
    )
    suspend fun markRenewedLocally(remoteId: String)

    // ------------------------------------------------------------ extended mirror (v4)

    @Upsert
    suspend fun upsertTeachers(rows: List<SyncedTeacherEntity>)

    @Upsert
    suspend fun upsertSubjects(rows: List<SyncedSubjectEntity>)

    @Upsert
    suspend fun upsertGroups(rows: List<SyncedGroupEntity>)

    @Upsert
    suspend fun upsertPayments(rows: List<SyncedPaymentEntity>)

    @Upsert
    suspend fun upsertAnnouncements(rows: List<SyncedAnnouncementEntity>)

    @Upsert
    suspend fun upsertMaterials(rows: List<SyncedMaterialEntity>)

    @Upsert
    suspend fun upsertSlots(rows: List<SyncedSlotEntity>)

    @Upsert
    suspend fun upsertSessions(rows: List<SyncedSessionEntity>)

    @Upsert
    suspend fun upsertAttendance(rows: List<SyncedAttendanceEntity>)

    @Upsert
    suspend fun upsertEnrollments(rows: List<SyncedEnrollmentEntity>)

    @Query("SELECT * FROM synced_teachers")
    fun observeTeachers(): Flow<List<SyncedTeacherEntity>>

    @Query("SELECT * FROM synced_subjects")
    fun observeSubjects(): Flow<List<SyncedSubjectEntity>>

    @Query("SELECT * FROM synced_groups")
    fun observeGroups(): Flow<List<SyncedGroupEntity>>

    @Query("SELECT * FROM synced_payments ORDER BY createdAtIso DESC")
    fun observePayments(): Flow<List<SyncedPaymentEntity>>

    @Query("SELECT * FROM synced_announcements ORDER BY publishedAtIso DESC")
    fun observeAnnouncements(): Flow<List<SyncedAnnouncementEntity>>

    @Query("SELECT * FROM synced_materials")
    fun observeMaterials(): Flow<List<SyncedMaterialEntity>>

    @Query("SELECT * FROM synced_schedule_slots")
    fun observeSlots(): Flow<List<SyncedSlotEntity>>

    @Query("SELECT * FROM synced_sessions ORDER BY startsAtIso DESC")
    fun observeSessions(): Flow<List<SyncedSessionEntity>>

    @Query("SELECT * FROM synced_attendance ORDER BY recordedAtIso DESC")
    fun observeAttendance(): Flow<List<SyncedAttendanceEntity>>

    @Query("SELECT * FROM synced_enrollments")
    fun observeEnrollments(): Flow<List<SyncedEnrollmentEntity>>

    @Query("SELECT * FROM synced_teachers WHERE profileId = :profileId LIMIT 1")
    suspend fun teacherByProfile(profileId: String): SyncedTeacherEntity?

    @Query("SELECT COUNT(*) FROM synced_teachers")
    suspend fun teacherCount(): Int
}
