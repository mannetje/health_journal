package nl.healthjournal.domain.medication.command

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.command.DomainError
import nl.healthjournal.domain.command.ValidationField
import nl.healthjournal.domain.command.ValidationRule
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.MedicationForm
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.MedicationName
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.StrengthUnit
import nl.healthjournal.domain.testing.FakeMedicationRepository
import nl.healthjournal.domain.usecase.ArchiveMedicationUseCase
import nl.healthjournal.domain.usecase.ChangeMedicationScheduleUseCase
import nl.healthjournal.domain.usecase.DeleteMedicationUseCase
import nl.healthjournal.domain.usecase.GetAdherenceUseCase
import nl.healthjournal.domain.usecase.GetPillboxDayUseCase
import nl.healthjournal.domain.usecase.RecordIntakeUseCase
import nl.healthjournal.domain.usecase.RecordSlotIntakesUseCase
import nl.healthjournal.domain.usecase.SaveMedicationUseCase
import nl.healthjournal.domain.usecase.TakeAllForSlotUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class MedicationHandlersTest {
    private val clock = Clock.fixed(Instant.parse("2026-05-04T12:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 5, 4)
    private val profile = ProfileId.generate()
    private val repository = FakeMedicationRepository()
    private val daily = Schedule.Recurring(listOf(LocalTime.of(8, 0)), DayPattern.EVERY_DAY, today.minusDays(10))

    private val recordIntake = RecordIntakeUseCase(repository, clock)
    private val recordSlot = RecordSlotIntakesUseCase(recordIntake, clock)
    private val save = SaveMedicationHandler(SaveMedicationUseCase(repository), clock)

    private fun saveCommand(name: String = "Medication A") = SaveMedicationCommand(
        profileId = profile, name = name, form = MedicationForm.TABLET, strengthAmount = BigDecimal("10"),
        strengthUnit = StrengthUnit.MG, amountPerIntake = BigDecimal.ONE, doseUnit = DoseUnit.TABLETS,
        appearance = null, schedule = daily, effectiveFrom = today.minusDays(10)
    )

    private fun saved() = runBlocking { save.handle(saveCommand()).getOrNull()!! }

    private fun notFound(id: MedicationId) = DomainError.NotFound("MEDICATION", id.toString())

    @Test fun `save trims the name and stores the medication`() = runBlocking {
        val medication = save.handle(saveCommand("  Medication A ")).getOrNull()!!
        assertEquals("Medication A", medication.name.value)
        assertNotNull(repository.getMedication(medication.id))
    }

    @Test fun `save with a blank or too long name is a typed error and stores nothing`() = runBlocking {
        val blank = save.handle(saveCommand("  ")).errorOrNull() as DomainError.Invalid
        assertEquals(ValidationField.NAME, blank.field)
        assertEquals(ValidationRule.REQUIRED, blank.rule)

        val long = save.handle(saveCommand("x".repeat(MedicationName.MAX_LENGTH + 1))).errorOrNull() as DomainError.Invalid
        assertEquals(ValidationRule.TOO_LONG, long.rule)
        assertEquals(listOf(MedicationName.MAX_LENGTH), long.params)
        assertTrue(repository.getMedications(profile).isEmpty())
    }

    @Test fun `changing the schedule adds a version and an unknown medication is NotFound`() = runBlocking {
        val medication = saved()
        val handler = ChangeMedicationScheduleHandler(repository, ChangeMedicationScheduleUseCase(repository), clock)
        val twice = Schedule.Recurring(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), DayPattern.EVERY_DAY, today)

        val changed = handler.handle(ChangeMedicationScheduleCommand(medication.id, twice)).getOrNull()!!
        assertEquals(2, changed.schedules.size)

        val unknown = MedicationId.generate()
        assertEquals(notFound(unknown), handler.handle(ChangeMedicationScheduleCommand(unknown, twice)).errorOrNull())
    }

    @Test fun `archiving stops the plan and an unknown medication is NotFound`() = runBlocking {
        val medication = saved()
        val handler = ArchiveMedicationHandler(repository, ArchiveMedicationUseCase(repository), clock)
        assertTrue(handler.handle(ArchiveMedicationCommand(medication.id)).getOrNull()!!.archivedFrom != null)

        val unknown = MedicationId.generate()
        assertEquals(notFound(unknown), handler.handle(ArchiveMedicationCommand(unknown)).errorOrNull())
    }

    @Test fun `deleting removes the medication and an unknown medication is NotFound`() = runBlocking {
        val medication = saved()
        val handler = DeleteMedicationHandler(repository, DeleteMedicationUseCase(repository))
        assertEquals(Unit, handler.handle(DeleteMedicationCommand(medication.id)).getOrNull())
        assertNull(repository.getMedication(medication.id))
        assertEquals(notFound(medication.id), handler.handle(DeleteMedicationCommand(medication.id)).errorOrNull())
    }

    @Test fun `recording an intake stores it and an unknown medication is NotFound`() = runBlocking {
        val medication = saved()
        val handler = RecordIntakeHandler(repository, recordIntake)
        val intake = handler.handle(RecordIntakeCommand(medication.id, today.atTime(8, 0))).getOrNull()!!
        assertEquals(IntakeStatus.TAKEN, intake.status)

        val unknown = MedicationId.generate()
        assertEquals(notFound(unknown), handler.handle(RecordIntakeCommand(unknown, today.atTime(8, 0))).errorOrNull())
    }

    @Test fun `recording a slot stores one intake per medication and fails on the first unknown id`() = runBlocking {
        val a = saved()
        val b = save.handle(saveCommand("Medication B")).getOrNull()!!
        val handler = RecordSlotIntakesHandler(repository, recordSlot)

        val intakes = handler.handle(RecordSlotIntakesCommand(listOf(a.id, b.id), today.atTime(8, 0))).getOrNull()!!
        assertEquals(2, intakes.size)

        val unknown = MedicationId.generate()
        val failed = handler.handle(RecordSlotIntakesCommand(listOf(a.id, unknown), today.atTime(20, 0)))
        assertEquals(notFound(unknown), failed.errorOrNull())
    }

    @Test fun `take all records only what is still open in the slot`() = runBlocking {
        val a = saved()
        val b = save.handle(saveCommand("Medication B")).getOrNull()!!
        recordIntake(a.id, today.atTime(8, 0))
        val pillbox = GetPillboxDayUseCase(repository, clock)
        val handler = TakeAllForSlotHandler(TakeAllForSlotUseCase(pillbox, recordSlot, clock))

        val taken = handler.handle(TakeAllForSlotCommand(profile, today.atTime(8, 0))).getOrNull()!!
        assertEquals(listOf(b.id), taken.map { it.medicationId })
    }

    @Test fun `pillbox day and adherence are queries and an unsupported range is invalid`() = runBlocking {
        saved()
        val day = GetPillboxDayHandler(GetPillboxDayUseCase(repository, clock))
            .handle(GetPillboxDayQuery(profile, today)).getOrNull()!!
        assertEquals(today, day.date)

        val adherence = GetAdherenceHandler(GetAdherenceUseCase(repository, clock))
        assertNotNull(adherence.handle(GetAdherenceQuery(profile, 7)).getOrNull())
        val invalid = adherence.handle(GetAdherenceQuery(profile, 5)).errorOrNull() as DomainError.Invalid
        assertEquals(ValidationField.TIME_RANGE, invalid.field)
        assertEquals(ValidationRule.OUT_OF_RANGE, invalid.rule)
    }
}
