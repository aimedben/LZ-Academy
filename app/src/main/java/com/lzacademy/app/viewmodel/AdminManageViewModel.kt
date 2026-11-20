package com.lzacademy.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.data.supabase.AuthExpiredException
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.repository.SupabaseSyncRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real CRUD for the admin "Gestion" screen: students, teachers, rooms and tariff packs.
 * Every action is a real write to the Supabase database (RLS admin policies) followed by a
 * re-fetch of the written table. No local demo list, nothing invented.
 *
 * Data is fetched LIVE (not from the Room mirror): every action is a server write anyway,
 * so the screen works online — offline it shows an honest error state.
 */
class AdminManageViewModel(application: Application) : AndroidViewModel(application) {

    private val api = SupabaseApi()
    private val preferences = AppPreferences(application)

    data class ManageData(
        val students: List<com.lzacademy.app.data.supabase.RemoteStudent> = emptyList(),
        val teachers: List<com.lzacademy.app.data.supabase.RemoteTeacher> = emptyList(),
        val rooms: List<SupabaseApi.RemoteRoom> = emptyList(),
        val packs: List<SupabaseApi.RemotePack> = emptyList(),
        val levels: List<SupabaseApi.RemoteLevel> = emptyList(),
        val groups: List<com.lzacademy.app.data.supabase.RemoteGroup> = emptyList(),
        /** Active enrollments per group id (left_on IS NULL) — drives the group cards' "N élèves". */
        val enrollmentCountByGroup: Map<String, Int> = emptyMap(),
        /** Real enrollment rows (public.enrollments) — drives the student detail sheet's course list. */
        val enrollments: List<com.lzacademy.app.data.supabase.RemoteEnrollment> = emptyList(),
        /** Real salary/material payments (public.teacher_payments), newest first. */
        val teacherPayments: List<com.lzacademy.app.data.supabase.RemoteTeacherPayment> = emptyList(),
        /** Weekly schedule slots (public.group_schedule_slots) — what students see by weekday. */
        val slots: List<com.lzacademy.app.data.supabase.RemoteSlot> = emptyList(),
        /** Dated sessions (public.sessions) — what teachers see in their planning. */
        val sessions: List<com.lzacademy.app.data.supabase.RemoteSession> = emptyList(),
        /** Subjects for the session form. */
        val subjects: List<com.lzacademy.app.data.supabase.RemoteSubject> = emptyList(),
    )

    private val _state = MutableStateFlow<UiState<ManageData>>(UiState.Loading)
    val state: StateFlow<UiState<ManageData>> = _state.asStateFlow()

    /** Last successful [load], for the 60 s re-open guard. */
    private var lastLoadedAt = 0L

    /** One-shot result message of the last action; null = no message. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        load()
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun load(force: Boolean = false) {
        if (!force && _state.value is UiState.Success &&
            System.currentTimeMillis() - lastLoadedAt < RELOAD_GUARD_MS
        ) {
            return
        }
        viewModelScope.launch {
            if (_state.value !is UiState.Success) _state.value = UiState.Loading
            if (preferences.supabaseToken == null) {
                _state.value = UiState.Error("Non connecté — connectez-vous en tant qu'administrateur.")
                return@launch
            }
            try {
                _state.value = UiState.Success(loadDataWithRefresh())
                lastLoadedAt = System.currentTimeMillis()
            } catch (e: AuthExpiredException) {
                _state.value = UiState.Error("Session expirée — reconnectez-vous.")
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Impossible de charger les données.")
            }
        }
    }

    /**
     * One refresh-token grant and one retry when the 1 h JWT expired — no liveness
     * probe, no second network round-trip on the happy path.
     */
    private suspend fun loadDataWithRefresh(): ManageData {
        val token = preferences.supabaseToken ?: error("Non connecté — connectez-vous en tant qu'administrateur.")
        return try {
            loadData(token)
        } catch (e: AuthExpiredException) {
            val refreshed = preferences.supabaseRefreshToken?.let { api.refreshSession(it) } ?: throw e
            preferences.supabaseToken = refreshed.accessToken
            preferences.supabaseRefreshToken = refreshed.refreshToken.takeIf { it.isNotBlank() }
            loadData(refreshed.accessToken)
        }
    }

