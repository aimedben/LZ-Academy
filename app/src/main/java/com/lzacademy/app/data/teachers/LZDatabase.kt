package com.lzacademy.app.data.teachers

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lzacademy.app.data.auth.AuthDao
import com.lzacademy.app.data.auth.UserEntity
import com.lzacademy.app.data.attendance.AttendanceDao
import com.lzacademy.app.data.attendance.AttendanceEntity
import com.lzacademy.app.data.attendance.StudentEntity
import com.lzacademy.app.data.attendance.SubscriptionEntity
import com.lzacademy.app.data.supabase.SyncDao
import com.lzacademy.app.data.supabase.SyncedAnnouncementEntity
import com.lzacademy.app.data.supabase.SyncedAttendanceEntity
import com.lzacademy.app.data.supabase.SyncedEnrollmentEntity
import com.lzacademy.app.data.supabase.SyncedGroupEntity
import com.lzacademy.app.data.supabase.SyncedMaterialEntity
import com.lzacademy.app.data.supabase.SyncedPaymentEntity
import com.lzacademy.app.data.supabase.SyncedSessionEntity
import com.lzacademy.app.data.supabase.SyncedSlotEntity
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import com.lzacademy.app.data.supabase.SyncedSubjectEntity
import com.lzacademy.app.data.supabase.SyncedSubscriptionEntity
import com.lzacademy.app.data.supabase.SyncedTeacherEntity

/**
 * Single local database of the app. A real backend would replace the repository, not this class:
 * every screen observes Room flows, so a setting changed on one screen is immediately visible
 * everywhere else in the app.
 */
