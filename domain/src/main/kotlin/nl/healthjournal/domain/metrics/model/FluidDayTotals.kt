package nl.healthjournal.domain.metrics.model

import java.time.LocalDate
import java.time.ZoneId

/** The total of one local calendar day. */
data class DayTotal(val date: LocalDate, val ml: Int)

object FluidDayTotals {
    /** One total per local day that has at least one entry, oldest first. Days without entries are not returned. */
    fun of(entries: List<FluidIntakeEntry>, zone: ZoneId): List<DayTotal> =
        entries.groupBy { it.timestamp.atZone(zone).toLocalDate() }
            .map { (date, list) -> DayTotal(date, list.sumOf { it.amount.value }) }
            .sortedBy { it.date }

    /** The total of [date] alone, zero when there is none. */
    fun forDay(entries: List<FluidIntakeEntry>, date: LocalDate, zone: ZoneId): Int =
        entries.filter { it.timestamp.atZone(zone).toLocalDate() == date }.sumOf { it.amount.value }
}

/**
 * Plain figures over day totals. A day without entries is unknown, not zero, so it takes no part in the average.
 * [below], [within] and [above] count logged days against the person's own numbers and are zero without any.
 */
data class FluidStatistics(
    val averageMl: Int,
    val highest: DayTotal,
    val daysLogged: Int,
    val below: Int,
    val within: Int,
    val above: Int
) {
    companion object {
        /** Null for no logged day. The average is whole ml, rounded half up. */
        fun of(days: List<DayTotal>, limits: FluidLimits = FluidLimits.NONE): FluidStatistics? {
            if (days.isEmpty()) return null
            val sum = days.sumOf { it.ml }
            val average = (sum + days.size / 2) / days.size
            val positions = days.map { fluidPosition(it.ml, limits) }
            return FluidStatistics(
                averageMl = average,
                highest = days.maxWith(compareBy<DayTotal> { it.ml }.thenByDescending { it.date }),
                daysLogged = days.size,
                below = positions.count { it == FluidPosition.BELOW_MIN },
                within = positions.count { it == FluidPosition.WITHIN },
                above = positions.count { it == FluidPosition.ABOVE_MAX }
            )
        }
    }
}
