package com.lzacademy.app.data.teachers

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lzacademy.app.models.Teacher
import com.lzacademy.app.models.TeacherMessage
import com.lzacademy.app.models.TeacherMessageStatus
import java.time.LocalDate

internal const val LIST_SEPARATOR = "|"

/**
 * Persisted teacher record. List fields are stored as `|`-separated strings to keep the schema
 * simple and the database free of extra type converters.
 */
@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val photoUrl: String?,
    val subjects: String,
    val professionalDescription: String,
    val isFeaturedOnStudentHome: Boolean,
    val acceptsStudentMessages: Boolean,
    val featuredRotationPriority: Int,
    val lastFeaturedAtEpochDay: Long?,
    val levels: String,
    val yearsOfExperience: Int,
    val groups: Int,
    val isActive: Boolean,
    /** Supabase profile uuid; null for a row seeded locally, set by the sync for real rows. */
    val profileId: String? = null,
)

internal fun TeacherEntity.toDomain(): Teacher = Teacher(
    id = id,
    fullName = fullName,
    photoUrl = photoUrl,
    subjects = subjects.split(LIST_SEPARATOR).filter { it.isNotBlank() },
    professionalDescription = professionalDescription,
    isFeaturedOnStudentHome = isFeaturedOnStudentHome,
    acceptsStudentMessages = acceptsStudentMessages,
    featuredRotationPriority = featuredRotationPriority,
    lastFeaturedAt = lastFeaturedAtEpochDay?.let(LocalDate::ofEpochDay),
    levels = levels.split(LIST_SEPARATOR).filter { it.isNotBlank() },
    yearsOfExperience = yearsOfExperience,
    groups = groups,
    isActive = isActive,
)

/**
 * The teachers selected for one rotation day. Persisting the page is what makes the carousel
 * stable across recompositions, screen refreshes and app restarts within the same day.
 */
@Entity(tableName = "featured_days")
data class FeaturedDayEntity(
    @PrimaryKey val epochDay: Long,
    val teacherIds: String,
)

@Entity(tableName = "teacher_messages")
data class TeacherMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teacherId: String,
    val body: String,
    val createdAtMillis: Long,
)

internal fun TeacherMessageEntity.toDomain(): TeacherMessage = TeacherMessage(
    id = id,
    teacherId = teacherId,
    body = body,
    createdAtMillis = createdAtMillis,
    status = TeacherMessageStatus.Pending,
)
