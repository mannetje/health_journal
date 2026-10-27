package nl.healthjournal.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Runs the real migrations against the exported schemas in `data/schemas` (one JSON file per version).
 * [MigrationTestHelper] creates the database exactly as that version of the app did and, after migrating,
 * checks the result against the schema of the target version, so a migration that drifts from the entities fails here.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationChainTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HealthJournalDatabase::class.java
    )

    @Test
    fun every_version_migrates_to_the_current_schema() {
        for (start in FIRST_VERSION until CURRENT_VERSION) {
            helper.createDatabase(DB_NAME, start).close()
            helper.runMigrationsAndValidate(DB_NAME, CURRENT_VERSION, true, *ALL_MIGRATIONS).close()
            java.io.File(DB_NAME).delete()
        }
    }

    @Test
    fun a_version_1_database_keeps_its_rows_through_every_migration() {
        helper.createDatabase(DB_NAME, FIRST_VERSION).use { db ->
            db.execSQL("INSERT INTO profiles VALUES ('p1', 'Test', '1980-01-01', 180, 1)")
            db.execSQL("INSERT INTO weights VALUES ('w1', 'p1', 1000, 80.5, 24.8)")
            db.execSQL("INSERT INTO blood_pressures VALUES ('b1', 'p1', 1000, 120, 80, 'NORMAL')")
            db.execSQL("INSERT INTO glucoses VALUES ('g1', 'p1', 1000, 5.5, 'FASTING', 'NORMAL')")
            db.execSQL("INSERT INTO activities VALUES ('a1', 'p1', 1000, 4000, 3200.0)")
        }

        helper.runMigrationsAndValidate(DB_NAME, CURRENT_VERSION, true, *ALL_MIGRATIONS).use { db ->
            assertEquals(1, db.count("profiles"))
            assertEquals(1, db.count("weights"))
            assertEquals(1, db.count("blood_pressures"))
            assertEquals(1, db.count("glucoses"))
            assertEquals(1, db.count("activities"))
            // Columns added later start empty on the rows that already existed.
            assertEquals(0, db.count("profiles WHERE sex IS NOT NULL"))
            assertEquals(0, db.count("blood_pressures WHERE pulse IS NOT NULL"))
            assertEquals(0, db.count("weights WHERE comment IS NOT NULL"))
            // Tables added later start empty.
            listOf("waist_circumferences", "medications", "intakes", "migraines", "migraine_tags")
                .forEach { assertEquals(it, 0, db.count(it)) }
        }
    }

    private fun SupportSQLiteDatabase.count(tableAndFilter: String): Int =
        query("SELECT COUNT(*) FROM $tableAndFilter").use {
            it.moveToFirst()
            it.getInt(0)
        }

    private companion object {
        val DB_NAME: String = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath("migration-chain-test").absolutePath
        const val FIRST_VERSION = 1
        const val CURRENT_VERSION = 7
        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
        )
    }
}
