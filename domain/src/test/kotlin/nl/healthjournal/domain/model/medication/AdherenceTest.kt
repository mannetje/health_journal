package nl.healthjournal.domain.model.medication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AdherenceTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val start = LocalDate.of(2026, 1, 1)
    private val eight = LocalTime.of(8, 0)

    private fun daily(from: LocalDate = start, end: LocalDate? = null) =
        Schedule.Recurring(listOf(eight), DayPattern.EVERY_DAY, from, end)

    private fun med(name: String, vararg versions: ScheduleVersion) = medication(name, versions.toList())

    private fun at(date: String, time: String = "08:00") = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time))

    private fun outcome(m: Medication, planned: LocalDateTime, status: IntakeStatus = IntakeStatus.TAKEN) = Intake(
        id = IntakeId.generate(),
        medicationId = m.id,
        planned = planned,
        status = status,
        takenAt = if (status == IntakeStatus.TAKEN) planned.atZone(zone).toInstant() else null
    )

    private fun report(
        medications: List<Medication>,
        intakes: List<Intake>,
        now: LocalDateTime,
        days: Int = 30
    ) = Adherence.report(medications, intakes, days, now, zone)

    @Test
    fun `26 of 28 due is 93 percent`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-01", "12:00")
        val days = generateSequence(LocalDate.of(2026, 1, 5)) { it.plusDays(1) }.take(28).toList()
        val intakes = days.take(26).map { outcome(a, it.atTime(eight)) }
        val r = report(listOf(a), intakes, now, days = 28)
        assertEquals(28, r.rows.single().counts.due)
        assertEquals(26, r.rows.single().counts.taken)
        assertEquals(93, r.rows.single().counts.percentage)
    }

    @Test
    fun `rounds half up`() {
        assertEquals(88, Adherence.percent(7, 8))
        assertEquals(13, Adherence.percent(1, 8))
        assertEquals(67, Adherence.percent(2, 3))
        assertEquals(100, Adherence.percent(5, 5))
        assertEquals(0, Adherence.percent(0, 5))
    }

    @Test
    fun `nothing due shows no percentage`() {
        assertNull(Adherence.percent(0, 0))
        val a = med("Medication A", ScheduleVersion(LocalDate.of(2026, 3, 1), daily(LocalDate.of(2026, 3, 1))))
        val r = report(listOf(a), emptyList(), at("2026-02-01"))
        assertTrue(r.rows.isEmpty())
        assertNull(r.overall)
    }

    @Test
    fun `skipped counts as due and not taken, shown apart from missed`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "12:00")
        val intakes = listOf(
            outcome(a, at("2026-02-08")),
            outcome(a, at("2026-02-09"), IntakeStatus.SKIPPED)
        )
        val c = report(listOf(a), intakes, now, days = 3).rows.single().counts
        assertEquals(1, c.taken)
        assertEquals(1, c.skipped)
        assertEquals(1, c.missed)
        assertEquals(3, c.due)
        assertEquals(33, c.percentage)
    }

    @Test
    fun `an intake inside the grace period is neither missed nor due`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "09:30")
        val r = report(listOf(a), emptyList(), now, days = 1)
        assertTrue(r.rows.isEmpty())
        assertTrue(r.missed.isEmpty())
    }

    @Test
    fun `after the grace period it is missed and listed newest first`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "10:00")
        val r = report(listOf(a), emptyList(), now, days = 2)
        assertEquals(2, r.rows.single().counts.missed)
        assertEquals(listOf(at("2026-02-10"), at("2026-02-09")), r.missed.map { it.planned })
    }

    @Test
    fun `as-needed shows doses and no percentage`() {
        val a = med("Medication A", ScheduleVersion(start, Schedule.AsNeeded))
        val now = at("2026-02-10", "12:00")
        val dose = Intake(IntakeId.generate(), a.id, null, IntakeStatus.TAKEN, at("2026-02-09", "21:00").atZone(zone).toInstant())
        val old = Intake(IntakeId.generate(), a.id, null, IntakeStatus.TAKEN, at("2025-12-01", "21:00").atZone(zone).toInstant())
        val row = report(listOf(a), listOf(dose, old), now, days = 7).rows.single()
        assertEquals(1, row.counts.asNeededDoses)
        assertEquals(0, row.counts.due)
        assertNull(row.counts.percentage)
    }

    @Test
    fun `days before the start date are not due`() {
        val a = med("Medication A", ScheduleVersion(LocalDate.of(2026, 2, 8), daily(LocalDate.of(2026, 2, 8))))
        val c = report(listOf(a), emptyList(), at("2026-02-10", "12:00"), days = 7).rows.single().counts
        assertEquals(3, c.due)
    }

    @Test
    fun `days after the end date are not due`() {
        val a = med("Medication A", ScheduleVersion(start, daily(end = LocalDate.of(2026, 2, 7))))
        val c = report(listOf(a), emptyList(), at("2026-02-10", "12:00"), days = 7).rows.single().counts
        assertEquals(4, c.due)
    }

    @Test
    fun `days from the archive date are not due`() {
        val a = med("Medication A", ScheduleVersion(start, daily())).archivedOn(LocalDate.of(2026, 2, 8))
        val c = report(listOf(a), emptyList(), at("2026-02-10", "12:00"), days = 7).rows.single().counts
        assertEquals(4, c.due)
    }

    @Test
    fun `an edit from today leaves earlier days unchanged`() {
        val before = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "12:00")
        val twice = Schedule.Recurring(listOf(eight, LocalTime.of(20, 0)), DayPattern.EVERY_DAY, start)
        val edited = before.withScheduleFrom(LocalDate.of(2026, 2, 10), twice)
        val dueBefore = report(listOf(before), emptyList(), now, days = 4).rows.single().counts.due
        val dueAfter = report(listOf(edited), emptyList(), now, days = 4).rows.single().counts.due
        // Days 7 to 9 stay at one intake each; today has two planned, of which only the 08:00 is past its time.
        assertEquals(4, dueBefore)
        assertEquals(4, dueAfter)
    }

    @Test
    fun `an edit from an earlier date changes the days from that date`() {
        val before = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "12:00")
        val twice = Schedule.Recurring(listOf(eight, LocalTime.of(20, 0)), DayPattern.EVERY_DAY, start)
        val corrected = before.withScheduleFrom(LocalDate.of(2026, 2, 8), twice)
        val c = report(listOf(corrected), emptyList(), now, days = 4).rows.single().counts
        // 7th: 1, 8th: 2, 9th: 2, 10th: 1 (the 20:00 is not past yet)
        assertEquals(6, c.due)
    }

    @Test
    fun `an outcome without a plan is not due and not counted`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val orphan = outcome(a, at("2026-02-09", "13:00"))
        val c = report(listOf(a), listOf(orphan), at("2026-02-10", "12:00"), days = 3).rows.single().counts
        assertEquals(0, c.taken)
        assertEquals(3, c.due)
    }

    @Test
    fun `changing a past outcome changes the figures`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "12:00")
        val missed = report(listOf(a), emptyList(), now, days = 1).rows.single().counts
        val corrected = report(listOf(a), listOf(outcome(a, at("2026-02-10"))), now, days = 1).rows.single().counts
        assertEquals(0, missed.percentage)
        assertEquals(100, corrected.percentage)
    }

    @Test
    fun `overall adds up the medications`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val b = med("Medication B", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "12:00")
        val r = report(listOf(a, b), listOf(outcome(a, at("2026-02-10"))), now, days = 1)
        assertEquals(1, r.overall!!.taken)
        assertEquals(2, r.overall.due)
        assertEquals(50, r.overall.percentage)
        assertEquals(listOf("Medication A", "Medication B"), r.rows.map { it.medication.name.value })
    }

    @Test
    fun `streak counts days in a row with everything taken`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "12:00")
        val intakes = listOf("2026-02-10", "2026-02-09", "2026-02-08").map { outcome(a, at(it)) }
        assertEquals(3, report(listOf(a), intakes, now).streak)
    }

    @Test
    fun `a missed day ends the streak`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "12:00")
        val intakes = listOf("2026-02-10", "2026-02-08", "2026-02-07").map { outcome(a, at(it)) }
        assertEquals(1, report(listOf(a), intakes, now).streak)
    }

    @Test
    fun `a skipped intake ends the streak`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "12:00")
        val intakes = listOf(outcome(a, at("2026-02-10")), outcome(a, at("2026-02-09"), IntakeStatus.SKIPPED))
        assertEquals(1, report(listOf(a), intakes, now).streak)
    }

    @Test
    fun `today still waiting for an outcome neither extends nor breaks the streak`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "09:00")
        val intakes = listOf("2026-02-09", "2026-02-08").map { outcome(a, at(it)) }
        assertEquals(2, report(listOf(a), intakes, now).streak)
    }

    @Test
    fun `days with nothing planned neither extend nor break the streak`() {
        val every3 = Schedule.Recurring(listOf(eight), DayPattern.EveryNDays(3), start)
        val a = med("Medication A", ScheduleVersion(start, every3))
        // Planned every third day from 1 January: ... 3 Feb, 6 Feb, 9 Feb. The days in between have nothing planned.
        val planned = generateSequence(start) { it.plusDays(3) }.takeWhile { it <= LocalDate.of(2026, 2, 10) }.toList()
        val now = at("2026-02-10", "12:00")
        val intakes = planned.takeLast(3).map { outcome(a, it.atTime(eight)) }
        assertEquals(3, report(listOf(a), intakes, now).streak)
    }

    @Test
    fun `the streak does not depend on the range`() {
        val a = med("Medication A", ScheduleVersion(start, daily()))
        val now = at("2026-02-10", "12:00")
        val intakes = listOf("2026-02-10", "2026-02-09", "2026-02-08").map { outcome(a, at(it)) }
        assertEquals(report(listOf(a), intakes, now, days = 7).streak, report(listOf(a), intakes, now, days = 90).streak)
    }
}
