package nl.healthjournal.domain.backup.command

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.shared.command.BackupProblem
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.ValidationField
import nl.healthjournal.domain.shared.command.ValidationRule
import nl.healthjournal.domain.backup.model.BackupDocument
import nl.healthjournal.domain.backup.model.BackupException
import nl.healthjournal.domain.backup.model.BackupPassphrase
import nl.healthjournal.domain.backup.model.BackupProfile
import nl.healthjournal.domain.backup.model.MergeResult
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.backup.port.BackupCodecPort
import nl.healthjournal.domain.backup.port.BackupSnapshotPort
import nl.healthjournal.domain.backup.usecase.CreateBackupUseCase
import nl.healthjournal.domain.backup.usecase.RestoreBackupUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class BackupHandlersTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC)
    private val good = "correct horse".toCharArray()

    private class Snapshot : BackupSnapshotPort {
        var merged: List<BackupProfile>? = null
        override suspend fun snapshot() = listOf(
            BackupProfile(Profile.create(name = "Test", dateOfBirth = LocalDate.of(1980, 1, 1)), isActive = true)
        )
        override suspend fun merge(profiles: List<BackupProfile>): MergeResult {
            merged = profiles
            return MergeResult(profiles.size, 0, 0, 0, 0)
        }
    }

    private class Codec(var failure: Exception? = null, var decoded: BackupDocument? = null) : BackupCodecPort {
        override fun encode(document: BackupDocument, passphrase: CharArray): ByteArray = byteArrayOf(1)
        override fun decode(bytes: ByteArray, passphrase: CharArray): BackupDocument =
            failure?.let { throw it } ?: decoded!!
    }

    private val snapshot = Snapshot()
    private val codec = Codec()
    private val create = CreateBackupHandler(CreateBackupUseCase(snapshot, codec, clock))
    private val open = OpenBackupHandler(RestoreBackupUseCase(snapshot, codec))

    @Test fun `create returns the encoded bytes`() = runBlocking {
        val bytes = create.handle(CreateBackupCommand(good, good.copyOf(), null, "1.7.1")).getOrNull()
        assertEquals(1, bytes?.size)
    }

    @Test fun `a short passphrase is TOO_SHORT with the minimum as parameter`() = runBlocking {
        val short = "abc".toCharArray()
        val error = create.handle(CreateBackupCommand(short, short.copyOf(), null, null)).errorOrNull()
        assertEquals(
            DomainError.Invalid(ValidationField.PASSPHRASE, ValidationRule.TOO_SHORT, listOf(BackupPassphrase.MIN_LENGTH)),
            error
        )
    }

    @Test fun `a different confirmation is MISMATCH`() = runBlocking {
        val error = create.handle(CreateBackupCommand(good, "something else".toCharArray(), null, null)).errorOrNull()
        assertEquals(DomainError.Invalid(ValidationField.PASSPHRASE, ValidationRule.MISMATCH), error)
    }

    @Test fun `open maps every BackupException to its problem`() = runBlocking {
        val cases = mapOf(
            BackupException.NotABackup() to BackupProblem.NOT_A_BACKUP,
            BackupException.Unsupported("v99") to BackupProblem.UNSUPPORTED,
            BackupException.WrongPassphraseOrCorrupt() to BackupProblem.WRONG_PASSPHRASE_OR_CORRUPT,
            BackupException.Malformed("bad") to BackupProblem.MALFORMED
        )
        for ((exception, problem) in cases) {
            codec.failure = exception
            assertEquals(DomainError.Backup(problem), open.handle(OpenBackupQuery(byteArrayOf(1), good)).errorOrNull())
        }
    }

    @Test fun `open maps any other exception to Storage`() = runBlocking {
        codec.failure = IllegalStateException("disk")
        assertTrue(open.handle(OpenBackupQuery(byteArrayOf(1), good)).errorOrNull() is DomainError.Storage)
    }

    @Test fun `open returns the decoded document and restore merges it`() = runBlocking {
        val document = BackupDocument(Instant.parse("2026-10-21T10:00:00Z"), "1.7.1", null, snapshot.snapshot())
        codec.decoded = document
        assertSame(document, open.handle(OpenBackupQuery(byteArrayOf(1), good)).getOrNull())

        val result = RestoreBackupHandler(RestoreBackupUseCase(snapshot, codec))
            .handle(RestoreBackupCommand(document)).getOrNull()
        assertEquals(1, result?.profilesAdded)
        assertEquals(document.profiles, snapshot.merged)
    }
}
