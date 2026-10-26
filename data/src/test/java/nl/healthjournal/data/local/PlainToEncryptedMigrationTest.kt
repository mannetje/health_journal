package nl.healthjournal.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlainToEncryptedMigrationTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val dbFile: File get() = context.getDatabasePath(HealthJournalDatabase.DATABASE_NAME)

    private class FakeDatabaseKeyProvider(context: Context) : DatabaseKeyProvider(context) {
        override fun getOrCreatePassphrase(): ByteArray = "test-passphrase-32-bytes-long-12".toByteArray()
    }

    private lateinit var keyProvider: FakeDatabaseKeyProvider

    @Before
    fun setUp() {
        context.deleteDatabase(HealthJournalDatabase.DATABASE_NAME)
        keyProvider = FakeDatabaseKeyProvider(context)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(HealthJournalDatabase.DATABASE_NAME)
        val tempEnc = File(dbFile.parentFile, PlainToEncryptedMigration.TEMP_ENC_NAME)
        if (tempEnc.exists()) tempEnc.delete()
    }

    private fun createPlainDb(populate: (SQLiteDatabase) -> Unit) {
        dbFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL("CREATE TABLE profiles (id TEXT PRIMARY KEY, name TEXT)")
        db.execSQL("CREATE TABLE weights (id TEXT PRIMARY KEY, weight REAL)")
        db.version = 6
        populate(db)
        db.close()
    }

    // A test fake export step that copies the plain DB into encPath and sets the version
    private val fakeExportStep: (String, String, ByteArray, Int) -> Unit = { plainPath, encPath, _, version ->
        File(plainPath).copyTo(File(encPath), overwrite = true)
        val enc = SQLiteDatabase.openOrCreateDatabase(encPath, null)
        enc.version = version
        enc.close()
    }

    // A test fake verify step using plain SQLite
    private val fakeVerifyStep: (String, ByteArray, Map<String, Long>, Int) -> Unit = { encPath, _, expectedCounts, expectedVersion ->
        val enc = SQLiteDatabase.openDatabase(encPath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            assertEquals(expectedVersion, enc.version)
            for ((table, expectedCount) in expectedCounts) {
                enc.rawQuery("SELECT COUNT(*) FROM `$table`", null).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals(expectedCount, c.getLong(0))
                }
            }
        } finally {
            enc.close()
        }
    }

    @Test
    fun fresh_install_returns_AlreadyEncrypted() {
        assertFalse(dbFile.exists())
        val migration = PlainToEncryptedMigration(context, keyProvider)
        val result = migration.migrate()
        assertEquals(PlainToEncryptedMigration.MigrationResult.AlreadyEncrypted, result)
    }

    @Test
    fun already_encrypted_returns_AlreadyEncrypted() {
        dbFile.parentFile?.mkdirs()
        // Write random encrypted bytes (not SQLite format 3)
        FileOutputStream(dbFile).use { it.write(ByteArray(64) { 0x42 }) }

        val migration = PlainToEncryptedMigration(context, keyProvider)
        val result = migration.migrate()
        assertEquals(PlainToEncryptedMigration.MigrationResult.AlreadyEncrypted, result)
    }

    @Test
    fun empty_database_migrates_successfully() {
        createPlainDb { /* no rows */ }
        assertTrue(PlainToEncryptedMigration.isPlainSqlite(dbFile))

        val migration = PlainToEncryptedMigration(
            context = context,
            keyProvider = keyProvider,
            exportStep = fakeExportStep,
            verifyStep = fakeVerifyStep
        )
        val result = migration.migrate()
        assertEquals(PlainToEncryptedMigration.MigrationResult.Success, result)
        assertTrue(dbFile.exists())
    }

    @Test
    fun seeded_database_migrates_and_row_counts_match() {
        createPlainDb { db ->
            db.execSQL("INSERT INTO profiles VALUES ('p1', 'Maria')")
            db.execSQL("INSERT INTO weights VALUES ('w1', 72.5)")
            db.execSQL("INSERT INTO weights VALUES ('w2', 73.0)")
        }

        val migration = PlainToEncryptedMigration(
            context = context,
            keyProvider = keyProvider,
            exportStep = fakeExportStep,
            verifyStep = fakeVerifyStep
        )
        val result = migration.migrate()
        assertEquals(PlainToEncryptedMigration.MigrationResult.Success, result)
        assertTrue(dbFile.exists())

        // Verify the database contains the exact data
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        db.use {
            it.rawQuery("SELECT COUNT(*) FROM profiles", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1L, c.getLong(0))
            }
            it.rawQuery("SELECT COUNT(*) FROM weights", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2L, c.getLong(0))
            }
        }
    }

    @Test
    fun large_database_migrates_and_row_counts_match() {
        createPlainDb { db ->
            for (i in 1..250) {
                db.execSQL("INSERT INTO profiles VALUES ('p$i', 'Person $i')")
                db.execSQL("INSERT INTO weights VALUES ('w$i', ${70.0 + i * 0.1})")
            }
        }

        val migration = PlainToEncryptedMigration(
            context = context,
            keyProvider = keyProvider,
            exportStep = fakeExportStep,
            verifyStep = fakeVerifyStep
        )
        val result = migration.migrate()
        assertEquals(PlainToEncryptedMigration.MigrationResult.Success, result)

        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        db.use {
            it.rawQuery("SELECT COUNT(*) FROM profiles", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(250L, c.getLong(0))
            }
            it.rawQuery("SELECT COUNT(*) FROM weights", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(250L, c.getLong(0))
            }
        }
    }

    @Test
    fun interrupted_migration_leaves_plain_file_intact() {
        createPlainDb { db ->
            db.execSQL("INSERT INTO profiles VALUES ('p1', 'Maria')")
            db.execSQL("INSERT INTO weights VALUES ('w1', 72.5)")
        }

        val failingExportStep: (String, String, ByteArray, Int) -> Unit = { _, encPath, _, _ ->
            // Simulate crash mid-export
            File(encPath).writeText("partial garbage")
            throw RuntimeException("Simulated power loss or export failure")
        }

        val migration = PlainToEncryptedMigration(
            context = context,
            keyProvider = keyProvider,
            exportStep = failingExportStep,
            verifyStep = fakeVerifyStep
        )
        val result = migration.migrate()
        assertTrue(result is PlainToEncryptedMigration.MigrationResult.Failed)

        // Plain database must be untouched and valid
        assertTrue(dbFile.exists())
        assertTrue(PlainToEncryptedMigration.isPlainSqlite(dbFile))
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        db.use {
            it.rawQuery("SELECT COUNT(*) FROM profiles", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1L, c.getLong(0))
            }
        }

        // Temp file must be cleaned up
        val tempEnc = File(dbFile.parentFile, PlainToEncryptedMigration.TEMP_ENC_NAME)
        assertFalse(tempEnc.exists())
    }

    @Test
    fun verification_mismatch_aborts_and_leaves_plain_file_intact() {
        createPlainDb { db ->
            db.execSQL("INSERT INTO profiles VALUES ('p1', 'Maria')")
        }

        val mismatchVerifyStep: (String, ByteArray, Map<String, Long>, Int) -> Unit = { _, _, _, _ ->
            throw IllegalStateException("Row count mismatch simulated!")
        }

        val migration = PlainToEncryptedMigration(
            context = context,
            keyProvider = keyProvider,
            exportStep = fakeExportStep,
            verifyStep = mismatchVerifyStep
        )
        val result = migration.migrate()
        assertTrue(result is PlainToEncryptedMigration.MigrationResult.Failed)

        // Plain database must still exist and be intact
        assertTrue(dbFile.exists())
        assertTrue(PlainToEncryptedMigration.isPlainSqlite(dbFile))

        // Temp file must be cleaned up
        val tempEnc = File(dbFile.parentFile, PlainToEncryptedMigration.TEMP_ENC_NAME)
        assertFalse(tempEnc.exists())
    }

    private fun backupOf(file: File) = File(file.parentFile, file.name + PlainToEncryptedMigration.BACKUP_SUFFIX)

    private fun migrationWithFakeSteps() = PlainToEncryptedMigration(
        context = context,
        keyProvider = keyProvider,
        exportStep = fakeExportStep,
        verifyStep = fakeVerifyStep
    )

    @Test
    fun successful_migration_leaves_no_set_aside_files() {
        createPlainDb { db -> db.execSQL("INSERT INTO profiles VALUES ('p1', 'Maria')") }

        val result = migrationWithFakeSteps().migrate()

        assertTrue(result is PlainToEncryptedMigration.MigrationResult.Success)
        assertFalse(backupOf(dbFile).exists())
    }

    @Test
    fun crash_after_setting_the_plain_files_aside_restores_them_and_migrates_again() {
        createPlainDb { db -> db.execSQL("INSERT INTO profiles VALUES ('p1', 'Maria')") }
        // The state a crash leaves behind: the plain database set aside, nothing moved in yet.
        check(dbFile.renameTo(backupOf(dbFile)))
        val walBackup = backupOf(File(dbFile.parentFile, HealthJournalDatabase.DATABASE_NAME + "-wal"))
        walBackup.writeText("")

        val result = migrationWithFakeSteps().migrate()

        assertTrue(result is PlainToEncryptedMigration.MigrationResult.Success)
        assertFalse(backupOf(dbFile).exists())
        assertFalse(walBackup.exists())
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use {
            it.rawQuery("SELECT COUNT(*) FROM profiles", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1L, c.getLong(0))
            }
        }
    }

    @Test
    fun crash_after_the_encrypted_file_is_in_place_drops_the_set_aside_files() {
        createPlainDb { db -> db.execSQL("INSERT INTO profiles VALUES ('p1', 'Maria')") }
        check(dbFile.renameTo(backupOf(dbFile)))
        dbFile.writeBytes(ByteArray(64) { 0x7F })

        val result = migrationWithFakeSteps().migrate()

        assertTrue(result is PlainToEncryptedMigration.MigrationResult.AlreadyEncrypted)
        assertFalse(backupOf(dbFile).exists())
        assertEquals(64L, dbFile.length())
    }

    @Test
    fun stale_unreadable_key_on_a_plain_database_is_discarded_and_replaced() {
        createPlainDb { db -> db.execSQL("INSERT INTO profiles VALUES ('p1', 'Maria')") }

        val provider = object : DatabaseKeyProvider(context) {
            var discarded = false
            override fun getOrCreatePassphrase(): ByteArray {
                if (!discarded) throw DatabaseKeyLostException("stale key")
                return "test-passphrase-32-bytes-long-12".toByteArray()
            }

            override fun discardKey() {
                discarded = true
            }
        }

        val result = PlainToEncryptedMigration(
            context = context,
            keyProvider = provider,
            exportStep = fakeExportStep,
            verifyStep = fakeVerifyStep
        ).migrate()

        assertEquals(PlainToEncryptedMigration.MigrationResult.Success, result)
        assertTrue(provider.discarded)
    }
}
