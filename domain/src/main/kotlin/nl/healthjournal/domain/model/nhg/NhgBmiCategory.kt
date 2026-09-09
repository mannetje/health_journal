package nl.healthjournal.domain.model.nhg

import java.math.BigDecimal

enum class NhgBmiCategory {
    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
    OBESE;

    companion object {
        private val UNDERWEIGHT_THRESHOLD = BigDecimal("18.5")
        private val NORMAL_THRESHOLD = BigDecimal("25.0")
        private val OVERWEIGHT_THRESHOLD = BigDecimal("30.0")

        fun classify(bmi: BigDecimal): NhgBmiCategory = when {
            bmi < UNDERWEIGHT_THRESHOLD -> UNDERWEIGHT
            bmi < NORMAL_THRESHOLD -> NORMAL
            bmi < OVERWEIGHT_THRESHOLD -> OVERWEIGHT
            else -> OBESE
        }
    }
}
