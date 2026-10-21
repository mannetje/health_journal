package nl.healthjournal.data.local

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.BadPaddingException
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
open class DatabaseKeyProvider(context: Context) {

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
    open fun getOrCreatePassphrase(): ByteArray {
        return if (hasStoredKey()) {
            unwrap()
        } else {
            createAndStore()
        }
    }

    /** True when both wrapped-key files are present (they may still be unreadable, see [unwrap]). */
    open fun hasStoredKey(): Boolean = ivFile.exists() && keyFile.exists()

    /**
     * Deletes the wrapped passphrase files and the Keystore key. Only for recovery: any database
     * encrypted with the old passphrase becomes permanently unreadable.
     */
    open fun discardKey() {
        ivFile.delete()
        keyFile.delete()
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }.deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {
            // Nothing to delete, or the Keystore is unavailable; a new key replaces it on demand.
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        // Key does not exist yet — create it. StrongBox (hardware security module) is tried
        // first on API 28+; when the device has none, generateKey() throws
        // StrongBoxUnavailableException and the key is created in the TEE instead.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generateKey(strongBox = true)
            } catch (_: StrongBoxUnavailableException) {
                // Fall through to the TEE.
            }
        }
        return generateKey(strongBox = false)
    }

    private fun generateKey(strongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    private fun createAndStore(): ByteArray {
        val passphrase = java.security.SecureRandom().generateSeed(32)

        val key = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val ciphertext = cipher.doFinal(passphrase)
        val iv = cipher.iv // GCM always produces a 12-byte IV

        // Atomic writes, key file last: an interrupted write never leaves a half-written key behind,
        // and hasStoredKey() only turns true once both files are complete.
        writeAtomically(ivFile, iv)
        writeAtomically(keyFile, ciphertext)

        return passphrase
    }

    private fun writeAtomically(target: java.io.File, bytes: ByteArray) {
        val temp = target.resolveSibling(target.name + ".tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            temp.delete()
            throw java.io.IOException("Could not write ${target.name}")
        }
    }

    /**
     * Throws [DatabaseKeyLostException] only when the key is permanently gone (missing, invalidated
     * or failing authentication). Transient Keystore errors propagate unchanged, so a temporary
     * failure is never mistaken for lost data.
     */
    private fun unwrap(): ByteArray {
        val iv = ivFile.readBytes()
        val ciphertext = keyFile.readBytes()

        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }
            val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
                ?: throw DatabaseKeyLostException("Keystore key $KEY_ALIAS is missing")

            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            return cipher.doFinal(ciphertext)
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw DatabaseKeyLostException("Keystore key was invalidated", e)
        } catch (e: UnrecoverableKeyException) {
            throw DatabaseKeyLostException("Keystore key cannot be recovered", e)
        } catch (e: BadPaddingException) {
            throw DatabaseKeyLostException("Wrapped passphrase failed authentication", e)
        }
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
