package nl.healthjournal.domain.usecase

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeId
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationForm
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.PlannedStatus
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.StrengthUnit
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class FakeMedicationRepository : MedicationRepositoryPort {
    val medications = mutableMapOf<MedicationId, Medication>()
    val intakes = mutableListOf<Intake>()

    override suspend fun saveMedication(medication: Medication) {
        medications[medication.id] = medication
    }

    override suspend fun getMedication(id: MedicationId): Medication? = medications[id]

    override suspend fun getMedications(profileId: ProfileId, includeArchived: Boolean): List<Medication> =
        medications.values.filter { it.profileId == profileId && (includeArchived || it.archivedFrom == null) }

    override suspend fun deleteMedication(id: MedicationId) {
        medications.remove(id)
        intakes.removeAll { it.medicationId == id }
    }

    override suspend fun saveIntake(intake: Intake) {
        // Same replace rule as the real repository: one outcome per (medication, planned time).
        if (intake.planned != null) {
            intakes.removeAll { it.medicationId == intake.medicationId && it.planned == intake.planned }
        }
        intakes.removeAll { it.id == intake.id }
        intakes.add(intake)
    }

    override suspend fun getIntake(medicationId: MedicationId, planned: LocalDateTime): Intake? =
        intakes.firstOrNull { it.medicationId == medicationId && it.planned == planned }

    override suspend fun deleteIntake(id: IntakeId) {
        intakes.removeAll { it.id == id }
    }

    override suspend fun getAllIntakes(profileId: ProfileId): List<Intake> =
        intakes.filter { medications[it.medicationId]?.profileId == profileId }

    override suspend fun getIntakesForDay(profileId: ProfileId, date: LocalDate, zone: ZoneId): List<Intake> =
        getAllIntakes(profileId).filter { intake ->
            val planned = intake.planned
            if (planned != null) planned.toLocalDate() == date
            else intake.takenAt!!.atZone(zone).toLocalDate() == date
        }
}

class MedicationUseCasesTest {
    private val profile = ProfileId.generate()
    private val day = LocalDate.of(2026, 5, 4)
    private val eight = LocalTime.of(8, 0)
    private val twenty = LocalTime.of(20, 0)
    private val daily = Schedule.Recurring(listOf(eight), DayPattern.EVERY_DAY, day.minusDays(10))

    private lateinit var repository: FakeMedicationRepository
    private lateinit var save: SaveMedicationUseCase
    private lateinit var record: RecordIntakeUseCase

    @Before
    fun setUp() {
        repository = FakeMedicationRepository()
        save = SaveMedicationUseCase(repository)
        record = RecordIntakeUseCase(repository)
    }

    private fun create(name: String = "Medication A", schedule: Schedule = daily, comment: String? = null) =
        runBlocking {
            save(
                profileId = profile,
                name = name,
                form = MedicationForm.TABLET,
                strengthAmount = BigDecimal("10"),
                strengthUnit = StrengthUnit.MG,
                amountPerIntake = BigDecimal.ONE,
                doseUnit = DoseUnit.TABLETS,
                appearance = null,
                schedule = schedule,
                comment = comment,
                effectiveFrom = day.minusDays(10)
            )
        }

    private val plannedAtEight: LocalDateTime get() = day.atTime(eight)

    // --- SaveMedicationUseCase

    @Test
    fun `save creates a medication with one schedule version and stores it`() {
        val medication = create(name = "  Medication A  ")

        assertEquals("Medication A", medication.name.value)
        assertEquals(1, medication.schedules.size)
        assertEquals(day.minusDays(10), medication.schedules.single().effectiveFrom)
        assertEquals(medication, repository.medications[medication.id])
    }

    @Test
    fun `save keeps the strength only when both amount and unit are given`() {
        val withBoth = create()
        val withoutUnit = runBlocking {
            save(profile, "Medication B", null, BigDecimal("10"), null, BigDecimal.ONE, DoseUnit.TABLETS, null, daily)
        }

        assertNotNull(withBoth.dosage.strength)
        assertNull(withoutUnit.dosage.strength)
    }

    @Test
    fun `save stores a blank comment as no comment`() {
        assertNull(create(comment = "   ").comment)
        assertEquals("Left arm", create(comment = "Left arm").comment?.text)
    }

