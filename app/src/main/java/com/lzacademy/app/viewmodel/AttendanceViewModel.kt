package com.lzacademy.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.models.AttendanceOutcome
import com.lzacademy.app.models.AttendanceRecord
import com.lzacademy.app.models.AttendanceStatus
import com.lzacademy.app.models.AttendanceUndo
import com.lzacademy.app.models.RenewalResult
import com.lzacademy.app.models.Student
import com.lzacademy.app.models.StudentRosterEntry
import com.lzacademy.app.models.Subscription
import com.lzacademy.app.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Shared state of the attendance layer.
 *
 * The teacher roll-call screen observes [roster]; the student dashboard and profile observe
 * [subscriptions] to display the real counter and to renew the pack. Every screen uses the same
 * Room database, so a presence marked by the teacher is visible on the student side immediately.
 */
class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AttendanceRepository(LZDatabase.get(application))

    /** The real roll-call session of the day (no demo session exists). */
    val sessionLabel: String = AttendanceRepository.DEFAULT_SESSION_LABEL

    val today: LocalDate = LocalDate.now()

    val roster: StateFlow<List<StudentRosterEntry>> = repository
        .observeRoster(today, sessionLabel)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Roll-call of ONE group only (real enrollments of that group). */
    fun roster(groupId: String): Flow<List<StudentRosterEntry>> =
        repository.observeRoster(today, sessionLabel, groupId = groupId)

    val students: StateFlow<List<Student>> = repository.students
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subscriptions: StateFlow<Map<String, Subscription>> = repository.subscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch { repository.hydrateSubscriptions() }
    }

    fun subscription(studentId: String?): Subscription? =
        studentId?.let { subscriptions.value[it] }

    fun record(
        studentId: String,
        status: AttendanceStatus,
        onResult: (AttendanceOutcome) -> Unit = {},
    ) {
        viewModelScope.launch {
            onResult(repository.record(studentId, today, sessionLabel, status))
        }
    }

    fun undo(recordId: Long, onResult: (AttendanceUndo) -> Unit = {}) {
        viewModelScope.launch {
            onResult(repository.undo(recordId))
        }
    }

    fun renew(studentId: String, onResult: (RenewalResult) -> Unit = {}) {
        viewModelScope.launch {
            onResult(repository.renew(studentId))
        }
    }

    fun history(studentId: String, limit: Int = 20): Flow<List<AttendanceRecord>> =
        repository.observeHistory(studentId, limit)
}
