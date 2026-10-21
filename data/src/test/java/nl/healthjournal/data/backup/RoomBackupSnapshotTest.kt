package nl.healthjournal.data.backup

import kotlinx.coroutines.test.runTest
import nl.healthjournal.data.local.HealthJournalDatabase
import nl.healthjournal.data.local.mapper.HealthLogMapper
import nl.healthjournal.data.local.mapper.ProfileMapper
import nl.healthjournal.domain.model.backup.BackupDocument
import nl.healthjournal.domain.model.backup.BackupProfile
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.model.profile.ProfileId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** The Room merge and snapshot against a real in-memory SQLite, fed by the decoded published test vector. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomBackupSnapshotTest {
    private lateinit var db: HealthJournalDatabase
    private lateinit var port: RoomBackupSnapshot
    private lateinit var sample: BackupDocument

    @Before
    fun open() {
        db = HealthJournalDatabase.createInMemory(RuntimeEnvironment.getApplication())
        port = RoomBackupSnapshot(db)
        val file = File("../docs/backup-format/test-vectors/sample-fast-nogzip.hjbackup").readBytes()
        sample = HjBackupCodec(BackupContainer(Argon2KeyDerivation(), memoryKiB = 1024, iterations = 1))
            .decode(file, "correct horse battery staple".toCharArray())
    }

    @After
    fun close() = db.close()

    @Test
    fun `merging into an empty database adds everything and the snapshot reads it back`() = runTest {
        val result = port.merge(sample.profiles)

        val profile = sample.profiles.single()
        assertEquals(1, result.profilesAdded)
        assertEquals(profile.entryCount, result.entriesAdded)
        assertEquals(profile.medications.size, result.medicationsAdded)
        assertEquals(profile.intakeCount, result.intakesAdded)
        assertEquals(0, result.alreadyPresent)

        val back = port.snapshot().single()
        assertEquals(profile.profile, back.profile)
        assertTrue(back.isActive)
        assertEquals(profile.weights.map { it.id }.toSet(), back.weights.map { it.id }.toSet())
        assertEquals(profile.entryCount, back.entryCount)
        assertEquals(profile.medications.single().medication, back.medications.single().medication)
        assertEquals(profile.intakeCount, back.intakeCount)
    }

    @Test
    fun `restoring the same file twice adds nothing the second time`() = runTest {
        port.merge(sample.profiles)
        val second = port.merge(sample.profiles)

        assertEquals(0, second.profilesAdded + second.entriesAdded + second.medicationsAdded + second.intakesAdded)
        assertTrue(second.alreadyPresent > 0)
        assertEquals(sample.profiles.single().entryCount, port.snapshot().single().entryCount)
    }

    @Test
    fun `an existing entry is never overwritten`() = runTest {
        port.merge(sample.profiles)
        val original = sample.profiles.single().weights.first()
        val changed = HealthLogMapper.toEntity(original).copy(comment = "edited on this device")
        db.weightDao().insert(changed)

        port.merge(sample.profiles)

        val stored = db.weightDao().getByProfileId(original.profileId.value.toString()).single { it.id == changed.id }
        assertEquals("edited on this device", stored.comment)
    }

    @Test
    fun `the active profile on the device is kept`() = runTest {
        val mine = Profile.create(name = "Mine", dateOfBirth = LocalDate.of(1990, 5, 5))
        db.profileDao().insert(ProfileMapper.toEntity(mine, isActive = true))

        port.merge(sample.profiles)

        assertEquals(mine.id.value.toString(), db.profileDao().getActive()?.id)
        assertEquals(2, db.profileDao().getAll().size)
    }

    @Test
    fun `a failure part-way leaves the database unchanged`() = runTest {
        val broken: BackupProfile = sample.profiles.single().let {
            it.copy(weights = it.weights + it.weights.first().copy(id = MeasurementId.generate(), profileId = ProfileId.generate()))
        }

        val failure = runCatching { port.merge(listOf(broken)) }.exceptionOrNull()
        assertNotNull(failure)

        assertEquals(0, db.profileDao().getAll().size)
    }
}
