package com.lzacademy.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lzacademy.app.data.FrenchDates
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.data.sms.SmsRecipient
import com.lzacademy.app.data.sms.SmsRecipients
import com.lzacademy.app.models.ChartSeriesUi
import com.lzacademy.app.repository.DashboardComputer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Real numbers for the admin Reports and Communication screens, computed from the
 * Room mirror ONLY. Empty mirror → honest empty data, nothing is invented.
 */
class AdminReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LZDatabase.get(application)
    private val preferences = AppPreferences(application)
    private val dao = database.syncDao()

    data class ReportsData(
        val totalStudents: Int,
        val monthRevenue: Double,
        val prevMonthRevenue: Double,
        /** Paid revenue of the last 6 months, oldest first. */
        val revenueTrend: ChartSeriesUi,
        /** Cumulative headcount of students over the last 6 months, oldest first. */
        val growth: ChartSeriesUi,
        /** Real students per level, count descending. */
        val studentsByLevel: List<Pair<String, Int>>,
        /** Real attendance rate (present / recorded) or null when no call is recorded. */
        val attendanceRate: Float?,
        val attendanceCalls: Int,
        val pendingPayments: Int,
    )

    data class TargetsData(
        /** "Tous les élèves" count (active students). */
        val students: Int,
        /** Students per real level, count descending. */
        val levels: List<Pair<String, Int>>,
        val teachers: Int,
    )

    private val _reports = MutableStateFlow<UiState<ReportsData>>(UiState.Loading)
    val reports: StateFlow<UiState<ReportsData>> = _reports.asStateFlow()

    private val _targets = MutableStateFlow<UiState<TargetsData>>(UiState.Loading)
    val targets: StateFlow<UiState<TargetsData>> = _targets.asStateFlow()

    /**
     * SMS recipients, straight from the Room mirror: available offline and updated
     * as soon as a sync lands. Shows "sans numéro" rows so the admin sees who cannot
     * be reached instead of a silently shorter list.
     */
    val recipients: StateFlow<List<SmsRecipient>> = dao.observeStudents()
        .map { SmsRecipients.fromStudents(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _reports.value = UiState.Loading
            _targets.value = UiState.Loading
            try {
                val dao = database.syncDao()
                val students = dao.observeStudents().first()
                val teachers = dao.observeTeachers().first()
                val payments = dao.observePayments().first()
                val subscriptions = dao.observeSubscriptions().first()
                val groups = dao.observeGroups().first()
                val sessions = dao.observeSessions().first()
                val enrollments = dao.observeEnrollments().first()
                val attendance = dao.observeAttendance().first()
                val today = LocalDate.now()

                val numbers = DashboardComputer.admin(
                    students = students,
                    subscriptions = subscriptions,
                    teachers = teachers,
                    groups = groups,
                    payments = payments,
                    sessions = sessions,
                    enrollments = enrollments,
                    today = today,
                )

                // The 12-month window is oldest→newest, so the last 6 values are the 6
                // months ending with the current one.
                val months = lastSixMonths(today)
                val window = numbers.monthlyRevenue.takeLast(6)
                val growthWindow = numbers.monthlyNewStudents.takeLast(6)

                _reports.value = UiState.Success(
                    ReportsData(
                        totalStudents = numbers.activeStudents,
                        monthRevenue = numbers.monthRevenue,
                        prevMonthRevenue = numbers.prevMonthRevenue,
                        revenueTrend = ChartSeriesUi(
                            values = window.map { it.toFloat() },
                            labels = months.map { FrenchDates.monthShort[it.monthValue - 1] },
                        ),
                        growth = ChartSeriesUi(
                            values = growthWindow.map { it.toFloat() },
                            labels = months.map { FrenchDates.monthShort[it.monthValue - 1] },
                        ),
                        studentsByLevel = numbers.studentsByLevel,
                        attendanceRate = attendanceRate(attendance),
                        attendanceCalls = attendance.size,
                        pendingPayments = numbers.pendingPayments,
                    ),
                )

                _targets.value = UiState.Success(
                    TargetsData(
                        students = numbers.activeStudents,
                        levels = numbers.studentsByLevel,
                        teachers = numbers.teachers,
                    ),
                )
            } catch (e: Exception) {
                _reports.value = UiState.Error(e.message ?: "Impossible de charger les rapports.")
                _targets.value = UiState.Error(e.message ?: "Impossible de charger les cibles.")
            }
        }
    }

    /** Real attendance rate over recorded calls only; null when nothing is recorded. */
    private fun attendanceRate(attendance: List<com.lzacademy.app.data.supabase.SyncedAttendanceEntity>): Float? {
        if (attendance.isEmpty()) return null
        val present = attendance.count { it.status.equals("present", ignoreCase = true) }
        return present.toFloat() / attendance.size
    }
}

/**
 * The 6 months drawn by the revenue and growth charts: oldest first, ending with the
 * current month. The dashboard window is 12 months oldest→newest, so these are its last 6.
 */
internal fun lastSixMonths(today: LocalDate): List<YearMonth> =
    (5 downTo 0).map { YearMonth.from(today.minusMonths(it.toLong())) }
