package nl.healthjournal.app.ui.history.charts

import java.time.Instant
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** The look-back periods of the change table, counted back from the newest weight entry. */
enum class ChangePeriod(val days: Long? = null, val months: Long? = null) {
    WEEK(days = 7),
    FIFTEEN_DAYS(days = 15),
    MONTH(months = 1),
    THREE_MONTHS(months = 3),
    SIX_MONTHS(months = 6),
    YEAR(months = 12)
}

/** One weight measurement in kilograms. */
data class WeightPoint(val instant: Instant, val kg: Double)

/** The change over a period: [kg] newest minus start, [percent] of the start weight, both also per week over the real days between the two entries. */
data class PeriodChange(val kg: Double, val percent: Double, val kgPerWeek: Double, val percentPerWeek: Double)

/**
 * Statistics over all weight entries, in kilograms. Pure, so it is tested on the JVM; the screen converts
 * to the display unit. Several entries on one day: the last of that day counts.
 */
class WeightStatistics(points: List<WeightPoint>, zone: ZoneId) {

    private val sorted = points.sortedBy { it.instant }
    private val dated = sorted.map { it.instant.atZone(zone).toLocalDate() to it.kg }

    /** The lowest weight; on a tie the newest. */
    val lowest: WeightPoint? = sorted.withIndex().minWithOrNull(compareBy({ it.value.kg }, { -it.index }))?.value

    /** The highest weight; on a tie the newest. */
    val highest: WeightPoint? = sorted.withIndex().maxWithOrNull(compareBy({ it.value.kg }, { it.index }))?.value

    /** Change over [period], or null when the history does not reach back that far. */
    fun changeOver(period: ChangePeriod): PeriodChange? {
        if (dated.size < 2) return null
        val (newestDate, newest) = dated.last()
        val startDate = if (period.days != null) newestDate.minusDays(period.days) else newestDate.minusMonths(period.months!!)
        val start = dated.lastOrNull { it.first <= startDate } ?: return null
        val delta = newest - start.second
        val weeks = ChronoUnit.DAYS.between(start.first, newestDate) / 7.0
        val percent = if (start.second != 0.0) delta / start.second * 100 else 0.0
        return PeriodChange(delta, percent, delta / weeks, percent / weeks)
    }

    /** Years that have entries, oldest first. */
    val years: List<Int> = dated.map { it.first.year }.distinct()

    private val lastOfMonth: Map<YearMonth, Double> =
        linkedMapOf<YearMonth, Double>().also { m -> dated.forEach { m[YearMonth.from(it.first)] = it.second } }

    private val lastOfYear: Map<Int, Double> =
        linkedMapOf<Int, Double>().also { m -> dated.forEach { m[it.first.year] = it.second } }

    /** Per month of [year]: last weight of the month minus last weight of the previous month with entries. No bar for the first month of data. */
    fun monthlyChange(year: Int): Map<Month, Double> {
        val months = lastOfMonth.keys.sorted()
        val result = linkedMapOf<Month, Double>()
        months.forEachIndexed { i, month ->
            if (i > 0 && month.year == year) result[month.month] = lastOfMonth.getValue(month) - lastOfMonth.getValue(months[i - 1])
        }
        return result
    }

    /** Per year: last weight of the year minus last weight of the previous year with entries. */
    fun yearlyChange(): Map<Int, Double> {
        val ys = lastOfYear.keys.sorted()
        return ys.drop(1).withIndex().associate { (i, y) -> y to lastOfYear.getValue(y) - lastOfYear.getValue(ys[i]) }
    }
}
