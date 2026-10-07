package nl.healthjournal.domain.usecase

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.ScheduleVersion
import nl.healthjournal.domain.model.medication.medication
import nl.healthjournal.domain.port.secondary.ReminderSchedulerPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private class FakeScheduler : ReminderSchedulerPort {
    var armed: LocalDateTime? = null
    var cancelled = 0

    override fun arm(slot: LocalDateTime) {
        armed = slot
    }

    override fun cancel() {
        armed = null
        cancelled++
    }
}

class MedicationReminderUseCasesTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val day = LocalDate.of(2026, 5, 4)
    private val profile = ProfileId.generate()
    private val start = day.minusDays(10)
    private val seven30 = LocalTime.of(7, 30)

    private lateinit var repository: FakeMedicationRepository
    private lateinit var scheduler: FakeScheduler

    private fun clockAt(time: String) =
        Clock.fixed(day.atTime(LocalTime.parse(time)).atZone(zone).toInstant(), zone)

    private fun daily(vararg times: LocalTime) = Schedule.Recurring(times.toList(), DayPattern.EVERY_DAY, start)

    private fun add(name: String, schedule: Schedule, owner: ProfileId = profile) =
        medication(name, listOf(ScheduleVersion(start, schedule)), owner).also {
            runBlocking { repository.saveMedication(it) }
        }

    @Before
    fun setUp() {
        repository = FakeMedicationRepository()
        scheduler = FakeScheduler()
    }

    // --- RearmRemindersUseCase

    @Test
    fun `arms the next slot of the profile`() = runBlocking {
        add("Medication A", daily(seven30, LocalTime.of(19, 30)))
        val rearm = RearmRemindersUseCase(repository, scheduler, clockAt("08:00"))

        assertEquals(day.atTime(19, 30), rearm(profile))
        assertEquals(day.atTime(19, 30), scheduler.armed)
    }

    @Test
    fun `cancels the alarm when nothing is planned`() = runBlocking {
        add("Medication A", Schedule.AsNeeded)
        val rearm = RearmRemindersUseCase(repository, scheduler, clockAt("08:00"))

        assertNull(rearm(profile))
        assertEquals(1, scheduler.cancelled)
    }

    @Test
    fun `cancels the alarm when there is no active profile`() = runBlocking {
        add("Medication A", daily(seven30))
        val rearm = RearmRemindersUseCase(repository, scheduler, clockAt("06:00"))

        assertNull(rearm(null))
        assertEquals(1, scheduler.cancelled)
    }

    @Test
    fun `only the given profile's medications count`() = runBlocking {
        add("Medication A", daily(LocalTime.of(9, 0)), owner = ProfileId.generate())
        add("Medication B", daily(LocalTime.of(10, 0)))
        val rearm = RearmRemindersUseCase(repository, scheduler, clockAt("06:00"))

        assertEquals(day.atTime(10, 0), rearm(profile))
    }

    @Test
    fun `a medication archived from today is no longer armed`() = runBlocking {
        val a = add("Medication A", daily(seven30))
        ArchiveMedicationUseCase(repository)(a.id, day)
        val rearm = RearmRemindersUseCase(repository, scheduler, clockAt("06:00"))

        assertNull(rearm(profile))
        assertEquals(1, scheduler.cancelled)
    }

    @Test
    fun `a medication archived from tomorrow is still armed today`() = runBlocking {
        val a = add("Medication A", daily(seven30))
        ArchiveMedicationUseCase(repository)(a.id, day.plusDays(1))
        val rearm = RearmRemindersUseCase(repository, scheduler, clockAt("06:00"))

        assertEquals(day.atTime(seven30), rearm(profile))
    }

    // --- TakeAllForSlotUseCase

    private fun takeAll(now: String): TakeAllForSlotUseCase {
        val clock = clockAt(now)
        return TakeAllForSlotUseCase(
            GetPillboxDayUseCase(repository, clock),
            RecordSlotIntakesUseCase(RecordIntakeUseCase(repository)),
            clock
        )
    }

    @Test
    fun `taken all records every open intake of the slot with the current time`() = runBlocking {
        val a = add("Medication A", daily(seven30))
        val b = add("Medication B", daily(seven30))
        val other = add("Medication C", daily(LocalTime.of(19, 30)))

        val recorded = takeAll("07:31")(profile, day.atTime(seven30))

        assertEquals(setOf(a.id, b.id), recorded.map { it.medicationId }.toSet())
        assertTrue(recorded.all { it.status == IntakeStatus.TAKEN })
        assertEquals(clockAt("07:31").instant(), recorded.first().takenAt)
        assertTrue(repository.intakes.none { it.medicationId == other.id })
    }

    @Test
    fun `taken all never overwrites an existing outcome`() = runBlocking {
        val a = add("Medication A", daily(seven30))
        val b = add("Medication B", daily(seven30))
        RecordIntakeUseCase(repository)(a.id, day.atTime(seven30), IntakeStatus.SKIPPED)

        val recorded = takeAll("07:31")(profile, day.atTime(seven30))

        assertEquals(listOf(b.id), recorded.map { it.medicationId })
        assertEquals(IntakeStatus.SKIPPED, repository.intakes.single { it.medicationId == a.id }.status)
    }

    @Test
    fun `taken all leaves out a medication archived after the alarm was set`() = runBlocking {
        val a = add("Medication A", daily(seven30))
        val b = add("Medication B", daily(seven30))
        ArchiveMedicationUseCase(repository)(a.id, day)

        val recorded = takeAll("07:31")(profile, day.atTime(seven30))

        assertEquals(listOf(b.id), recorded.map { it.medicationId })
    }

    @Test
    fun `taken all does nothing when the slot is already done`() = runBlocking {
        val a = add("Medication A", daily(seven30))
        RecordIntakeUseCase(repository)(a.id, day.atTime(seven30), IntakeStatus.TAKEN)

        assertTrue(takeAll("07:31")(profile, day.atTime(seven30)).isEmpty())
    }

    @Test
    fun `taken all does nothing for a time nothing is planned at`() = runBlocking {
        add("Medication A", daily(seven30))

        assertTrue(takeAll("07:31")(profile, day.atTime(12, 0)).isEmpty())
        assertTrue(repository.intakes.isEmpty())
    }

    @Test
    fun `slotAt returns only the slot of that time`() = runBlocking {
        add("Medication A", daily(seven30, LocalTime.of(19, 30)))

        val slot = takeAll("07:31").slotAt(profile, day.atTime(19, 30))

        assertEquals(day.atTime(19, 30), slot?.time)
        assertEquals(1, slot?.items?.size)
    }
}
