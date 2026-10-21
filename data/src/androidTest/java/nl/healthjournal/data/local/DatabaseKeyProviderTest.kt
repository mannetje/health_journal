package nl.healthjournal.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

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
    fun key_creation_leaves_no_temporary_files() {
        provider.getOrCreatePassphrase()
        val names = context.noBackupFilesDir.listFiles().orEmpty().map { it.name }.toSet()
        assertEquals(
            "Only the two key files may exist (no leftover .tmp from the atomic write)",
            setOf(DatabaseKeyProvider.WRAPPED_IV_FILE, DatabaseKeyProvider.WRAPPED_KEY_FILE),
            names
        )
    }

    @Test
    fun hasStoredKey_is_false_until_a_key_is_created() {
        assertFalse(provider.hasStoredKey())
        provider.getOrCreatePassphrase()
        assertTrue(provider.hasStoredKey())
    }

    @Test
    fun a_single_leftover_key_file_does_not_count_as_a_stored_key() {
        provider.getOrCreatePassphrase()
        context.noBackupFilesDir.resolve(DatabaseKeyProvider.WRAPPED_KEY_FILE).delete()
        assertFalse(provider.hasStoredKey())
        // A new key is created instead of failing on the half-present state.
        assertEquals(32, provider.getOrCreatePassphrase().size)
        assertTrue(provider.hasStoredKey())
    }

    @Test
    fun discardKey_removes_the_files_and_the_next_key_is_different() {
        val first = provider.getOrCreatePassphrase()
        provider.discardKey()
        assertFalse(provider.hasStoredKey())
        val second = provider.getOrCreatePassphrase()
        assertFalse("A discarded key must not come back", first.contentEquals(second))
    }

    @Test
    fun missing_keystore_key_is_reported_as_lost() {
        provider.getOrCreatePassphrase()
        KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }.deleteEntry("health_journal_db_key")
        try {
            provider.getOrCreatePassphrase()
            fail("Expected DatabaseKeyLostException")
        } catch (_: DatabaseKeyLostException) {
            // expected: files exist but the Keystore key is gone
        }
    }

    @Test
    fun corrupted_wrapped_key_is_reported_as_lost() {
        provider.getOrCreatePassphrase()
        val keyFile = context.noBackupFilesDir.resolve(DatabaseKeyProvider.WRAPPED_KEY_FILE)
        keyFile.writeBytes(keyFile.readBytes().also { it[0] = (it[0].toInt() xor 0xFF).toByte() })
        try {
            provider.getOrCreatePassphrase()
            fail("Expected DatabaseKeyLostException")
        } catch (_: DatabaseKeyLostException) {
            // expected: GCM authentication fails
        }
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
