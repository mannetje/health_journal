package nl.healthjournal.domain.model.migraine

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MigraineStatisticsTest {
    private val profile = ProfileId.generate()
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val march = LocalDate.of(2026, 3, 1)..LocalDate.of(2026, 3, 31)

    private fun at(day: Int, hour: Int, minute: Int = 0): Instant =
        LocalDateTime.of(2026, 3, day, hour, minute).atZone(zone).toInstant()

    private fun entry(
        start: Instant,
        hours: Long? = null,
        intensity: Int? = null,
        tookSomething: Boolean = false,
        woke: Boolean = false,
        symptoms: Set<MigraineSymptom> = emptySet(),
        triggers: Set<MigraineTrigger> = emptySet(),
        customTriggers: Set<MigraineTagId> = emptySet(),
        relief: Set<ReliefMethod> = emptySet(),
        pain: Set<PainCharacter> = emptySet()
    ) = MigraineEntry(
        id = MeasurementId.generate(), profileId = profile, start = start, wokeUpWithIt = woke,
        end = hours?.let { start.plus(Duration.ofHours(it)) }, intensity = intensity, tookSomething = tookSomething,
        symptoms = symptoms, triggers = triggers, customTriggerIds = customTriggers, reliefMethods = relief,
        painCharacters = pain
    )

    @Test fun `empty range gives zeros and all days migraine free`() {
        val s = MigraineStatistics.summary(emptyList(), march, zone)
        assertEquals(MigraineStatistics.Summary(0, 0, 0, 31), s)
        assertTrue(MigraineStatistics.timeline(emptyList(), march, zone).isEmpty())
        assertEquals(0, MigraineStatistics.timeOfDayGrid(emptyList(), march, zone).wokeUpWithIt)
    }

    @Test fun `single entry`() {
        val s = MigraineStatistics.summary(listOf(entry(at(5, 10), tookSomething = true)), march, zone)
        assertEquals(MigraineStatistics.Summary(1, 1, 1, 30), s)
    }

    @Test fun `two on one day count as one day with migraine`() {
        val s = MigraineStatistics.summary(listOf(entry(at(5, 8)), entry(at(5, 20), tookSomething = true)), march, zone)
        assertEquals(2, s.migraines)
        assertEquals(1, s.daysWithMigraine)
        assertEquals(1, s.daysWithSomethingTaken)
        assertEquals(30, s.migraineFreeDays)
    }

    @Test fun `entries outside the range are left out and midnight belongs to the new day`() {
        val inside = entry(at(1, 0, 0))
        val before = entry(at(1, 0, 0).minusSeconds(1))
        val s = MigraineStatistics.summary(listOf(inside, before), march, zone)
        assertEquals(1, s.migraines)
    }

    @Test fun `ongoing entry is counted aside in durations and kept in the timeline`() {
        val ongoing = entry(at(3, 9))
        val b = MigraineStatistics.durationBuckets(listOf(ongoing), march, zone)
        assertEquals(MigraineStatistics.DurationBuckets(0, 0, 0, 1), b)
        assertTrue(MigraineStatistics.timeline(listOf(ongoing), march, zone).single().ongoing)
    }

    @Test fun `duration bucket boundaries`() {
        val list = listOf(
            entry(at(2, 8), hours = 3), entry(at(3, 8), hours = 4), entry(at(4, 8), hours = 12), entry(at(5, 8), hours = 13)
        )
        assertEquals(MigraineStatistics.DurationBuckets(1, 2, 1, 0), MigraineStatistics.durationBuckets(list, march, zone))
    }

    @Test fun `intensity bucket boundaries and missing aside`() {
        val list = listOf(0, 3, 4, 6, 7, 10).mapIndexed { i, v -> entry(at(2 + i, 8), intensity = v) } + entry(at(20, 8))
        assertEquals(MigraineStatistics.IntensityBuckets(2, 2, 2, 1), MigraineStatistics.intensityBuckets(list, march, zone))
    }

    @Test fun `weekday counts start at the chosen first day`() {
        val monday = entry(at(9, 10)) // 2026-03-09 is a Monday
        val sunday = entry(at(15, 10))
        val fromMonday = MigraineStatistics.weekdayCounts(listOf(monday, sunday), march, zone, DayOfWeek.MONDAY)
        assertEquals(DayOfWeek.MONDAY, fromMonday.first().first)
        assertEquals(DayOfWeek.SUNDAY, fromMonday.last().first)
        assertEquals(1, fromMonday.first().second)
        assertEquals(1, fromMonday.last().second)
        val fromSunday = MigraineStatistics.weekdayCounts(listOf(monday, sunday), march, zone, DayOfWeek.SUNDAY)
        assertEquals(DayOfWeek.SUNDAY, fromSunday.first().first)
        assertEquals(1, fromSunday.first().second)
        assertEquals(7, fromSunday.size)
    }

    @Test fun `time of day grid boundaries and woke up entries counted aside`() {
        val list = listOf(
            entry(at(9, 5, 59)), entry(at(9, 6, 0)), entry(at(9, 17, 59)), entry(at(9, 18, 0)),
            entry(at(9, 7), woke = true)
        )
        val grid = MigraineStatistics.timeOfDayGrid(list, march, zone)
        assertEquals(1, grid.count(DayOfWeek.MONDAY, TimeOfDay.NIGHT))
        assertEquals(1, grid.count(DayOfWeek.MONDAY, TimeOfDay.MORNING))
        assertEquals(1, grid.count(DayOfWeek.MONDAY, TimeOfDay.AFTERNOON))
        assertEquals(1, grid.count(DayOfWeek.MONDAY, TimeOfDay.EVENING))
        assertEquals(1, grid.wokeUpWithIt)
    }

    @Test fun `symptom and pain character counts`() {
        val list = listOf(
            entry(at(2, 8), symptoms = setOf(MigraineSymptom.NAUSEA, MigraineSymptom.AURA), pain = setOf(PainCharacter.THROBBING)),
            entry(at(3, 8), symptoms = setOf(MigraineSymptom.NAUSEA))
        )
        assertEquals(2, MigraineStatistics.symptomCounts(list, march, zone).codes[MigraineSymptom.NAUSEA])
        assertEquals(1, MigraineStatistics.symptomCounts(list, march, zone).codes[MigraineSymptom.AURA])
        assertEquals(1, MigraineStatistics.painCharacterCounts(list, march, zone).codes[PainCharacter.THROBBING])
    }

    @Test fun `trigger and relief counts include personal tags by label`() {
        val lunch = MigraineTag(MigraineTagId.generate(), profile, MigraineTagKind.TRIGGER, "skipped lunch")
        val walk = MigraineTag(MigraineTagId.generate(), profile, MigraineTagKind.RELIEF, "walk")
        val list = listOf(
            MigraineEntry(
                id = MeasurementId.generate(), profileId = profile, start = at(2, 8),
                triggers = setOf(MigraineTrigger.STRESS), customTriggerIds = setOf(lunch.id),
                reliefMethods = setOf(ReliefMethod.HEAT), customReliefIds = setOf(walk.id), reliefHelpedMe = true
            ),
            entry(at(3, 8), triggers = setOf(MigraineTrigger.STRESS))
        )
        val triggers = MigraineStatistics.triggerCounts(list, march, zone, listOf(lunch, walk))
        assertEquals(2, triggers.codes[MigraineTrigger.STRESS])
        assertEquals(1, triggers.personal["skipped lunch"])
        val relief = MigraineStatistics.reliefCounts(list, march, zone, listOf(lunch, walk))
        assertEquals(1, relief.codes[ReliefMethod.HEAT])
        assertEquals(1, relief.personal["walk"])
    }

    @Test fun `day counts for the calendar`() {
        val list = listOf(entry(at(5, 8)), entry(at(5, 20)), entry(at(6, 8)))
        val counts = MigraineStatistics.dayCounts(list, march, zone)
        assertEquals(2, counts[LocalDate.of(2026, 3, 5)])
        assertEquals(1, counts[LocalDate.of(2026, 3, 6)])
        assertEquals(2, counts.size)
    }

    @Test fun `days with something taken are marked by the start day`() {
        val list = listOf(
            entry(at(5, 8), tookSomething = true),
            entry(at(5, 20)),
            entry(at(6, 8)),
            entry(at(31, 23), tookSomething = true)
        )
        val taken = MigraineStatistics.daysWithSomethingTaken(list, march, zone)
        assertEquals(setOf(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 31)), taken)
        assertEquals(emptySet<LocalDate>(), MigraineStatistics.daysWithSomethingTaken(list, LocalDate.of(2026, 4, 1)..LocalDate.of(2026, 4, 30), zone))
    }
}
