package nl.healthjournal.data.local

import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Opens a hand-built version 5 database through Room with the real migrations.
 * The schema is not exported, so the version 5 tables are written out here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private var db: HealthJournalDatabase? = null

    @Before
    fun createVersion5Database() {
        context.deleteDatabase(HealthJournalDatabase.DATABASE_NAME)
        val file = context.getDatabasePath(HealthJournalDatabase.DATABASE_NAME)
        file.parentFile?.mkdirs()
        val old = SQLiteDatabase.openOrCreateDatabase(file, null)
        VERSION_5_SCHEMA.forEach(old::execSQL)
        old.execSQL("INSERT INTO profiles VALUES ('p1', 'Test', '1980-01-01', 180, NULL, 1)")
        old.execSQL("INSERT INTO weights VALUES ('w1', 'p1', 1000, 80.5, 24.8, 'note')")
        old.execSQL("INSERT INTO weights VALUES ('w2', 'p1', 2000, 79.0, NULL, NULL)")
        old.execSQL("INSERT INTO blood_pressures VALUES ('b1', 'p1', 1000, 120, 80, 'NORMAL', 60, NULL)")
        old.execSQL("INSERT INTO glucoses VALUES ('g1', 'p1', 1000, 5.5, 'FASTING', 'NORMAL', NULL)")
        old.execSQL("INSERT INTO activities VALUES ('a1', 'p1', 1000, 4000, 3200.0, NULL)")
        old.execSQL("INSERT INTO waist_circumferences VALUES ('c1', 'p1', 1000, 90.0, NULL, NULL)")
        old.version = 5
        old.close()
    }

    @After
    fun tearDown() {
        db?.close()
        context.deleteDatabase(HealthJournalDatabase.DATABASE_NAME)
    }

    private fun count(table: String): Int =
        db!!.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use {
            it.moveToFirst()
            it.getInt(0)
        }

    @Test
    fun migrating_5_to_6_keeps_every_existing_row_and_adds_the_medication_tables() {
        db = HealthJournalDatabase.create(context)

        // Opening runs the migrations and then Room checks the schema against its entities.
        assertEquals(1, count("profiles"))
        assertEquals(2, count("weights"))
        assertEquals(1, count("blood_pressures"))
        assertEquals(1, count("glucoses"))
        assertEquals(1, count("activities"))
        assertEquals(1, count("waist_circumferences"))
        listOf("medications", "medication_schedules", "medication_times", "intakes")
            .forEach { assertEquals(it, 0, count(it)) }
        assertEquals(6, db!!.openHelper.readableDatabase.version)
    }

    @Test
    fun a_fresh_install_opens_at_the_current_version() {
        context.deleteDatabase(HealthJournalDatabase.DATABASE_NAME)
        db = HealthJournalDatabase.create(context)
        assertTrue(count("medications") == 0)
        assertEquals(6, db!!.openHelper.readableDatabase.version)
    }

    private companion object {
        val VERSION_5_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `profiles` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `dateOfBirth` TEXT NOT NULL, `heightCm` INTEGER, `sex` TEXT, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `weights` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `bmi` REAL, `comment` TEXT, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_weights_profileId` ON `weights` (`profileId`)",
            "CREATE INDEX IF NOT EXISTS `index_weights_timestamp` ON `weights` (`timestamp`)",
            "CREATE TABLE IF NOT EXISTS `blood_pressures` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `systolic` INTEGER NOT NULL, `diastolic` INTEGER NOT NULL, `category` TEXT NOT NULL, `pulse` INTEGER, `comment` TEXT, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_blood_pressures_profileId` ON `blood_pressures` (`profileId`)",
            "CREATE INDEX IF NOT EXISTS `index_blood_pressures_timestamp` ON `blood_pressures` (`timestamp`)",
            "CREATE TABLE IF NOT EXISTS `glucoses` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `glucoseMmolL` REAL NOT NULL, `context` TEXT NOT NULL, `category` TEXT NOT NULL, `comment` TEXT, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_glucoses_profileId` ON `glucoses` (`profileId`)",
            "CREATE INDEX IF NOT EXISTS `index_glucoses_timestamp` ON `glucoses` (`timestamp`)",
            "CREATE TABLE IF NOT EXISTS `activities` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `distanceMeters` REAL NOT NULL, `comment` TEXT, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_activities_profileId` ON `activities` (`profileId`)",
            "CREATE INDEX IF NOT EXISTS `index_activities_startTime` ON `activities` (`startTime`)",
            "CREATE TABLE IF NOT EXISTS `waist_circumferences` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `waistCm` REAL NOT NULL, `category` TEXT, `comment` TEXT, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_waist_circumferences_profileId` ON `waist_circumferences` (`profileId`)",
            "CREATE INDEX IF NOT EXISTS `index_waist_circumferences_timestamp` ON `waist_circumferences` (`timestamp`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
        )
    }
}
