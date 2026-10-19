package nl.healthjournal.app.ui.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class HistoryTimelineTest {

    private val utc = ZoneId.of("UTC")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, zone: ZoneId = utc): Instant =
        LocalDateTime.of(year, month, day, hour, 0).atZone(zone).toInstant()

    /** Newest first, as the History list sorts them. */
    private val sample = listOf(
        at(2025, 8, 15), at(2025, 8, 14), at(2025, 8, 2),
        at(2025, 6, 20), at(2025, 6, 1),
        at(2024, 3, 9)
    )

    @Test
    fun `items group the entries under month headers newest first`() {
        val t = HistoryTimeline(sample, utc)
        assertEquals(
            listOf(
                TimelineItem.Header(YearMonth.of(2025, 8), 3),
                TimelineItem.Entry(0), TimelineItem.Entry(1), TimelineItem.Entry(2),
                TimelineItem.Header(YearMonth.of(2025, 6), 2),
                TimelineItem.Entry(3), TimelineItem.Entry(4),
                TimelineItem.Header(YearMonth.of(2024, 3), 1),
                TimelineItem.Entry(5)
            ),
            t.items
        )
        assertEquals(listOf(YearMonth.of(2025, 8), YearMonth.of(2025, 6), YearMonth.of(2024, 3)), t.months)
        assertEquals(listOf(2024, 2025), t.years)
    }

    @Test
    fun `a jump goes to the header of the month and the year to its newest month`() {
        val t = HistoryTimeline(sample, utc)
        assertEquals(0, t.indexOf(YearMonth.of(2025, 8)))
        assertEquals(4, t.indexOf(YearMonth.of(2025, 6)))
        assertEquals(7, t.indexOf(YearMonth.of(2024, 3)))
        assertEquals(0, t.indexOfYear(2025))
        assertEquals(7, t.indexOfYear(2024))
        assertNull(t.indexOf(YearMonth.of(2025, 7)))
        assertNull(t.indexOfYear(2023))
    }

    @Test
    fun `the period follows the list position`() {
        val t = HistoryTimeline(sample, utc)
        assertEquals(YearMonth.of(2025, 8), t.monthAt(0))
        assertEquals(YearMonth.of(2025, 8), t.monthAt(3))
        assertEquals(YearMonth.of(2025, 6), t.monthAt(4))
        assertEquals(YearMonth.of(2025, 6), t.monthAt(6))
        assertEquals(YearMonth.of(2024, 3), t.monthAt(8))
        // Past the end stays on the oldest month.
        assertEquals(YearMonth.of(2024, 3), t.monthAt(99))
    }

    @Test
    fun `a chart item before the entries shifts the indexes and selects the newest month while shown`() {
        val t = HistoryTimeline(sample, utc, leading = 1)
        assertEquals(YearMonth.of(2025, 8), t.monthAt(0))
        assertEquals(1, t.indexOf(YearMonth.of(2025, 8)))
        assertEquals(5, t.indexOf(YearMonth.of(2025, 6)))
        assertEquals(YearMonth.of(2025, 6), t.monthAt(5))
        assertEquals(YearMonth.of(2025, 8), t.monthAt(4))
    }

    @Test
    fun `an entry just before and just after new year belongs to different years`() {
        val amsterdam = ZoneId.of("Europe/Amsterdam")
        val t = HistoryTimeline(listOf(at(2026, 1, 1, 0, amsterdam), at(2025, 12, 31, 23, amsterdam)), amsterdam)
        assertEquals(listOf(YearMonth.of(2026, 1), YearMonth.of(2025, 12)), t.months)
        assertEquals(listOf(2025, 2026), t.years)
    }

    @Test
    fun `grouping uses the zone of the row text`() {
        // 23:30 UTC on 31 July is already 1 August in Amsterdam.
        val instant = at(2025, 7, 31, 23).plusSeconds(30 * 60)
        assertEquals(YearMonth.of(2025, 7), HistoryTimeline(listOf(instant), utc).months.single())
        assertEquals(YearMonth.of(2025, 8), HistoryTimeline(listOf(instant), ZoneId.of("Europe/Amsterdam")).months.single())
    }

    @Test
    fun `months with entries are listed per year`() {
        val t = HistoryTimeline(sample, utc)
        assertEquals(setOf(YearMonth.of(2025, 8), YearMonth.of(2025, 6)), t.monthsWithEntries(2025))
        assertEquals(setOf(YearMonth.of(2024, 3)), t.monthsWithEntries(2024))
        assertTrue(t.monthsWithEntries(2023).isEmpty())
    }

    @Test
    fun `an empty list has nothing to navigate`() {
        val t = HistoryTimeline(emptyList(), utc)
        assertTrue(t.items.isEmpty())
        assertFalse(t.navigable)
        assertNull(t.monthAt(0))
    }

    @Test
    fun `one month is not navigable`() {
        val t = HistoryTimeline(listOf(at(2025, 8, 15), at(2025, 8, 1)), utc)
        assertFalse(t.navigable)
        assertEquals(1, t.months.size)
    }

    @Test
    fun `two months are navigable`() {
        assertTrue(HistoryTimeline(listOf(at(2025, 8, 15), at(2025, 7, 1)), utc).navigable)
    }
}
