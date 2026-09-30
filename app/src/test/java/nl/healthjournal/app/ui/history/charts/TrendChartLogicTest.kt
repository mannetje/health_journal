package nl.healthjournal.app.ui.history.charts

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class TrendChartLogicTest {
    private val newest = Instant.parse("2024-03-31T08:00:00Z")
    private fun daysBefore(n: Long) = newest.minus(n, ChronoUnit.DAYS)

    @Test
    fun `range is anchored to the newest entry, not the system clock`() {
        val entries = listOf(daysBefore(100), daysBefore(60), daysBefore(20), daysBefore(3), newest)
        assertEquals(3, entries.filterByDateRange(TrendDateRange.THIRTY_DAYS) { it }.size)
        assertEquals(4, entries.filterByDateRange(TrendDateRange.NINETY_DAYS) { it }.size)
        assertEquals(2, entries.filterByDateRange(TrendDateRange.SEVEN_DAYS) { it }.size)
        assertEquals(5, entries.filterByDateRange(TrendDateRange.ALL) { it }.size)
    }

    @Test
    fun `sparse window is widened to the two newest entries so a trend can be drawn`() {
        val entries = listOf(daysBefore(400), daysBefore(190), newest)
        assertEquals(listOf(daysBefore(190), newest), entries.filterByDateRange(TrendDateRange.NINETY_DAYS) { it })
        assertEquals(listOf(newest), listOf(newest).filterByDateRange(TrendDateRange.SEVEN_DAYS) { it })
    }

    @Test
    fun `empty input yields an empty list`() {
        assertEquals(emptyList<Instant>(), emptyList<Instant>().filterByDateRange(TrendDateRange.NINETY_DAYS) { it })
    }

    private val utc = ZoneId.of("UTC")
    private fun ticks(step: TickStep, from: String, to: String) =
        calendarTicks(step, Instant.parse(from), Instant.parse(to), utc).map { it.toLocalDate().toString() }

    @Test
    fun `tick step gets finer as the visible span shrinks`() {
        assertEquals(TickUnit.YEAR, chooseTickStep(5 * 365.0).unit)
        assertEquals(TickUnit.MONTH, chooseTickStep(400.0).unit)
        assertEquals(TickUnit.WEEK, chooseTickStep(90.0).unit)
        assertEquals(TickUnit.WEEK, chooseTickStep(30.0).unit)
        assertEquals(TickUnit.DAY, chooseTickStep(5.0).unit)
        assertEquals(TickUnit.HOUR, chooseTickStep(1.0).unit)
    }

    @Test
    fun `month ticks fall on the first of the month`() {
        val t = ticks(TickStep(TickUnit.MONTH, 1, 30.4), "2025-11-10T00:00:00Z", "2026-02-20T00:00:00Z")
        assertEquals(listOf("2025-10-01", "2025-11-01", "2025-12-01", "2026-01-01", "2026-02-01", "2026-03-01"), t)
    }

    @Test
    fun `week ticks fall on mondays`() {
        val t = calendarTicks(TickStep(TickUnit.WEEK, 1, 7.0), Instant.parse("2026-01-07T00:00:00Z"), Instant.parse("2026-01-20T00:00:00Z"), utc)
        assertEquals(listOf("2025-12-29", "2026-01-05", "2026-01-12", "2026-01-19", "2026-01-26"), t.map { it.toLocalDate().toString() })
    }

    @Test
    fun `labels never show a bare first of january`() {
        val jan = Instant.parse("2026-01-01T00:00:00Z")
        assertEquals("Jan 2026", formatTick(TickStep(TickUnit.MONTH, 1, 30.4), jan, false, utc))
        assertEquals("2026", formatTick(TickStep(TickUnit.YEAR, 1, 365.25), jan, false, utc))
        assertEquals("1 Jan 2026", formatTick(TickStep(TickUnit.DAY, 1, 1.0), jan, true, utc))
        assertEquals("1 Jan", formatTick(TickStep(TickUnit.DAY, 1, 1.0), jan, false, utc))
    }
}