    private suspend fun loadData(token: String): ManageData = withContext(kotlinx.coroutines.Dispatchers.IO) {
        kotlinx.coroutines.coroutineScope {
            val students = async { api.fetchStudents(token) }
            val teachers = async { api.fetchTeachers(token) }
            val rooms = async { api.fetchRooms(token) }
            val packs = async { api.fetchPacks(token) }
            val levels = async { api.fetchLevels(token) }
            val groups = async { api.fetchGroups(token) }
            val enrollments = async { api.fetchEnrollments(token) }
            val teacherPayments = async { api.fetchTeacherPayments(token) }
            val slots = async { api.fetchSlots(token) }
            val sessions = async { api.fetchSessions(token) }
            val subjects = async { api.fetchSubjects(token) }
            val enrollmentRows = enrollments.await()
            ManageData(
                students = students.await(),
                teachers = teachers.await(),
                rooms = rooms.await(),
                packs = packs.await(),
                levels = levels.await(),
                groups = groups.await(),
                enrollmentCountByGroup = enrollmentRows
                    .filter { it.leftOn == null }
                    .groupingBy { it.groupId }
                    .eachCount(),
                enrollments = enrollmentRows,
                teacherPayments = teacherPayments.await(),
                slots = slots.await(),
                sessions = sessions.await(),
                subjects = subjects.await(),
            )
        }
    }

    /** Re-fetch one table and publish it, keeping the other lists untouched. */
    private suspend fun reloadTable(token: String, table: String) {
        val current = (_state.value as? UiState.Success)?.data ?: ManageData()
        val reloaded = when (table) {
            "students" -> current.copy(students = api.fetchStudents(token))
            "teachers" -> current.copy(teachers = api.fetchTeachers(token))
            "rooms" -> current.copy(rooms = api.fetchRooms(token))
            "packs" -> current.copy(packs = api.fetchPacks(token))
            "groups" -> {
                val fresh = api.fetchEnrollments(token)
                current.copy(
                    groups = api.fetchGroups(token),
                    enrollmentCountByGroup = fresh
                        .filter { it.leftOn == null }
                        .groupingBy { it.groupId }
                        .eachCount(),
                    enrollments = fresh,
                )
            }
            "enrollments" -> {
                val fresh = api.fetchEnrollments(token)
                current.copy(
                    enrollments = fresh,
                    enrollmentCountByGroup = fresh
                        .filter { it.leftOn == null }
                        .groupingBy { it.groupId }
                        .eachCount(),
                )
            }
            "teacher_payments" -> current.copy(teacherPayments = api.fetchTeacherPayments(token))
            "subjects" -> current.copy(subjects = api.fetchSubjects(token))
            "group_schedule_slots" -> current.copy(slots = api.fetchSlots(token))
            "sessions" -> current.copy(sessions = api.fetchSessions(token))
            else -> current
        }
        _state.value = UiState.Success(reloaded)
    }

