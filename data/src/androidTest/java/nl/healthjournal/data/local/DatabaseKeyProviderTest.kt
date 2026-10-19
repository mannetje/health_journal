package nl.healthjournal.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [DatabaseKeyProvider].
 *
 * These run on a real device or emulator because they exercise the Android Keystore and
 * the no-backup file system — neither is available in Robolectric / JVM tests.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseKeyProviderTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var provider: DatabaseKeyProvider

    @Before
    fun setUp() {
        // Start each test with a clean no-backup dir so there is no leftover wrapped key.
        context.noBackupFilesDir.listFiles()?.forEach { it.delete() }
        provider = DatabaseKeyProvider(context)
    }

    @After
    fun tearDown() {
        // Clean up wrapped key files after each test.
        context.noBackupFilesDir.resolve(DatabaseKeyProvider.WRAPPED_IV_FILE).delete()
        context.noBackupFilesDir.resolve(DatabaseKeyProvider.WRAPPED_KEY_FILE).delete()
    }

    @Test
    fun getOrCreatePassphrase_returns_32_bytes_on_first_call() {
        val passphrase = provider.getOrCreatePassphrase()
        assertEquals("passphrase must be 256 bits (32 bytes)", 32, passphrase.size)
    }

    @Test
    fun getOrCreatePassphrase_writes_wrapped_key_files_to_no_backup_dir() {
        provider.getOrCreatePassphrase()
        assertTrue(
            "IV file must exist after first call",
            context.noBackupFilesDir.resolve(DatabaseKeyProvider.WRAPPED_IV_FILE).exists()
        )
        assertTrue(
            "Ciphertext file must exist after first call",
            context.noBackupFilesDir.resolve(DatabaseKeyProvider.WRAPPED_KEY_FILE).exists()
        )
    }

    @Test
    fun getOrCreatePassphrase_returns_same_passphrase_on_reopen() {
        val first = provider.getOrCreatePassphrase()
        // Simulate app restart: create a new provider instance with the same context.
        val second = DatabaseKeyProvider(context).getOrCreatePassphrase()
        assertArrayEquals(
            "Passphrase must be identical across app restarts (Keystore key is stable)",
            first,
            second
        )
    }

    @Test
    fun getOrCreatePassphrase_is_non_zero() {
        val passphrase = provider.getOrCreatePassphrase()
        // A random 32-byte array has a near-zero probability of being all-zero.
        assertFalse(
            "Passphrase must not be all zeroes",
            passphrase.all { it == 0.toByte() }
        )
    }

    @Test
    fun database_opens_and_stays_readable_after_simulated_reopen() {
        System.loadLibrary("sqlcipher")

        // Create the database (first open — generates and stores key).
        val db1 = HealthJournalDatabase.create(context)
        db1.openHelper.readableDatabase // triggers open + key creation
        val version1 = db1.openHelper.readableDatabase.version
        db1.close()

        // Reopen — must use the same passphrase from Keystore.
        val db2 = HealthJournalDatabase.create(context)
        val version2 = db2.openHelper.readableDatabase.version
        db2.close()

        assertEquals("DB version must be the same after reopen", version1, version2)
        assertEquals("DB must be at the expected schema version", 6, version1)
    }
}
