package com.lzacademy.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.models.Teacher
import com.lzacademy.app.models.TeacherMessage
import com.lzacademy.app.repository.SendMessageResult
import com.lzacademy.app.repository.TeacherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shared teacher state. Every screen creates its own instance but they all observe the same
 * Room database, so a setting changed on the teacher side is instantly reflected on the student
 * side (and vice versa).
 */
class TeachersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TeacherRepository(LZDatabase.get(application))

    /** All active teachers, including those who opted out of the student-home showcase. */
    val allTeachers: StateFlow<List<Teacher>> = repository.observeTeachers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The stable selection of the day shown on the student home. */
    val featuredTeachers: StateFlow<List<Teacher>> = repository.observeFeatured()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun teacher(teacherId: String): Flow<Teacher?> = repository.observeTeacher(teacherId)

    /** The real teacher linked to the signed-in Supabase profile (teacher side). */
    fun teacherByProfile(profileId: String?): Flow<Teacher?> =
        repository.observeTeacherByProfile(profileId)

    fun messages(teacherId: String): Flow<List<TeacherMessage>> = repository.observeMessages(teacherId)

    fun messageCount(teacherId: String): Flow<Int> = repository.observeMessageCount(teacherId)

    fun setShowcaseVisibility(teacherId: String, value: Boolean) {
        viewModelScope.launch { repository.setShowcaseVisibility(teacherId, value) }
    }

    fun setAcceptsMessages(teacherId: String, value: Boolean) {
        viewModelScope.launch { repository.setAcceptsMessages(teacherId, value) }
    }

    fun sendMessage(teacherId: String, body: String, onResult: (SendMessageResult) -> Unit) {
        viewModelScope.launch { onResult(repository.sendMessage(teacherId, body)) }
    }
}
