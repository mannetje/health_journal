package nl.healthjournal.app.ui.history.charts

import java.time.Instant
import java.time.temporal.ChronoUnit

enum class TrendDateRange(val days: Long?) {
    SEVEN_DAYS(7),
    THIRTY_DAYS(30),
    NINETY_DAYS(90),
    ALL(null)
}

/**
 * Keeps the entries within [range] of the newest entry in this list, not of the system clock, so
 * imported historical data still fills the chart. An empty list yields an empty list.
 *
 * A trend needs two points: when the window holds fewer (sparse data, e.g. one entry in the last 90
 * days but the one before it months earlier) it is widened back to the second-newest entry.
 */
fun <T> List<T>.filterByDateRange(range: TrendDateRange, timestampOf: (T) -> Instant): List<T> {
    val days = range.days ?: return this
    val newest = maxOfOrNull(timestampOf) ?: return emptyList()
    val lower = newest.minus(days, ChronoUnit.DAYS)
    val inWindow = filter { timestampOf(it) in lower..newest }
    if (inWindow.size >= MIN_TREND_POINTS || size < MIN_TREND_POINTS) return inWindow
    val secondNewest = map(timestampOf).sortedDescending()[MIN_TREND_POINTS - 1]
    return filter { timestampOf(it) >= secondNewest }
}

private const val MIN_TREND_POINTS = 2
