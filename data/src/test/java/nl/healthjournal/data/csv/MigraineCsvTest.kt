package nl.healthjournal.data.csv

import kotlinx.coroutines.test.runTest
import nl.healthjournal.data.local.HealthJournalDatabase
import nl.healthjournal.data.repository.RoomMigraineRepository
import nl.healthjournal.data.repository.RoomProfileRepository
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.migraine.DailyActivities
import nl.healthjournal.domain.model.migraine.HeadArea
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineSymptom
import nl.healthjournal.domain.model.migraine.MigraineTagKind
import nl.healthjournal.domain.model.migraine.MigraineType
import nl.healthjournal.domain.model.migraine.PainCharacter
import nl.healthjournal.domain.model.migraine.ReliefMethod
import nl.healthjournal.domain.model.profile.Profile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/** Migraine CSV export and import against a real in-memory database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigraineCsvTest {
    private lateinit var db: HealthJournalDatabase
    private lateinit var repo: RoomMigraineRepository
    private lateinit var export: CsvMigraineExportAdapter
    private lateinit var import: CsvMigraineImportAdapter
    private lateinit var profile: Profile

    private val start = Instant.parse("2026-10-20T06:30:00Z")

    @Before
    fun open() = kotlinx.coroutines.runBlocking {
        db = HealthJournalDatabase.createInMemory(RuntimeEnvironment.getApplication())
        repo = RoomMigraineRepository(db.migraineDao())
        val profiles = RoomProfileRepository(db.profileDao())
        profile = Profile.create(name = "Sample", dateOfBirth = LocalDate.of(1990, 1, 1))
        profiles.save(profile)
        export = CsvMigraineExportAdapter(repo)
        import = CsvMigraineImportAdapter(repo, profiles)
    }

    @After
    fun close() = db.close()

    private val header = MIGRAINES_HEADER

    @Test
    fun `export of nothing is only the header`() = runTest {
        assertEquals("$header\n", export.exportMigraineCsv(profile.id))
    }

    @Test
    fun `a full entry exports and imports back with new ids, tags created`() = runTest {
        val csv = "$header\n" +
            "2026-10-20T06:30:00Z,true,2026-10-20T10:00:00Z,8,LEFT_SIDE|TEMPLES,THROBBING,WITH_AURA,AURA|NAUSEA,STRESS," +
            "true,\"my usual, one\",true,3,DARK_QUIET_ROOM|HEAT,skipped lunch,walk,true,true,HAD_TO_STOP,\"slept badly\"\n"
        val result = import.importMigraineCsv(profile.id, csv)
        assertEquals(1, result.importedCount)
        assertTrue(result.skippedRows.toString(), result.skippedRows.isEmpty())

        val e = repo.getMigraineHistory(profile.id).single()
        assertTrue(e.wokeUpWithIt)
        assertEquals(setOf(HeadArea.LEFT_SIDE, HeadArea.TEMPLES), e.areas)
        assertEquals(setOf(PainCharacter.THROBBING), e.painCharacters)
        assertEquals(MigraineType.WITH_AURA, e.type)
        assertEquals(setOf(MigraineSymptom.AURA, MigraineSymptom.NAUSEA), e.symptoms)
        assertEquals("my usual, one", e.medicationText)
        assertEquals(3, e.intensityAfter2Hours)
        assertEquals(setOf(ReliefMethod.DARK_QUIET_ROOM, ReliefMethod.HEAT), e.reliefMethods)
        assertEquals(DailyActivities.HAD_TO_STOP, e.activities)
        assertEquals(EntryComment.ofOrNull("slept badly"), e.comment)
        val tags = repo.getMigraineTags(profile.id)
        assertEquals(setOf("skipped lunch" to MigraineTagKind.TRIGGER, "walk" to MigraineTagKind.RELIEF), tags.map { it.label to it.kind }.toSet())

        // Export and import again gives the same row (round trip).
        assertEquals(csv.replace("\"slept badly\"", "slept badly"), export.exportMigraineCsv(profile.id))
    }

    @Test
    fun `an ongoing entry with only a start round trips`() = runTest {
        repo.saveMigraine(MigraineEntry(MeasurementId.generate(), profile.id, start))
        val csv = export.exportMigraineCsv(profile.id)
        assertEquals("$header\n2026-10-20T06:30:00Z,false,,,,,,,,false,,false,,,,,false,false,,\n", csv)
        val second = import.importMigraineCsv(profile.id, csv)
        assertEquals(1, second.importedCount)
        assertTrue(repo.getMigraineHistory(profile.id).all { it.ongoing })
    }

    @Test
    fun `the same personal label reuses the tag ignoring case`() = runTest {
        val row = "2026-10-20T06:30:00Z,false,,,,,,,,false,,false,,,%s,,false,false,,\n"
        import.importMigraineCsv(profile.id, header + "\n" + row.format("Skipped Lunch") + row.format("skipped lunch"))
        assertEquals(1, repo.getMigraineTags(profile.id).size)
    }

    @Test
    fun `rejected rows import nothing and create no tag`() = runTest {
        val base = "2026-10-20T06:30:00Z"
        val rows = listOf(
            "$base,false,2026-10-20T05:00:00Z,,,,,,,false,,false,,,newtag,,false,false,,", // end before start
            "$base,false,,11,,,,,,false,,false,,,newtag,,false,false,,", // intensity out of range
            "$base,false,,,,,,,,false,,false,3,,newtag,,false,false,,", // after 2h without something taken
            "$base,false,,,NOT_AN_AREA,,,,,false,,false,,,newtag,,false,false,,", // unknown code
            "$base,false,,,,,,,,false,,false,,,bad|label,,false,false,,", // label with a pipe splits fine; see below
            "$base,false,,,,,,,,false,,false,,,,,false,true,,", // relief helped without relief
            "$base,maybe,,,,,,,,false,,false,,,newtag,,false,false,," // bad flag
        )
        val result = import.importMigraineCsv(profile.id, header + "\n" + rows.joinToString("\n") + "\n")
        // "bad|label" is two valid labels, so exactly that one row is accepted; the other six are rejected.
        assertEquals(1, result.importedCount)
        assertEquals(6, result.skippedRows.size)
        assertEquals(listOf(2, 3, 4, 5, 7, 8), result.skippedRows.map { it.lineNumber })
        assertEquals(setOf("bad", "label"), repo.getMigraineTags(profile.id).map { it.label }.toSet())
    }

    @Test
    fun `a label over 30 characters rejects the row`() = runTest {
        val long = "x".repeat(31)
        val result = import.importMigraineCsv(profile.id, "$header\n2026-10-20T06:30:00Z,false,,,,,,,,false,,false,,,$long,,false,false,,\n")
        assertEquals(0, result.importedCount)
        assertEquals(1, result.skippedRows.size)
        assertTrue(repo.getMigraineTags(profile.id).isEmpty())
    }

    @Test
    fun `a file without the start column is refused`() = runTest {
        val result = import.importMigraineCsv(profile.id, "intensity\n5\n")
        assertEquals(0, result.importedCount)
        assertEquals("Missing column start_timestamp", result.skippedRows.single().reason)
    }
}
