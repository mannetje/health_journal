package nl.healthjournal.app.ui.history.charts

import nl.healthjournal.domain.model.profile.Sex
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/** One waist measurement in centimetres. */
data class WaistPoint(val instant: Instant, val cm: Double)

/** The change over a period: [cm] newest minus start, [percent] of the start waist. */
data class WaistChange(val cm: Double, val percent: Double)

/** The two NHG limits in cm: from [increased] the risk is increased, from [high] it is high. They equal the NHG classification. */
data class WaistLimits(val increased: Double, val high: Double) {
    companion object {
        fun forSex(sex: Sex): WaistLimits = when (sex) {
            Sex.FEMALE -> WaistLimits(80.0, 88.0)
            Sex.MALE -> WaistLimits(94.0, 102.0)
        }
    }
}

/** A waist paired with the nearest weight entry within [WaistStatistics.PAIR_WINDOW_DAYS] days. */
data class WaistWeightPair(val kg: Double, val cm: Double)

/** A least-squares line `cm = intercept + slope * kg`. */
data class LinearFit(val slope: Double, val intercept: Double)

/**
 * Statistics over waist entries, in centimetres. Pure, so it is tested on the JVM. Several entries on one
 * day: the last of that day counts for the change figures.
 */
class WaistStatistics(points: List<WaistPoint>, zone: ZoneId) {

    private val sorted = points.sortedBy { it.instant }
    private val dated = sorted.map { it.instant.atZone(zone).toLocalDate() to it.cm }

    val newest: WaistPoint? = sorted.lastOrNull()

    /** The lowest waist; on a tie the newest. */
    val lowest: WaistPoint? = sorted.withIndex().minWithOrNull(compareBy({ it.value.cm }, { -it.index }))?.value

    /** The highest waist; on a tie the newest. */
    val highest: WaistPoint? = sorted.withIndex().maxWithOrNull(compareBy({ it.value.cm }, { it.index }))?.value

    /** Change over [period], or null when the history does not reach back that far. */
    fun changeOver(period: ChangePeriod): WaistChange? {
        if (dated.size < 2) return null
        val (newestDate, newestCm) = dated.last()
        val startDate = if (period.days != null) newestDate.minusDays(period.days) else newestDate.minusMonths(period.months!!)
        val start = dated.lastOrNull { it.first <= startDate } ?: return null
        val delta = newestCm - start.second
        val percent = if (start.second != 0.0) delta / start.second * 100 else 0.0
        return WaistChange(delta, percent)
    }

    companion object {
        const val PAIR_WINDOW_DAYS = 3L
        const val MIN_PAIRS = 5

        /** Waist minus [limit] in cm: positive above the limit. */
        fun distanceToLimit(cm: Double, limit: Double): Double = cm - limit

        /** The waist as a percentage of the height; null without a positive height. */
        fun heightRatioPercent(cm: Double, heightCm: Int?): Double? =
            heightCm?.takeIf { it > 0 }?.let { cm / it * 100 }

        /** Pairs every waist with the nearest weight within [PAIR_WINDOW_DAYS] days; on a tie the earlier weight. */
        fun pair(waists: List<WaistPoint>, weights: List<WeightPoint>): List<WaistWeightPair> {
            if (weights.isEmpty()) return emptyList()
            val window = PAIR_WINDOW_DAYS * 24 * 3600
            return waists.mapNotNull { w ->
                val nearest = weights.minWithOrNull(
                    compareBy({ abs(it.instant.epochSecond - w.instant.epochSecond) }, { it.instant })
                ) ?: return@mapNotNull null
                if (abs(nearest.instant.epochSecond - w.instant.epochSecond) <= window) WaistWeightPair(nearest.kg, w.cm) else null
            }
        }

        /** Least-squares fit, or null with fewer than [MIN_PAIRS] pairs or when all weights are equal. */
        fun fit(pairs: List<WaistWeightPair>): LinearFit? {
            if (pairs.size < MIN_PAIRS) return null
            val meanX = pairs.sumOf { it.kg } / pairs.size
            val meanY = pairs.sumOf { it.cm } / pairs.size
            val varX = pairs.sumOf { (it.kg - meanX) * (it.kg - meanX) }
            if (varX < 1e-9) return null
            val cov = pairs.sumOf { (it.kg - meanX) * (it.cm - meanY) }
            val slope = cov / varX
            return LinearFit(slope, meanY - slope * meanX)
        }
    }
}
