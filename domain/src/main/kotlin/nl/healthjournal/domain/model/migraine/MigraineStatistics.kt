package nl.healthjournal.domain.model.migraine

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Plain counts over the person's own entries. Nothing is scored, ranked, compared or flagged, and no clock is read:
 * the range and zone are passed in. Entries are placed on the day they started.
 * The "helped me" ticks are deliberately not used here.
 */
object MigraineStatistics {
    data class Summary(
        val migraines: Int,
        val daysWithMigraine: Int,
        val daysWithSomethingTaken: Int,
        val migraineFreeDays: Int
    )

    data class TimelinePoint(val start: Instant, val intensity: Int?, val ongoing: Boolean)

    data class DurationBuckets(val under4h: Int, val from4To12h: Int, val over12h: Int, val ongoing: Int)

    data class IntensityBuckets(val low0To3: Int, val mid4To6: Int, val high7To10: Int, val missing: Int)

    data class TimeOfDayGrid(val counts: Map<Pair<DayOfWeek, TimeOfDay>, Int>, val wokeUpWithIt: Int) {
        fun count(day: DayOfWeek, time: TimeOfDay): Int = counts[day to time] ?: 0
    }

    /** Counts per code, per personal tag label, with entries without any of them left out. */
    data class LabelCounts<C>(val codes: Map<C, Int>, val personal: Map<String, Int>)

    private fun inRange(entry: MigraineEntry, range: ClosedRange<LocalDate>, zone: ZoneId) =
        entry.startDay(zone) in range

    fun summary(entries: List<MigraineEntry>, range: ClosedRange<LocalDate>, zone: ZoneId): Summary {
        val inside = entries.filter { inRange(it, range, zone) }
        val daysWith = inside.map { it.startDay(zone) }.toSet()
        val daysTaken = inside.filter { it.tookSomething }.map { it.startDay(zone) }.toSet()
        val totalDays = ChronoUnit.DAYS.between(range.start, range.endInclusive).toInt() + 1
        return Summary(
            migraines = inside.size,
            daysWithMigraine = daysWith.size,
            daysWithSomethingTaken = daysTaken.size,
            migraineFreeDays = maxOf(0, totalDays - daysWith.size)
        )
    }

    fun timeline(entries: List<MigraineEntry>, range: ClosedRange<LocalDate>, zone: ZoneId): List<TimelinePoint> =
        entries.filter { inRange(it, range, zone) }
            .sortedBy { it.start }
            .map { TimelinePoint(it.start, it.intensity, it.ongoing) }

    fun durationBuckets(entries: List<MigraineEntry>, range: ClosedRange<LocalDate>, zone: ZoneId): DurationBuckets {
        var under = 0
        var mid = 0
        var over = 0
        var ongoing = 0
        for (e in entries.filter { inRange(it, range, zone) }) {
            val d = e.duration
            when {
                d == null -> ongoing++
                d < Duration.ofHours(4) -> under++
                d <= Duration.ofHours(12) -> mid++
                else -> over++
            }
        }
        return DurationBuckets(under, mid, over, ongoing)
    }

    fun intensityBuckets(entries: List<MigraineEntry>, range: ClosedRange<LocalDate>, zone: ZoneId): IntensityBuckets {
        var low = 0
        var mid = 0
        var high = 0
        var missing = 0
        for (e in entries.filter { inRange(it, range, zone) }) {
            when (val i = e.intensity) {
                null -> missing++
                in 0..3 -> low++
                in 4..6 -> mid++
                else -> { require(i in 7..10); high++ }
            }
        }
        return IntensityBuckets(low, mid, high, missing)
    }

    /** Counts per weekday, in the order starting at [firstDayOfWeek]. */
    fun weekdayCounts(
        entries: List<MigraineEntry>,
        range: ClosedRange<LocalDate>,
        zone: ZoneId,
        firstDayOfWeek: DayOfWeek
    ): List<Pair<DayOfWeek, Int>> {
        val counts = entries.filter { inRange(it, range, zone) }.groupingBy { it.startWeekday(zone) }.eachCount()
        return (0L..6L).map { firstDayOfWeek.plus(it) }.map { it to (counts[it] ?: 0) }
    }

    /** Weekday by time of day. Entries noted as woke up with it have no known time and are counted aside. */
    fun timeOfDayGrid(entries: List<MigraineEntry>, range: ClosedRange<LocalDate>, zone: ZoneId): TimeOfDayGrid {
        val inside = entries.filter { inRange(it, range, zone) }
        val (woke, timed) = inside.partition { it.wokeUpWithIt }
        val counts = timed.groupingBy { it.startWeekday(zone) to it.startTimeOfDay(zone) }.eachCount()
        return TimeOfDayGrid(counts, woke.size)
    }

    private fun <C> labelCounts(
        entries: List<MigraineEntry>,
        range: ClosedRange<LocalDate>,
        zone: ZoneId,
        codes: (MigraineEntry) -> Set<C>,
        tagIds: (MigraineEntry) -> Set<MigraineTagId>,
        tags: List<MigraineTag>
    ): LabelCounts<C> {
        val labelById = tags.associate { it.id to it.label }
        val codeCounts = mutableMapOf<C, Int>()
        val personal = mutableMapOf<String, Int>()
        for (e in entries.filter { inRange(it, range, zone) }) {
            codes(e).forEach { codeCounts.merge(it, 1, Int::plus) }
            tagIds(e).mapNotNull { labelById[it] }.forEach { personal.merge(it, 1, Int::plus) }
        }
        return LabelCounts(codeCounts, personal)
    }

    fun symptomCounts(entries: List<MigraineEntry>, range: ClosedRange<LocalDate>, zone: ZoneId) =
        labelCounts(entries, range, zone, { it.symptoms }, { emptySet() }, emptyList())

    fun triggerCounts(entries: List<MigraineEntry>, range: ClosedRange<LocalDate>, zone: ZoneId, tags: List<MigraineTag>) =
        labelCounts(entries, range, zone, { it.triggers }, { it.customTriggerIds }, tags)

    fun reliefCounts(entries: List<MigraineEntry>, range: ClosedRange<LocalDate>, zone: ZoneId, tags: List<MigraineTag>) =
        labelCounts(entries, range, zone, { it.reliefMethods }, { it.customReliefIds }, tags)

    fun painCharacterCounts(entries: List<MigraineEntry>, range: ClosedRange<LocalDate>, zone: ZoneId) =
        labelCounts(entries, range, zone, { it.painCharacters }, { emptySet() }, emptyList())

    /** Number of migraines per day, only for days that have one. */
    fun dayCounts(entries: List<MigraineEntry>, range: ClosedRange<LocalDate>, zone: ZoneId): Map<LocalDate, Int> =
        entries.filter { inRange(it, range, zone) }.groupingBy { it.startDay(zone) }.eachCount()
}
