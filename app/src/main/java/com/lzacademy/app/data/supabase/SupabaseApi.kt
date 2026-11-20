package com.lzacademy.app.data.supabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * One student of the academy, exactly as stored in Supabase (public.students).
 */
data class RemoteStudent(
    val remoteId: String,
    val matricule: String,
    val fullName: String,
    val phone: String?,
    val levelName: String?,
    val avatarPath: String?,
    val isActive: Boolean,
    val profileId: String?,
    /** ISO timestamp of the row creation (real enrollment growth chart). */
    val createdAt: String,
)

/**
 * One active subscription (public.subscriptions). `sessionsRemaining` is a generated
 * column computed server-side as sessions_total - sessions_used.
 */
data class RemoteSubscription(
    val remoteId: String,
    val studentMatricule: String,
    val sessionsTotal: Int,
    val sessionsUsed: Int,
    val sessionsRemaining: Int,
    val status: String,
)

/** One teacher (public.teachers), subjects flattened via teacher_subjects → subjects. */
data class RemoteTeacher(
    val remoteId: String,
    val fullName: String,
    val photoUrl: String?,
    val description: String?,
    val yearsOfExperience: Int,
    val isFeatured: Boolean,
    val acceptsMessages: Boolean,
    val rotationPriority: Int,
    val lastFeaturedDate: java.time.LocalDate?,
    val isActive: Boolean,
    /** Supabase profile uuid; equals the signed-in user id for the account's own teacher. */
    val profileId: String?,
    val subjects: List<String>,
)

/** One subject (public.subjects). */
data class RemoteSubject(val remoteId: String, val code: String?, val name: String)

/** One group (public.groups), level name embedded. */
data class RemoteGroup(
    val remoteId: String,
    val name: String,
    val levelName: String?,
    val teacherId: String?,
    val capacity: Int,
    val roomName: String? = null,
    val teacherName: String? = null,
)

/** One payment (public.payments) — source of the real revenue numbers. */
data class RemotePayment(
    val remoteId: String,
    val studentId: String,
    val amountDzd: Double,
    val method: String,
    val status: String,
    val reference: String?,
    val dueOn: java.time.LocalDate?,
    val paidAtIso: String?,
    val createdAtIso: String,
)

/** One salary/material payment made to a teacher (public.teacher_payments). */
data class RemoteTeacherPayment(
    val remoteId: String,
    val teacherId: String,
    val amountDzd: Double,
    val method: String,
    val note: String?,
    val paidAtIso: String,
)

/** One announcement (public.announcements). */
data class RemoteAnnouncement(
    val remoteId: String,
    val title: String,
    val body: String,
    val audience: String,
    val levelId: String?,
    val isPinned: Boolean,
    val publishedAtIso: String?,
    val expiresAtIso: String?,
    val createdAtIso: String,
)

