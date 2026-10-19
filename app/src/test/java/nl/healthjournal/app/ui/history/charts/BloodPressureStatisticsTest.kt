package nl.healthjournal.app.ui.history.charts

import nl.healthjournal.domain.model.metrics.BloodPressureReading
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class BloodPressureStatisticsTest {

    private val now = Instant.parse("2026-10-19T08:00:00Z")

    private fun at(daysAgo: Long, sys: Int, dia: Int, pulse: Int? = null) =
        BpPoint(now.minus(daysAgo, ChronoUnit.DAYS), sys, dia, pulse)

    @Test
    fun `steadiness is the median and the largest change between consecutive readings`() {
        val stats = BloodPressureStatistics(
            listOf(at(4, 130, 80), at(3, 133, 82), at(2, 131, 90), at(1, 140, 88)),
            TrendDateRange.ALL
        )
        val s = stats.steadiness!!
        assertEquals(3.0, s.usualSystolic, 1e-9) // 3, 2, 9 -> median 3
        assertEquals(2.0, s.usualDiastolic, 1e-9) // 2, 8, 2 -> median 2
        assertEquals(9, s.biggestSystolic)
        assertEquals(8, s.biggestDiastolic)
    }

    @Test
    fun `steadiness of an even number of changes averages the two middle ones`() {
        val stats = BloodPressureStatistics(listOf(at(3, 130, 80), at(2, 132, 80), at(1, 138, 80)), TrendDateRange.ALL)
        assertEquals(4.0, stats.steadiness!!.usualSystolic, 1e-9) // 2, 6
    }

    @Test
    fun `steadiness needs two readings`() {
        assertNull(BloodPressureStatistics(listOf(at(1, 130, 80)), TrendDateRange.ALL).steadiness)
        assertNull(BloodPressureStatistics(emptyList(), TrendDateRange.ALL).steadiness)
    }

    @Test
    fun `window counts back from the newest reading, not from today`() {
        val stats = BloodPressureStatistics(listOf(at(40, 150, 95), at(8, 130, 80), at(0, 120, 70)), TrendDateRange.SEVEN_DAYS)
        assertEquals(1, stats.current.size)
        assertEquals(120, stats.current.single().systolic)
    }

    @Test
    fun `change compares the window with the window before it`() {
        val stats = BloodPressureStatistics(
            listOf(at(25, 140, 90), at(20, 144, 92), at(10, 130, 80), at(0, 134, 82)),
            TrendDateRange.THIRTY_DAYS
        )
        // Window of 30 days holds all four readings; there is nothing before it.
        assertNull(stats.summary(BpMetric.SYSTOLIC)!!.change)

        val weekly = BloodPressureStatistics(
            listOf(at(12, 140, 90), at(9, 144, 92), at(3, 130, 80), at(0, 134, 82)),
            TrendDateRange.SEVEN_DAYS
        )
        val sys = weekly.summary(BpMetric.SYSTOLIC)!!
        assertEquals(134.0, sys.latest, 1e-9)
        assertEquals(130.0, sys.min, 1e-9)
        assertEquals(134.0, sys.max, 1e-9)
        assertEquals(132.0 - 142.0, sys.change!!, 1e-9)
    }

    @Test
    fun `All has no change`() {
        val stats = BloodPressureStatistics(listOf(at(200, 140, 90), at(0, 120, 70)), TrendDateRange.ALL)
        assertNull(stats.summary(BpMetric.DIASTOLIC)!!.change)
    }

    @Test
    fun `mean arterial pressure and pulse pressure`() {
        val stats = BloodPressureStatistics(listOf(at(0, 130, 80)), TrendDateRange.ALL)
        assertEquals(96.666, stats.summary(BpMetric.MEAN_ARTERIAL)!!.latest, 1e-3)
        assertEquals(50.0, stats.summary(BpMetric.PULSE_PRESSURE)!!.latest, 1e-9)
    }

    @Test
    fun `pulse is skipped for readings without one`() {
        val stats = BloodPressureStatistics(listOf(at(2, 130, 80, 70), at(1, 132, 82), at(0, 131, 81, 74)), TrendDateRange.ALL)
        val pulse = stats.summary(BpMetric.PULSE)!!
        assertEquals(74.0, pulse.latest, 1e-9)
        assertEquals(70.0, pulse.min, 1e-9)
        assertNull(BloodPressureStatistics(listOf(at(0, 130, 80)), TrendDateRange.ALL).summary(BpMetric.PULSE))
    }

    @Test
    fun `a small change counts as stable`() {
        assertTrue(MetricSummary(70.0, 68.0, 72.0, 1.9).isStable)
        assertTrue(MetricSummary(70.0, 68.0, 72.0, -1.9).isStable)
        assertFalse(MetricSummary(70.0, 68.0, 72.0, 2.0).isStable)
        assertFalse(MetricSummary(70.0, 68.0, 72.0, null).isStable)
    }

    @Test
    fun `scatter axes hold the usual range and widen for outliers`() {
        val usual = ScatterAxes.of(listOf(at(0, 130, 80)))
        assertEquals(ScatterAxes(40, 120, 80, 180), usual)
        val wide = ScatterAxes.of(listOf(at(0, 205, 125), at(1, 90, 35)))
        assertEquals(ScatterAxes(30, 130, 80, 220), wide)
        assertNotNull(ScatterAxes.of(emptyList()))
    }

    @Test
    fun `zone limits match the NHG classification`() {
        fun cat(sys: Int, dia: Int) = NhgBloodPressureCategory.classify(BloodPressureReading(sys, dia))
        assertEquals(NhgBloodPressureCategory.NORMAL, cat(BpZones.HIGH_SYSTOLIC - 1, BpZones.HIGH_DIASTOLIC - 1))
        assertEquals(NhgBloodPressureCategory.HIGH, cat(BpZones.HIGH_SYSTOLIC, 60))
        assertEquals(NhgBloodPressureCategory.HIGH, cat(120, BpZones.HIGH_DIASTOLIC))
        assertEquals(NhgBloodPressureCategory.HIGH, cat(BpZones.SERIOUS_SYSTOLIC - 1, BpZones.SERIOUS_DIASTOLIC - 1))
        assertEquals(NhgBloodPressureCategory.SERIOUSLY_RAISED, cat(BpZones.SERIOUS_SYSTOLIC, 60))
        assertEquals(NhgBloodPressureCategory.SERIOUSLY_RAISED, cat(120, BpZones.SERIOUS_DIASTOLIC))
    }
}
