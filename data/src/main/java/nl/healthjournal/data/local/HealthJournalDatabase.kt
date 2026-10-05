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

@Database(
    entities = [
        ProfileEntity::class,
        WeightEntity::class,
        BloodPressureEntity::class,
        GlucoseEntity::class,
        ActivityEntity::class,
        WaistCircumferenceEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class HealthJournalDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun glucoseDao(): GlucoseDao
    abstract fun activityDao(): ActivityDao
    abstract fun waistCircumferenceDao(): WaistCircumferenceDao

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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
        }
    }
}
