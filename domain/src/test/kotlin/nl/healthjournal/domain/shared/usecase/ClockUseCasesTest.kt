package nl.healthjournal.domain.shared.usecase

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.medication.model.DayPattern
import nl.healthjournal.domain.medication.model.DoseUnit
import nl.healthjournal.domain.medication.model.IntakeStatus
import nl.healthjournal.domain.medication.model.MedicationForm
import nl.healthjournal.domain.medication.model.Schedule
import nl.healthjournal.domain.medication.model.StrengthUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import nl.healthjournal.domain.medication.usecase.RecordIntakeUseCase
import nl.healthjournal.domain.medication.usecase.RecordSlotIntakesUseCase
import nl.healthjournal.domain.medication.usecase.SaveMedicationUseCase
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import nl.healthjournal.domain.testing.FakeMedicationRepository

/** A null timestamp resolves to the injected clock; an explicit one is kept. */
class ClockUseCasesTest {
    private val now = Instant.parse("2026-09-20T10:15:00Z")
    private val explicit = Instant.parse("2026-01-02T03:04:05Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val profile = ProfileId.generate()

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
