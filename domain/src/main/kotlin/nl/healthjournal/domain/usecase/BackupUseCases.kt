package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.backup.BackupDocument
import nl.healthjournal.domain.model.backup.BackupPassphrase
import nl.healthjournal.domain.model.backup.BackupPreferences
import nl.healthjournal.domain.model.backup.MergeResult
import nl.healthjournal.domain.port.secondary.BackupCodecPort
import nl.healthjournal.domain.port.secondary.BackupSnapshotPort
import java.time.Clock

/** Builds an encrypted backup of everything. The caller clears the passphrase arrays afterwards. */
class CreateBackupUseCase(
    private val snapshot: BackupSnapshotPort,
    private val codec: BackupCodecPort,
    private val clock: Clock = Clock.systemUTC()
) {
    /** @throws IllegalArgumentException when the passphrase breaks the rules (the UI checks first). */
    suspend operator fun invoke(
        passphrase: CharArray,
        confirmation: CharArray,
        preferences: BackupPreferences?,
        appVersion: String?
    ): ByteArray {
        require(BackupPassphrase.check(passphrase, confirmation) == BackupPassphrase.Check.OK) {
            "Passphrase not acceptable"
        }
        val document = BackupDocument(clock.instant(), appVersion, preferences, snapshot.snapshot())
        return codec.encode(document, passphrase)
    }
}

/** Opens a backup (nothing is written), then merges it. Split so the lost-key flow can verify before it deletes. */
class RestoreBackupUseCase(
    private val snapshot: BackupSnapshotPort,
    private val codec: BackupCodecPort
) {
    /** Decrypts and validates in memory. @throws nl.healthjournal.domain.model.backup.BackupException */
    fun open(bytes: ByteArray, passphrase: CharArray): BackupDocument = codec.decode(bytes, passphrase)

    suspend fun merge(document: BackupDocument): MergeResult = snapshot.merge(document.profiles)
}
