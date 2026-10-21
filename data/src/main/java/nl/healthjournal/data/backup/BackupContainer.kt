package nl.healthjournal.data.backup

import nl.healthjournal.domain.model.backup.BackupException
import nl.healthjournal.domain.model.backup.BackupPassphrase
import nl.healthjournal.domain.port.secondary.BackupKeyDerivationPort
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The `.hjbackup` container, byte for byte as in `docs/backup-format/README.md`:
 * 28-byte header, 16-byte salt, 12-byte nonce, then AES-256-GCM ciphertext and tag. Bytes 0 to 55 are the
 * associated data. The reader checks every header field against the limits before it derives a key.
 */
class BackupContainer(
    private val keyDerivation: BackupKeyDerivationPort,
    private val random: SecureRandom = SecureRandom(),
    private val memoryKiB: Int = DEFAULT_MEMORY_KIB,
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val parallelism: Int = DEFAULT_PARALLELISM,
    private val compress: Boolean = true
) {

    fun seal(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also(random::nextBytes)
        val nonce = ByteArray(NONCE_LENGTH).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_LENGTH).apply {
            put(MAGIC)
            putShort(CONTAINER_VERSION.toShort())
            put(KDF_ARGON2ID.toByte())
            put(CIPHER_AES_256_GCM.toByte())
            put(if (compress) COMPRESSION_GZIP.toByte() else COMPRESSION_NONE.toByte())
            put(parallelism.toByte())
            put(SALT_LENGTH.toByte())
            put(NONCE_LENGTH.toByte())
            putInt(memoryKiB)
            putInt(iterations)
            putInt(0)
            put(salt)
            put(nonce)
        }.array()

        val payload = if (compress) gzip(plaintext) else plaintext
        val key = deriveKey(passphrase, salt, memoryKiB, iterations, parallelism)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(header)
            return header + cipher.doFinal(payload)
        } finally {
            key.fill(0)
        }
    }

    /** Returns the decrypted (and, if flagged, decompressed) document bytes. */
    fun open(file: ByteArray, passphrase: CharArray): ByteArray {
        if (file.size < MAGIC.size || !file.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupException.NotABackup()
        }
        if (file.size < HEADER_LENGTH + TAG_BYTES) throw BackupException.NotABackup()

        val buf = ByteBuffer.wrap(file, 0, HEADER_LENGTH)
        buf.position(MAGIC.size)
        val version = buf.short.toInt() and 0xFFFF
        val kdf = buf.get().toInt() and 0xFF
        val cipherId = buf.get().toInt() and 0xFF
        val compression = buf.get().toInt() and 0xFF
        val lanes = buf.get().toInt() and 0xFF
        val saltLength = buf.get().toInt() and 0xFF
        val nonceLength = buf.get().toInt() and 0xFF
        val memory = buf.int.toLong() and 0xFFFFFFFFL
        val passes = buf.int.toLong() and 0xFFFFFFFFL
        val reserved = buf.int

        if (version != CONTAINER_VERSION) throw BackupException.Unsupported("container version $version")
        if (kdf != KDF_ARGON2ID) throw BackupException.Unsupported("key derivation $kdf")
        if (cipherId != CIPHER_AES_256_GCM) throw BackupException.Unsupported("cipher $cipherId")
        if (compression != COMPRESSION_NONE && compression != COMPRESSION_GZIP) {
            throw BackupException.Unsupported("compression $compression")
        }
        if (reserved != 0) throw BackupException.Unsupported("reserved bytes")
        if (saltLength != SALT_LENGTH || nonceLength != NONCE_LENGTH) throw BackupException.Unsupported("salt or nonce length")
        if (lanes !in 1..MAX_LANES) throw BackupException.Unsupported("parallelism $lanes")
        if (memory > MAX_MEMORY_KIB || memory < 8L * lanes) throw BackupException.Unsupported("memory")
        if (passes !in 1..MAX_ITERATIONS) throw BackupException.Unsupported("iterations")

        val salt = file.copyOfRange(HEADER_LENGTH - SALT_LENGTH - NONCE_LENGTH, HEADER_LENGTH - NONCE_LENGTH)
        val nonce = file.copyOfRange(HEADER_LENGTH - NONCE_LENGTH, HEADER_LENGTH)
        val header = file.copyOfRange(0, HEADER_LENGTH)

        val key = deriveKey(passphrase, salt, memory.toInt(), passes.toInt(), lanes)
        val plain = try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(header)
            cipher.doFinal(file, HEADER_LENGTH, file.size - HEADER_LENGTH)
        } catch (e: AEADBadTagException) {
            throw BackupException.WrongPassphraseOrCorrupt()
        } catch (e: GeneralSecurityException) {
            throw BackupException.WrongPassphraseOrCorrupt()
        } finally {
            key.fill(0)
        }
        return if (compression == COMPRESSION_GZIP) gunzip(plain) else plain
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, memory: Int, passes: Int, lanes: Int): ByteArray {
        val secret = BackupPassphrase.toUtf8(passphrase)
        try {
            return keyDerivation.derive(secret, salt, memory, passes, lanes, KEY_LENGTH)
        } catch (e: OutOfMemoryError) {
            throw BackupException.Unsupported("not enough memory on this device")
        } finally {
            secret.fill(0)
        }
    }

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    /** The data is authentic by now, but a size cap still stops an absurd expansion. */
    private fun gunzip(data: ByteArray): ByteArray {
        try {
            GZIPInputStream(ByteArrayInputStream(data)).use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_DOCUMENT_BYTES) throw BackupException.Malformed("document too large")
                    out.write(buffer, 0, n)
                }
                return out.toByteArray()
            }
        } catch (e: java.io.IOException) {
            throw BackupException.Malformed("compression")
        }
    }

    companion object {
        val MAGIC = "HJBACKUP".toByteArray(Charsets.US_ASCII)
        const val HEADER_LENGTH = 56
        const val CONTAINER_VERSION = 1
        const val KDF_ARGON2ID = 1
        const val CIPHER_AES_256_GCM = 1
        const val COMPRESSION_NONE = 0
        const val COMPRESSION_GZIP = 1
        const val SALT_LENGTH = 16
        const val NONCE_LENGTH = 12
        const val KEY_LENGTH = 32
        const val TAG_BITS = 128
        const val TAG_BYTES = 16
        const val DEFAULT_MEMORY_KIB = 65536
        const val DEFAULT_ITERATIONS = 3
        const val DEFAULT_PARALLELISM = 1
        const val MAX_MEMORY_KIB = 1_048_576L
        const val MAX_ITERATIONS = 100L
        const val MAX_LANES = 16
        const val MAX_DOCUMENT_BYTES = 256L * 1024 * 1024
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
