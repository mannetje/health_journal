package nl.healthjournal.domain.usecase

import kotlinx.coroutines.test.runTest
import nl.healthjournal.domain.model.backup.BackupDocument
import nl.healthjournal.domain.model.backup.BackupException
import nl.healthjournal.domain.model.backup.BackupProfile
import nl.healthjournal.domain.model.backup.MergeResult
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.BackupCodecPort
import nl.healthjournal.domain.port.secondary.BackupSnapshotPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class BackupUseCasesTest {

    private val now = Instant.parse("2026-10-21T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun profile() = BackupProfile(
        Profile.create(name = "Test", dateOfBirth = LocalDate.of(1980, 1, 1)),
        isActive = true
    )

    private class FakeSnapshot(var profiles: List<BackupProfile>) : BackupSnapshotPort {
        var merged: List<BackupProfile>? = null
        override suspend fun snapshot() = profiles
        override suspend fun merge(profiles: List<BackupProfile>): MergeResult {
            merged = profiles
            return MergeResult(profiles.size, 0, 0, 0, 0)
        }
    }

    private class FakeCodec : BackupCodecPort {
        var encoded: BackupDocument? = null
        var decodeResult: BackupDocument? = null
        override fun encode(document: BackupDocument, passphrase: CharArray): ByteArray {
            encoded = document
            return byteArrayOf(1)
        }

        override fun decode(bytes: ByteArray, passphrase: CharArray): BackupDocument =
            decodeResult ?: throw BackupException.WrongPassphraseOrCorrupt()
    }

    @Test
    fun `create encodes a snapshot stamped with the clock`() = runTest {
        val p = profile()
        val codec = FakeCodec()
        val bytes = CreateBackupUseCase(FakeSnapshot(listOf(p)), codec, clock)(
            "correct horse".toCharArray(), "correct horse".toCharArray(), null, "1.6.0"
        )
        assertEquals(1, bytes.size)
        assertEquals(now, codec.encoded?.createdAt)
        assertEquals("1.6.0", codec.encoded?.appVersion)
        assertEquals(listOf(p), codec.encoded?.profiles)
    }

    @Test
    fun `create refuses a short or mismatched passphrase and encodes nothing`() = runTest {
        val codec = FakeCodec()
        val create = CreateBackupUseCase(FakeSnapshot(emptyList()), codec, clock)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { create("short".toCharArray(), "short".toCharArray(), null, null) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { create("long enough".toCharArray(), "long enough!".toCharArray(), null, null) }
        }
        assertEquals(null, codec.encoded)
    }

    @Test
    fun `open does not touch the database and merge hands the profiles to the port`() = runTest {
        val snapshot = FakeSnapshot(emptyList())
        val codec = FakeCodec()
        val document = BackupDocument(now, null, null, listOf(profile()))
        codec.decodeResult = document
        val restore = RestoreBackupUseCase(snapshot, codec)

        val opened = restore.open(byteArrayOf(1), "x".toCharArray())
        assertSame(document, opened)
        assertEquals(null, snapshot.merged)

        assertEquals(1, restore.merge(opened).profilesAdded)
        assertEquals(document.profiles, snapshot.merged)
    }

    @Test
    fun `open reports a wrong passphrase`() {
        val restore = RestoreBackupUseCase(FakeSnapshot(emptyList()), FakeCodec())
        assertThrows(BackupException.WrongPassphraseOrCorrupt::class.java) {
            restore.open(byteArrayOf(1), "wrong".toCharArray())
        }
    }
}
