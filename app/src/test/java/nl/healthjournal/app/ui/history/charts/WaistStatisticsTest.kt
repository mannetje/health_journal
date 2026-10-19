package nl.healthjournal.app.ui.history.charts

import nl.healthjournal.domain.model.metrics.WaistCircumferenceCm
import nl.healthjournal.domain.model.nhg.NhgWaistCircumferenceCategory
import nl.healthjournal.domain.model.profile.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WaistStatisticsTest {

    private val utc = ZoneId.of("UTC")

    private fun at(y: Int, m: Int, d: Int, hour: Int = 12) = LocalDateTime.of(y, m, d, hour, 0).atZone(utc).toInstant()
    private fun w(y: Int, m: Int, d: Int, cm: Double) = WaistPoint(at(y, m, d), cm)
    private fun kg(y: Int, m: Int, d: Int, value: Double) = WeightPoint(at(y, m, d), value)
    private fun stats(vararg points: WaistPoint) = WaistStatistics(points.toList(), utc)

    @Test
    fun `limits equal the NHG classification`() {
        Sex.entries.forEach { sex ->
            val limits = WaistLimits.forSex(sex)
            fun category(cm: Double) = NhgWaistCircumferenceCategory.classify(WaistCircumferenceCm(cm), sex)
            assertEquals(NhgWaistCircumferenceCategory.HEALTHY, category(limits.increased - 0.1))
            assertEquals(NhgWaistCircumferenceCategory.INCREASED_RISK, category(limits.increased))
            assertEquals(NhgWaistCircumferenceCategory.INCREASED_RISK, category(limits.high - 0.1))
            assertEquals(NhgWaistCircumferenceCategory.HIGH_RISK, category(limits.high))
        }
    }

    @Test
    fun `extremes take the newest on a tie`() {
        val s = stats(w(2024, 1, 1, 90.0), w(2024, 2, 1, 85.0), w(2024, 3, 1, 95.0), w(2024, 4, 1, 85.0), w(2024, 5, 1, 95.0))
        assertEquals(w(2024, 4, 1, 85.0), s.lowest)
        assertEquals(w(2024, 5, 1, 95.0), s.highest)
        assertEquals(w(2024, 5, 1, 95.0), s.newest)
    }

    @Test
    fun `an empty history has no figures`() {
        val s = stats()
        assertNull(s.newest)
        assertNull(s.lowest)
        assertNull(s.changeOver(ChangePeriod.MONTH))
    }

    @Test
    fun `change over three months in cm and percent`() {
        val s = stats(w(2024, 1, 10, 94.0), w(2024, 4, 10, 90.0))
        val change = s.changeOver(ChangePeriod.THREE_MONTHS)
        assertNotNull(change)
        assertEquals(-4.0, change!!.cm, 1e-9)
        assertEquals(-4.0 / 94.0 * 100, change.percent, 1e-9)
    }

    @Test
    fun `a short history has no change for long periods`() {
        val s = stats(w(2024, 3, 1, 94.0), w(2024, 4, 5, 90.0))
        assertNotNull(s.changeOver(ChangePeriod.MONTH))
        assertNull(s.changeOver(ChangePeriod.SIX_MONTHS))
        assertNull(s.changeOver(ChangePeriod.YEAR))
    }

    @Test
    fun `distance to a limit is positive above it`() {
        assertEquals(4.0, WaistStatistics.distanceToLimit(98.0, 94.0), 1e-9)
        assertEquals(-4.0, WaistStatistics.distanceToLimit(98.0, 102.0), 1e-9)
    }

    @Test
    fun `height ratio needs a height`() {
        assertEquals(50.0, WaistStatistics.heightRatioPercent(90.0, 180)!!, 1e-9)
        assertNull(WaistStatistics.heightRatioPercent(90.0, null))
    }

    @Test
    fun `pairing uses the nearest weight within three days`() {
        val waists = listOf(w(2024, 1, 10, 90.0), w(2024, 2, 10, 91.0), w(2024, 3, 10, 92.0))
        val weights = listOf(kg(2024, 1, 8, 80.0), kg(2024, 1, 12, 81.0), kg(2024, 2, 20, 82.0), kg(2024, 3, 13, 83.0))
        val pairs = WaistStatistics.pair(waists, weights)
        assertEquals(listOf(WaistWeightPair(80.0, 90.0), WaistWeightPair(83.0, 92.0)), pairs)
    }

    @Test
    fun `pairing without weights gives nothing`() {
        assertEquals(emptyList<WaistWeightPair>(), WaistStatistics.pair(listOf(w(2024, 1, 1, 90.0)), emptyList()))
    }

    @Test
    fun `fit finds the slope of a straight line`() {
        val pairs = (0 until 5).map { WaistWeightPair(70.0 + it, 80.0 + 0.8 * it) }
        val fit = WaistStatistics.fit(pairs)
        assertNotNull(fit)
        assertEquals(0.8, fit!!.slope, 1e-9)
        assertEquals(80.0 - 0.8 * 70.0, fit.intercept, 1e-9)
    }

    @Test
    fun `fit needs five pairs and varying weights`() {
        assertNull(WaistStatistics.fit((0 until 4).map { WaistWeightPair(70.0 + it, 80.0 + it) }))
        assertNull(WaistStatistics.fit((0 until 5).map { WaistWeightPair(70.0, 80.0 + it) }))
    }
}
