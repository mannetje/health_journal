package nl.healthjournal.domain.model.medication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class PillboxTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val day = LocalDate.of(2026, 5, 4)
    private val eight = LocalTime.of(8, 0)
    private val twenty = LocalTime.of(20, 0)

    private fun daily(vararg times: LocalTime) =
        Schedule.Recurring(times.toList(), DayPattern.EVERY_DAY, day.minusDays(30))

    private fun medicationA(vararg times: LocalTime) =
        medication("Medication A", listOf(ScheduleVersion(day.minusDays(30), daily(*times))))

    private fun medicationB(vararg times: LocalTime) =
        medication("Medication B", listOf(ScheduleVersion(day.minusDays(30), daily(*times))))

    private fun taken(medication: Medication, planned: LocalDateTime?, takenAt: Instant? = null) = Intake(
        id = IntakeId.generate(),
        medicationId = medication.id,
        planned = planned,
        status = IntakeStatus.TAKEN,
        takenAt = takenAt ?: planned?.atZone(zone)?.toInstant()
    )

    @Test
    fun `medications planned at the same time share one slot`() {
        val a = medicationA(eight)
        val b = medicationB(eight, twenty)
        val result = Pillbox.dayOf(day, listOf(b, a), emptyList(), day.atTime(7, 0), zone)

        assertEquals(listOf(day.atTime(eight), day.atTime(twenty)), result.slots.map { it.time })
        assertEquals(listOf("Medication A", "Medication B"), result.slots[0].items.map { it.medication.name.value })
        assertEquals(listOf("Medication B"), result.slots[1].items.map { it.medication.name.value })
    }

    @Test
    fun `an intake is pending until the grace period ends and then missed`() {
        val a = medicationA(eight)
        val planned = day.atTime(eight)

        assertEquals(PlannedStatus.PENDING, Pillbox.statusOf(planned, null, day.atTime(7, 0)))
        assertEquals(PlannedStatus.PENDING, Pillbox.statusOf(planned, null, day.atTime(9, 59)))
        assertEquals(PlannedStatus.MISSED, Pillbox.statusOf(planned, null, day.atTime(10, 0)))

        val result = Pillbox.dayOf(day, listOf(a), emptyList(), day.atTime(12, 0), zone)
        assertEquals(PlannedStatus.MISSED, result.slots.single().items.single().status)
    }

    @Test
    fun `a recorded outcome wins over the clock`() {
        val a = medicationA(eight)
        val planned = day.atTime(eight)
        val intake = taken(a, planned)
        val skipped = intake.copy(status = IntakeStatus.SKIPPED, takenAt = null)

        assertEquals(PlannedStatus.TAKEN, Pillbox.statusOf(planned, intake, day.atTime(23, 0)))
        assertEquals(PlannedStatus.SKIPPED, Pillbox.statusOf(planned, skipped, day.atTime(23, 0)))
        assertEquals(
            PlannedStatus.TAKEN,
            Pillbox.dayOf(day, listOf(a), listOf(intake), day.atTime(23, 0), zone).slots.single().items.single().status
        )
    }

    @Test
    fun `taken all acts on the open items only`() {
        val a = medicationA(eight)
        val b = medicationB(eight)
        val result = Pillbox.dayOf(day, listOf(a, b), listOf(taken(a, day.atTime(eight))), day.atTime(8, 30), zone)

        assertEquals(listOf("Medication B"), result.slots.single().openItems.map { it.medication.name.value })
    }

    @Test
    fun `an outcome whose planned time is no longer produced is kept as logged and not planned`() {
        val before = medicationA(eight)
        val orphan = taken(before, day.atTime(eight))
        val edited = before.withScheduleFrom(day.minusDays(1), daily(twenty))

        val result = Pillbox.dayOf(day, listOf(edited), listOf(orphan), day.atTime(23, 0), zone)

        assertEquals(listOf(day.atTime(twenty)), result.slots.map { it.time })
        assertEquals(PlannedStatus.MISSED, result.slots.single().items.single().status)
        assertEquals(listOf(orphan), result.logged)
    }

    @Test
    fun `two as needed doses on one day are both logged`() {
        val a = medication("Medication A", listOf(ScheduleVersion(day.minusDays(30), Schedule.AsNeeded)))
        val first = taken(a, null, day.atTime(9, 15).atZone(zone).toInstant())
        val second = taken(a, null, day.atTime(15, 40).atZone(zone).toInstant())
        val otherDay = taken(a, null, day.plusDays(1).atTime(9, 0).atZone(zone).toInstant())

        val result = Pillbox.dayOf(day, listOf(a), listOf(second, otherDay, first), day.atTime(18, 0), zone)

        assertTrue(result.slots.isEmpty())
        assertEquals(listOf(first, second), result.logged)
    }

    @Test
    fun `intakes of unknown medications are ignored`() {
        val a = medicationA(eight)
        val stranger = taken(medicationB(eight), day.atTime(eight))
        val result = Pillbox.dayOf(day, listOf(a), listOf(stranger), day.atTime(8, 30), zone)

        assertTrue(result.logged.isEmpty())
        assertEquals(PlannedStatus.PENDING, result.slots.single().items.single().status)
    }

    @Test
    fun `an archived medication shows its earlier days and none after`() {
        val a = medicationA(eight).archivedOn(day)
        assertTrue(Pillbox.dayOf(day, listOf(a), emptyList(), day.atTime(23, 0), zone).slots.isEmpty())
        assertEquals(
            1,
            Pillbox.dayOf(day.minusDays(1), listOf(a), emptyList(), day.atTime(23, 0), zone).slots.size
        )
    }
}
