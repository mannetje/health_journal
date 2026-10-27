package nl.healthjournal.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented tests for the SQLCipher part of the storage: the native library only exists on a device or
 * emulator, so none of this can run in the Robolectric tests. The unit tests cover the file handling of
 * [PlainToEncryptedMigration] with a fake export; these use the real export and verification.
 *
 * The test APK has its own package, so its database and key files never touch the installed app.
 */
@RunWith(AndroidJUnit4::class)
class SqlCipherDatabaseTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbFile: File get() = context.getDatabasePath(HealthJournalDatabase.DATABASE_NAME)
    private lateinit var keyProvider: DatabaseKeyProvider

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        removeDatabaseAndKey()
        keyProvider = DatabaseKeyProvider(context)
    }

    @After
    fun tearDown() = removeDatabaseAndKey()

    private fun removeDatabaseAndKey() {
        context.deleteDatabase(HealthJournalDatabase.DATABASE_NAME)
        context.noBackupFilesDir.listFiles()?.forEach { it.delete() }
    }

    private fun openPlain(): HealthJournalDatabase =
        Room.databaseBuilder(context, HealthJournalDatabase::class.java, HealthJournalDatabase.DATABASE_NAME)
            .build()

    /** The factory hands the array to SQLCipher, which clears it after opening, so every open gets its own copy. */
    private fun openEncrypted(passphrase: ByteArray): HealthJournalDatabase =
        Room.databaseBuilder(context, HealthJournalDatabase::class.java, HealthJournalDatabase.DATABASE_NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf()))
            .build()

    private fun HealthJournalDatabase.insertProfile() =
        openHelper.writableDatabase.execSQL("INSERT INTO profiles VALUES ('p1', 'Test', '1980-01-01', 180, NULL, 1)")

    private fun HealthJournalDatabase.profileCount(): Int =
        openHelper.readableDatabase.query("SELECT COUNT(*) FROM profiles").use {
            it.moveToFirst()
            it.getInt(0)
        }

    @Test
    fun an_encrypted_database_reads_back_with_the_same_key_and_is_not_plain_on_disk() {
        val passphrase = keyProvider.getOrCreatePassphrase()
        openEncrypted(passphrase).apply {
            insertProfile()
            close()
        }

        assertFalse("the file must not start with the SQLite header", PlainToEncryptedMigration.isPlainSqlite(dbFile))
        openEncrypted(passphrase).apply {
            assertEquals(1, profileCount())
            close()
        }
    }

    @Test
    fun an_encrypted_database_cannot_be_opened_with_another_key() {
        val passphrase = keyProvider.getOrCreatePassphrase()
        openEncrypted(passphrase).apply {
            insertProfile()
            close()
        }

        val wrongKey = ByteArray(passphrase.size) { (passphrase[it] + 1).toByte() }
        val wrong = openEncrypted(wrongKey)
        try {
            wrong.profileCount()
            fail("a wrong key must not open the database")
        } catch (_: Exception) {
            // expected: the file does not decrypt with this key
        } finally {
            wrong.close()
        }
    }

    @Test
    fun the_real_migration_encrypts_a_plain_database_and_keeps_its_rows() {
        openPlain().apply {
            insertProfile()
            close()
        }
        assertTrue(PlainToEncryptedMigration.isPlainSqlite(dbFile))

        val result = PlainToEncryptedMigration(context, keyProvider).migrate()

        assertEquals(PlainToEncryptedMigration.MigrationResult.Success, result)
        assertFalse("the file must be encrypted now", PlainToEncryptedMigration.isPlainSqlite(dbFile))
        val leftovers = dbFile.parentFile?.listFiles().orEmpty().map { it.name }
            .filter { it.endsWith(PlainToEncryptedMigration.BACKUP_SUFFIX) || it == PlainToEncryptedMigration.TEMP_ENC_NAME }
        assertTrue("no set-aside or temporary files may remain: $leftovers", leftovers.isEmpty())
        openEncrypted(keyProvider.getOrCreatePassphrase()).apply {
            assertEquals(1, profileCount())
            close()
        }
    }

    @Test
    fun a_second_migration_run_finds_the_database_already_encrypted() {
        openPlain().close()
        PlainToEncryptedMigration(context, keyProvider).migrate()

        assertEquals(
            PlainToEncryptedMigration.MigrationResult.AlreadyEncrypted,
            PlainToEncryptedMigration(context, keyProvider).migrate()
        )
    }
}
