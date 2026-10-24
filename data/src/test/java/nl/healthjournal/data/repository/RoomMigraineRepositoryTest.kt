package nl.healthjournal.data.repository

import kotlinx.coroutines.test.runTest
import nl.healthjournal.data.local.HealthJournalDatabase
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.migraine.DailyActivities
import nl.healthjournal.domain.model.migraine.HeadArea
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineSymptom
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.model.migraine.MigraineTagKind
import nl.healthjournal.domain.model.migraine.MigraineTrigger
import nl.healthjournal.domain.model.migraine.MigraineType
import nl.healthjournal.domain.model.migraine.PainCharacter
import nl.healthjournal.domain.model.migraine.ReliefMethod
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

/** The Migraine repository against a real in-memory SQLite: round trip of every field, ongoing entries and tags. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigraineRepositoryTest {
    private lateinit var db: HealthJournalDatabase
    private lateinit var repo: RoomMigraineRepository
    private val profile = ProfileId.generate()
    private val start = Instant.ofEpochMilli(1_800_000_000_000)

    @Before
    fun open() {
        db = HealthJournalDatabase.createInMemory(RuntimeEnvironment.getApplication())
        repo = RoomMigraineRepository(db.migraineDao(), clock = { 42L })
    }

    @After
    fun close() = db.close()

    private fun minimal(at: Instant = start) = MigraineEntry(id = MeasurementId.generate(), profileId = profile, start = at)

    @Test
    fun `a minimal entry round trips and is ongoing`() = runTest {
        val e = minimal()
        repo.saveMigraine(e)
        val back = repo.getMigraine(e.id)!!
        assertEquals(e, back)
        assertTrue(back.ongoing)
    }

    @Test
    fun `a full entry round trips with every field`() = runTest {
        val trigger = MigraineTag(MigraineTagId.generate(), profile, MigraineTagKind.TRIGGER, "skipped lunch")
        val relief = MigraineTag(MigraineTagId.generate(), profile, MigraineTagKind.RELIEF, "walk")
        val e = MigraineEntry(
            id = MeasurementId.generate(), profileId = profile, start = start, wokeUpWithIt = true,
            end = start.plusSeconds(3600), intensity = 8,
            areas = setOf(HeadArea.LEFT_SIDE, HeadArea.TEMPLES), painCharacters = setOf(PainCharacter.THROBBING),
            type = MigraineType.WITH_AURA, symptoms = setOf(MigraineSymptom.NAUSEA, MigraineSymptom.AURA),
            triggers = setOf(MigraineTrigger.STRESS), customTriggerIds = setOf(trigger.id),
            tookSomething = true, medicationText = "my usual one", secondDose = true, intensityAfter2Hours = 3,
            medicationHelpedMe = true, reliefMethods = setOf(ReliefMethod.DARK_QUIET_ROOM, ReliefMethod.HEAT),
            customReliefIds = setOf(relief.id), reliefHelpedMe = true, activities = DailyActivities.HAD_TO_STOP,
            comment = EntryComment.ofOrNull("slept badly")
        )
        repo.saveMigraine(e)
        assertEquals(e, repo.getMigraine(e.id))
    }

    @Test
    fun `history is newest first per profile and update and delete report unknown ids`() = runTest {
        val old = minimal(start)
        val new = minimal(start.plusSeconds(86_400))
        val other = MigraineEntry(id = MeasurementId.generate(), profileId = ProfileId.generate(), start = start)
        listOf(old, new, other).forEach { repo.saveMigraine(it) }
        assertEquals(listOf(new.id, old.id), repo.getMigraineHistory(profile).map { it.id })

        assertTrue(repo.updateMigraine(old.copy(end = start.plusSeconds(60))))
        assertFalse(repo.getMigraine(old.id)!!.ongoing)
        assertFalse(repo.updateMigraine(minimal()))
        assertTrue(repo.deleteMigraine(old.id))
        assertFalse(repo.deleteMigraine(old.id))
        assertNull(repo.getMigraine(old.id))
    }

    @Test
    fun `tags rename and archive keep their id and are listed with archived ones`() = runTest {
        val tag = MigraineTag(MigraineTagId.generate(), profile, MigraineTagKind.RELIEF, "walk")
        repo.saveMigraineTag(tag)
        assertTrue(repo.updateMigraineTag(tag.copy(label = "long walk", archived = true)))
        val back = repo.getMigraineTags(profile).single()
        assertEquals(tag.id, back.id)
        assertEquals("long walk", back.label)
        assertTrue(back.archived)
        assertEquals(MigraineTagKind.RELIEF, back.kind)
        assertFalse(repo.updateMigraineTag(tag.copy(id = MigraineTagId.generate())))
    }

    @Test
    fun `unknown codes and ids written by a newer version are ignored on read`() = runTest {
        val e = minimal()
        repo.saveMigraine(e)
        db.openHelper.writableDatabase.execSQL(
            "UPDATE migraines SET symptoms = 'NAUSEA,FUTURE_CODE', customTriggers = 'not-a-uuid', type = 'FUTURE_TYPE'"
        )
        val back = repo.getMigraine(e.id)!!
        assertEquals(setOf(MigraineSymptom.NAUSEA), back.symptoms)
        assertTrue(back.customTriggerIds.isEmpty())
        assertNull(back.type)
    }
}
