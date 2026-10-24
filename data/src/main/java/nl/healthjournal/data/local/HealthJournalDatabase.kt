package nl.healthjournal.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
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

/**
 * Adds the Migraine tables (`migraines`, `migraine_tags`). Tables only: no existing row is touched.
 * The SQL mirrors what Room expects for the entities in `MigraineEntities.kt`.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `migraines` (
                `id` TEXT NOT NULL,
                `profileId` TEXT NOT NULL,
                `startTime` INTEGER NOT NULL,
                `wokeUpWithIt` INTEGER NOT NULL,
                `endTime` INTEGER,
                `intensity` INTEGER,
                `areas` TEXT NOT NULL,
                `painCharacters` TEXT NOT NULL,
                `type` TEXT,
                `symptoms` TEXT NOT NULL,
                `triggers` TEXT NOT NULL,
                `customTriggers` TEXT NOT NULL,
                `tookSomething` INTEGER NOT NULL,
                `medicationText` TEXT,
                `secondDose` INTEGER NOT NULL,
                `intensityAfter2Hours` INTEGER,
                `medicationHelpedMe` INTEGER NOT NULL,
                `reliefMethods` TEXT NOT NULL,
                `customReliefMethods` TEXT NOT NULL,
                `reliefHelpedMe` INTEGER NOT NULL,
                `activities` TEXT,
                `comment` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_migraines_profileId_startTime` ON `migraines` (`profileId`, `startTime`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `migraine_tags` (
                `id` TEXT NOT NULL,
                `profileId` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `archived` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_migraine_tags_profileId_kind` ON `migraine_tags` (`profileId`, `kind`)")
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
        IntakeEntity::class,
        MigraineEntity::class,
        MigraineTagEntity::class
    ],
    version = 7,
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
    abstract fun migraineDao(): MigraineDao

    companion object {
        const val DATABASE_NAME = "health_journal.db"

        fun createInMemory(context: Context): HealthJournalDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                HealthJournalDatabase::class.java
            ).allowMainThreadQueries().build()
        }

        /**
         * Opens or creates the on-disk encrypted database.
         * The passphrase is obtained from [DatabaseKeyProvider] (Keystore-wrapped, no-backup
         * storage). The native SQLCipher library is loaded here (required before any DB access).
         *
         * The outcome is published in [DatabaseEncryptionStatus]: when the key is gone
         * ([EncryptionState.KeyLost]) an empty in-memory database is returned so the app can start
         * and show the recovery screen instead of crashing.
         */
        fun create(context: Context): HealthJournalDatabase {
            val hasSqlCipher = try {
                System.loadLibrary("sqlcipher")
                true
            } catch (_: UnsatisfiedLinkError) {
                false
            }

            val builder = Room.databaseBuilder(
                context,
                HealthJournalDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)

            if (!hasSqlCipher) {
                return notEncrypted(builder, "the SQLCipher library could not be loaded")
            }

            val keyProvider = DatabaseKeyProvider(context)
            val dbFile = context.getDatabasePath(DATABASE_NAME)

            // An encrypted database without its key files cannot be opened (and a new key would not help).
            if (dbFile.exists() && !PlainToEncryptedMigration.isPlainSqlite(dbFile) && !keyProvider.hasStoredKey()) {
                return keyLost(context, "the database is encrypted but its key files are missing")
            }

            // Attempt one-time plain to encrypted migration if needed.
            val migration = PlainToEncryptedMigration(context, keyProvider).migrate()

            // If the database file is still plain SQLite (migration failed), fall back to plain SQLite so
            // the user does not lose access to their data. The next app start retries.
            if (dbFile.exists() && PlainToEncryptedMigration.isPlainSqlite(dbFile)) {
                val reason = (migration as? PlainToEncryptedMigration.MigrationResult.Failed)?.reason
                    ?: "the migration did not complete"
                return notEncrypted(builder, reason)
            }

            val passphrase = try {
                keyProvider.getOrCreatePassphrase()
            } catch (e: DatabaseKeyLostException) {
                return keyLost(context, e.message ?: "the key cannot be unwrapped")
            }
            DatabaseEncryptionStatus.state = EncryptionState.Active
            // The factory keeps this array and Room opens the database lazily; SQLCipher clears it
            // itself after opening. Zeroing it here would open the database with an all-zero key.
            return builder.openHelperFactory(SupportOpenHelperFactory(passphrase)).build()
        }

        private fun notEncrypted(
            builder: RoomDatabase.Builder<HealthJournalDatabase>,
            reason: String
        ): HealthJournalDatabase {
            Log.w(TAG, "Database is NOT encrypted: $reason. Retrying at the next start.")
            DatabaseEncryptionStatus.state = EncryptionState.NotActive(reason)
            return builder.build()
        }

        private fun keyLost(context: Context, reason: String): HealthJournalDatabase {
            Log.e(TAG, "Database key lost: $reason. Starting with an empty in-memory database.")
            DatabaseEncryptionStatus.state = EncryptionState.KeyLost
            return createInMemory(context)
        }

        private const val TAG = "HealthJournalDb"
    }
}
