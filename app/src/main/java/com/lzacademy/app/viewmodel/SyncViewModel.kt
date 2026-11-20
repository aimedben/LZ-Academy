package com.lzacademy.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lzacademy.app.data.supabase.RemotePayment
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.data.supabase.SyncedSubscriptionEntity
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.repository.SupabaseSyncRepository
import com.lzacademy.app.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Sync status shown in the admin subscriptions header. */
data class SyncUiState(
    val busy: Boolean = false,
    val lastResult: SyncResult? = null,
)

/**
 * Online layer of the app: exposes the offline Room mirror of the Supabase
 * academy database and the admin renewal action. Screens observe [students] and
 * [subscriptions] whether or not the device is online — data comes from the
 * last successful sync.
 */
class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SupabaseSyncRepository(
        database = LZDatabase.get(application),
        preferences = com.lzacademy.app.data.preferences.AppPreferences(application),
    )

    val students: StateFlow<List<SyncedStudentEntity>> =
        repository.observeStudents()
            .onEach {
                android.util.Log.d(
                    "LZSync",
                    "flow students émis: ${it.size} vm=${System.identityHashCode(this@SyncViewModel)}",
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subscriptions: StateFlow<List<SyncedSubscriptionEntity>> =
        repository.observeSubscriptions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Course enrollments of the student (mirror of public.enrollments). */
    val enrollments: StateFlow<List<com.lzacademy.app.data.supabase.SyncedEnrollmentEntity>> =
        repository.observeEnrollments()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Academy groups (mirror of public.groups) — course names for the profile. */
    val groups: StateFlow<List<com.lzacademy.app.data.supabase.SyncedGroupEntity>> =
        repository.observeGroups()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Academy teachers (mirror of public.teachers) — names attached to the courses. */
    val teachers: StateFlow<List<com.lzacademy.app.data.supabase.SyncedTeacherEntity>> =
        repository.observeTeachers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _syncState = MutableStateFlow(SyncUiState())
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    init {
        // Silent refresh on screen open when a token is already stored.
        refreshIfPossible()
        // Weekly message retention: keeps the local store bounded even if nobody sends.
        viewModelScope.launch {
            runCatching {
                com.lzacademy.app.repository.TeacherRepository(
                    com.lzacademy.app.data.teachers.LZDatabase.get(application),
                ).purgeOldMessages()
            }
        }
    }

    fun refreshIfPossible(force: Boolean = false) {
        if (_syncState.value.busy) return
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(busy = true)
            val result = repository.refresh(force = force)
            _syncState.value = SyncUiState(busy = false, lastResult = result)
        }
    }

    /** Admin-only: renew the pack of the student with this matricule (PATCH + local mirror). */
    fun renew(matricule: String, onDone: (Boolean) -> Unit = {}) {
        if (_syncState.value.busy) return
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(busy = true)
            val ok = repository.renewSubscription(matricule)
            _syncState.value = SyncUiState(busy = false, lastResult = if (ok) SyncResult.Success else SyncResult.Error)
            onDone(ok)
        }
    }

    // ------------------------------------------------ real paid renewal + payment history

    private val _packs = MutableStateFlow<List<SupabaseApi.RemotePack>>(emptyList())
    val packs: StateFlow<List<SupabaseApi.RemotePack>> = _packs.asStateFlow()

    /** Live read of the active packs once, for the renewal dialog. */
    fun loadPacks() {
        viewModelScope.launch {
            if (_packs.value.isEmpty()) _packs.value = repository.activePacks()
        }
    }

    private val _studentPayments = MutableStateFlow<List<RemotePayment>>(emptyList())
    val studentPayments: StateFlow<List<RemotePayment>> = _studentPayments.asStateFlow()

    /** Live read of one student's payment history (real public.payments rows). */
    fun loadPaymentsOf(studentRemoteId: String) {
        viewModelScope.launch {
            _studentPayments.value = repository.paymentsOfStudent(studentRemoteId)
        }
    }

    /**
     * Real paid renewal: subscription reset on the chosen pack + a PAID payment row recorded,
     * then the offline mirror is refreshed so the counter is instantly correct.
     */
    fun renewWithPayment(
        matricule: String,
        packId: String,
        sessionsCount: Int,
        priceDzd: Double,
        durationMonths: Int?,
        method: String,
        onDone: (String?) -> Unit = {},
    ) {
        if (_syncState.value.busy) return
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(busy = true)
            val error = repository.renewWithPayment(matricule, packId, sessionsCount, priceDzd, durationMonths, method)
            _syncState.value = SyncUiState(busy = false, lastResult = if (error == null) SyncResult.Success else SyncResult.Error)
            onDone(error)
        }
    }
}
