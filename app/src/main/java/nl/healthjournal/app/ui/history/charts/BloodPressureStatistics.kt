package nl.healthjournal.app.ui.history.charts

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** One blood pressure reading in mmHg, with an optional pulse in bpm. */
data class BpPoint(val instant: Instant, val systolic: Int, val diastolic: Int, val pulse: Int? = null)

/** The figures shown next to each other in the comparison card. */
enum class BpMetric {
    SYSTOLIC,
    DIASTOLIC,
    PULSE,
    MEAN_ARTERIAL,
    PULSE_PRESSURE;

    fun valueOf(point: BpPoint): Double? = when (this) {
        SYSTOLIC -> point.systolic.toDouble()
        DIASTOLIC -> point.diastolic.toDouble()
        PULSE -> point.pulse?.toDouble()
        MEAN_ARTERIAL -> (point.systolic + 2.0 * point.diastolic) / 3.0
        PULSE_PRESSURE -> (point.systolic - point.diastolic).toDouble()
    }
}

/**
 * [latest] is the newest value in the window, [min]..[max] its spread and [change] the average of the window
 * minus the average of the window before it (null when there is no earlier window or the range is All).
 */
data class MetricSummary(val latest: Double, val min: Double, val max: Double, val change: Double?) {
    /** A change smaller than [STABLE_BELOW] is shown as stable, not as a figure. */
    val isStable: Boolean get() = change != null && abs(change) < STABLE_BELOW

    companion object {
        const val STABLE_BELOW = 2.0
    }
}

/** The typical (median) and the largest change between two consecutive readings, per pressure. */
data class Steadiness(val usualSystolic: Double, val usualDiastolic: Double, val biggestSystolic: Int, val biggestDiastolic: Int)

/** The limits of the NHG bands in the (diastolic, systolic) plane; they mirror `NhgBloodPressureCategory.classify`. */
object BpZones {
    const val HIGH_SYSTOLIC = 140
    const val HIGH_DIASTOLIC = 90
    const val SERIOUS_SYSTOLIC = 180
    const val SERIOUS_DIASTOLIC = 110
}

/** Axis limits of the scatter chart: at least the usual range, widened to hold every reading. */
data class ScatterAxes(val diastolicMin: Int, val diastolicMax: Int, val systolicMin: Int, val systolicMax: Int) {
    companion object {
        fun of(points: List<BpPoint>): ScatterAxes {
            val dia = points.map { it.diastolic }
            val sys = points.map { it.systolic }
            return ScatterAxes(
                diastolicMin = min(40, (dia.minOrNull() ?: 40).roundDownTo(10)),
                diastolicMax = max(120, (dia.maxOrNull() ?: 120).roundUpTo(10)),
                systolicMin = min(80, (sys.minOrNull() ?: 80).roundDownTo(20)),
                systolicMax = max(180, (sys.maxOrNull() ?: 180).roundUpTo(20))
            )
        }

        private fun Int.roundDownTo(step: Int) = (floor(this / step.toDouble()) * step).toInt()
        private fun Int.roundUpTo(step: Int) = (ceil(this / step.toDouble()) * step).toInt()
    }
}

/**
 * Statistics over blood pressure readings, in mmHg and bpm. Pure, so it is tested on the JVM. The window is
 * the selected [range] counted back from the newest reading (like the chart); the change compares it with the
 * window of the same length before it. For All there is one window and no change.
 */
class BloodPressureStatistics(points: List<BpPoint>, range: TrendDateRange) {

    private val sorted = points.sortedBy { it.instant }
    private val newest: Instant? = sorted.lastOrNull()?.instant
    private val days = range.days

    /** The readings inside the window, oldest first. */
    val current: List<BpPoint> = when {
        newest == null -> emptyList()
        days == null -> sorted
        else -> sorted.filter { it.instant >= newest.minus(days, ChronoUnit.DAYS) }
    }

    private val previous: List<BpPoint> =
        if (newest == null || days == null) {
            emptyList()
        } else {
            val windowStart = newest.minus(days, ChronoUnit.DAYS)
            sorted.filter { it.instant >= windowStart.minus(days, ChronoUnit.DAYS) && it.instant < windowStart }
        }

    /** Null when the window holds fewer than two readings. */
    val steadiness: Steadiness? = if (current.size < 2) null else {
        val pairs = current.zipWithNext()
        val sys = pairs.map { (a, b) -> abs(b.systolic - a.systolic) }
        val dia = pairs.map { (a, b) -> abs(b.diastolic - a.diastolic) }
        Steadiness(median(sys), median(dia), sys.max(), dia.max())
    }

    /** Null when no reading in the window has a value for [metric] (a pulse that was never entered). */
    fun summary(metric: BpMetric): MetricSummary? {
        val values = current.mapNotNull { metric.valueOf(it) }
        if (values.isEmpty()) return null
        val before = previous.mapNotNull { metric.valueOf(it) }
        val change = if (before.isEmpty()) null else values.average() - before.average()
        val latest = current.reversed().firstNotNullOf { metric.valueOf(it) }
        return MetricSummary(latest, values.min(), values.max(), change)
    }

    private fun median(values: List<Int>): Double {
        val s = values.sorted()
        return if (s.size % 2 == 1) s[s.size / 2].toDouble() else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
    }
}
