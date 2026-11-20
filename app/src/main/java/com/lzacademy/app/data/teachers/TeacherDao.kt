package com.lzacademy.app.data.teachers

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {

    @Query("SELECT * FROM teachers WHERE isActive = 1 ORDER BY fullName")
    fun observeAll(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<TeacherEntity?>

    @Query("SELECT * FROM teachers WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): TeacherEntity?

    /** Mirror upsert from the Supabase sync: server values are the source of truth. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMirror(teachers: List<TeacherEntity>)

    @Query("SELECT * FROM teachers WHERE profileId = :profileId LIMIT 1")
    fun observeByProfile(profileId: String): Flow<TeacherEntity?>

    @Query("UPDATE teachers SET isFeaturedOnStudentHome = :value WHERE id = :id")
    suspend fun updateShowcaseVisibility(id: String, value: Boolean)

    @Query("UPDATE teachers SET acceptsStudentMessages = :value WHERE id = :id")
    suspend fun updateAcceptsMessages(id: String, value: Boolean)

    @Query("SELECT * FROM featured_days WHERE epochDay = :epochDay LIMIT 1")
    suspend fun dayPage(epochDay: Long): FeaturedDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayPage(page: FeaturedDayEntity)

    @Query("UPDATE teachers SET lastFeaturedAtEpochDay = :epochDay WHERE id IN (:ids)")
    suspend fun markFeatured(ids: List<String>, epochDay: Long)

    @Insert
    suspend fun insertMessage(message: TeacherMessageEntity): Long

    @Query("SELECT * FROM teacher_messages WHERE teacherId = :teacherId ORDER BY createdAtMillis DESC")
    fun observeMessages(teacherId: String): Flow<List<TeacherMessageEntity>>

    @Query("SELECT COUNT(*) FROM teacher_messages WHERE teacherId = :teacherId")
    fun observeMessageCount(teacherId: String): Flow<Int>

    /** Number of messages sent to one teacher since a timestamp (daily quota). */
    @Query("SELECT COUNT(*) FROM teacher_messages WHERE teacherId = :teacherId AND createdAtMillis >= :sinceMillis")
    suspend fun countMessagesSince(teacherId: String, sinceMillis: Long): Int

    /** Weekly purge: removes messages older than the retention cutoff. */
    @Query("DELETE FROM teacher_messages WHERE createdAtMillis < :cutoffMillis")
    suspend fun deleteMessagesBefore(cutoffMillis: Long)
}
