package nl.healthjournal.domain.backup.command

import nl.healthjournal.domain.shared.command.BackupProblem
import nl.healthjournal.domain.shared.command.Command
import nl.healthjournal.domain.shared.command.CommandHandler
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.Query
import nl.healthjournal.domain.shared.command.QueryHandler
import nl.healthjournal.domain.shared.command.ValidationField
import nl.healthjournal.domain.shared.command.ValidationRule
import nl.healthjournal.domain.shared.command.domainCatching
import nl.healthjournal.domain.backup.model.BackupDocument
import nl.healthjournal.domain.backup.model.BackupException
import nl.healthjournal.domain.backup.model.BackupPassphrase
import nl.healthjournal.domain.backup.model.BackupPreferences
import nl.healthjournal.domain.backup.model.MergeResult
import nl.healthjournal.domain.backup.usecase.CreateBackupUseCase
import nl.healthjournal.domain.backup.usecase.RestoreBackupUseCase
import kotlin.coroutines.cancellation.CancellationException

/**
 * Builds an encrypted backup of everything and returns its bytes. The caller clears the passphrase arrays
 * afterwards. Plain classes, not data classes: a data class would compare and print the secrets.
 */
class CreateBackupCommand(
    val passphrase: CharArray,
    val confirmation: CharArray,
    val preferences: BackupPreferences?,
    val appVersion: String?
) : Command<ByteArray>

/** Decrypts and validates a backup in memory; nothing is written. */
class OpenBackupQuery(val bytes: ByteArray, val passphrase: CharArray) : Query<BackupDocument>

/** Merges an opened backup into the data on the device. Nothing that is already there is changed. */
data class RestoreBackupCommand(val document: BackupDocument) : Command<MergeResult>

class CreateBackupHandler(
    private val create: CreateBackupUseCase
) : CommandHandler<CreateBackupCommand, ByteArray> {
    override suspend fun handle(command: CreateBackupCommand): DomainResult<ByteArray> {
        when (BackupPassphrase.check(command.passphrase, command.confirmation)) {
            BackupPassphrase.Check.OK -> Unit
            BackupPassphrase.Check.TOO_SHORT -> return DomainResult.failure(
                DomainError.Invalid(ValidationField.PASSPHRASE, ValidationRule.TOO_SHORT, listOf(BackupPassphrase.MIN_LENGTH))
            )
            BackupPassphrase.Check.MISMATCH -> return DomainResult.failure(
                DomainError.Invalid(ValidationField.PASSPHRASE, ValidationRule.MISMATCH)
            )
        }
        return domainCatching { create(command.passphrase, command.confirmation, command.preferences, command.appVersion) }
    }
}

class OpenBackupHandler(
    private val restore: RestoreBackupUseCase
) : QueryHandler<OpenBackupQuery, BackupDocument> {
    override suspend fun handle(query: OpenBackupQuery): DomainResult<BackupDocument> =
        try {
            DomainResult.success(restore.open(query.bytes, query.passphrase))
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackupException) {
            DomainResult.failure(DomainError.Backup(e.toProblem()))
        } catch (e: Exception) {
            DomainResult.failure(DomainError.Storage(e))
        }

    private fun BackupException.toProblem(): BackupProblem = when (this) {
        is BackupException.NotABackup -> BackupProblem.NOT_A_BACKUP
        is BackupException.Unsupported -> BackupProblem.UNSUPPORTED
        is BackupException.WrongPassphraseOrCorrupt -> BackupProblem.WRONG_PASSPHRASE_OR_CORRUPT
        is BackupException.Malformed -> BackupProblem.MALFORMED
    }
}

class RestoreBackupHandler(
    private val restore: RestoreBackupUseCase
) : CommandHandler<RestoreBackupCommand, MergeResult> {
    override suspend fun handle(command: RestoreBackupCommand): DomainResult<MergeResult> =
        domainCatching { restore.merge(command.document) }
}
