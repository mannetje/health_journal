package nl.healthjournal.app.ui.history.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class PointSelectionTest {

    private val day1 = Instant.parse("2024-01-01T08:00:00Z")
    private val day2 = Instant.parse("2024-01-02T08:00:00Z")
    private val day3 = Instant.parse("2024-01-03T08:00:00Z")

    @Test
    fun `nearest picks the closest entry`() {
        assertEquals(day2, PointSelection.nearest(listOf(day1, day2, day3), day2.plusSeconds(3600)))
        assertEquals(day1, PointSelection.nearest(listOf(day1, day2, day3), day1.minusSeconds(86_400)))
    }

    @Test
    fun `nearest takes the newest on a tie`() {
        assertEquals(day2, PointSelection.nearest(listOf(day1, day2), day1.plusSeconds(43_200)))
    }

    @Test
    fun `nearest without entries is null`() {
        assertNull(PointSelection.nearest(emptyList(), day1))
    }

    @Test
    fun `a first tap selects, a second adds in time order, a third starts over`() {
        val first = PointSelection.next(emptyList(), day2)
        assertEquals(listOf(day2), first)
        val second = PointSelection.next(first, day1)
        assertEquals(listOf(day1, day2), second)
        assertEquals(listOf(day3), PointSelection.next(second, day3))
    }

    @Test
    fun `tapping the selected entry again clears it`() {
        assertEquals(emptyList<Instant>(), PointSelection.next(listOf(day1), day1))
    }

    @Test
    fun `tapping a selected entry of a pair starts over with that entry`() {
        assertEquals(listOf(day1), PointSelection.next(listOf(day1, day2), day1))
    }

    @Test
    fun `difference is later minus earlier with percent of the earlier`() {
        val d = PointSelection.difference(80f, 78f, day1, day1.plus(Duration.ofDays(10)))
        assertEquals(-2f, d.delta, 1e-6f)
        assertEquals(-2.5, d.percent!!, 1e-6)
        assertEquals(Duration.ofDays(10), d.between)
    }

    @Test
    fun `percent is missing when the earlier value is zero`() {
        assertNull(PointSelection.difference(0f, 3f, day1, day2).percent)
    }
}
