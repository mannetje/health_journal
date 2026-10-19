package nl.healthjournal.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Provides and persists the SQLCipher database passphrase.
 *
 * The passphrase is a random 32-byte (256-bit) value. It is never stored in the clear: it is
 * wrapped (AES-GCM) by a non-exportable Android Keystore key and written to no-backup storage
 * as two files: [WRAPPED_IV_FILE] (12-byte GCM IV) and [WRAPPED_KEY_FILE] (ciphertext).
 *
 * The Keystore key does **not** require user authentication, so changing a fingerprint or the
 * screen lock never makes the database unreadable (ADR 0023).
 * StrongBox is tried first; if unavailable the key falls back to the TEE.
 *
 * Thread-safety: all calls are expected from a single background thread (Room's open-helper
 * initialization). No additional synchronisation is provided here.
 */
class DatabaseKeyProvider(context: Context) {

    // No-backup directory: excluded from cloud backup and device-transfer by the extraction rules
    // added in task 3.3. getNoBackupFilesDir() is available from API 21.
    private val noBackupDir = context.noBackupFilesDir.also { it.mkdirs() }
    private val ivFile = noBackupDir.resolve(WRAPPED_IV_FILE)
    private val keyFile = noBackupDir.resolve(WRAPPED_KEY_FILE)

    /**
     * Returns the plaintext passphrase (32 bytes).
     * On first call, generates a new random passphrase, wraps it with the Keystore key, and
     * writes the ciphertext + IV to no-backup storage.
     * On subsequent calls, reads and unwraps the stored ciphertext.
     */
    fun getOrCreatePassphrase(): ByteArray {
        return if (ivFile.exists() && keyFile.exists()) {
            unwrap()
        } else {
            createAndStore()
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        // Key does not exist yet — create it. StrongBox is tried first (hardware security
        // module); falls back to TEE automatically when StrongBox is not available.
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .apply {
                trySetStrongBoxBacked(this)
            }
            .build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /** Attempts to enable StrongBox; silently skips if the device does not support it. */
    private fun trySetStrongBoxBacked(builder: KeyGenParameterSpec.Builder) {
        try {
            builder.setIsStrongBoxBacked(true)
        } catch (_: Exception) {
            // StrongBox not available on this device — TEE is used instead.
        }
    }

    private fun createAndStore(): ByteArray {
        val passphrase = java.security.SecureRandom().generateSeed(32)

        val key = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val ciphertext = cipher.doFinal(passphrase)
        val iv = cipher.iv // GCM always produces a 12-byte IV

        ivFile.writeBytes(iv)
        keyFile.writeBytes(ciphertext)

        return passphrase
    }

    private fun unwrap(): ByteArray {
        val iv = ivFile.readBytes()
        val ciphertext = keyFile.readBytes()

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }
        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        return cipher.doFinal(ciphertext)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "health_journal_db_key"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128

        /** IV for the wrapped passphrase. Stored in no-backup storage. */
        const val WRAPPED_IV_FILE = "db_key.iv"

        /** AES-GCM ciphertext of the database passphrase. Stored in no-backup storage. */
        const val WRAPPED_KEY_FILE = "db_key.enc"
    }
}
