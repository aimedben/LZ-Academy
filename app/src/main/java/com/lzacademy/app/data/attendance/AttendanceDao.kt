package com.lzacademy.app.data.attendance

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    /** IGNORE: an existing student record is authoritative, re-seeding never resets it. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedStudents(students: List<StudentEntity>)

    /** IGNORE: re-seeding must never reset a pack that is being consumed. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedSubscriptions(subscriptions: List<SubscriptionEntity>)

    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY fullName")
    fun observeStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM subscriptions")
    fun observeSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE studentId = :studentId LIMIT 1")
    suspend fun subscription(studentId: String): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions WHERE studentId = :studentId LIMIT 1")
    fun observeSubscription(studentId: String): Flow<SubscriptionEntity?>

    @Query(
        "SELECT * FROM attendance WHERE sessionDateEpochDay = :epochDay AND sessionLabel = :sessionLabel " +
            "ORDER BY recordedAtMillis",
    )
    fun observeDay(epochDay: Long, sessionLabel: String): Flow<List<AttendanceEntity>>

    @Query(
        "SELECT * FROM attendance WHERE studentId = :studentId AND sessionDateEpochDay = :epochDay " +
            "AND sessionLabel = :sessionLabel LIMIT 1",
    )
    suspend fun byKey(studentId: String, epochDay: Long, sessionLabel: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): AttendanceEntity?

    /** Returns `-1` when the unique (student, day, label) key already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttendance(record: AttendanceEntity): Long

    @Update
    suspend fun updateAttendance(record: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendance(id: Long): Int

    @Query("UPDATE subscriptions SET remainingSessions = :remaining, status = :status WHERE studentId = :studentId")
    suspend fun updateRemaining(studentId: String, remaining: Int, status: String)

    /** Realigns the local counter on a pack whose size changed in the mirror. */
    @Query(
        "UPDATE subscriptions SET remainingSessions = :remaining, totalSessions = :total, " +
            "status = :status WHERE studentId = :studentId",
    )
    suspend fun alignPack(studentId: String, remaining: Int, total: Int, status: String)

    @Query(
        "UPDATE subscriptions SET remainingSessions = :remaining, totalSessions = :total, status = :status, " +
            "lastRenewedEpochDay = :renewedAtEpochDay, renewalCount = renewalCount + 1 WHERE studentId = :studentId",
    )
    suspend fun renew(
        studentId: String,
        remaining: Int,
        total: Int,
        status: String,
        renewedAtEpochDay: Long,
    )

    @Query(
        "SELECT * FROM attendance WHERE studentId = :studentId " +
            "ORDER BY sessionDateEpochDay DESC, recordedAtMillis DESC LIMIT :limit",
    )
    fun observeHistory(studentId: String, limit: Int = 20): Flow<List<AttendanceEntity>>
}
