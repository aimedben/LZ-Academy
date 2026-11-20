package com.lzacademy.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.models.AdminDashboardData
import com.lzacademy.app.models.StudentDashboardData
import com.lzacademy.app.models.TeacherDashboardData
import com.lzacademy.app.repository.DashboardRepository
import com.lzacademy.app.repository.SupabaseDashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class StudentDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DashboardRepository =
        SupabaseDashboardRepository(LZDatabase.get(application), AppPreferences(application))

    private val _state = MutableStateFlow<UiState<StudentDashboardData>>(UiState.Loading)
    val state: StateFlow<UiState<StudentDashboardData>> = _state.asStateFlow()

    private val _selectedDay = MutableStateFlow(0)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val data = repository.studentDashboard()
                _selectedDay.value = data.todayIndex
                UiState.Success(data)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Impossible de charger le tableau de bord.")
            }
        }
    }

    fun selectDay(index: Int) {
        _selectedDay.value = index
    }

    /** Clears the unread dot of an announcement once the student opened it. */
    fun markAnnouncementRead(id: String) {
        _state.update { current ->
            if (current is UiState.Success) {
                UiState.Success(
                    current.data.copy(
                        announcements = current.data.announcements.map {
                            if (it.id == id) it.copy(unread = false) else it
                        },
                    ),
                )
            } else current
        }
    }
}

class TeacherDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DashboardRepository =
        SupabaseDashboardRepository(LZDatabase.get(application), AppPreferences(application))

    private val _state = MutableStateFlow<UiState<TeacherDashboardData>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherDashboardData>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.teacherDashboard())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Impossible de charger le tableau de bord.")
            }
        }
    }
}

class AdminDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DashboardRepository =
        SupabaseDashboardRepository(LZDatabase.get(application), AppPreferences(application))

    private val _state = MutableStateFlow<UiState<AdminDashboardData>>(UiState.Loading)
    val state: StateFlow<UiState<AdminDashboardData>> = _state.asStateFlow()

    /** Index in [AdminDashboardData.periods] (3, 6 or 12 months). Defaults to 12 months. */
    private val _periodIndex = MutableStateFlow(2)
    val periodIndex: StateFlow<Int> = _periodIndex.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.adminDashboard())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Impossible de charger le tableau de bord.")
            }
        }
    }

    fun selectPeriod(index: Int) {
        _periodIndex.value = index
    }
}