@Database(
    entities = [
        TeacherEntity::class,
        FeaturedDayEntity::class,
        TeacherMessageEntity::class,
        UserEntity::class,
        StudentEntity::class,
        SubscriptionEntity::class,
        AttendanceEntity::class,
        SyncedStudentEntity::class,
        SyncedSubscriptionEntity::class,
        SyncedTeacherEntity::class,
        SyncedSubjectEntity::class,
        SyncedGroupEntity::class,
        SyncedPaymentEntity::class,
        SyncedAnnouncementEntity::class,
        SyncedMaterialEntity::class,
        SyncedSlotEntity::class,
        SyncedSessionEntity::class,
        SyncedAttendanceEntity::class,
        SyncedEnrollmentEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class LZDatabase : RoomDatabase() {

    abstract fun teacherDao(): TeacherDao

    abstract fun authDao(): AuthDao

    abstract fun attendanceDao(): AttendanceDao

    abstract fun syncDao(): SyncDao

    companion object {
        private const val NAME = "lz-academy.db"

        /**
         * v1 → v2 adds the local accounts, the students, their packs and the attendance table.
         * The migration (instead of a destructive fallback) keeps the teacher settings already
         * stored on the device (showcase visibility, message preference, rotation dates).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `users` (" +
                        "`id` TEXT NOT NULL, `email` TEXT NOT NULL, `fullName` TEXT NOT NULL, " +
                        "`initials` TEXT NOT NULL, `role` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, " +
                        "`passwordSalt` TEXT NOT NULL, `linkedTeacherId` TEXT, `linkedStudentId` TEXT, " +
                        "`isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `students` (" +
                        "`id` TEXT NOT NULL, `fullName` TEXT NOT NULL, `initials` TEXT NOT NULL, " +
                        "`levelLabel` TEXT NOT NULL, `groupLabel` TEXT NOT NULL, `email` TEXT NOT NULL, " +
                        "`isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `subscriptions` (" +
                        "`studentId` TEXT NOT NULL, `planLabel` TEXT NOT NULL, " +
                        "`totalSessions` INTEGER NOT NULL, `remainingSessions` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, `periodStartEpochDay` INTEGER NOT NULL, " +
                        "`lastRenewedEpochDay` INTEGER, `renewalCount` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`studentId`))",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `attendance` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `studentId` TEXT NOT NULL, " +
                        "`sessionLabel` TEXT NOT NULL, `groupLabel` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                        "`consumedSession` INTEGER NOT NULL, `sessionDateEpochDay` INTEGER NOT NULL, " +
                        "`recordedAtMillis` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_attendance_studentId_sessionDateEpochDay_sessionLabel` " +
                        "ON `attendance` (`studentId`, `sessionDateEpochDay`, `sessionLabel`)",
                )
            }
        }

        /**
         * v2 → v3 adds the offline mirror of the Supabase database (synced_students,
         * synced_subscriptions). The demo attendance tables stay untouched.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_students` (" +
                        "`remoteId` TEXT NOT NULL, `matricule` TEXT NOT NULL, `fullName` TEXT NOT NULL, " +
                        "`phone` TEXT, `levelName` TEXT, `avatarPath` TEXT, `isActive` INTEGER NOT NULL, " +
                        "`profileId` TEXT, PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_synced_students_matricule` " +
                        "ON `synced_students` (`matricule`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_subscriptions` (" +
                        "`remoteId` TEXT NOT NULL, `studentMatricule` TEXT NOT NULL, " +
                        "`sessionsTotal` INTEGER NOT NULL, `sessionsUsed` INTEGER NOT NULL, " +
                        "`sessionsRemaining` INTEGER NOT NULL, `status` TEXT NOT NULL, " +
                        "PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_synced_subscriptions_studentMatricule` " +
                        "ON `synced_subscriptions` (`studentMatricule`)",
                )
            }
        }

        /**
         * v3 → v4 extends the Supabase mirror: synced_students is rebuilt with `createdAt`
         * (simple mirror — a re-sync repopulates it), `teachers` gains `profileId`, and ten new
         * synced_* tables appear (teachers, subjects, groups, payments, announcements,
         * materials, schedule_slots, sessions, attendance, enrollments).
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `synced_students`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_students` (" +
                        "`remoteId` TEXT NOT NULL, `matricule` TEXT NOT NULL, `fullName` TEXT NOT NULL, " +
                        "`phone` TEXT, `levelName` TEXT, `avatarPath` TEXT, `isActive` INTEGER NOT NULL, " +
                        "`profileId` TEXT, `createdAt` TEXT NOT NULL, PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_synced_students_matricule` " +
                        "ON `synced_students` (`matricule`)",
                )
                db.execSQL("ALTER TABLE `teachers` ADD COLUMN `profileId` TEXT")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_teachers` (" +
                        "`remoteId` TEXT NOT NULL, `fullName` TEXT NOT NULL, `photoUrl` TEXT, " +
                        "`description` TEXT, `yearsOfExperience` INTEGER NOT NULL, " +
                        "`isFeatured` INTEGER NOT NULL, `acceptsMessages` INTEGER NOT NULL, " +
                        "`rotationPriority` INTEGER NOT NULL, `lastFeaturedEpochDay` INTEGER, " +
                        "`isActive` INTEGER NOT NULL, `profileId` TEXT, `subjects` TEXT NOT NULL, " +
                        "PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_subjects` (" +
                        "`remoteId` TEXT NOT NULL, `code` TEXT, `name` TEXT NOT NULL, " +
                        "PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_groups` (" +
                        "`remoteId` TEXT NOT NULL, `name` TEXT NOT NULL, `levelName` TEXT, " +
                        "`teacherId` TEXT, `capacity` INTEGER NOT NULL, PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_payments` (" +
                        "`remoteId` TEXT NOT NULL, `studentId` TEXT NOT NULL, `amountDzd` REAL NOT NULL, " +
                        "`method` TEXT NOT NULL, `status` TEXT NOT NULL, `reference` TEXT, " +
                        "`dueOn` TEXT, `paidAtIso` TEXT, `createdAtIso` TEXT NOT NULL, " +
                        "PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_announcements` (" +
                        "`remoteId` TEXT NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, " +
                        "`audience` TEXT NOT NULL, `levelId` TEXT, `isPinned` INTEGER NOT NULL, " +
                        "`publishedAtIso` TEXT, `expiresAtIso` TEXT, `createdAtIso` TEXT NOT NULL, " +
                        "PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_materials` (" +
                        "`remoteId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, " +
                        "`kind` TEXT NOT NULL, `storagePath` TEXT, `sizeBytes` INTEGER NOT NULL, " +
                        "`subjectId` TEXT, `groupName` TEXT, `isPublished` INTEGER NOT NULL, " +
                        "`publishedAtIso` TEXT, PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_schedule_slots` (" +
                        "`remoteId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `groupName` TEXT, " +
                        "`weekday` INTEGER NOT NULL, `start` TEXT NOT NULL, `end` TEXT NOT NULL, " +
                        "`roomName` TEXT, PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_sessions` (" +
                        "`remoteId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `groupName` TEXT, " +
                        "`teacherId` TEXT, `teacherName` TEXT, `subjectName` TEXT, `roomName` TEXT, " +
                        "`startsAtIso` TEXT NOT NULL, `endsAtIso` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                        "`topic` TEXT, PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_attendance` (" +
                        "`remoteId` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `studentId` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, `recordedAtIso` TEXT NOT NULL, PRIMARY KEY(`remoteId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `synced_enrollments` (" +
                        "`remoteId` TEXT NOT NULL, `studentId` TEXT NOT NULL, `groupId` TEXT NOT NULL, " +
                        "`joinedOn` TEXT, `leftOn` TEXT, PRIMARY KEY(`remoteId`))",
                )
            }
        }

        @Volatile
        private var instance: LZDatabase? = null

        fun get(context: Context): LZDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LZDatabase::class.java,
                    NAME,
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
