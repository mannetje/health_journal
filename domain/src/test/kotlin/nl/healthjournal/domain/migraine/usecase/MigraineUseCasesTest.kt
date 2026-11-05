package nl.healthjournal.domain.migraine.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.migraine.model.MigraineEntry
import nl.healthjournal.domain.migraine.model.MigraineTag
import nl.healthjournal.domain.migraine.model.MigraineTagId
import nl.healthjournal.domain.migraine.model.MigraineTagKind
import nl.healthjournal.domain.migraine.port.MigraineRepositoryPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import nl.healthjournal.domain.testing.FakeMigraineRepository

class MigraineUseCasesTest {
    private val repo = FakeMigraineRepository()
    private val profile = ProfileId.generate()
    private val start = Instant.parse("2026-03-09T08:00:00Z")

    private fun entry(end: Instant? = null) =
        MigraineEntry(id = MeasurementId.generate(), profileId = profile, start = start, end = end)

    @Test fun `record update and delete`() = runBlocking {
        val e = RecordMigraineUseCase(repo)(entry())
        assertEquals(1, repo.migraines.size)
        val changed = e.copy(intensity = 6)
        assertEquals(6, UpdateMigraineUseCase(repo)(changed)?.intensity)
        assertNull(UpdateMigraineUseCase(repo)(entry()))
        assertTrue(DeleteMigraineUseCase(repo)(e.id))
        assertFalse(DeleteMigraineUseCase(repo)(e.id))
    }

    @Test fun `end sets the end of an ongoing migraine only once and not before the start`() = runBlocking {
        val e = RecordMigraineUseCase(repo)(entry())
        assertNull(EndMigraineUseCase(repo)(e.id, start.minusSeconds(1)))
        val ended = EndMigraineUseCase(repo)(e.id, start.plusSeconds(3600))
        assertEquals(start.plusSeconds(3600), ended?.end)
        assertNull(EndMigraineUseCase(repo)(e.id, start.plusSeconds(7200)))
        assertNull(EndMigraineUseCase(repo)(MeasurementId.generate(), start))
    }

    @Test fun `adding a tag trims it and a duplicate returns the existing tag`() = runBlocking {
        val add = AddMigraineTagUseCase(repo)
        val first = add(profile, MigraineTagKind.TRIGGER, "  Skipped lunch ") as AddTagResult.Added
        assertFalse(first.alreadyExisted)
        assertEquals("Skipped lunch", first.tag.label)
        val again = add(profile, MigraineTagKind.TRIGGER, "skipped LUNCH") as AddTagResult.Added
        assertTrue(again.alreadyExisted)
        assertEquals(first.tag.id, again.tag.id)
        assertEquals(1, repo.tags.size)
    }

    @Test fun `the same label may exist once per kind`() = runBlocking {
        val add = AddMigraineTagUseCase(repo)
        add(profile, MigraineTagKind.TRIGGER, "walk")
        val relief = add(profile, MigraineTagKind.RELIEF, "walk") as AddTagResult.Added
        assertFalse(relief.alreadyExisted)
        assertEquals(2, repo.tags.size)
    }

    @Test fun `limit of 30 active tags per kind and archiving frees a place`() = runBlocking {
        val add = AddMigraineTagUseCase(repo)
        val added = (1..30).map { (add(profile, MigraineTagKind.TRIGGER, "t$it") as AddTagResult.Added).tag }
        assertEquals(AddTagResult.LimitReached, add(profile, MigraineTagKind.TRIGGER, "one more"))
        assertNotNull(add(profile, MigraineTagKind.RELIEF, "other kind") as? AddTagResult.Added)
        assertTrue(ArchiveMigraineTagUseCase(repo)(added.first().id))
        assertTrue(add(profile, MigraineTagKind.TRIGGER, "one more") is AddTagResult.Added)
    }

    @Test fun `adding an archived label brings it back`() = runBlocking {
        val add = AddMigraineTagUseCase(repo)
        val tag = (add(profile, MigraineTagKind.RELIEF, "walk") as AddTagResult.Added).tag
        ArchiveMigraineTagUseCase(repo)(tag.id)
        val back = add(profile, MigraineTagKind.RELIEF, "Walk") as AddTagResult.Added
        assertEquals(tag.id, back.tag.id)
        assertFalse(back.tag.archived)
        assertEquals(1, repo.tags.size)
    }

    @Test fun `rename keeps the id and refuses a label another tag has`() = runBlocking {
        val add = AddMigraineTagUseCase(repo)
        val a = (add(profile, MigraineTagKind.TRIGGER, "a") as AddTagResult.Added).tag
        add(profile, MigraineTagKind.TRIGGER, "b")
        val renamed = RenameMigraineTagUseCase(repo)(a.id, " c ") as RenameTagResult.Renamed
        assertEquals(a.id, renamed.tag.id)
        assertEquals("c", renamed.tag.label)
        assertEquals(RenameTagResult.LabelTaken, RenameMigraineTagUseCase(repo)(a.id, "B"))
        assertEquals(RenameTagResult.UnknownTag, RenameMigraineTagUseCase(repo)(MigraineTagId.generate(), "x"))
    }

    @Test fun `archive unknown tag returns false`() = runBlocking {
        assertFalse(ArchiveMigraineTagUseCase(repo)(MigraineTagId.generate()))
    }
}
