package nl.healthjournal.domain.model.nhg

import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseLevel
import java.math.BigDecimal

enum class NhgGlucoseCategory {
    HYPOGLYCAEMIA,
    NORMAL,
    IMPAIRED_FASTING,
    IMPAIRED_GLUCOSE_TOLERANCE,
    DIABETES_RANGE;

    companion object {
        private val HYPO_THRESHOLD = BigDecimal("3.5")
        private val FASTING_NORMAL_MAX = BigDecimal("6.0")
        private val FASTING_IMPAIRED_MAX = BigDecimal("6.9")

        private val POSTPRANDIAL_NORMAL_MAX = BigDecimal("7.8")
        private val POSTPRANDIAL_IMPAIRED_MAX = BigDecimal("11.0")

        fun classify(glucose: GlucoseLevel, context: GlucoseContext): NhgGlucoseCategory {
            val v = glucose.valueInMmolL
            return when (context) {
                GlucoseContext.FASTING -> when {
                    v < HYPO_THRESHOLD -> HYPOGLYCAEMIA
                    v <= FASTING_NORMAL_MAX -> NORMAL
                    v <= FASTING_IMPAIRED_MAX -> IMPAIRED_FASTING
                    else -> DIABETES_RANGE
                }
                GlucoseContext.POSTPRANDIAL -> when {
                    v < HYPO_THRESHOLD -> HYPOGLYCAEMIA
                    v < POSTPRANDIAL_NORMAL_MAX -> NORMAL
                    v <= POSTPRANDIAL_IMPAIRED_MAX -> IMPAIRED_GLUCOSE_TOLERANCE
                    else -> DIABETES_RANGE
                }
            }
        }
    }
}
