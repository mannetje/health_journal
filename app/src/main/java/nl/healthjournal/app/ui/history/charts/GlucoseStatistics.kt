package nl.healthjournal.app.ui.history.charts

import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.sqrt

/** One glucose reading in mmol/L with its context and stored NHG category. */
data class GlucosePoint(val instant: Instant, val mmol: Double, val context: GlucoseContext, val category: NhgGlucoseCategory)

/** The change of a value over a period: [delta] newest minus start in mmol/L, [percent] of the start. */
data class GlucoseChange(val delta: Double, val percent: Double)

/** Average, sample standard deviation (null with fewer than two readings) and range of one context. */
data class GlucoseSpread(val count: Int, val average: Double, val standardDeviation: Double?, val lowest: Double, val highest: Double)

/** Readings of a period: how many, their average and the share in the normal category (both null when empty). */
data class GlucosePeriod(val count: Int, val average: Double?, val normalPercent: Double?)

/** Two consecutive readings less than [GlucoseStatistics.RISE_WINDOW_HOURS] hours apart; [delta] is the rise in mmol/L. */
data class GlucoseRise(val from: GlucosePoint, val to: GlucosePoint) {
    val delta: Double get() = to.mmol - from.mmol
}

/** A part of the day by local hour: night 0-6, morning 6-12, afternoon 12-18, evening 18-24. */
enum class TimeOfDay(val fromHour: Int) {
    NIGHT(0),
    MORNING(6),
    AFTERNOON(12),
    EVENING(18);

    companion object {
        fun of(hour: Int): TimeOfDay = entries.last { hour >= it.fromHour }
    }
}

/** The readings of one [TimeOfDay]; average, lowest and highest are null without readings. */
data class TimeOfDayStats(val timeOfDay: TimeOfDay, val count: Int, val average: Double?, val lowest: Double?, val highest: Double?)

/**
 * Statistics over glucose readings in mmol/L. Pure, so it is tested on the JVM; the screen converts to the
 * display unit. Fasting and post-meal readings are never mixed for averages or changes. Several readings of
 * one context on one day: the last of that day counts for the change figures.
 */
class GlucoseStatistics(points: List<GlucosePoint>, private val zone: ZoneId) {

    private val sorted = points.sortedBy { it.instant }

    val newest: GlucosePoint? = sorted.lastOrNull()

    /** The lowest reading; on a tie the newest. */
    val lowest: GlucosePoint? = sorted.withIndex().minWithOrNull(compareBy({ it.value.mmol }, { -it.index }))?.value

    /** The highest reading; on a tie the newest. */
    val highest: GlucosePoint? = sorted.withIndex().maxWithOrNull(compareBy({ it.value.mmol }, { it.index }))?.value

    private fun dated(context: GlucoseContext): List<Pair<LocalDate, Double>> =
        sorted.filter { it.context == context }.map { it.instant.atZone(zone).toLocalDate() to it.mmol }

    /** Change of the newest [context] reading over [period], or null when that context's history does not reach back that far. */
    fun changeOver(context: GlucoseContext, period: ChangePeriod): GlucoseChange? {
        val dated = dated(context)
        if (dated.size < 2) return null
        val (newestDate, newestValue) = dated.last()
        val startDate = if (period.days != null) newestDate.minusDays(period.days) else newestDate.minusMonths(period.months!!)
        val start = dated.lastOrNull { it.first <= startDate } ?: return null
        val delta = newestValue - start.second
        val percent = if (start.second != 0.0) delta / start.second * 100 else 0.0
        return GlucoseChange(delta, percent)
    }

    /** Average, standard deviation and range of [context], or null without readings. */
    fun spread(context: GlucoseContext): GlucoseSpread? {
        val values = sorted.filter { it.context == context }.map { it.mmol }
        if (values.isEmpty()) return null
        val average = values.average()
        val sd = if (values.size < 2) null else sqrt(values.sumOf { (it - average) * (it - average) } / (values.size - 1))
        return GlucoseSpread(values.size, average, sd, values.min(), values.max())
    }

    /** The 30 days ending at the newest reading. */
    fun last30Days(context: GlucoseContext): GlucosePeriod = window(context, 0)

    /** The 30 days before [last30Days]. */
    fun previous30Days(context: GlucoseContext): GlucosePeriod = window(context, 1)

    private fun window(context: GlucoseContext, index: Int): GlucosePeriod {
        val anchor = newest?.instant?.atZone(zone)?.toLocalDate() ?: return GlucosePeriod(0, null, null)
        val end = anchor.minusDays(30L * index)
        val start = end.minusDays(29)
        val inWindow = sorted.filter {
            val d = it.instant.atZone(zone).toLocalDate()
            it.context == context && !d.isBefore(start) && !d.isAfter(end)
        }
        if (inWindow.isEmpty()) return GlucosePeriod(0, null, null)
        val normal = inWindow.count { it.category == NhgGlucoseCategory.NORMAL }
        return GlucosePeriod(inWindow.size, inWindow.map { it.mmol }.average(), normal.toDouble() / inWindow.size * 100)
    }

    /** The largest increase between two consecutive readings less than [RISE_WINDOW_HOURS] hours apart; on a tie the newest. */
    fun largestRise(): GlucoseRise? = sorted.zipWithNext()
        .filter { (a, b) -> Duration.between(a.instant, b.instant) < Duration.ofHours(RISE_WINDOW_HOURS) && b.mmol > a.mmol }
        .map { (a, b) -> GlucoseRise(a, b) }
        .withIndex()
        .maxWithOrNull(compareBy({ it.value.delta }, { it.index }))
        ?.value

    companion object {
        const val RISE_WINDOW_HOURS = 3L

        /** Readings of [context] per NHG category, in category order, categories without readings left out. */
        fun categoryCounts(points: List<GlucosePoint>, context: GlucoseContext): List<Pair<NhgGlucoseCategory, Int>> {
            val ofContext = points.filter { it.context == context }
            return NhgGlucoseCategory.entries
                .map { category -> category to ofContext.count { it.category == category } }
                .filter { it.second > 0 }
        }

        /** All four parts of the day, in order, with their readings (all contexts together). */
        fun byTimeOfDay(points: List<GlucosePoint>, zone: ZoneId): List<TimeOfDayStats> {
            val grouped = points.groupBy { TimeOfDay.of(it.instant.atZone(zone).hour) }
            return TimeOfDay.entries.map { part ->
                val values = grouped[part].orEmpty().map { it.mmol }
                TimeOfDayStats(part, values.size, values.takeIf { it.isNotEmpty() }?.average(), values.minOrNull(), values.maxOrNull())
            }
        }
    }
}