    @Test
    fun `save rejects a blank name and an invalid dose without storing anything`() {
        try {
            create(name = "   ")
            fail("blank name accepted")
        } catch (_: IllegalArgumentException) {
        }
        try {
            runBlocking {
                save(profile, "Medication A", null, null, null, BigDecimal.ZERO, DoseUnit.TABLETS, null, daily)
            }
            fail("zero dose accepted")
        } catch (_: IllegalArgumentException) {
        }
        assertTrue(repository.medications.isEmpty())
    }

    @Test
    fun `saving an existing medication updates the details but keeps schedules and archived state`() {
        val original = create()
        val archived = runBlocking { ArchiveMedicationUseCase(repository)(original.id, day) }
        val changeTo = Schedule.Recurring(listOf(twenty), DayPattern.EVERY_DAY, day)

        val updated = runBlocking {
            save(
                profileId = profile,
                name = "Medication A2",
                form = null,
                strengthAmount = null,
                strengthUnit = null,
                amountPerIntake = BigDecimal("2"),
                doseUnit = DoseUnit.TABLETS,
                appearance = null,
                schedule = changeTo,
                existingId = original.id
            )
        }

        assertEquals(original.id, updated.id)
        assertEquals("Medication A2", updated.name.value)
        assertEquals(BigDecimal("2"), updated.dosage.amountPerIntake)
        assertEquals(archived.schedules, updated.schedules)
        assertEquals(day, updated.archivedFrom)
        assertEquals(1, repository.medications.size)
    }

    // --- RecordIntakeUseCase

    @Test
    fun `record stores a taken intake with the time taken`() {
        val medication = create()
        val takenAt = Instant.parse("2026-05-04T06:30:00Z")

        val intake = runBlocking { record(medication.id, plannedAtEight, takenAt = takenAt) }

        assertEquals(IntakeStatus.TAKEN, intake.status)
        assertEquals(takenAt, intake.takenAt)
        assertEquals(listOf(intake), repository.intakes)
    }

    @Test
    fun `record skipped drops the time and amount`() {
        val medication = create()

        val intake = runBlocking {
            record(medication.id, plannedAtEight, IntakeStatus.SKIPPED, actualAmount = BigDecimal.ONE)
        }

        assertEquals(IntakeStatus.SKIPPED, intake.status)
        assertNull(intake.takenAt)
        assertNull(intake.actualAmount)
    }

    @Test
    fun `recording again for the same planned time replaces the earlier outcome`() {
        val medication = create()
        val first = runBlocking { record(medication.id, plannedAtEight, IntakeStatus.SKIPPED) }

        val second = runBlocking { record(medication.id, plannedAtEight, IntakeStatus.TAKEN) }

        assertEquals(first.id, second.id)
        assertEquals(1, repository.intakes.size)
        assertEquals(IntakeStatus.TAKEN, repository.intakes.single().status)
    }

    @Test
    fun `as needed doses get their own id each time`() {
        val medication = create(schedule = Schedule.AsNeeded)

        val a = runBlocking { record(medication.id, null) }
        val b = runBlocking { record(medication.id, null) }

        assertNotEquals(a.id, b.id)
        assertEquals(2, repository.intakes.size)
        assertTrue(a.isAsNeeded)
    }

    @Test
    fun `as needed dose cannot be recorded as skipped`() {
        val medication = create(schedule = Schedule.AsNeeded)
        try {
            runBlocking { record(medication.id, null, IntakeStatus.SKIPPED) }
            fail("skipped as-needed accepted")
        } catch (_: IllegalArgumentException) {
        }
        assertTrue(repository.intakes.isEmpty())
    }

    @Test
    fun `record rejects an unknown medication`() {
        try {
            runBlocking { record(MedicationId.generate(), plannedAtEight) }
            fail("unknown medication accepted")
        } catch (_: IllegalArgumentException) {
        }
        assertTrue(repository.intakes.isEmpty())
    }

    // --- RecordSlotIntakesUseCase

