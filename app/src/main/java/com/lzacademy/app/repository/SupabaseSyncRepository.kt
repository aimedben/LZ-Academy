package com.lzacademy.app.repository

import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.data.settings.ADMIN_AUTO_SYNC
import com.lzacademy.app.data.supabase.AuthExpiredException
import com.lzacademy.app.data.supabase.RemotePayment
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.data.supabase.SyncedAnnouncementEntity
import com.lzacademy.app.data.supabase.SyncedAttendanceEntity
import com.lzacademy.app.data.supabase.SyncedEnrollmentEntity
import com.lzacademy.app.data.supabase.SyncedGroupEntity
import com.lzacademy.app.data.supabase.SyncedMaterialEntity
import com.lzacademy.app.data.supabase.SyncedPaymentEntity
import com.lzacademy.app.data.supabase.SyncedSessionEntity
import com.lzacademy.app.data.supabase.SyncedSlotEntity
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import com.lzacademy.app.data.supabase.SyncedSubjectEntity
import com.lzacademy.app.data.supabase.SyncedSubscriptionEntity
import com.lzacademy.app.data.supabase.SyncedTeacherEntity
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.data.teachers.TeacherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** Outcome of a sync attempt, shown in the admin sync card. */
sealed class SyncResult {
    data object Success : SyncResult()
    data object Offline : SyncResult()
    data object AuthFailed : SyncResult()
    data object Error : SyncResult()
}

/** One full snapshot of every mirrored Supabase table, built by a single sync cycle. */
data class RemoteSnapshot(
    val students: List<com.lzacademy.app.data.supabase.RemoteStudent>,
    val subscriptions: List<com.lzacademy.app.data.supabase.RemoteSubscription>,
    val teachers: List<com.lzacademy.app.data.supabase.RemoteTeacher>,
    val subjects: List<com.lzacademy.app.data.supabase.RemoteSubject>,
    val groups: List<com.lzacademy.app.data.supabase.RemoteGroup>,
    val payments: List<com.lzacademy.app.data.supabase.RemotePayment>,
    val announcements: List<com.lzacademy.app.data.supabase.RemoteAnnouncement>,
    val materials: List<com.lzacademy.app.data.supabase.RemoteMaterial>,
    val slots: List<com.lzacademy.app.data.supabase.RemoteSlot>,
    val sessions: List<com.lzacademy.app.data.supabase.RemoteSession>,
    val attendance: List<com.lzacademy.app.data.supabase.RemoteAttendance>,
    val enrollments: List<com.lzacademy.app.data.supabase.RemoteEnrollment>,
)

/**
 * Offline-first bridge between the academy Supabase database and the Room mirror.
 *
 * - [loginAndSync] runs right after a successful sign-in: Supabase password auth, then the
 *   students + subscriptions visible to that role are cached in Room.
 * - [refresh] re-runs the fetch with the stored token (app relaunch).
 * - [renewSubscription] is the admin write path: PATCH in Supabase, then the local mirror.
 * Every network failure is swallowed and reported — the app stays fully usable offline.
 */
