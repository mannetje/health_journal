package nl.healthjournal.domain.profile.command

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.command.DomainError
import nl.healthjournal.domain.command.GetHistoryHandler
import nl.healthjournal.domain.command.GetHistoryQuery
import nl.healthjournal.domain.command.ValidationField
import nl.healthjournal.domain.migraine.command.ObserveMigraineHistoryHandler
import nl.healthjournal.domain.migraine.command.ObserveMigraineHistoryQuery
import nl.healthjournal.domain.migraine.command.ObserveMigraineTagsHandler
import nl.healthjournal.domain.migraine.command.ObserveMigraineTagsQuery
import nl.healthjournal.domain.model.common.HeightCm
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.model.profile.Sex
import nl.healthjournal.domain.testing.FakeHealthLogRepository
import nl.healthjournal.domain.usecase.GetHealthHistoryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import nl.healthjournal.domain.testing.FakeMigraineRepository
import nl.healthjournal.domain.testing.FakeProfileRepository

class ProfileAndHistoryHandlersTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-20T10:15:00Z"), ZoneOffset.UTC)
    private val profiles = FakeProfileRepository()
    private val save = SaveProfileHandler(profiles, clock)
    private val born = LocalDate.of(1980, 1, 1)

    @Test fun `no profile gives null`() = runBlocking {
        assertNull(GetActiveProfileHandler(profiles).handle(GetActiveProfileQuery).getOrNull())
    }

    @Test fun `the first save creates and activates, the second edits the same profile`() = runBlocking {
        val created = save.handle(SaveProfileCommand("  Ann ", born, 170, Sex.FEMALE)).getOrNull()!!
        assertEquals("Ann", created.name)
        assertEquals(created, GetActiveProfileHandler(profiles).handle(GetActiveProfileQuery).getOrNull())

        val edited = save.handle(SaveProfileCommand("Anna", born, null)).getOrNull()!!
        assertEquals(created.id, edited.id)
        assertEquals("Anna", edited.name)
        assertNull(edited.height)
        assertEquals(1, profiles.profiles.size)
    }

    @Test fun `invalid input is a typed error and nothing is saved`() = runBlocking {
        suspend fun field(c: SaveProfileCommand) = (save.handle(c).errorOrNull() as DomainError.Invalid).field
        assertEquals(ValidationField.NAME, field(SaveProfileCommand(" ", born)))
        assertEquals(ValidationField.HEIGHT, field(SaveProfileCommand("A", born, HeightCm.MAX_HEIGHT_CM + 1)))
        assertEquals(ValidationField.DATE_OF_BIRTH, field(SaveProfileCommand("A", LocalDate.of(2026, 9, 20))))
        assertEquals(ValidationField.DATE_OF_BIRTH, field(SaveProfileCommand("A", LocalDate.of(1800, 1, 1))))
        assertTrue(profiles.profiles.isEmpty())
    }

    @Test fun `history joins the six metrics and the migraines`() = runBlocking {
        val health = FakeHealthLogRepository()
        val migraines = FakeMigraineRepository()
        val profile = Profile.create("A", born).also { profiles.profiles[it.id] = it }
        migraines.saveMigraine(MigraineEntry(MeasurementId.generate(), profile.id, Instant.parse("2026-01-01T08:00:00Z")))
        val useCase = GetHealthHistoryUseCase(health, health, health, health, health, health)
        val snapshot = GetHistoryHandler(useCase, migraines).handle(GetHistoryQuery(profile.id)).getOrNull()!!
        assertEquals(1, snapshot.migraines.size)
        assertTrue(snapshot.health.weights.isEmpty())
    }

    @Test fun `observe queries emit the repository lists`() = runBlocking {
        val migraines = FakeMigraineRepository()
        val profile = Profile.create("A", born)
        migraines.saveMigraine(MigraineEntry(MeasurementId.generate(), profile.id, Instant.parse("2026-01-01T08:00:00Z")))
        val history = ObserveMigraineHistoryHandler(migraines).observe(ObserveMigraineHistoryQuery(profile.id)).first()
        assertEquals(1, history.getOrNull()!!.size)
        val tags = ObserveMigraineTagsHandler(migraines).observe(ObserveMigraineTagsQuery(profile.id)).first()
        assertTrue(tags.getOrNull()!!.isEmpty())
    }
}
