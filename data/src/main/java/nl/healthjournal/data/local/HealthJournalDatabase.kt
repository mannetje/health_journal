package nl.healthjournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import nl.healthjournal.data.local.dao.*
import nl.healthjournal.data.local.entity.*

/** Adds the optional, sex-independent `sex` demographic column to `profiles`. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN sex TEXT")
    }
}

/** Adds the `waist_circumferences` table for tracking waist circumference measurements. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `waist_circumferences` (
                `id` TEXT NOT NULL,
                `profileId` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `waistCm` REAL NOT NULL,
                `category` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_waist_circumferences_profileId` ON `waist_circumferences` (`profileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_waist_circumferences_timestamp` ON `waist_circumferences` (`timestamp`)")
    }
}

/** Adds the optional `pulse` column to `blood_pressures`. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE blood_pressures ADD COLUMN pulse INTEGER")
    }
}

/** Adds the optional `comment` column to the five entry tables. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (table in listOf("weights", "blood_pressures", "glucoses", "waist_circumferences", "activities")) {
            db.execSQL("ALTER TABLE $table ADD COLUMN comment TEXT")
        }
    }
}

/**
 * Adds the medication tables (medications, schedule versions, daily times, intakes). Tables only: no existing
 * row is touched. The SQL mirrors what Room expects for the entities in `MedicationEntities.kt`.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `medications` (
                `id` TEXT NOT NULL,
                `profileId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `form` TEXT,
                `strengthAmount` TEXT,
                `strengthUnit` TEXT,
                `doseAmount` TEXT NOT NULL,
                `doseUnit` TEXT NOT NULL,
                `color` TEXT,
                `shape` TEXT,
                `comment` TEXT,
                `archivedFrom` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medications_profileId` ON `medications` (`profileId`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `medication_schedules` (
                `medicationId` TEXT NOT NULL,
                `effectiveFrom` TEXT NOT NULL,
                `asNeeded` INTEGER NOT NULL,
                `daysOfWeek` TEXT,
                `intervalDays` INTEGER,
                `startDate` TEXT,
                `endDate` TEXT,
                PRIMARY KEY(`medicationId`, `effectiveFrom`),
                FOREIGN KEY(`medicationId`) REFERENCES `medications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `medication_times` (
                `medicationId` TEXT NOT NULL,
                `effectiveFrom` TEXT NOT NULL,
                `localTime` TEXT NOT NULL,
                PRIMARY KEY(`medicationId`, `effectiveFrom`, `localTime`),
                FOREIGN KEY(`medicationId`, `effectiveFrom`) REFERENCES `medication_schedules`(`medicationId`, `effectiveFrom`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `intakes` (
                `id` TEXT NOT NULL,
                `medicationId` TEXT NOT NULL,
                `planned` TEXT,
                `status` TEXT NOT NULL,
                `takenAt` INTEGER,
                `actualAmount` TEXT,
                `comment` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`medicationId`) REFERENCES `medications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_intakes_medicationId_planned` ON `intakes` (`medicationId`, `planned`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_intakes_medicationId_takenAt` ON `intakes` (`medicationId`, `takenAt`)")
    }
}

@Database(
    entities = [
        ProfileEntity::class,
        WeightEntity::class,
        BloodPressureEntity::class,
        GlucoseEntity::class,
        ActivityEntity::class,
        WaistCircumferenceEntity::class,
        MedicationEntity::class,
        MedicationScheduleEntity::class,
        MedicationTimeEntity::class,
        IntakeEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class HealthJournalDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun glucoseDao(): GlucoseDao
    abstract fun activityDao(): ActivityDao
    abstract fun waistCircumferenceDao(): WaistCircumferenceDao
    abstract fun medicationDao(): MedicationDao

    companion object {
        const val DATABASE_NAME = "health_journal.db"

        fun createInMemory(context: Context): HealthJournalDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                HealthJournalDatabase::class.java
            ).allowMainThreadQueries().build()
        }

        fun create(context: Context): HealthJournalDatabase {
            return Room.databaseBuilder(
                context,
                HealthJournalDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
        }
    }
}