    @Test
    fun `taken all records one taken outcome per medication at the same time`() {
        val a = create("Medication A")
        val b = create("Medication B")
        val takenAt = Instant.parse("2026-05-04T06:05:00Z")

        val result = runBlocking {
            RecordSlotIntakesUseCase(record)(listOf(a.id, b.id, a.id), plannedAtEight, takenAt)
        }

        assertEquals(2, result.size)
        assertTrue(result.all { it.status == IntakeStatus.TAKEN && it.takenAt == takenAt })
        assertEquals(setOf(a.id, b.id), repository.intakes.map { it.medicationId }.toSet())
    }

    // --- ChangeMedicationScheduleUseCase

    @Test
    fun `changing the schedule adds a version and leaves recorded outcomes alone`() {
        val medication = create()
        runBlocking { record(medication.id, plannedAtEight) }
        val evening = Schedule.Recurring(listOf(twenty), DayPattern.EVERY_DAY, day)

        val updated = runBlocking { ChangeMedicationScheduleUseCase(repository)(medication.id, evening, day) }

        assertEquals(2, updated.schedules.size)
        assertEquals(listOf(plannedAtEight), updated.plannedFor(day.minusDays(1)).map { it.plusDays(1) })
        assertEquals(listOf(day.atTime(twenty)), updated.plannedFor(day))
        assertEquals(1, repository.intakes.size)
    }

    @Test
    fun `changing the schedule of an unknown medication fails`() {
        try {
            runBlocking { ChangeMedicationScheduleUseCase(repository)(MedicationId.generate(), daily, day) }
            fail("unknown medication accepted")
        } catch (_: IllegalArgumentException) {
        }
    }

    // --- ArchiveMedicationUseCase / DeleteMedicationUseCase

    @Test
    fun `archiving stops planned intakes from the date and keeps earlier days`() {
        val medication = create()

        val archived = runBlocking { ArchiveMedicationUseCase(repository)(medication.id, day) }

        assertEquals(day, archived.archivedFrom)
        assertEquals(1, archived.plannedFor(day.minusDays(1)).size)
        assertTrue(archived.plannedFor(day).isEmpty())
        assertTrue(archived.plannedFor(day.plusDays(3)).isEmpty())
    }

    @Test
    fun `archiving an unknown medication fails`() {
        try {
            runBlocking { ArchiveMedicationUseCase(repository)(MedicationId.generate(), day) }
            fail("unknown medication accepted")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `deleting removes the medication together with its intakes`() {
        val a = create("Medication A")
        val b = create("Medication B")
        runBlocking {
            record(a.id, plannedAtEight)
            record(b.id, plannedAtEight)
            DeleteMedicationUseCase(repository)(a.id)
        }

        assertNull(repository.medications[a.id])
        assertEquals(listOf(b.id), repository.intakes.map { it.medicationId })
    }

    // --- GetPillboxDayUseCase

    private fun clockAt(local: LocalDateTime) = Clock.fixed(local.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Test
    fun `pillbox day derives pending and missed from the clock`() {
        create()
        val early = GetPillboxDayUseCase(repository, clockAt(day.atTime(9, 0)))
        val late = GetPillboxDayUseCase(repository, clockAt(day.atTime(10, 30)))

        assertEquals(PlannedStatus.PENDING, runBlocking { early(profile, day) }.slots.single().items.single().status)
        assertEquals(PlannedStatus.MISSED, runBlocking { late(profile, day) }.slots.single().items.single().status)
    }

    @Test
    fun `pillbox day shows a recorded outcome and an as needed dose`() {
        val scheduled = create("Medication A")
        val asNeeded = create("Medication B", Schedule.AsNeeded)
        runBlocking {
            record(scheduled.id, plannedAtEight)
            record(asNeeded.id, null, takenAt = day.atTime(12, 0).toInstant(ZoneOffset.UTC))
        }

        val result = runBlocking { GetPillboxDayUseCase(repository, clockAt(day.atTime(13, 0)))(profile, day) }

        assertEquals(PlannedStatus.TAKEN, result.slots.single().items.single().status)
        assertEquals(1, result.logged.size)
        assertFalse(result.logged.single().planned != null)
    }

    @Test
    fun `pillbox day keeps archived medications that were still planned that day`() {
        val medication = create()
        runBlocking { ArchiveMedicationUseCase(repository)(medication.id, day.plusDays(1)) }

        val result = runBlocking { GetPillboxDayUseCase(repository, clockAt(day.atTime(9, 0)))(profile, day) }

        assertEquals(1, result.slots.size)
    }
}