class SupabaseSyncRepository(
    private val database: LZDatabase,
    private val preferences: AppPreferences,
    private val api: SupabaseApi = SupabaseApi(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.syncDao()

    suspend fun loginAndSync(email: String, password: String): SyncResult = withContext(Dispatchers.IO) {
        val session = api.login(email.trim(), password)
            ?: return@withContext SyncResult.Offline.also {
                android.util.Log.w("LZSync", "loginAndSync: session nulle")
            }
        android.util.Log.d("LZSync", "loginAndSync: session OK, sync en cours")
        preferences.supabaseToken = session.accessToken
        preferences.supabaseRefreshToken = session.refreshToken.takeIf { it.isNotBlank() }
        fetchAndStore(session.userId)
    }

    /**
     * Auto-refresh of the offline mirror. Free-tier friendly: skipped when a successful
     * sync ran less than [MIN_SYNC_INTERVAL_MS] ago (the Room mirror is already fresh).
     * Pass [force] = true for an explicit user action ("Actualiser").
     */
    suspend fun refresh(force: Boolean = false): SyncResult = withContext(Dispatchers.IO) {
        if (preferences.supabaseToken == null) return@withContext SyncResult.Offline
        val sinceLast = clock() - preferences.lastSyncAtMillis
        if (!force && !shouldSync(
                preferences.lastSyncAtMillis,
                clock(),
                dao.studentCount() > 0,
                preferences.notificationEnabled(ADMIN_AUTO_SYNC, true),
            )
        ) {
            android.util.Log.d("LZSync", "refresh sauté (dernière sync il y a ${sinceLast / 60_000} min)")
            return@withContext SyncResult.Success
        }
        fetchAndStore(null)
    }

    suspend fun renewSubscription(matricule: String): Boolean = withContext(Dispatchers.IO) {
        val token = preferences.supabaseToken ?: return@withContext false
        val row = dao.subscriptionByMatricule(matricule) ?: return@withContext false
        val ok = api.renewSubscription(token, row.remoteId)
        if (ok) dao.markRenewedLocally(row.remoteId)
        ok
    }

    /** Active tariff packs (live read) for the renewal dialog. */
    suspend fun activePacks(): List<SupabaseApi.RemotePack> = withContext(Dispatchers.IO) {        val token = preferences.supabaseToken ?: return@withContext emptyList()
        runCatching { api.fetchPacks(token) }.getOrDefault(emptyList())
    }

    /** Real payment history of one student (live read). */
    suspend fun paymentsOfStudent(studentRemoteId: String): List<RemotePayment> = withContext(Dispatchers.IO) {
        val token = preferences.supabaseToken ?: return@withContext emptyList()
        runCatching { api.fetchPayments(token).filter { it.studentId == studentRemoteId } }
            .getOrDefault(emptyList())
    }

    /**
     * Real paid renewal: subscription reset on the chosen pack + a PAID payment row recorded.
     * Returns null on success, an error message otherwise.
     */
    suspend fun renewWithPayment(
        matricule: String,
        packId: String,
        sessionsCount: Int,
        priceDzd: Double,
        durationMonths: Int?,
        method: String,
    ): String? = withContext(Dispatchers.IO) {
        val token = preferences.supabaseToken ?: return@withContext "Non connecté — action impossible."
        val student = dao.studentByMatricule(matricule)
            ?: return@withContext "Élève introuvable dans le miroir local."
        val endsOn = durationMonths?.let { java.time.LocalDate.now().plusMonths(it.toLong()) }
        val subId = dao.subscriptionByMatricule(matricule)?.remoteId
        val error = api.renewWithPayment(
            token = token,
            subscriptionId = subId,
            studentId = student.remoteId,
            packId = packId,
            sessionsCount = sessionsCount,
            priceDzd = priceDzd,
            endsOn = endsOn,
            method = method,
        )
        if (error == null) refresh(force = true)
        error
    }

    /** Public URL of a student's photo, or null when none. */
    fun avatarUrlOf(path: String?): String? = path?.let { api.avatarUrl(it) }

    /** Offline mirror of public.students (Room flow, instantly visible after a sync). */
    fun observeStudents() = dao.observeStudents()

    /** Offline mirror of public.subscriptions (Room flow). */
    fun observeSubscriptions() = dao.observeSubscriptions()

    /** Offline mirror of public.enrollments (Room flow) — the student's multiple courses. */
    fun observeEnrollments() = dao.observeEnrollments()

    /** Offline mirror of public.groups (Room flow) — course names. */
    fun observeGroups() = dao.observeGroups()

    /** Offline mirror of public.teachers (Room flow) — course teacher names. */
    fun observeTeachers() = dao.observeTeachers()

    private suspend fun fetchAndStore(userId: String?, token: String? = null): SyncResult {
        val accessToken = token ?: preferences.supabaseToken
            ?: return SyncResult.Offline.also { android.util.Log.w("LZSync", "fetchAndStore: pas de token") }
        return try {
            store(snapshot(accessToken), userId)
        } catch (expired: AuthExpiredException) {
            // Access token expired (1 h JWT): ONE cheap refresh-token grant, one retry.
            android.util.Log.w("LZSync", "token expiré — tentative de refresh")
            val refreshToken = preferences.supabaseRefreshToken
            val session = refreshToken?.let { api.refreshSession(it) }
            if (session == null) {
                android.util.Log.w("LZSync", "token expiré et refresh impossible")
                return SyncResult.Offline
            }
            preferences.supabaseToken = session.accessToken
            preferences.supabaseRefreshToken = session.refreshToken.takeIf { it.isNotBlank() }
            try {
                store(snapshot(session.accessToken), userId)
            } catch (t: Throwable) {
                android.util.Log.e("LZSync", "store échoué après refresh", t)
                SyncResult.Error
            }
        } catch (t: Throwable) {
            android.util.Log.e("LZSync", "store échoué", t)
            SyncResult.Error
        }
    }

    /** Fetches every mirrored table with the given token (12 GETs in parallel, throttled upstream). */
    private suspend fun snapshot(token: String): RemoteSnapshot = coroutineScope {
        val students = async { api.fetchStudents(token) }
        val subscriptions = async { api.fetchSubscriptions(token) }
        val teachers = async { api.fetchTeachers(token) }
        val subjects = async { api.fetchSubjects(token) }
        val groups = async { api.fetchGroups(token) }
        val payments = async { api.fetchPayments(token) }
        val announcements = async { api.fetchAnnouncements(token) }
        val materials = async { api.fetchMaterials(token) }
        val slots = async { api.fetchSlots(token) }
        val sessions = async { api.fetchSessions(token) }
        val attendance = async { api.fetchAttendance(token) }
        val enrollments = async { api.fetchEnrollments(token) }
        RemoteSnapshot(
            students = students.await(),
            subscriptions = subscriptions.await(),
            teachers = teachers.await(),
            subjects = subjects.await(),
            groups = groups.await(),
            payments = payments.await(),
            announcements = announcements.await(),
            materials = materials.await(),
            slots = slots.await(),
            sessions = sessions.await(),
            attendance = attendance.await(),
            enrollments = enrollments.await(),
        )
    }

    private suspend fun store(snapshot: RemoteSnapshot, userId: String?): SyncResult {
        dao.upsertStudents(
            snapshot.students.map {
                SyncedStudentEntity(
                    remoteId = it.remoteId,
                    matricule = it.matricule,
                    fullName = it.fullName,
                    phone = it.phone,
                    levelName = it.levelName,
                    avatarPath = it.avatarPath,
                    isActive = it.isActive,
                    profileId = it.profileId,
                    createdAt = it.createdAt,
                )
            },
        )
        dao.upsertSubscriptions(
            snapshot.subscriptions.map {
                SyncedSubscriptionEntity(
                    remoteId = it.remoteId,
                    studentMatricule = it.studentMatricule,
                    sessionsTotal = it.sessionsTotal,
                    sessionsUsed = it.sessionsUsed,
                    sessionsRemaining = it.sessionsRemaining,
                    status = it.status,
                )
            },
        )
        dao.upsertTeachers(
            snapshot.teachers.map {
                SyncedTeacherEntity(
                    remoteId = it.remoteId,
                    fullName = it.fullName,
                    photoUrl = it.photoUrl,
                    description = it.description,
                    yearsOfExperience = it.yearsOfExperience,
                    isFeatured = it.isFeatured,
                    acceptsMessages = it.acceptsMessages,
                    rotationPriority = it.rotationPriority,
                    lastFeaturedEpochDay = it.lastFeaturedDate?.toEpochDay(),
                    isActive = it.isActive,
                    profileId = it.profileId,
                    subjects = it.subjects.joinToString("|"),
                )
            },
        )
        // the directory tables read the local `teachers` rows: mirror them so the
        // annuaire / rotation / messages work on the REAL teachers after a sync
        database.teacherDao().upsertMirror(
            snapshot.teachers.map {
                TeacherEntity(
                    id = it.remoteId,
                    fullName = it.fullName,
                    photoUrl = it.photoUrl,
                    subjects = it.subjects.joinToString("|"),
                    professionalDescription = it.description ?: "",
                    isFeaturedOnStudentHome = it.isFeatured,
                    acceptsStudentMessages = it.acceptsMessages,
                    featuredRotationPriority = it.rotationPriority,
                    lastFeaturedAtEpochDay = it.lastFeaturedDate?.toEpochDay(),
                    levels = "",
                    yearsOfExperience = it.yearsOfExperience,
                    groups = 0,
                    isActive = it.isActive,
                    profileId = it.profileId,
                )
            },
        )
        dao.upsertSubjects(
            snapshot.subjects.map { SyncedSubjectEntity(it.remoteId, it.code, it.name) },
        )
        dao.upsertGroups(
            snapshot.groups.map {
                SyncedGroupEntity(it.remoteId, it.name, it.levelName, it.teacherId, it.capacity)
            },
        )
        dao.upsertPayments(
            snapshot.payments.map {
                SyncedPaymentEntity(
                    remoteId = it.remoteId,
                    studentId = it.studentId,
                    amountDzd = it.amountDzd,
                    method = it.method,
                    status = it.status,
                    reference = it.reference,
                    dueOn = it.dueOn?.toString(),
                    paidAtIso = it.paidAtIso,
                    createdAtIso = it.createdAtIso,
                )
            },
        )
        dao.upsertAnnouncements(
            snapshot.announcements.map {
                SyncedAnnouncementEntity(
                    remoteId = it.remoteId,
                    title = it.title,
                    body = it.body,
                    audience = it.audience,
                    levelId = it.levelId,
                    isPinned = it.isPinned,
                    publishedAtIso = it.publishedAtIso,
                    expiresAtIso = it.expiresAtIso,
                    createdAtIso = it.createdAtIso,
                )
            },
        )
        dao.upsertMaterials(
            snapshot.materials.map {
                SyncedMaterialEntity(
                    remoteId = it.remoteId,
                    title = it.title,
                    description = it.description,
                    kind = it.kind,
                    storagePath = it.storagePath,
                    sizeBytes = it.sizeBytes,
                    subjectId = it.subjectId,
                    groupName = it.groupName,
                    isPublished = it.isPublished,
                    publishedAtIso = it.publishedAtIso,
                )
            },
        )
        dao.upsertSlots(
            snapshot.slots.map {
                SyncedSlotEntity(
                    remoteId = it.remoteId,
                    groupId = it.groupId,
                    groupName = it.groupName,
                    weekday = it.weekday,
                    start = it.start,
                    end = it.end,
                    roomName = it.roomName,
                )
            },
        )
        dao.upsertSessions(
            snapshot.sessions.map {
                SyncedSessionEntity(
                    remoteId = it.remoteId,
                    groupId = it.groupId,
                    groupName = it.groupName,
                    teacherId = it.teacherId,
                    teacherName = it.teacherName,
                    subjectName = it.subjectName,
                    roomName = it.roomName,
                    startsAtIso = it.startsAtIso,
                    endsAtIso = it.endsAtIso,
                    status = it.status,
                    topic = it.topic,
                )
            },
        )
        dao.upsertAttendance(
            snapshot.attendance.map {
                SyncedAttendanceEntity(
                    remoteId = it.remoteId,
                    sessionId = it.sessionId,
                    studentId = it.studentId,
                    status = it.status,
                    recordedAtIso = it.recordedAtIso,
                )
            },
        )
        dao.upsertEnrollments(
            snapshot.enrollments.map {
                SyncedEnrollmentEntity(
                    remoteId = it.remoteId,
                    studentId = it.studentId,
                    groupId = it.groupId,
                    joinedOn = it.joinedOn?.toString(),
                    leftOn = it.leftOn?.toString(),
                )
            },
        )
        if (userId != null) {
            val own = dao.studentByProfile(userId)
            preferences.ownMatricule = own?.matricule
            preferences.ownProfileId = userId
        }
        preferences.lastSyncAtMillis = clock()
        android.util.Log.d(
            "LZSync",
            "store terminé: ${snapshot.students.size} élèves, ${snapshot.subscriptions.size} abonnements, " +
                "${snapshot.teachers.size} enseignants, ${snapshot.groups.size} groupes, " +
                "${snapshot.subjects.size} matières, ${snapshot.payments.size} paiements, " +
                "${snapshot.announcements.size} annonces, ${snapshot.materials.size} supports, " +
                "${snapshot.slots.size} créneaux, ${snapshot.sessions.size} séances, " +
                "${snapshot.attendance.size} présences, ${snapshot.enrollments.size} inscriptions",
        )
        return SyncResult.Success
    }

    companion object {
        /** Minimum delay between automatic syncs (manual "Actualiser" is never throttled). */
        const val MIN_SYNC_INTERVAL_MS = 60 * 60 * 1000L

        /**
         * Automatic sync is skipped while the mirror is fresh. A full snapshot is
         * ~322 kB of JSON (~30 kB gzipped) and always optional: the app works offline.
         *
         * [autoSyncEnabled] is the admin's "Synchronisation automatique" switch; when it is off
         * no automatic snapshot runs at all (an explicit refresh still passes [force] = true).
         */
        fun shouldSync(
            lastSyncAtMillis: Long,
            nowMillis: Long,
            hasMirror: Boolean,
            autoSyncEnabled: Boolean = true,
        ): Boolean = autoSyncEnabled && (!hasMirror || nowMillis - lastSyncAtMillis >= MIN_SYNC_INTERVAL_MS)
    }
}
