package nl.healthjournal.app.ui.history.charts

import java.time.Instant
import java.time.temporal.ChronoUnit

enum class TrendDateRange(val days: Long?) {
    SEVEN_DAYS(7),
    THIRTY_DAYS(30),
    NINETY_DAYS(90),
    ALL(null)
}

fun <T> List<T>.filterByDateRange(range: TrendDateRange, timestampOf: (T) -> Instant): List<T> {
    val days = range.days ?: return this
    val cutoff = Instant.now().minus(days, ChronoUnit.DAYS)
    return filter { timestampOf(it) >= cutoff }
}
