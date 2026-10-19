package nl.healthjournal.app.ui.history.charts

import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class GlucoseStatisticsTest {

    private val utc = ZoneId.of("UTC")
    private val fasting = GlucoseContext.FASTING
    private val post = GlucoseContext.POSTPRANDIAL

    private fun p(
        y: Int,
        m: Int,
        d: Int,
        mmol: Double,
        context: GlucoseContext = fasting,
        hour: Int = 8,
        minute: Int = 0,
        category: NhgGlucoseCategory = NhgGlucoseCategory.NORMAL
    ) = GlucosePoint(LocalDateTime.of(y, m, d, hour, minute).atZone(utc).toInstant(), mmol, context, category)

    private fun stats(vararg points: GlucosePoint) = GlucoseStatistics(points.toList(), utc)

    @Test
    fun `extremes take the newest on a tie`() {
        val s = stats(p(2024, 1, 1, 5.0), p(2024, 1, 2, 7.0), p(2024, 1, 3, 5.0), p(2024, 1, 4, 7.0))
        assertEquals(p(2024, 1, 3, 5.0), s.lowest)
        assertEquals(p(2024, 1, 4, 7.0), s.highest)
    }

    @Test
    fun `an empty history has no figures`() {
        val s = stats()
        assertNull(s.newest)
        assertNull(s.largestRise())
        assertNull(s.spread(fasting))
        assertNull(s.changeOver(fasting, ChangePeriod.MONTH))
        assertEquals(GlucosePeriod(0, null, null), s.last30Days(fasting))
    }

    @Test
    fun `change is per context`() {
        val s = stats(
            p(2024, 1, 10, 6.0, fasting),
            p(2024, 1, 11, 9.0, post),
            p(2024, 4, 10, 5.4, fasting),
            p(2024, 4, 11, 10.0, post)
        )
        val f = s.changeOver(fasting, ChangePeriod.THREE_MONTHS)
        assertNotNull(f)
        assertEquals(-0.6, f!!.delta, 1e-9)
        assertEquals(-10.0, f.percent, 1e-9)
        assertEquals(1.0, s.changeOver(post, ChangePeriod.THREE_MONTHS)!!.delta, 1e-9)
        assertNull(s.changeOver(post, ChangePeriod.YEAR))
    }

    @Test
    fun `a short history has no change for long periods`() {
        val s = stats(p(2024, 3, 1, 6.0), p(2024, 4, 5, 5.0))
        assertNotNull(s.changeOver(fasting, ChangePeriod.MONTH))
        assertNull(s.changeOver(fasting, ChangePeriod.SIX_MONTHS))
        assertNull(s.changeOver(fasting, ChangePeriod.YEAR))
    }

    @Test
    fun `spread uses the sample standard deviation`() {
        val s = stats(p(2024, 1, 1, 5.0), p(2024, 1, 2, 6.0), p(2024, 1, 3, 7.0), p(2024, 1, 4, 9.0, post))
        val spread = s.spread(fasting)!!
        assertEquals(3, spread.count)
        assertEquals(6.0, spread.average, 1e-9)
        assertEquals(1.0, spread.standardDeviation!!, 1e-9)
        assertEquals(5.0, spread.lowest, 1e-9)
        assertEquals(7.0, spread.highest, 1e-9)
    }

    @Test
    fun `one reading has no standard deviation`() {
        assertNull(stats(p(2024, 1, 1, 5.0)).spread(fasting)!!.standardDeviation)
    }

    @Test
    fun `category counts follow the category order and skip empty ones`() {
        val points = listOf(
            p(2024, 1, 1, 5.0),
            p(2024, 1, 2, 5.5),
            p(2024, 1, 3, 6.5, category = NhgGlucoseCategory.IMPAIRED_FASTING),
            p(2024, 1, 4, 3.5, category = NhgGlucoseCategory.HYPOGLYCAEMIA),
            p(2024, 1, 5, 12.0, post, category = NhgGlucoseCategory.DIABETES_RANGE)
        )
        assertEquals(
            listOf(
                NhgGlucoseCategory.HYPOGLYCAEMIA to 1,
                NhgGlucoseCategory.NORMAL to 2,
                NhgGlucoseCategory.IMPAIRED_FASTING to 1
            ),
            GlucoseStatistics.categoryCounts(points, fasting)
        )
    }

    @Test
    fun `time of day buckets by local hour`() {
        val points = listOf(
            p(2024, 1, 1, 5.0, hour = 0),
            p(2024, 1, 1, 6.0, hour = 5, minute = 59),
            p(2024, 1, 1, 7.0, hour = 6),
            p(2024, 1, 1, 8.0, hour = 12),
            p(2024, 1, 1, 9.0, hour = 23)
        )
        val parts = GlucoseStatistics.byTimeOfDay(points, utc).associateBy { it.timeOfDay }
        assertEquals(2, parts.getValue(TimeOfDay.NIGHT).count)
        assertEquals(5.5, parts.getValue(TimeOfDay.NIGHT).average!!, 1e-9)
        assertEquals(1, parts.getValue(TimeOfDay.MORNING).count)
        assertEquals(1, parts.getValue(TimeOfDay.AFTERNOON).count)
        assertEquals(9.0, parts.getValue(TimeOfDay.EVENING).highest!!, 1e-9)
    }

    @Test
    fun `an empty part of the day has no figures`() {
        val parts = GlucoseStatistics.byTimeOfDay(listOf(p(2024, 1, 1, 5.0, hour = 8)), utc).associateBy { it.timeOfDay }
        assertEquals(0, parts.getValue(TimeOfDay.EVENING).count)
        assertNull(parts.getValue(TimeOfDay.EVENING).average)
    }

    @Test
    fun `thirty day windows end at the newest reading`() {
        val s = stats(
            p(2024, 2, 1, 7.0, category = NhgGlucoseCategory.IMPAIRED_FASTING),
            p(2024, 2, 19, 6.5, category = NhgGlucoseCategory.IMPAIRED_FASTING),
            p(2024, 3, 10, 5.5),
            p(2024, 3, 20, 6.5, category = NhgGlucoseCategory.IMPAIRED_FASTING),
            p(2024, 3, 20, 9.0, post)
        )
        val last = s.last30Days(fasting)
        assertEquals(2, last.count)
        assertEquals(6.0, last.average!!, 1e-9)
        assertEquals(50.0, last.normalPercent!!, 1e-9)
        val before = s.previous30Days(fasting)
        assertEquals(2, before.count)
        assertEquals(6.75, before.average!!, 1e-9)
        assertEquals(0.0, before.normalPercent!!, 1e-9)
    }

    @Test
    fun `largest rise needs two readings under three hours apart`() {
        val s = stats(
            p(2024, 1, 1, 5.0, hour = 12),
            p(2024, 1, 1, 8.4, hour = 13, minute = 30),
            p(2024, 1, 2, 4.0, hour = 8),
            p(2024, 1, 2, 9.5, hour = 11),
            p(2024, 1, 3, 4.0, hour = 8),
            p(2024, 1, 3, 4.5, hour = 9)
        )
        val rise = s.largestRise()!!
        assertEquals(3.4, rise.delta, 1e-9)
        assertEquals(8.4, rise.to.mmol, 1e-9)
    }

    @Test
    fun `no rise when readings are far apart or falling`() {
        assertNull(stats(p(2024, 1, 1, 5.0, hour = 8), p(2024, 1, 1, 9.0, hour = 12)).largestRise())
        assertNull(stats(p(2024, 1, 1, 9.0, hour = 8), p(2024, 1, 1, 5.0, hour = 9)).largestRise())
    }
}
