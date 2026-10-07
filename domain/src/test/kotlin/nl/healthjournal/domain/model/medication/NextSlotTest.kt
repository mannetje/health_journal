package nl.healthjournal.domain.model.medication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class NextSlotTest {
    private val start = LocalDate.of(2026, 1, 1)

    private fun daily(vararg times: LocalTime, from: LocalDate = start) =
        Schedule.Recurring(times.toList(), DayPattern.EVERY_DAY, from)

    private fun med(name: String, schedule: Schedule, from: LocalDate = start) =
        medication(name, listOf(ScheduleVersion(from, schedule)))

    private fun at(date: String, time: String) = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time))

    @Test
    fun `no medications means no slot`() {
        assertNull(NextSlot.after(emptyList(), at("2026-05-04", "07:00")))
    }

    @Test
    fun `an as-needed medication adds no slot`() {
        assertNull(NextSlot.after(listOf(med("Medication A", Schedule.AsNeeded)), at("2026-05-04", "07:00")))
    }

    @Test
    fun `picks the next time later the same day`() {
        val a = med("Medication A", daily(LocalTime.of(8, 0), LocalTime.of(20, 0)))
        assertEquals(at("2026-05-04", "20:00"), NextSlot.after(listOf(a), at("2026-05-04", "08:00")))
    }

    @Test
    fun `is strictly after the given moment`() {
        val a = med("Medication A", daily(LocalTime.of(8, 0)))
        assertEquals(at("2026-05-05", "08:00"), NextSlot.after(listOf(a), at("2026-05-04", "08:00")))
        assertEquals(at("2026-05-04", "08:00"), NextSlot.after(listOf(a), at("2026-05-04", "07:59:59")))
    }

    @Test
    fun `crosses midnight to the first time of the next day`() {
        val a = med("Medication A", daily(LocalTime.of(8, 0)))
        assertEquals(at("2026-05-05", "08:00"), NextSlot.after(listOf(a), at("2026-05-04", "23:59")))
    }

    @Test
    fun `several medications at one time give one slot`() {
        val a = med("Medication A", daily(LocalTime.of(7, 30)))
        val b = med("Medication B", daily(LocalTime.of(7, 30)))
        val c = med("Medication C", daily(LocalTime.of(19, 30)))
        assertEquals(at("2026-05-04", "07:30"), NextSlot.after(listOf(a, b, c), at("2026-05-04", "06:00")))
    }

    @Test
    fun `the earliest time over all medications wins`() {
        val late = med("Medication A", daily(LocalTime.of(21, 0)))
        val early = med("Medication B", daily(LocalTime.of(9, 15)))
        assertEquals(at("2026-05-04", "09:15"), NextSlot.after(listOf(late, early), at("2026-05-04", "06:00")))
    }

    @Test
    fun `skips days the weekday pattern does not include`() {
        val mondays = Schedule.Recurring(listOf(LocalTime.of(8, 0)), DayPattern.Weekdays(setOf(DayOfWeek.MONDAY)), start)
        // 2026-05-04 is a Monday; after its 08:00 the next one is a week later.
        assertEquals(at("2026-05-11", "08:00"), NextSlot.after(listOf(med("Medication A", mondays)), at("2026-05-04", "08:00")))
    }

    @Test
    fun `follows an every-n-days interval, even the longest`() {
        val yearly = Schedule.Recurring(listOf(LocalTime.of(8, 0)), DayPattern.EveryNDays(365), start)
        assertEquals(at("2027-01-01", "08:00"), NextSlot.after(listOf(med("Medication A", yearly)), at("2026-01-01", "09:00")))
    }

    @Test
    fun `uses the schedule version that is in force on each date`() {
        val morning = daily(LocalTime.of(8, 0))
        val evening = daily(LocalTime.of(20, 0))
        val changed = medication(
            "Medication A",
            listOf(ScheduleVersion(start, morning), ScheduleVersion(LocalDate.of(2026, 5, 6), evening))
        )
        assertEquals(at("2026-05-05", "08:00"), NextSlot.after(listOf(changed), at("2026-05-04", "09:00")))
        // From 6 May the evening version applies, so the 08:00 of that day is gone.
        assertEquals(at("2026-05-06", "20:00"), NextSlot.after(listOf(changed), at("2026-05-05", "09:00")))
    }

    @Test
    fun `a schedule that starts in the future is found`() {
        val later = med("Medication A", daily(LocalTime.of(8, 0), from = LocalDate.of(2026, 6, 1)))
        assertEquals(at("2026-06-01", "08:00"), NextSlot.after(listOf(later), at("2026-05-04", "09:00")))
    }

    @Test
    fun `an ended schedule gives no slot`() {
        val ended = Schedule.Recurring(listOf(LocalTime.of(8, 0)), DayPattern.EVERY_DAY, start, end = LocalDate.of(2026, 5, 4))
        assertNull(NextSlot.after(listOf(med("Medication A", ended)), at("2026-05-04", "09:00")))
    }

    @Test
    fun `an archived medication stops from its archive date`() {
        val archived = med("Medication A", daily(LocalTime.of(8, 0))).archivedOn(LocalDate.of(2026, 5, 5))
        assertEquals(at("2026-05-04", "08:00"), NextSlot.after(listOf(archived), at("2026-05-04", "07:00")))
        assertNull(NextSlot.after(listOf(archived), at("2026-05-04", "09:00")))
    }

    @Test
    fun `a time inside the spring-forward gap is still reported and resolves forward`() {
        val zone = ZoneId.of("Europe/Amsterdam")
        val a = med("Medication A", daily(LocalTime.of(2, 30)))
        // 2026-03-29: 02:00 jumps to 03:00 in Amsterdam, so 02:30 does not exist that day.
        val next = NextSlot.after(listOf(a), at("2026-03-29", "00:00"))
        assertEquals(at("2026-03-29", "02:30"), next)
        assertEquals(at("2026-03-29", "03:30").atZone(zone).toInstant(), next!!.resolveIn(zone))
    }

    @Test
    fun `the repeated hour when the clock goes back is one slot`() {
        val a = med("Medication A", daily(LocalTime.of(2, 30)))
        // 2026-10-25: 03:00 goes back to 02:00, so 02:30 happens twice; the planned local time is one slot.
        assertEquals(at("2026-10-25", "02:30"), NextSlot.after(listOf(a), at("2026-10-25", "00:00")))
        assertEquals(at("2026-10-26", "02:30"), NextSlot.after(listOf(a), at("2026-10-25", "02:30")))
    }
}