    /**
     * One server write + targeted reload. Returns null on success, an error message otherwise.
     * Called from a coroutine; the public actions below wrap it with [withResult].
     */
    private suspend fun writeAndReload(
        table: String,
        method: String,
        id: String? = null,
        payload: JSONObject? = null,
        successMessage: String,
    ): String? = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val token = preferences.supabaseToken ?: return@withContext "Non connecté — action impossible."
        val error = api.adminWrite(token, table, method, id, payload)
        if (error != null) {
            Log.w("LZManage", "$method $table failed: $error")
            return@withContext "Erreur : ${error.take(180)}"
        }
        // The row is already written server-side. A token expiry while reloading must never
        // bubble up: withResult() would replay the whole action and duplicate the row. So the
        // refresh here only retries the READ; the write is not repeated.
        try {
            reloadTable(token, table)
        } catch (e: AuthExpiredException) {
            val refreshed = preferences.supabaseRefreshToken?.let { api.refreshSession(it) }
            if (refreshed != null) {
                preferences.supabaseToken = refreshed.accessToken
                runCatching { reloadTable(refreshed.accessToken, table) }
            }
        }
        null
    }

    /** Runs a write action in the view scope and emits its result message once. */
    private fun withResult(block: suspend () -> String?) {
        viewModelScope.launch {
            _message.value = try {
                block()
            } catch (e: AuthExpiredException) {
                val refreshed = preferences.supabaseRefreshToken?.let { api.refreshSession(it) }
                if (refreshed != null) {
                    preferences.supabaseToken = refreshed.accessToken
                    try {
                        block()
                    } catch (e2: Exception) {
                        "Erreur : ${e2.message ?: "inconnue"}"
                    }
                } else {
                    "Session expirée — reconnectez-vous."
                }
            } catch (e: Exception) {
                Log.w("LZManage", "action failed", e)
                "Erreur : ${e.message ?: "inconnue"}"
            }
        }
    }

    // ------------------------------------------------------------ students

    fun addStudent(fullName: String, levelId: String?, phone: String?, guardianName: String?, guardianPhone: String?) = withResult {
        val token = preferences.supabaseToken ?: return@withResult "Non connecté — action impossible."
        val matricule = api.nextMatricule(token)
            ?: return@withResult "Impossible d'attribuer un matricule — vérifiez la connexion."
        val payload = JSONObject()
            .put("matricule", matricule)
            .put("full_name", fullName)
            .put("is_active", true)
        levelId?.takeIf { it.isNotBlank() }?.let { payload.put("level_id", it) }
        phone?.takeIf { it.isNotBlank() }?.let { payload.put("phone", it) }
        guardianName?.takeIf { it.isNotBlank() }?.let { payload.put("guardian_name", it) }
        guardianPhone?.takeIf { it.isNotBlank() }?.let { payload.put("guardian_phone", it) }
        val error = writeAndReload("students", "POST", payload = payload, successMessage = "Inscription réussie ($matricule).")
        if (error == null) "Élève inscrit : $fullName ($matricule)." else error
    }

    fun updateStudent(id: String, fullName: String, levelId: String?, phone: String?, guardianName: String?, guardianPhone: String?) = withResult {
        val payload = JSONObject().put("full_name", fullName)
        if (levelId.isNullOrBlank()) payload.put("level_id", JSONObject.NULL) else payload.put("level_id", levelId)
        if (phone.isNullOrBlank()) payload.put("phone", JSONObject.NULL) else payload.put("phone", phone)
        if (guardianName.isNullOrBlank()) payload.put("guardian_name", JSONObject.NULL) else payload.put("guardian_name", guardianName)
        if (guardianPhone.isNullOrBlank()) payload.put("guardian_phone", JSONObject.NULL) else payload.put("guardian_phone", guardianPhone)
        val error = writeAndReload("students", "PATCH", id = id, payload = payload, successMessage = "Mise à jour.")
        if (error == null) "Élève mis à jour : $fullName." else error
    }

    fun toggleStudentActive(id: String, isActive: Boolean) = withResult {
        val error = writeAndReload(
            "students", "PATCH", id = id,
            payload = JSONObject().put("is_active", isActive),
            successMessage = "Statut mis à jour.",
        )
        if (error == null) (if (isActive) "Élève réactivé." else "Élève désactivé.") else error
    }

    fun deleteStudent(id: String, fullName: String) = withResult {
        val error = writeAndReload("students", "DELETE", id = id, successMessage = "Suppression.")
        when {
            error == null -> "Élève supprimé : $fullName."
            error.contains("foreign key", ignoreCase = true) ->
                "Suppression impossible : cet élève a des paiements/séances liés. Désactivez-le plutôt."
            else -> error
        }
    }

    // ------------------------------------------------------------ teachers

    fun addTeacher(fullName: String, description: String, years: Int) = withResult {
        val payload = JSONObject()
            .put("full_name", fullName)
            .put("professional_description", description)
            .put("years_of_experience", years)
            .put("is_active", true)
            .put("accepts_student_messages", true)
        val error = writeAndReload("teachers", "POST", payload = payload, successMessage = "Ajout.")
        if (error == null) "Enseignant ajouté : $fullName." else error
    }

    fun updateTeacher(id: String, fullName: String, description: String, years: Int) = withResult {
        val payload = JSONObject()
            .put("full_name", fullName)
            .put("professional_description", description)
            .put("years_of_experience", years)
        val error = writeAndReload("teachers", "PATCH", id = id, payload = payload, successMessage = "Mise à jour.")
        if (error == null) "Enseignant mis à jour : $fullName." else error
    }

    fun deleteTeacher(id: String, fullName: String) = withResult {
        val error = writeAndReload("teachers", "DELETE", id = id, successMessage = "Suppression.")
        when {
            error == null -> "Enseignant supprimé : $fullName."
            error.contains("foreign key", ignoreCase = true) ->
                "Suppression impossible : cet enseignant est lié à des groupes/séances."
            else -> error
        }
    }

    /** Real server write of the showcase visibility (was local-only before). */
    fun toggleTeacherFeatured(id: String, featured: Boolean) = withResult {
        val error = writeAndReload(
            "teachers", "PATCH", id = id,
            payload = JSONObject().put("is_featured_on_student_home", featured),
            successMessage = "Visibilité mise à jour.",
        )
        if (error == null) (if (featured) "Remis dans l'accueil élèves." else "Retiré de l'accueil élèves.") else error
    }

    // ------------------------------------------------------------ groups

    fun addGroup(name: String, levelId: String?, teacherId: String?, roomId: String?, capacity: Int) = withResult {
        val payload = JSONObject()
            .put("name", name)
            .put("capacity", capacity)
        levelId?.let { payload.put("level_id", it) }
        teacherId?.let { payload.put("teacher_id", it) }
        roomId?.let { payload.put("default_room_id", it) }
        val error = writeAndReload("groups", "POST", payload = payload, successMessage = "Création.")
        if (error == null) "Groupe créé : $name." else error
    }

    fun updateGroup(id: String, name: String, levelId: String?, teacherId: String?, roomId: String?, capacity: Int) = withResult {
        val payload = JSONObject().put("name", name).put("capacity", capacity)
        if (levelId == null) payload.put("level_id", JSONObject.NULL) else payload.put("level_id", levelId)
        if (teacherId == null) payload.put("teacher_id", JSONObject.NULL) else payload.put("teacher_id", teacherId)
        if (roomId == null) payload.put("default_room_id", JSONObject.NULL) else payload.put("default_room_id", roomId)
        val error = writeAndReload("groups", "PATCH", id = id, payload = payload, successMessage = "Mise à jour.")
        if (error == null) "Groupe mis à jour : $name." else error
    }

    fun deleteGroup(id: String, name: String) = withResult {
        val error = writeAndReload("groups", "DELETE", id = id, successMessage = "Suppression.")
        when {
            error == null -> "Groupe supprimé : $name."
            error.contains("foreign key", ignoreCase = true) ->
                "Suppression impossible : des élèves sont inscrits à ce groupe. Retirez-les d'abord."
            else -> error
        }
    }

    /** Enroll one student into a course (group) from the detail sheet. */
    fun addEnrollment(studentId: String, groupId: String, groupName: String) = withResult {
        val token = preferences.supabaseToken ?: return@withResult "Non connecté — action impossible."
        val alreadyIn = api.fetchEnrollments(token)
            .filter { it.groupId == groupId && it.studentId == studentId && it.leftOn == null }
        if (alreadyIn.isNotEmpty()) return@withResult "L'élève est déjà inscrit à « $groupName »."
        val rows = JSONArray().put(JSONObject().put("student_id", studentId).put("group_id", groupId))
        val error = api.adminWrite(token, "enrollments", "POST", payload = rows)
        if (error != null) {
            Log.w("LZManage", "enrollment POST failed: $error")
            return@withResult "Erreur : ${error.take(180)}"
        }
        reloadTable(token, "enrollments")
        "Inscrit au cours « $groupName »."
    }

    /** Remove one enrollment row (the student leaves that course). */
    fun removeEnrollment(enrollmentId: String, groupName: String) = withResult {
        val error = writeAndReload("enrollments", "DELETE", id = enrollmentId, successMessage = "Suppression.")
        if (error == null) "Retiré du cours « $groupName »." else error
    }

    // ------------------------------------------------------------ planning (créneaux + séances)

    /** "16:00" → "16:00:00" (Postgres `time`). */
    private fun normalizeTime(value: String): String =
        if (value.count { it == ':' } == 1) "$value:00" else value

    /**
     * "2026-11-20" + "16:00" → ISO-8601 avec le décalage de l'appareil
     * (ex. "2026-11-20T16:00+01:00"), pour que la base stocke l'instant voulu.
     */
    private fun isoDateTime(dateIso: String, time: String): String? = runCatching {
        val date = java.time.LocalDate.parse(dateIso.trim())
        val local = java.time.LocalTime.parse(normalizeTime(time.trim()))
        java.time.ZonedDateTime.of(date, local, java.time.ZoneId.systemDefault())
            .toOffsetDateTime()
            .toString()
    }.getOrNull()

    /** Weekly slot: the recurring planning shown to students (and to the group's teacher). */
    fun addSlot(groupId: String, weekday: Int, start: String, end: String, roomId: String?) = withResult {
        val payload = JSONObject()
            .put("group_id", groupId)
            .put("weekday", weekday)
            .put("starts_at", normalizeTime(start.trim()))
            .put("ends_at", normalizeTime(end.trim()))
        if (roomId == null) payload.put("room_id", JSONObject.NULL) else payload.put("room_id", roomId)
        val error = writeAndReload("group_schedule_slots", "POST", payload = payload, successMessage = "Créneau créé.")
        if (error == null) "Créneau ajouté au planning hebdomadaire." else error
    }

    fun updateSlot(id: String, groupId: String, weekday: Int, start: String, end: String, roomId: String?) = withResult {
        val payload = JSONObject()
            .put("group_id", groupId)
            .put("weekday", weekday)
            .put("starts_at", normalizeTime(start.trim()))
            .put("ends_at", normalizeTime(end.trim()))
        if (roomId == null) payload.put("room_id", JSONObject.NULL) else payload.put("room_id", roomId)
        val error = writeAndReload("group_schedule_slots", "PATCH", id = id, payload = payload, successMessage = "Mise à jour.")
        if (error == null) "Créneau mis à jour." else error
    }

    fun deleteSlot(id: String, label: String) = withResult {
        val error = writeAndReload("group_schedule_slots", "DELETE", id = id, successMessage = "Suppression.")
        if (error == null) "Créneau supprimé : $label." else error
    }

    /** Dated session: the real lesson shown in teachers' planning and students' "next lesson". */
    fun addSession(
        groupId: String,
        teacherId: String?,
        subjectId: String?,
        roomId: String?,
        dateIso: String,
        start: String,
        end: String,
        topic: String?,
        status: String,
    ) = withResult {
        val starts = isoDateTime(dateIso, start)
            ?: return@withResult "Date ou heure de début invalide (AAAA-MM-JJ / HH:mm)."
        val ends = isoDateTime(dateIso, end)
            ?: return@withResult "Heure de fin invalide (HH:mm)."
        val payload = sessionPayload(groupId, teacherId, subjectId, roomId, starts, ends, topic, status)
        val error = writeAndReload("sessions", "POST", payload = payload, successMessage = "Séance créée.")
        if (error == null) "Séance créée." else error
    }

    fun updateSession(
        id: String,
        groupId: String,
        teacherId: String?,
        subjectId: String?,
        roomId: String?,
        dateIso: String,
        start: String,
        end: String,
        topic: String?,
        status: String,
    ) = withResult {
        val starts = isoDateTime(dateIso, start)
            ?: return@withResult "Date ou heure de début invalide (AAAA-MM-JJ / HH:mm)."
        val ends = isoDateTime(dateIso, end)
            ?: return@withResult "Heure de fin invalide (HH:mm)."
        val payload = sessionPayload(groupId, teacherId, subjectId, roomId, starts, ends, topic, status)
        val error = writeAndReload("sessions", "PATCH", id = id, payload = payload, successMessage = "Mise à jour.")
        if (error == null) "Séance mise à jour." else error
    }

    private fun sessionPayload(
        groupId: String,
        teacherId: String?,
        subjectId: String?,
        roomId: String?,
        startsAtIso: String,
        endsAtIso: String,
        topic: String?,
        status: String,
    ): JSONObject {
        val payload = JSONObject()
            .put("group_id", groupId)
            .put("starts_at", startsAtIso)
            .put("ends_at", endsAtIso)
            .put("status", status)
        if (teacherId == null) payload.put("teacher_id", JSONObject.NULL) else payload.put("teacher_id", teacherId)
        if (subjectId == null) payload.put("subject_id", JSONObject.NULL) else payload.put("subject_id", subjectId)
        if (roomId == null) payload.put("room_id", JSONObject.NULL) else payload.put("room_id", roomId)
        if (topic.isNullOrBlank()) payload.put("topic", JSONObject.NULL) else payload.put("topic", topic.trim())
        return payload
    }

    fun deleteSession(id: String, label: String) = withResult {
        val error = writeAndReload("sessions", "DELETE", id = id, successMessage = "Suppression.")
        when {
            error == null -> "Séance supprimée : $label."
            error.contains("foreign key", ignoreCase = true) ->
                "Suppression impossible : des présences sont liées à cette séance."
            else -> error
        }
    }

    // ------------------------------------------------------------ teacher payments

    /** One real salary/material payment recorded with its date. */
    fun addTeacherPayment(teacherId: String, amountDzd: Double, method: String, note: String?, paidAtIso: String) = withResult {
        val payload = JSONObject()
            .put("teacher_id", teacherId)
            .put("amount_dzd", amountDzd)
            .put("method", method)
            .put("paid_at", paidAtIso)
        note?.takeIf { it.isNotBlank() }?.let { payload.put("note", it) }
        val error = writeAndReload("teacher_payments", "POST", payload = payload, successMessage = "Paiement enregistré.")
        if (error == null) "Paiement enseignant enregistré." else error
    }

    fun deleteTeacherPayment(id: String) = withResult {
        val error = writeAndReload("teacher_payments", "DELETE", id = id, successMessage = "Suppression.")
        if (error == null) "Paiement supprimé." else error
    }

    // ------------------------------------------------------------ profile photos

    /**
     * Uploads a JPEG to the public `photos` bucket then stores the storage path on the
     * row (students.avatar_path / teachers.photo_url). Path convention matches the
     * existing avatar_path values: "<bucket>/<object>" resolved with SupabaseApi.avatarUrl.
     */
    fun updateStudentPhoto(studentId: String, bytes: ByteArray) = withResult {
        val token = preferences.supabaseToken ?: return@withResult "Non connecté — action impossible."
        val path = "photos/students/$studentId.jpg"
        val uploadError = api.uploadPhoto(token, path, bytes)
        if (uploadError != null) return@withResult "Erreur : ${uploadError.take(180)}"
        // Cache-buster: the storage URL must CHANGE, otherwise the disk-cached avatar
        // keeps showing the previous photo after a successful upload.
        val versionedPath = "$path?v=${System.currentTimeMillis()}"
        val error = writeAndReload(
            "students", "PATCH", id = studentId,
            payload = JSONObject().put("avatar_path", versionedPath),
            successMessage = "Photo mise à jour.",
        )
        if (error == null) {
            refreshMirrorAfterPhoto()
            "Photo de l'élève mise à jour."
        } else error
    }

    fun updateTeacherPhoto(teacherId: String, bytes: ByteArray) = withResult {
        val token = preferences.supabaseToken ?: return@withResult "Non connecté — action impossible."
        val path = "photos/teachers/$teacherId.jpg"
        val uploadError = api.uploadPhoto(token, path, bytes)
        if (uploadError != null) return@withResult "Erreur : ${uploadError.take(180)}"
        val versionedPath = "$path?v=${System.currentTimeMillis()}"
        val error = writeAndReload(
            "teachers", "PATCH", id = teacherId,
            payload = JSONObject().put("photo_url", versionedPath),
            successMessage = "Photo mise à jour.",
        )
        if (error == null) {
            refreshMirrorAfterPhoto()
            "Photo de l'enseignant mise à jour."
        } else error
    }

    /**
     * A photo write changes the offline mirror (Room) that the student home, the teacher
     * directory and the teacher's own profile read. Refresh it immediately (force = true)
     * so the new photo appears everywhere without waiting for the 10-minute sync window.
     */
    private suspend fun refreshMirrorAfterPhoto() {
        runCatching {
            SupabaseSyncRepository(
                database = LZDatabase.get(getApplication()),
                preferences = preferences,
            ).refresh(force = true)
        }.onFailure { Log.w("LZManage", "mirror refresh after photo failed", it) }
    }

    // ------------------------------------------------------------ rooms

    fun addRoom(name: String, building: String, capacity: Int) = withResult {
        val payload = JSONObject()
            .put("name", name)
            .put("building", building.ifBlank { "Bâtiment principal" })
            .put("capacity", capacity)
            .put("is_active", true)
        val error = writeAndReload("rooms", "POST", payload = payload, successMessage = "Création.")
        if (error == null) "Salle créée : $name." else error
    }

    fun updateRoom(id: String, name: String, building: String, capacity: Int, isActive: Boolean) = withResult {
        val payload = JSONObject()
            .put("name", name)
            .put("building", building.ifBlank { "Bâtiment principal" })
            .put("capacity", capacity)
            .put("is_active", isActive)
        val error = writeAndReload("rooms", "PATCH", id = id, payload = payload, successMessage = "Mise à jour.")
        if (error == null) "Salle mise à jour : $name." else error
    }

    fun deleteRoom(id: String, name: String) = withResult {
        val error = writeAndReload("rooms", "DELETE", id = id, successMessage = "Suppression.")
        if (error == null) "Salle supprimée : $name." else error
    }

    // ------------------------------------------------------------ bulk actions (multi-selection)

    /**
     * One PATCH call for the whole selection: PostgREST filter `id=in.(uuid,...)`.
     * Returns null on success, an error message otherwise.
     */
    private suspend fun bulkPatch(table: String, payload: JSONObject, ids: List<String>): String? {
        val token = preferences.supabaseToken ?: return "Non connecté — action impossible."
        val filter = "id=in.(" + ids.joinToString(",") + ")"
        val error = api.adminWrite(token, table, "PATCH", payload = payload, filter = filter)
        if (error != null) {
            Log.w("LZManage", "bulk PATCH $table failed: $error")
            return "Erreur : ${error.take(180)}"
        }
        reloadTable(token, table)
        return null
    }

    /** Change the level of every selected student in one server call. */
    fun bulkAssignLevel(ids: List<String>, levelId: String, levelName: String) = withResult {
        if (ids.isEmpty()) return@withResult null
        val error = bulkPatch("students", JSONObject().put("level_id", levelId), ids)
        if (error == null) "Niveau « $levelName » affecté à ${ids.size} élève(s)." else error
    }

    /**
     * Enroll every selected student into a group (real rows in public.enrollments).
     * Students already enrolled in that group are skipped (unique constraint student+group),
     * so the message reports the exact counts.
     */
    fun bulkAddToGroup(ids: List<String>, groupId: String, groupName: String) = withResult {
        if (ids.isEmpty()) return@withResult null
        val token = preferences.supabaseToken ?: return@withResult "Non connecté — action impossible."
        val alreadyIn = api.fetchEnrollments(token)
            .filter { it.groupId == groupId && it.leftOn == null }
            .map { it.studentId }
            .toSet()
        val newIds = ids.filterNot { it in alreadyIn }
        if (newIds.isEmpty()) return@withResult "Tous les élèves sélectionnés sont déjà dans « $groupName »."
        val rows = JSONArray()
        newIds.forEach { rows.put(JSONObject().put("student_id", it).put("group_id", groupId)) }
        val error = api.adminWrite(token, "enrollments", "POST", payload = rows)
        if (error != null) {
            Log.w("LZManage", "bulk enrollments POST failed: $error")
            return@withResult "Erreur : ${error.take(180)}"
        }
        val skipped = ids.size - newIds.size
        val suffix = if (skipped > 0) " ($skipped déjà inscrit(s) ignoré(s))" else ""
        "${newIds.size} élève(s) ajouté(s) au groupe « $groupName »$suffix."
    }

    /** Enable/disable every selected student in one server call. */
    fun bulkSetStudentActive(ids: List<String>, isActive: Boolean) = withResult {
        if (ids.isEmpty()) return@withResult null
        val error = bulkPatch("students", JSONObject().put("is_active", isActive), ids)
        if (error == null) {
            if (isActive) "${ids.size} élève(s) réactivé(s)." else "${ids.size} élève(s) désactivé(s)."
        } else error
    }

    /** Enable/disable every selected teacher in one server call. */
    fun bulkSetTeacherActive(ids: List<String>, isActive: Boolean) = withResult {
        if (ids.isEmpty()) return@withResult null
        val error = bulkPatch("teachers", JSONObject().put("is_active", isActive), ids)
        if (error == null) {
            if (isActive) "${ids.size} enseignant(s) réactivé(s)." else "${ids.size} enseignant(s) désactivé(s)."
        } else error
    }

    /** Put back / remove every selected teacher from the student home showcase. */
    fun bulkSetTeacherFeatured(ids: List<String>, featured: Boolean) = withResult {        if (ids.isEmpty()) return@withResult null
        val error = bulkPatch("teachers", JSONObject().put("is_featured_on_student_home", featured), ids)
        if (error == null) {
            if (featured) "${ids.size} enseignant(s) remis sur l'accueil élèves."
            else "${ids.size} enseignant(s) retiré(s) de l'accueil élèves."
        } else error
    }

    // ------------------------------------------------------------ packs

    private fun packPayload(name: String, description: String, priceDzd: Double, sessionsCount: Int, durationMonths: Int?, isActive: Boolean): JSONObject {
        val payload = JSONObject()
            .put("name", name)
            .put("description", description.ifBlank { JSONObject.NULL })
            .put("price_dzd", priceDzd)
            .put("sessions_count", sessionsCount)
            .put("is_active", isActive)
        if (durationMonths == null) payload.put("duration_months", JSONObject.NULL) else payload.put("duration_months", durationMonths)
        return payload
    }

    fun addPack(name: String, description: String, priceDzd: Double, sessionsCount: Int, durationMonths: Int?) = withResult {
        val payload = packPayload(name, description, priceDzd, sessionsCount, durationMonths, isActive = true)
        val error = writeAndReload("packs", "POST", payload = payload, successMessage = "Création.")
        if (error == null) "Pack créé : $name." else error
    }

    fun updatePack(id: String, name: String, description: String, priceDzd: Double, sessionsCount: Int, durationMonths: Int?, isActive: Boolean) = withResult {
        val payload = packPayload(name, description, priceDzd, sessionsCount, durationMonths, isActive)
        val error = writeAndReload("packs", "PATCH", id = id, payload = payload, successMessage = "Mise à jour.")
        if (error == null) "Pack mis à jour : $name." else error
    }

    fun deletePack(id: String, name: String) = withResult {
        val error = writeAndReload("packs", "DELETE", id = id, successMessage = "Suppression.")
        if (error == null) "Pack supprimé : $name." else error
    }

    private companion object {
        /** A screen re-opened within this window reuses the data already loaded. */
        const val RELOAD_GUARD_MS = 60_000L
    }
}
