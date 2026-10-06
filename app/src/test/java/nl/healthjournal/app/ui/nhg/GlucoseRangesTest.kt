package nl.healthjournal.app.ui.nhg

import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseLevel
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class GlucoseRangesTest {

    @Test
    fun `fasting ranges`() {
        assertEquals(GlucoseRange(RangeShape.BELOW, 3.9), glucoseRange(NhgGlucoseCategory.HYPOGLYCAEMIA, GlucoseContext.FASTING))
        assertEquals(GlucoseRange(RangeShape.BETWEEN, 3.9, 6.0), glucoseRange(NhgGlucoseCategory.NORMAL, GlucoseContext.FASTING))
        assertEquals(GlucoseRange(RangeShape.ABOVE_TO, 6.0, 6.9), glucoseRange(NhgGlucoseCategory.IMPAIRED_FASTING, GlucoseContext.FASTING))
        assertEquals(GlucoseRange(RangeShape.ABOVE, 6.9), glucoseRange(NhgGlucoseCategory.DIABETES_RANGE, GlucoseContext.FASTING))
    }

    @Test
    fun `after a meal ranges`() {
        assertEquals(GlucoseRange(RangeShape.BETWEEN_BELOW, 3.9, 7.8), glucoseRange(NhgGlucoseCategory.NORMAL, GlucoseContext.POSTPRANDIAL))
        assertEquals(
            GlucoseRange(RangeShape.BETWEEN, 7.8, 11.0),
            glucoseRange(NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE, GlucoseContext.POSTPRANDIAL)
        )
        assertEquals(GlucoseRange(RangeShape.ABOVE, 11.0), glucoseRange(NhgGlucoseCategory.DIABETES_RANGE, GlucoseContext.POSTPRANDIAL))
    }

    @Test
    fun `shown range limits agree with the classifier`() {
        for (context in GlucoseContext.entries) {
            for (tenths in 20..140) {
                val level = GlucoseLevel(BigDecimal(tenths).divide(BigDecimal(10)))
                val category = NhgGlucoseCategory.classify(level, context)
                val range = glucoseRange(category, context)
                val v = level.valueInMmolL.toDouble()
                when (range.shape) {
                    RangeShape.BELOW -> assertTrue("$v $context", v < range.low)
                    RangeShape.BETWEEN -> assertTrue("$v $context", v >= range.low && v <= range.high!!)
                    RangeShape.BETWEEN_BELOW -> assertTrue("$v $context", v >= range.low && v < range.high!!)
                    RangeShape.ABOVE_TO -> assertTrue("$v $context", v > range.low && v <= range.high!!)
                    RangeShape.ABOVE -> assertTrue("$v $context", v > range.low)
                }
            }
        }
    }
}
