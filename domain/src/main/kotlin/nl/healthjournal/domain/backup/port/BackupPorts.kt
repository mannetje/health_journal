package nl.healthjournal.domain.backup.port

import nl.healthjournal.domain.backup.model.BackupDocument
import nl.healthjournal.domain.backup.model.BackupProfile
import nl.healthjournal.domain.backup.model.MergeResult

/** Reads and merges everything the app stores for the backup. */
interface BackupSnapshotPort {
    /** All profiles with their entries, medications and intakes, as one consistent read. */
    suspend fun snapshot(): List<BackupProfile>

    /**
     * Adds what is missing in one transaction: nothing existing is overwritten or deleted, and restoring the same
     * file twice adds nothing the second time. A failure leaves the database unchanged.
     */
    suspend fun merge(profiles: List<BackupProfile>): MergeResult
}

/** Turns a [BackupDocument] into the `.hjbackup` bytes and back. Both directions are CPU and memory heavy. */
interface BackupCodecPort {
    fun encode(document: BackupDocument, passphrase: CharArray): ByteArray

    /** @throws nl.healthjournal.domain.backup.model.BackupException when the file cannot be opened. */
    fun decode(bytes: ByteArray, passphrase: CharArray): BackupDocument
}

/** Argon2id (RFC 9106, version 0x13, no secret, no associated data). */
interface BackupKeyDerivationPort {
    fun derive(
        passphrase: ByteArray,
        salt: ByteArray,
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int,
        length: Int
    ): ByteArray
}
