package nl.healthjournal.domain.migraine.command

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.command.DomainError
import nl.healthjournal.domain.command.ValidationField
import nl.healthjournal.domain.command.ValidationRule
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.model.migraine.MigraineTagKind
import nl.healthjournal.domain.testing.FakeMigraineRepository
import nl.healthjournal.domain.usecase.AddMigraineTagUseCase
import nl.healthjournal.domain.usecase.AddTagResult
import nl.healthjournal.domain.usecase.ArchiveMigraineTagUseCase
import nl.healthjournal.domain.usecase.RenameMigraineTagUseCase
import nl.healthjournal.domain.usecase.RenameTagResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MigraineHandlersTest {
    private val start = Instant.parse("2026-03-09T08:00:00Z")
    private val clock = Clock.fixed(start.plusSeconds(3600), ZoneOffset.UTC)
    private val profile = ProfileId.generate()
    private val repository = FakeMigraineRepository()

    private fun entry() = MigraineEntry(id = MeasurementId.generate(), profileId = profile, start = start)

    private val record = RecordMigraineHandler(repository)
    private val update = UpdateMigraineHandler(repository)
    private val end = EndMigraineHandler(repository, clock)

    @Test fun `record stores the migraine and update replaces it`() = runBlocking {
        val e = record.handle(RecordMigraineCommand(entry())).getOrNull()!!
        assertEquals(1, repository.migraines.size)
        assertEquals(6, update.handle(UpdateMigraineCommand(e.copy(intensity = 6))).getOrNull()?.intensity)
    }

    @Test fun `update of an unknown migraine is NotFound`() = runBlocking {
        val e = entry()
        assertEquals(
            DomainError.NotFound("MIGRAINE", e.id.toString()),
            update.handle(UpdateMigraineCommand(e)).errorOrNull()
        )
    }

    @Test fun `end without a time ends at the clock`() = runBlocking {
        val e = record.handle(RecordMigraineCommand(entry())).getOrNull()!!
        assertEquals(clock.instant(), end.handle(EndMigraineCommand(e.id)).getOrNull()?.end)
    }

    @Test fun `end before the start or on an ended migraine is END_NOT_AFTER_START`() = runBlocking {
        val e = record.handle(RecordMigraineCommand(entry())).getOrNull()!!
        val expected = DomainError.Invalid(ValidationField.TIME_RANGE, ValidationRule.END_NOT_AFTER_START)
        assertEquals(expected, end.handle(EndMigraineCommand(e.id, start.minusSeconds(1))).errorOrNull())

        assertTrue(end.handle(EndMigraineCommand(e.id, start.plusSeconds(60))).getOrNull() != null)
        assertEquals(expected, end.handle(EndMigraineCommand(e.id, start.plusSeconds(120))).errorOrNull())
    }

    @Test fun `end of an unknown migraine is NotFound`() = runBlocking {
        val id = MeasurementId.generate()
        assertEquals(DomainError.NotFound("MIGRAINE", id.toString()), end.handle(EndMigraineCommand(id)).errorOrNull())
    }

    @Test fun `tags can be added, renamed and archived`() = runBlocking {
        val add = AddMigraineTagHandler(AddMigraineTagUseCase(repository))
        val added = add.handle(AddMigraineTagCommand(profile, MigraineTagKind.TRIGGER, " Skipped lunch ")).getOrNull()
            as AddTagResult.Added
        assertEquals("Skipped lunch", added.tag.label)

        val rename = RenameMigraineTagHandler(RenameMigraineTagUseCase(repository))
        val renamed = rename.handle(RenameMigraineTagCommand(added.tag.id, "Noise")).getOrNull()
        assertTrue(renamed is RenameTagResult.Renamed)

        val archive = ArchiveMigraineTagHandler(ArchiveMigraineTagUseCase(repository))
        assertEquals(Unit, archive.handle(ArchiveMigraineTagCommand(added.tag.id)).getOrNull())
    }

    @Test fun `an unknown tag is NotFound for archive and an outcome for rename`() = runBlocking {
        val id = MigraineTagId.generate()
        val archive = ArchiveMigraineTagHandler(ArchiveMigraineTagUseCase(repository))
        assertEquals(DomainError.NotFound("MIGRAINE_TAG", id.toString()), archive.handle(ArchiveMigraineTagCommand(id)).errorOrNull())

        val rename = RenameMigraineTagHandler(RenameMigraineTagUseCase(repository))
        assertEquals(RenameTagResult.UnknownTag, rename.handle(RenameMigraineTagCommand(id, "Noise")).getOrNull())
    }

    @Test fun `a blank tag label is invalid for add and rename`() = runBlocking {
        val expected = DomainError.Invalid(ValidationField.NAME, ValidationRule.REQUIRED)
        val add = AddMigraineTagHandler(AddMigraineTagUseCase(repository))
        assertEquals(expected, add.handle(AddMigraineTagCommand(profile, MigraineTagKind.TRIGGER, "  ")).errorOrNull())
        val rename = RenameMigraineTagHandler(RenameMigraineTagUseCase(repository))
        assertEquals(expected, rename.handle(RenameMigraineTagCommand(MigraineTagId.generate(), " ")).errorOrNull())
        assertTrue(repository.tags.isEmpty())
    }
}