/** One published material (public.materials). */
data class RemoteMaterial(
    val remoteId: String,
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

/** One weekly schedule slot (public.group_schedule_slots), times as "HH:mm[:ss]". */
data class RemoteSlot(
    val remoteId: String,
    val groupId: String,
    val groupName: String?,
    val weekday: Int,
    val start: String,
    val end: String,
    val roomName: String?,
    val roomId: String? = null,
)

/** One session (public.sessions), names embedded. */
data class RemoteSession(
    val remoteId: String,
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
    val subjectId: String? = null,
    val roomId: String? = null,
)

/** One attendance record (public.attendance). */
data class RemoteAttendance(
    val remoteId: String,
    val sessionId: String,
    val studentId: String,
    val status: String,
    val recordedAtIso: String,
)

/** One enrollment (public.enrollments): which student belongs to which group. */
data class RemoteEnrollment(
    val remoteId: String,
    val studentId: String,
    val groupId: String,
    val joinedOn: java.time.LocalDate?,
    val leftOn: java.time.LocalDate?,
)

/** Result of a Supabase password sign-in. */
data class SupabaseSession(val accessToken: String, val refreshToken: String, val userId: String)

/** Thrown when Supabase answers 401: the access token expired and must be refreshed. */
class AuthExpiredException : Exception()

/**
 * Minimal Supabase REST client (no external dependency): password auth, read students +
 * subscriptions with the signed-in role enforced by RLS, and renew a pack as admin.
 * The anon key is public by design; every call carries the user token so the server-side
 * row level security decides what the caller may see.
 */
class SupabaseApi(
    private val projectUrl: String = DEFAULT_PROJECT_URL,
    private val anonKey: String = DEFAULT_ANON_KEY,
) {
    suspend fun login(email: String, password: String): SupabaseSession? = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("email", email).put("password", password)
            val response = http(
                url = "$projectUrl/auth/v1/token?grant_type=password",
                method = "POST",
                apiKey = anonKey,
                token = null,
                payload = body.toString().toByteArray(),
                contentType = "application/json",
            )
            android.util.Log.d("LZSync", "login http=${response.code} bytes=${response.text.length}")
            if (response.code !in 200..299) return@runCatching null
            val json = JSONObject(response.text)
            SupabaseSession(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token", ""),
                userId = json.getJSONObject("user").getString("id"),
            )
        }.onFailure { android.util.Log.e("LZSync", "login failed", it) }
            .getOrNull()
    }

    /**
     * Renews an expired access token with the stored refresh token — one cheap POST
     * instead of a full password login, and the session never silently dies.
     * Free-tier friendly: called at most once per fetch cycle (on 401 only).
     */
    suspend fun refreshSession(refreshToken: String): SupabaseSession? = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("refresh_token", refreshToken)
            val response = http(
                url = "$projectUrl/auth/v1/token?grant_type=refresh_token",
                method = "POST",
                apiKey = anonKey,
                token = null,
                payload = body.toString().toByteArray(),
                contentType = "application/json",
            )
            android.util.Log.d("LZSync", "refreshSession http=${response.code}")
            if (response.code !in 200..299) return@runCatching null
            val json = JSONObject(response.text)
            SupabaseSession(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token", refreshToken),
                userId = json.getJSONObject("user").getString("id"),
            )
        }.onFailure { android.util.Log.e("LZSync", "refreshSession failed", it) }
            .getOrNull()
    }

    /** All students visible to the signed-in role (admin: the whole academy). */
    suspend fun fetchStudents(token: String): List<RemoteStudent> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/students" +
            "?select=id,matricule,full_name,phone,avatar_path,is_active,profile_id,created_at,levels(name)" +
            "&order=matricule&limit=1000"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "students http=${response.code} n/a bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            val level = row.optJSONObject("levels")
            RemoteStudent(
                remoteId = row.getString("id"),
                matricule = row.getString("matricule"),
                fullName = row.getString("full_name"),
                phone = row.optString("phone", "").ifBlank { null },
                levelName = level?.optString("name")?.ifBlank { null },
                avatarPath = row.optString("avatar_path", "").ifBlank { null },
                isActive = row.optBoolean("is_active", true),
                profileId = row.optString("profile_id", "").ifBlank { null },
                createdAt = row.optString("created_at", ""),
            )
        }
    }

    /** All subscriptions visible to the signed-in role, joined with their student matricule. */
    suspend fun fetchSubscriptions(token: String): List<RemoteSubscription> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/subscriptions" +
            "?select=id,sessions_total,sessions_used,sessions_remaining,status,students(matricule)&limit=1000"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "subscriptions http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).mapNotNull { i ->
            val row = array.getJSONObject(i)
            val student = row.optJSONObject("students") ?: return@mapNotNull null
            RemoteSubscription(
                remoteId = row.getString("id"),
                studentMatricule = student.getString("matricule"),
                sessionsTotal = row.getInt("sessions_total"),
                sessionsUsed = row.getInt("sessions_used"),
                sessionsRemaining = row.getInt("sessions_remaining"),
                status = row.getString("status"),
            )
        }
    }

    // ---------------------------------------------------------- extended mirror fetchers
    // One method per mirrored table (12 GETs per sync cycle, throttled 15 min). Same contract
    // everywhere: 401 → AuthExpiredException (refresh + retry upstream), other HTTP errors →
    // empty list (the app keeps working offline), join names embedded via PostgREST.

    /** All teachers (21 rows), subjects attached via teacher_subjects → subjects. */
    suspend fun fetchTeachers(token: String): List<RemoteTeacher> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/teachers" +
            "?select=id,full_name,photo_url,professional_description,years_of_experience," +
            "is_featured_on_student_home,accepts_student_messages,featured_rotation_priority," +
            "last_featured_at,is_active,profile_id,teacher_subjects(subjects(name))&limit=200"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "teachers http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            val subjectNames = row.optJSONArray("teacher_subjects")
                ?.let { ts -> (0 until ts.length()).mapNotNull { ts.getJSONObject(it).optJSONObject("subjects")?.optString("name") } }
                .orEmpty()
            RemoteTeacher(
                remoteId = row.getString("id"),
                fullName = row.getString("full_name"),
                photoUrl = row.optString("photo_url", "").ifBlank { null },
                description = row.optString("professional_description", "").ifBlank { null },
                yearsOfExperience = row.optInt("years_of_experience", 0),
                isFeatured = row.optBoolean("is_featured_on_student_home", false),
                acceptsMessages = row.optBoolean("accepts_student_messages", true),
                rotationPriority = row.optInt("featured_rotation_priority", 0),
                lastFeaturedDate = row.optString("last_featured_at", "").takeIf { it.isNotBlank() }
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                isActive = row.optBoolean("is_active", true),
                profileId = row.optString("profile_id", "").ifBlank { null },
                subjects = subjectNames,
            )
        }
    }

    /** Every subject of the academy (15 rows). */
    suspend fun fetchSubjects(token: String): List<RemoteSubject> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/subjects?select=id,code,name&limit=100"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "subjects http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteSubject(
                remoteId = row.getString("id"),
                code = row.optString("code", "").ifBlank { null },
                name = row.getString("name"),
            )
        }
    }

    /** Groups (4 rows), level name embedded. */
    suspend fun fetchGroups(token: String): List<RemoteGroup> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/groups" +
            "?select=id,name,capacity,teacher_id,levels(name),rooms(name),teachers(full_name)&limit=100"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "groups http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteGroup(
                remoteId = row.getString("id"),
                name = row.getString("name"),
                levelName = row.optJSONObject("levels")?.optString("name")?.ifBlank { null },
                teacherId = row.optString("teacher_id", "").ifBlank { null },
                capacity = row.optInt("capacity", 0),
                roomName = row.optJSONObject("rooms")?.optString("name")?.ifBlank { null },
                teacherName = row.optJSONObject("teachers")?.optString("full_name")?.ifBlank { null },
            )
        }
    }

    /** Payments (12 rows), newest first — source of the real revenue numbers. */
    suspend fun fetchPayments(token: String): List<RemotePayment> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/payments" +
            "?select=id,student_id,amount_dzd,method,status,reference,due_on,paid_at,created_at" +
            "&order=created_at.desc&limit=500"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "payments http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemotePayment(
                remoteId = row.getString("id"),
                studentId = row.getString("student_id"),
                amountDzd = row.optDouble("amount_dzd", 0.0),
                method = row.optString("method", ""),
                status = row.getString("status"),
                reference = row.optString("reference", "").ifBlank { null },
                dueOn = row.optString("due_on", "").takeIf { it.isNotBlank() }
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                paidAtIso = row.optString("paid_at", "").ifBlank { null },
                createdAtIso = row.optString("created_at", ""),
            )
        }
    }

    /** Teacher payments (salary/material), newest first. */
    suspend fun fetchTeacherPayments(token: String): List<RemoteTeacherPayment> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/teacher_payments" +
            "?select=id,teacher_id,amount_dzd,method,note,paid_at&order=paid_at.desc&limit=300"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "teacher_payments http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteTeacherPayment(
                remoteId = row.getString("id"),
                teacherId = row.getString("teacher_id"),
                amountDzd = row.optDouble("amount_dzd", 0.0),
                method = row.optString("method", ""),
                note = row.optString("note", "").ifBlank { null },
                paidAtIso = row.optString("paid_at", ""),
            )
        }
    }

    /** Announcements (4 rows), newest first. */
    suspend fun fetchAnnouncements(token: String): List<RemoteAnnouncement> = withContext(Dispatchers.IO) {        val url = "$projectUrl/rest/v1/announcements" +
            "?select=id,title,body,audience,level_id,group_id,is_pinned,published_at,expires_at,created_at" +
            "&order=published_at.desc&limit=100"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "announcements http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteAnnouncement(
                remoteId = row.getString("id"),
                title = row.getString("title"),
                body = row.optString("body", ""),
                audience = row.optString("audience", "all"),
                levelId = row.optString("level_id", "").ifBlank { null },
                isPinned = row.optBoolean("is_pinned", false),
                publishedAtIso = row.optString("published_at", "").ifBlank { null },
                expiresAtIso = row.optString("expires_at", "").ifBlank { null },
                createdAtIso = row.optString("created_at", ""),
            )
        }
    }

    /** Published materials (4 rows), subject/group names embedded when possible. */
    suspend fun fetchMaterials(token: String): List<RemoteMaterial> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/materials" +
            "?select=id,title,description,kind,storage_path,size_bytes,subject_id,is_published," +
            "published_at,groups(name)&limit=100"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "materials http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteMaterial(
                remoteId = row.getString("id"),
                title = row.getString("title"),
                description = row.optString("description", "").ifBlank { null },
                kind = row.optString("kind", "pdf"),
                storagePath = row.optString("storage_path", "").ifBlank { null },
                sizeBytes = row.optLong("size_bytes", 0L),
                subjectId = row.optString("subject_id", "").ifBlank { null },
                groupName = row.optJSONObject("groups")?.optString("name")?.ifBlank { null },
                isPublished = row.optBoolean("is_published", true),
                publishedAtIso = row.optString("published_at", "").ifBlank { null },
            )
        }
    }

    /** Weekly schedule slots (7 rows), room + group names embedded. */
    suspend fun fetchSlots(token: String): List<RemoteSlot> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/group_schedule_slots" +
            "?select=id,group_id,weekday,starts_at,ends_at,room_id,rooms(name),groups(name)&limit=200"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "slots http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteSlot(
                remoteId = row.getString("id"),
                groupId = row.getString("group_id"),
                groupName = row.optJSONObject("groups")?.optString("name")?.ifBlank { null },
                weekday = row.getInt("weekday"),
                start = row.getString("starts_at"),
                end = row.getString("ends_at"),
                roomName = row.optJSONObject("rooms")?.optString("name")?.ifBlank { null },
                roomId = row.optString("room_id", "").ifBlank { null },
            )
        }
    }

    /** Sessions (28 rows), group/subject/room/teacher names embedded, newest first. */
    suspend fun fetchSessions(token: String): List<RemoteSession> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/sessions" +
            "?select=id,group_id,teacher_id,subject_id,room_id,starts_at,ends_at,status,topic," +
            "groups(name),subjects(name),rooms(name),teachers(full_name)&order=starts_at.desc&limit=200"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "sessions http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteSession(
                remoteId = row.getString("id"),
                groupId = row.getString("group_id"),
                groupName = row.optJSONObject("groups")?.optString("name")?.ifBlank { null },
                teacherId = row.optString("teacher_id", "").ifBlank { null },
                teacherName = row.optJSONObject("teachers")?.optString("full_name")?.ifBlank { null },
                subjectName = row.optJSONObject("subjects")?.optString("name")?.ifBlank { null },
                roomName = row.optJSONObject("rooms")?.optString("name")?.ifBlank { null },
                startsAtIso = row.getString("starts_at"),
                endsAtIso = row.getString("ends_at"),
                status = row.optString("status", "scheduled"),
                topic = row.optString("topic", "").ifBlank { null },
                subjectId = row.optString("subject_id", "").ifBlank { null },
                roomId = row.optString("room_id", "").ifBlank { null },
            )
        }
    }

    /** Attendance records (4 rows), newest first. */
    suspend fun fetchAttendance(token: String): List<RemoteAttendance> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/attendance" +
            "?select=id,session_id,student_id,status,recorded_at&order=recorded_at.desc&limit=500"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "attendance http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteAttendance(
                remoteId = row.getString("id"),
                sessionId = row.getString("session_id"),
                studentId = row.getString("student_id"),
                status = row.getString("status"),
                recordedAtIso = row.optString("recorded_at", ""),
            )
        }
    }

    /** Enrollments (16 rows): which student belongs to which group. */
    suspend fun fetchEnrollments(token: String): List<RemoteEnrollment> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/enrollments" +
            "?select=id,student_id,group_id,joined_on,left_on&limit=1000"
        val response = http(url, "GET", anonKey, token)
        android.util.Log.d("LZSync", "enrollments http=${response.code} bytes=${response.text.length}")
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteEnrollment(
                remoteId = row.getString("id"),
                studentId = row.getString("student_id"),
                groupId = row.getString("group_id"),
                joinedOn = row.optString("joined_on", "").takeIf { it.isNotBlank() }
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                leftOn = row.optString("left_on", "").takeIf { it.isNotBlank() }
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
            )
        }
    }

    /** Admin-only renewal: credit the pack again (counter used = 0, active). */
    suspend fun renewSubscription(token: String, subscriptionId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("sessions_used", 0)
                .put("status", "active")
                .put("starts_on", java.time.LocalDate.now().toString())
            val response = http(
                url = "$projectUrl/rest/v1/subscriptions?id=eq." + URLEncoder.encode(subscriptionId, "UTF-8"),
                method = "PATCH",
                apiKey = anonKey,
                token = token,
                payload = body.toString().toByteArray(),
                contentType = "application/json",
                preferReturn = false,
            )
            response.code in 200..299
        }.getOrDefault(false)
    }

    /**
     * Real paid renewal: (PATCH|POST) the subscription on the chosen pack (fresh counter,
     * dates recalculated) then record a PAID payment row. Returns null on success.
     * sessions_remaining is a generated column — never written.
     */
    suspend fun renewWithPayment(
        token: String,
        subscriptionId: String?,
        studentId: String,
        packId: String,
        sessionsCount: Int,
        priceDzd: Double,
        endsOn: java.time.LocalDate?,
        method: String,
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val today = java.time.LocalDate.now().toString()
            val subPayload = JSONObject()
                .put("pack_id", packId)
                .put("sessions_total", sessionsCount)
                .put("sessions_used", 0)
                .put("price_dzd", priceDzd)
                .put("status", "active")
                .put("starts_on", today)
            endsOn?.let { subPayload.put("ends_on", it.toString()) }
            var subRef = subscriptionId
            if (subRef == null) {
                subPayload.put("student_id", studentId)
                val response = http(
                    url = "$projectUrl/rest/v1/subscriptions",
                    method = "POST",
                    apiKey = anonKey,
                    token = token,
                    payload = subPayload.toString().toByteArray(),
                    contentType = "application/json",
                    preferReturn = true,
                )
                if (response.code !in 200..299) {
                    return@runCatching "Erreur abonnement : ${response.text.take(180)}"
                }
                subRef = JSONArray(response.text).getJSONObject(0).getString("id")
            } else {
                val response = http(
                    url = "$projectUrl/rest/v1/subscriptions?id=eq." + URLEncoder.encode(subRef, "UTF-8"),
                    method = "PATCH",
                    apiKey = anonKey,
                    token = token,
                    payload = subPayload.toString().toByteArray(),
                    contentType = "application/json",
                    preferReturn = false,
                )
                if (response.code !in 200..299) {
                    return@runCatching "Erreur abonnement : ${response.text.take(180)}"
                }
            }
            val payment = JSONObject()
                .put("student_id", studentId)
                .put("subscription_id", subRef)
                .put("amount_dzd", priceDzd)
                .put("method", method)
                .put("status", "paid")
                .put("paid_at", java.time.OffsetDateTime.now().format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            val payResponse = http(
                url = "$projectUrl/rest/v1/payments",
                method = "POST",
                apiKey = anonKey,
                token = token,
                payload = payment.toString().toByteArray(),
                contentType = "application/json",
                preferReturn = false,
            )
            if (payResponse.code !in 200..299) "Erreur paiement : ${payResponse.text.take(180)}" else null
        }.getOrElse { "Erreur : ${it.message ?: "inconnue"}" }
    }

    // ---------------------------------------------------------- admin CRUD (Gestion screen)

    /** One room of the academy (public.rooms). */
    data class RemoteRoom(
        val remoteId: String,
        val name: String,
        val building: String,
        val capacity: Int,
        val isActive: Boolean,
    )

    /** One tariff pack (public.packs). */
    data class RemotePack(
        val remoteId: String,
        val name: String,
        val description: String?,
        val priceDzd: Double,
        val sessionsCount: Int,
        val durationMonths: Int?,
        val isActive: Boolean,
    )

    /** One school level (public.levels), used by the student form. */
    data class RemoteLevel(val remoteId: String, val name: String)

    suspend fun fetchRooms(token: String): List<RemoteRoom> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/rooms?select=id,name,building,capacity,is_active&order=name&limit=100"
        val response = http(url, "GET", anonKey, token)
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteRoom(
                remoteId = row.getString("id"),
                name = row.getString("name"),
                building = row.optString("building", ""),
                capacity = row.optInt("capacity", 0),
                isActive = row.optBoolean("is_active", true),
            )
        }
    }

    suspend fun fetchPacks(token: String): List<RemotePack> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/packs?select=id,name,description,price_dzd,sessions_count,duration_months,is_active&order=price_dzd&limit=100"
        val response = http(url, "GET", anonKey, token)
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemotePack(
                remoteId = row.getString("id"),
                name = row.getString("name"),
                description = row.optString("description", "").ifBlank { null },
                priceDzd = row.optDouble("price_dzd", 0.0),
                sessionsCount = row.optInt("sessions_count", 0),
                durationMonths = if (row.isNull("duration_months")) null else row.optInt("duration_months"),
                isActive = row.optBoolean("is_active", true),
            )
        }
    }

    suspend fun fetchLevels(token: String): List<RemoteLevel> = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/levels?select=id,name&order=name&limit=50"
        val response = http(url, "GET", anonKey, token)
        if (response.code == 401) throw AuthExpiredException()
        if (response.code !in 200..299) return@withContext emptyList()
        val array = JSONArray(response.text)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            RemoteLevel(remoteId = row.getString("id"), name = row.getString("name"))
        }
    }

    /**
     * The next free student matricule, computed from the current maximum (LZ-NNN + 1).
     * Returns null when the fetch fails — the caller refuses the insert rather than
     * guessing an identifier that may collide.
     */
    suspend fun nextMatricule(token: String): String? = withContext(Dispatchers.IO) {
        val url = "$projectUrl/rest/v1/students?select=matricule&order=matricule.desc&limit=1"
        val response = http(url, "GET", anonKey, token)
        if (response.code !in 200..299) return@withContext null
        val array = JSONArray(response.text)
        if (array.length() == 0) return@withContext "LZ-001"
        val last = array.getJSONObject(0).optString("matricule", "")
        val n = Regex("(\\d+)").findAll(last).lastOrNull()?.value?.toIntOrNull() ?: return@withContext null
        "LZ-%03d".format(n + 1)
    }

    /**
     * Generic PostgREST write used by every admin CRUD action. Returns null on success or
     * a short server message on failure (honest error reporting in the UI).
     */
    suspend fun adminWrite(
        token: String,
        table: String,
        method: String,
        id: String? = null,
        payload: Any? = null,
        filter: String? = null,
    ): String? = withContext(Dispatchers.IO) {
        // payload is a JSONObject (single row / PATCH-all) or a JSONArray (bulk insert).
        val url = when {
            id != null -> "$projectUrl/rest/v1/$table?id=eq." + URLEncoder.encode(id, "UTF-8")
            // e.g. "id=in.(uuid1,uuid2)" — one PATCH call for a whole selection.
            filter != null -> "$projectUrl/rest/v1/$table?" + URLEncoder.encode(filter, "UTF-8")
            else -> "$projectUrl/rest/v1/$table"
        }
        val response = http(
            url = url,
            method = method,
            apiKey = anonKey,
            token = token,
            payload = payload?.toString()?.toByteArray(),
            contentType = "application/json",
            preferReturn = false,
        )
        when {
            response.code in 200..299 -> null
            else -> response.text.take(240).ifBlank { "Erreur serveur (${response.code})" }
        }
    }

    /** Public URL of a stored avatar (bucket `photos`, public). */
    fun avatarUrl(path: String): String = "$projectUrl/storage/v1/object/public/$path"

    /**
     * Uploads a JPEG (upsert) at `<bucket>/<object>` — e.g. `photos/students/<uuid>.jpg`.
     * The bucket is already part of [path], exactly like the value stored in
     * students.avatar_path / teachers.photo_url and resolved by [avatarUrl].
     * Returns null on success, an error message otherwise.
     */
    suspend fun uploadPhoto(token: String, path: String, bytes: ByteArray): String? =
        withContext(Dispatchers.IO) {
            val url = "$projectUrl/storage/v1/object/$path"
            val response = http(
                url = url,
                method = "POST",
                apiKey = anonKey,
                token = token,
                payload = bytes,
                contentType = "image/jpeg",
                preferReturn = false,
                extraHeaders = mapOf("x-upsert" to "true"),
            )
            if (response.code in 200..299) null
            else response.text.take(240).ifBlank { "Erreur stockage (${response.code})" }
        }

    private data class HttpResponse(val code: Int, val text: String)

    private fun http(
        url: String,
        method: String,
        apiKey: String,
        token: String?,
        payload: ByteArray? = null,
        contentType: String? = null,
        preferReturn: Boolean = true,
        extraHeaders: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("apikey", apiKey)
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (contentType != null) connection.setRequestProperty("Content-Type", contentType)
            if (preferReturn) connection.setRequestProperty("Prefer", "return=representation")
            extraHeaders.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (payload != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(payload) }
            }
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val text = stream?.use { readAll(it) } ?: ""
            return HttpResponse(connection.responseCode, text)
        } finally {
            connection.disconnect()
        }
    }

    private fun readAll(stream: java.io.InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        return out.toString("UTF-8")
    }

    companion object {
        const val DEFAULT_PROJECT_URL = "https://qxnuuhjjuzudxyyleplw.supabase.co"
        const val DEFAULT_ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InF4bnV1aGpqdXp1ZHh5eWxlcGx3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQyMzg0NzQsImV4cCI6MjEwOTgxNDQ3NH0.ct6CHkGLdLUiuL5JPsDowvPcI5r86lc-n1evoZ_8_MA"
    }
}
