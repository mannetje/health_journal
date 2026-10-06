package nl.healthjournal.app.ui.nhg

import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory

enum class RangeShape { BELOW, BETWEEN, BETWEEN_BELOW, ABOVE_TO, ABOVE }

/** The limits of a glucose band in mmol/L, the unit the band is decided in. */
data class GlucoseRange(val shape: RangeShape, val low: Double, val high: Double? = null)

/** The range shown with a glucose label. Mirrors the limits of [NhgGlucoseCategory.classify]. */
fun glucoseRange(category: NhgGlucoseCategory, context: GlucoseContext): GlucoseRange = when (context) {
    GlucoseContext.FASTING -> when (category) {
        NhgGlucoseCategory.HYPOGLYCAEMIA -> GlucoseRange(RangeShape.BELOW, 3.9)
        NhgGlucoseCategory.NORMAL -> GlucoseRange(RangeShape.BETWEEN, 3.9, 6.0)
        NhgGlucoseCategory.IMPAIRED_FASTING, NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE ->
            GlucoseRange(RangeShape.ABOVE_TO, 6.0, 6.9)
        NhgGlucoseCategory.DIABETES_RANGE -> GlucoseRange(RangeShape.ABOVE, 6.9)
    }
    GlucoseContext.POSTPRANDIAL -> when (category) {
        NhgGlucoseCategory.HYPOGLYCAEMIA -> GlucoseRange(RangeShape.BELOW, 3.9)
        NhgGlucoseCategory.NORMAL -> GlucoseRange(RangeShape.BETWEEN_BELOW, 3.9, 7.8)
        NhgGlucoseCategory.IMPAIRED_FASTING, NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE ->
            GlucoseRange(RangeShape.BETWEEN, 7.8, 11.0)
        NhgGlucoseCategory.DIABETES_RANGE -> GlucoseRange(RangeShape.ABOVE, 11.0)
    }
}
