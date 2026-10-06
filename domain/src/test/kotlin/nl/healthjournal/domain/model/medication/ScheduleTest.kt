package nl.healthjournal.domain.model.medication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ScheduleTest {
    private val start = LocalDate.of(2026, 3, 2) // a Monday
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)

    private fun daily(times: List<LocalTime> = listOf(morning), end: LocalDate? = null) =
        Schedule.Recurring(times, DayPattern.EVERY_DAY, start, end)

    @Test
    fun `weekdays schedule is due only on the selected days`() {
        val schedule = Schedule.Recurring(
            listOf(morning),
            DayPattern.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
            start
        )
        assertEquals(listOf(start.atTime(morning)), schedule.plannedFor(start))
        assertTrue(schedule.plannedFor(start.plusDays(1)).isEmpty())
        assertEquals(listOf(start.plusDays(3).atTime(morning)), schedule.plannedFor(start.plusDays(3)))
    }

    @Test
    fun `every N days counts from the start date`() {
        val schedule = Schedule.Recurring(listOf(morning), DayPattern.EveryNDays(3), start)
        assertEquals(1, schedule.plannedFor(start).size)
        assertTrue(schedule.plannedFor(start.plusDays(1)).isEmpty())
        assertTrue(schedule.plannedFor(start.plusDays(2)).isEmpty())
        assertEquals(1, schedule.plannedFor(start.plusDays(3)).size)
        assertEquals(1, schedule.plannedFor(start.plusDays(6)).size)
    }

    @Test
    fun `nothing is planned before the start or after the end`() {
        val schedule = daily(end = start.plusDays(2))
        assertTrue(schedule.plannedFor(start.minusDays(1)).isEmpty())
        assertEquals(1, schedule.plannedFor(start).size)
        assertEquals(1, schedule.plannedFor(start.plusDays(2)).size)
        assertTrue(schedule.plannedFor(start.plusDays(3)).isEmpty())
    }

    @Test
    fun `several times a day are planned in ascending order`() {
        val schedule = daily(listOf(morning, evening))
        assertEquals(listOf(start.atTime(morning), start.atTime(evening)), schedule.plannedFor(start))
    }

    @Test
    fun `as needed plans nothing`() {
        assertTrue(Schedule.AsNeeded.plannedFor(start).isEmpty())
    }

    @Test
    fun `of sorts and removes duplicate times`() {
        val schedule = Schedule.Recurring.of(listOf(evening, morning, evening), DayPattern.EVERY_DAY, start)
        assertEquals(listOf(morning, evening), schedule.times)
    }

    @Test
    fun `invalid schedules are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { daily(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { daily(listOf(evening, morning)) }
        assertThrows(IllegalArgumentException::class.java) { daily(listOf(morning, morning)) }
        assertThrows(IllegalArgumentException::class.java) { daily(end = start.minusDays(1)) }
        assertThrows(IllegalArgumentException::class.java) { DayPattern.Weekdays(emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { DayPattern.EveryNDays(0) }
        assertThrows(IllegalArgumentException::class.java) { DayPattern.EveryNDays(366) }
    }

    @Test
    fun `eight times a day is accepted and nine is rejected`() {
        daily((0 until 8).map { LocalTime.of(it + 6, 0) })
        assertThrows(IllegalArgumentException::class.java) {
            daily((0 until 9).map { LocalTime.of(it + 6, 0) })
        }
    }

    @Test
    fun `a planned time inside the daylight saving gap resolves to the next valid instant`() {
        val zone = ZoneId.of("Europe/Amsterdam")
        val gap = LocalDate.of(2026, 3, 29).atTime(2, 30) // clocks go from 02:00 to 03:00
        assertEquals(LocalDate.of(2026, 3, 29).atTime(3, 30).atZone(zone).toInstant(), gap.resolveIn(zone))
    }

    @Test
    fun `a planned time on a daylight saving day is still planned once`() {
        val schedule = Schedule.Recurring(listOf(LocalTime.of(2, 30)), DayPattern.EVERY_DAY, start)
        assertEquals(1, schedule.plannedFor(LocalDate.of(2026, 3, 29)).size)
        assertEquals(1, schedule.plannedFor(LocalDate.of(2026, 10, 25)).size)
    }

    @Test
    fun `version in force is the latest one not after the date`() {
        val first = ScheduleVersion(start, daily(listOf(morning)))
        val second = ScheduleVersion(start.plusDays(10), daily(listOf(evening)))
        val medication = medication("Medication A", listOf(first, second))

        assertNull(medication.scheduleInForce(start.minusDays(1)))
        assertEquals(first, medication.scheduleInForce(start))
        assertEquals(first, medication.scheduleInForce(start.plusDays(9)))
        assertEquals(second, medication.scheduleInForce(start.plusDays(10)))
        assertEquals(second, medication.scheduleInForce(start.plusDays(50)))
    }

    @Test
    fun `an edit from today leaves earlier days unchanged`() {
        val today = start.plusDays(10)
        val before = medication("Medication A", listOf(ScheduleVersion(start, daily(listOf(morning)))))
        val after = before.withScheduleFrom(today, daily(listOf(evening)))

        assertEquals(before.plannedFor(today.minusDays(1)), after.plannedFor(today.minusDays(1)))
        assertEquals(listOf(today.atTime(evening)), after.plannedFor(today))
        assertEquals(2, after.schedules.size)
    }

    @Test
    fun `an edit from an earlier date changes the derived past planned intakes`() {
        val before = medication("Medication A", listOf(ScheduleVersion(start, daily(listOf(morning)))))
        val after = before.withScheduleFrom(start.plusDays(5), daily(listOf(evening)))

        assertEquals(listOf(start.plusDays(6).atTime(evening)), after.plannedFor(start.plusDays(6)))
        assertEquals(listOf(start.plusDays(4).atTime(morning)), after.plannedFor(start.plusDays(4)))
    }

    @Test
    fun `a version on an existing date replaces it and versions stay ordered`() {
        val base = medication("Medication A", listOf(ScheduleVersion(start, daily(listOf(morning)))))
        val later = base.withScheduleFrom(start.plusDays(10), daily(listOf(evening)))
        val earlier = later.withScheduleFrom(start.plusDays(5), daily(listOf(morning, evening)))
        val replaced = earlier.withScheduleFrom(start.plusDays(5), Schedule.AsNeeded)

        assertEquals(listOf(start, start.plusDays(5), start.plusDays(10)), replaced.schedules.map { it.effectiveFrom })
        assertEquals(Schedule.AsNeeded, replaced.schedules[1].schedule)
    }

    @Test
    fun `archiving stops planned intakes from that date and keeps earlier days`() {
        val archived = medication("Medication A", listOf(ScheduleVersion(start, daily()))).archivedOn(start.plusDays(5))
        assertEquals(1, archived.plannedFor(start.plusDays(4)).size)
        assertTrue(archived.plannedFor(start.plusDays(5)).isEmpty())
        assertTrue(archived.plannedFor(start.plusDays(6)).isEmpty())
    }

    @Test
    fun `a medication needs ordered versions`() {
        assertThrows(IllegalArgumentException::class.java) { medication("Medication A", emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            medication(
                "Medication A",
                listOf(ScheduleVersion(start.plusDays(1), daily()), ScheduleVersion(start, daily()))
            )
        }
    }
}
