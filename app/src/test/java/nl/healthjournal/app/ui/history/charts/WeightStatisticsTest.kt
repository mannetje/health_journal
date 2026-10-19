package nl.healthjournal.app.ui.history.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId

class WeightStatisticsTest {

    private val utc = ZoneId.of("UTC")

    private fun p(y: Int, m: Int, d: Int, kg: Double, hour: Int = 12) =
        WeightPoint(LocalDateTime.of(y, m, d, hour, 0).atZone(utc).toInstant(), kg)

    private fun stats(vararg points: WeightPoint) = WeightStatistics(points.toList(), utc)

    @Test
    fun `lowest and highest take the newest on a tie`() {
        val s = stats(p(2024, 1, 1, 80.0), p(2024, 2, 1, 70.0), p(2024, 3, 1, 90.0), p(2024, 4, 1, 70.0), p(2024, 5, 1, 90.0))
        assertEquals(p(2024, 4, 1, 70.0), s.lowest)
        assertEquals(p(2024, 5, 1, 90.0), s.highest)
    }

    @Test
    fun `an empty history has no extremes and no change`() {
        val s = stats()
        assertNull(s.lowest)
        assertNull(s.highest)
        assertNull(s.changeOver(ChangePeriod.WEEK))
    }

    @Test
    fun `a single entry has extremes but no change`() {
        val s = stats(p(2024, 1, 1, 80.0))
        assertNotNull(s.lowest)
        assertNull(s.changeOver(ChangePeriod.WEEK))
        assertEquals(emptyMap<Month, Double>(), s.monthlyChange(2024))
        assertEquals(emptyMap<Int, Double>(), s.yearlyChange())
    }

    @Test
    fun `month change uses the newest entry on or before the start date`() {
        val s = stats(p(2024, 9, 18, 83.0), p(2024, 9, 20, 82.0), p(2024, 10, 5, 81.0), p(2024, 10, 20, 80.0))
        val c = s.changeOver(ChangePeriod.MONTH)!!
        assertEquals(-2.0, c.kg, 1e-9)
        assertEquals(-2.0 / 82.0 * 100, c.percent, 1e-9)
        // 20 September to 20 October is 30 days.
        assertEquals(-2.0 / (30 / 7.0), c.kgPerWeek, 1e-9)
    }

    @Test
    fun `a start date between two entries uses the older one`() {
        val s = stats(p(2024, 9, 10, 85.0), p(2024, 10, 20, 80.0))
        assertEquals(-5.0, s.changeOver(ChangePeriod.MONTH)!!.kg, 1e-9)
    }

    @Test
    fun `a period longer than the history is null`() {
        val s = stats(p(2024, 10, 1, 82.0), p(2024, 10, 22, 80.0))
        assertNotNull(s.changeOver(ChangePeriod.WEEK))
        assertNotNull(s.changeOver(ChangePeriod.FIFTEEN_DAYS))
        assertNull(s.changeOver(ChangePeriod.MONTH))
        assertNull(s.changeOver(ChangePeriod.THREE_MONTHS))
        assertNull(s.changeOver(ChangePeriod.SIX_MONTHS))
        assertNull(s.changeOver(ChangePeriod.YEAR))
    }

    @Test
    fun `end of month start date clamps like calendar months`() {
        // One month before 31 March is 28 February (2023).
        val s = stats(p(2023, 2, 28, 90.0), p(2023, 3, 31, 88.0))
        assertEquals(-2.0, s.changeOver(ChangePeriod.MONTH)!!.kg, 1e-9)
    }

    @Test
    fun `several entries on one day count the last of the day`() {
        val s = stats(p(2024, 10, 1, 90.0, 8), p(2024, 10, 1, 85.0, 20), p(2024, 10, 20, 80.0))
        assertEquals(-5.0, s.changeOver(ChangePeriod.FIFTEEN_DAYS)!!.kg, 1e-9)
    }

    @Test
    fun `monthly change is last of month minus last of previous month with data`() {
        val s = stats(
            p(2023, 12, 15, 90.0),
            p(2024, 1, 10, 89.0), p(2024, 1, 28, 88.0),
            p(2024, 3, 5, 91.0) // February has no entries: compared with January
        )
        val m = s.monthlyChange(2024)
        assertEquals(setOf(Month.JANUARY, Month.MARCH), m.keys)
        assertEquals(-2.0, m.getValue(Month.JANUARY), 1e-9) // across the new year
        assertEquals(3.0, m.getValue(Month.MARCH), 1e-9)
        // The first month of data has no bar.
        assertEquals(emptyMap<Month, Double>(), s.monthlyChange(2023))
    }

    @Test
    fun `yearly change and the years list`() {
        val s = stats(p(2022, 6, 1, 100.0), p(2022, 12, 1, 98.0), p(2024, 5, 1, 95.0), p(2025, 1, 1, 96.5))
        assertEquals(listOf(2022, 2024, 2025), s.years)
        val y = s.yearlyChange()
        assertEquals(setOf(2024, 2025), y.keys)
        assertEquals(-3.0, y.getValue(2024), 1e-9)
        assertEquals(1.5, y.getValue(2025), 1e-9)
    }

    @Test
    fun `unsorted input is sorted`() {
        val s = stats(p(2024, 10, 20, 80.0), p(2024, 10, 1, 82.0))
        assertEquals(-2.0, s.changeOver(ChangePeriod.FIFTEEN_DAYS)!!.kg, 1e-9)
    }
}
