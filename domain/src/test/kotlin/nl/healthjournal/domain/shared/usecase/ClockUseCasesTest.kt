package nl.healthjournal.domain.shared.usecase

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.medication.model.DayPattern
import nl.healthjournal.domain.medication.model.DoseUnit
import nl.healthjournal.domain.medication.model.IntakeStatus
import nl.healthjournal.domain.medication.model.MedicationForm
import nl.healthjournal.domain.medication.model.Schedule
import nl.healthjournal.domain.medication.model.StrengthUnit
import nl.healthjournal.domain.metrics.model.GlucoseContext
import nl.healthjournal.domain.migraine.model.MigraineEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import nl.healthjournal.domain.medication.usecase.RecordIntakeUseCase
import nl.healthjournal.domain.medication.usecase.RecordSlotIntakesUseCase
import nl.healthjournal.domain.medication.usecase.SaveMedicationUseCase
import nl.healthjournal.domain.metrics.usecase.RecordOxygenSaturationUseCase
import nl.healthjournal.domain.migraine.usecase.EndMigraineUseCase
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import nl.healthjournal.domain.testing.FakeHealthLogRepository
import nl.healthjournal.domain.testing.FakeMedicationRepository
import nl.healthjournal.domain.testing.FakeMigraineRepository
import nl.healthjournal.domain.testing.FakeProfileRepository

/** A null timestamp resolves to the injected clock; an explicit one is kept. */
class ClockUseCasesTest {
    private val now = Instant.parse("2026-09-20T10:15:00Z")
    private val explicit = Instant.parse("2026-01-02T03:04:05Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val profile = ProfileId.generate()
    private val health = FakeHealthLogRepository()
    private val profiles = FakeProfileRepository()

    @Test fun `oxygen saturation`() = runBlocking {
        val use = RecordOxygenSaturationUseCase(health, clock)
        assertEquals(now, use(profile, 97).timestamp)
        assertEquals(explicit, use(profile, 97, timestamp = explicit).timestamp)
    }

    @Test fun `ending a migraine`() = runBlocking {
        val repo = FakeMigraineRepository()
        val start = now.minusSeconds(3600)
        val entry = MigraineEntry(id = MeasurementId.generate(), profileId = profile, start = start)
        repo.saveMigraine(entry)
        assertEquals(now, EndMigraineUseCase(repo, clock)(entry.id)?.end)

        val second = MigraineEntry(id = MeasurementId.generate(), profileId = profile, start = start)
        repo.saveMigraine(second)
        assertEquals(explicit.plusSeconds(86400 * 365), EndMigraineUseCase(repo, clock)(second.id, explicit.plusSeconds(86400 * 365))?.end)
    }

    @Test fun `ending a migraine before its start still fails with the clock`() = runBlocking {
        val repo = FakeMigraineRepository()
        val entry = MigraineEntry(id = MeasurementId.generate(), profileId = profile, start = now.plusSeconds(60))
        repo.saveMigraine(entry)
        assertNull(EndMigraineUseCase(repo, clock)(entry.id))
    }

    @Test fun `intakes`() = runBlocking {
        val meds = FakeMedicationRepository()
        val day = LocalDate.of(2026, 9, 20)
        val med = SaveMedicationUseCase(meds)(
            profileId = profile,
            name = "Medication A",
            form = MedicationForm.TABLET,
            strengthAmount = BigDecimal("10"),
            strengthUnit = StrengthUnit.MG,
            amountPerIntake = BigDecimal.ONE,
            doseUnit = DoseUnit.TABLETS,
            appearance = null,
            schedule = Schedule.Recurring(listOf(LocalTime.of(8, 0)), DayPattern.EVERY_DAY, day.minusDays(10)),
            comment = null,
            effectiveFrom = day.minusDays(10)
        )
        val record = RecordIntakeUseCase(meds, clock)
        val planned = LocalDateTime.of(day, LocalTime.of(8, 0))
        assertEquals(now, record(med.id, planned).takenAt)
        assertEquals(explicit, record(med.id, planned, takenAt = explicit).takenAt)
        assertNull(record(med.id, planned, IntakeStatus.SKIPPED).takenAt)

        val slot = RecordSlotIntakesUseCase(record, clock)
        assertEquals(listOf(now), slot(listOf(med.id), planned).map { it.takenAt })
        assertEquals(listOf(explicit), slot(listOf(med.id), planned, explicit).map { it.takenAt })
    }
}
