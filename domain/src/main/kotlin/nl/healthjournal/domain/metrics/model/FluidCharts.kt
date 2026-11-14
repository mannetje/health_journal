package nl.healthjournal.domain.metrics.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The average total of the logged days of each weekday, in whole ml (round half up). Weekdays without a day are absent. */
object FluidWeekdayAverages {
    fun of(days: List<DayTotal>, firstDay: DayOfWeek): List<Pair<DayOfWeek, Int>> {
        val byDay = days.groupBy { it.date.dayOfWeek }
        return (0..6).map { firstDay.plus(it.toLong()) }.mapNotNull { weekday ->
            byDay[weekday]?.let { list -> weekday to (list.sumOf { it.ml } + list.size / 2) / list.size }
        }
    }
}

/**
 * The average ml per logged day in each of the eight three-hour blocks (block 0 is 00 to 03). Blocks without
 * entries are absent. It shows when entries were made and says nothing about when to drink.
 */
object FluidTimeOfDay {
    const val BLOCKS = 8
    const val BLOCK_HOURS = 3

    fun of(entries: List<FluidIntakeEntry>, zone: ZoneId): List<Pair<Int, Int>> {
        if (entries.isEmpty()) return emptyList()
        val days = entries.map { it.timestamp.atZone(zone).toLocalDate() }.toSet().size
        val perBlock = entries.groupBy { it.timestamp.atZone(zone).hour / BLOCK_HOURS }
            .mapValues { (_, list) -> list.sumOf { it.amount.value } }
        return (0 until BLOCKS).mapNotNull { block ->
            perBlock[block]?.let { block to (it + days / 2) / days }
        }
    }
}

/** One cell of the calendar: a day of the month that has entries, with its total and position. */
data class FluidCalendarDay(val date: LocalDate, val ml: Int, val position: FluidPosition)

/** The days of one month that have entries. The grid and the weekday offsets are the screen's business. */
object FluidCalendarMonth {
    fun of(days: List<DayTotal>, month: YearMonth, limits: FluidLimits): List<FluidCalendarDay> =
        days.filter { YearMonth.from(it.date) == month }
            .map { FluidCalendarDay(it.date, it.ml, fluidPosition(it.ml, limits)) }
            .sortedBy { it.date }

    /**
     * Shade step 1 to [steps] in proportion to the day's total against the highest day of the month; the number is
     * always printed, so shade is never the only signal.
     */
    fun shadeStep(ml: Int, highestMl: Int, steps: Int = 4): Int =
        if (highestMl <= 0) 1 else ((ml.toLong() * steps + highestMl - 1) / highestMl).toInt().coerceIn(1, steps)
}
