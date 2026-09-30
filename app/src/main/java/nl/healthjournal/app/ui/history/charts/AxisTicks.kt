package nl.healthjournal.app.ui.history.charts

import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal enum class TickUnit { HOUR, DAY, WEEK, MONTH, YEAR }

/** Ticks every [n] [unit]s, which is about [approxDays] days apart. */
internal data class TickStep(val unit: TickUnit, val n: Int, val approxDays: Double)

private val TICK_STEPS = listOf(
    TickStep(TickUnit.HOUR, 6, 0.25),
    TickStep(TickUnit.HOUR, 12, 0.5),
    TickStep(TickUnit.DAY, 1, 1.0),
    TickStep(TickUnit.DAY, 2, 2.0),
    TickStep(TickUnit.WEEK, 1, 7.0),
    TickStep(TickUnit.WEEK, 2, 14.0),
    TickStep(TickUnit.MONTH, 1, 30.4),
    TickStep(TickUnit.MONTH, 2, 60.8),
    TickStep(TickUnit.MONTH, 3, 91.3),
    TickStep(TickUnit.MONTH, 6, 182.6),
    TickStep(TickUnit.YEAR, 1, 365.25),
    TickStep(TickUnit.YEAR, 2, 730.5),
    TickStep(TickUnit.YEAR, 5, 1826.25)
)

internal const val MAX_AXIS_LABELS = 7

/** The finest calendar step that keeps the number of labels across [visibleDays] within [maxLabels]. */
internal fun chooseTickStep(visibleDays: Double, maxLabels: Int = MAX_AXIS_LABELS): TickStep =
    TICK_STEPS.firstOrNull { visibleDays / it.approxDays <= maxLabels } ?: TICK_STEPS.last()

/**
 * Calendar-aligned tick instants (start of year / month / Monday / day / hour) covering
 * [from]..[to], plus one extra tick on either side so partially visible labels are drawn.
 */
internal fun calendarTicks(step: TickStep, from: Instant, to: Instant, zone: ZoneId): List<ZonedDateTime> {
    val n = step.n.toLong()
    val start = from.atZone(zone)
    val end = to.atZone(zone)
    val result = mutableListOf<ZonedDateTime>()
    if (step.unit == TickUnit.HOUR) {
        var t = start.truncatedTo(ChronoUnit.HOURS)
        while (t.hour % step.n != 0) t = t.minusHours(1)
        t = t.minusHours(n)
        while (true) {
            result += t
            if (t.isAfter(end)) break
            t = t.plusHours(n)
        }
        return result
    }
    var date: LocalDate = start.toLocalDate()
    date = when (step.unit) {
        TickUnit.DAY -> generateSequence(date) { it.minusDays(1) }.first { it.toEpochDay() % n == 0L }
        TickUnit.WEEK -> generateSequence(date.minusDays((date.dayOfWeek.value - 1).toLong())) { it.minusWeeks(1) }
            .first { Math.floorDiv(it.toEpochDay() - MONDAY_EPOCH_DAY_OFFSET, 7L) % n == 0L }
        TickUnit.MONTH -> generateSequence(date.withDayOfMonth(1)) { it.minusMonths(1) }
            .first { (it.year * 12L + it.monthValue - 1) % n == 0L }
        else -> generateSequence(date.withDayOfYear(1)) { it.minusYears(1) }.first { it.year % n == 0L }
    }
    fun LocalDate.next(): LocalDate = when (step.unit) {
        TickUnit.DAY -> plusDays(n)
        TickUnit.WEEK -> plusWeeks(n)
        TickUnit.MONTH -> plusMonths(n)
        else -> plusYears(n)
    }
    fun LocalDate.previous(): LocalDate = when (step.unit) {
        TickUnit.DAY -> minusDays(n)
        TickUnit.WEEK -> minusWeeks(n)
        TickUnit.MONTH -> minusMonths(n)
        else -> minusYears(n)
    }
    date = date.previous()
    while (true) {
        val t = date.atStartOfDay(zone)
        result += t
        if (t.isAfter(end)) break
        date = date.next()
    }
    return result
}

// 1970-01-01 was a Thursday, so Mondays fall on epoch days 4, 11, 18, ...
private const val MONDAY_EPOCH_DAY_OFFSET = 4L

private val YEAR_FORMAT = "yyyy"
private val MONTH_FORMAT = "MMM yyyy"
private val DAY_FORMAT = "d MMM"
private val DAY_YEAR_FORMAT = "d MMM yyyy"
private val HOUR_FORMAT = "d MMM HH:mm"

private fun formatter(pattern: String, zone: ZoneId) = DateTimeFormatter.ofPattern(pattern).withZone(zone)

/**
 * Label for a tick. Month and year steps always carry the year ("Jan 2026", never a bare "1 Jan"),
 * day and week steps carry it when [showYear] is set (the first label and the first after a new
 * year starts), so a label at a year boundary can't be misread.
 */
internal fun formatTick(step: TickStep, instant: Instant, showYear: Boolean, zone: ZoneId): String {
    val pattern = when (step.unit) {
        TickUnit.YEAR -> YEAR_FORMAT
        TickUnit.MONTH -> MONTH_FORMAT
        TickUnit.WEEK, TickUnit.DAY -> if (showYear) DAY_YEAR_FORMAT else DAY_FORMAT
        TickUnit.HOUR -> if (showYear) "$DAY_YEAR_FORMAT HH:mm" else HOUR_FORMAT
    }
    return formatter(pattern, zone).format(instant)
}

/** What the last draw pass decided; read by the label formatter (Vico gives it no visible range). */
internal class AxisTickState {
    @Volatile var step: TickStep? = null
    @Volatile var yearLabelXs: Set<Double> = emptySet()
}

/**
 * A [HorizontalAxis.ItemPlacer] that puts labels and guidelines on calendar boundaries and picks the
 * unit from the currently visible span, so zooming in adds finer ticks and zooming out merges them.
 * x-values are hours since [referenceEpochSecond].
 */
internal class CalendarItemPlacer(
    private val referenceEpochSecond: Long,
    private val state: AxisTickState,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val delegate: HorizontalAxis.ItemPlacer = HorizontalAxis.ItemPlacer.aligned(addExtremeLabelPadding = false)
) : HorizontalAxis.ItemPlacer by delegate {

    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float
    ): List<Double> {
        val visibleDays = (visibleXRange.endInclusive - visibleXRange.start) / 24.0
        val step = chooseTickStep(visibleDays)
        fun toInstant(x: Double) = Instant.ofEpochSecond(referenceEpochSecond + Math.round(x * 3600.0))
        val ticks = calendarTicks(step, toInstant(visibleXRange.start), toInstant(visibleXRange.endInclusive), zone)
            .map { it to (it.toEpochSecond() - referenceEpochSecond) / 3600.0 }
            .filter { (_, x) -> x in fullXRange }
        val yearXs = HashSet<Double>()
        var previousYear: Int? = null
        var firstVisibleSeen = false
        for ((t, x) in ticks) {
            val firstVisible = !firstVisibleSeen && x >= visibleXRange.start
            if (firstVisible) firstVisibleSeen = true
            if (firstVisible || (previousYear != null && previousYear != t.year)) yearXs += x
            previousYear = t.year
        }
        state.step = step
        state.yearLabelXs = yearXs
        return ticks.map { it.second }
    }
}
