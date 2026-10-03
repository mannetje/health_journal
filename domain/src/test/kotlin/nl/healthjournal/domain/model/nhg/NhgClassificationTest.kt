package nl.healthjournal.domain.model.nhg

import nl.healthjournal.domain.model.metrics.BloodPressureReading
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class NhgClassificationTest {

    @Test
    fun `BMI NHG classification scenarios`() {
        assertEquals(NhgBmiCategory.UNDERWEIGHT, NhgBmiCategory.classify(BigDecimal("18.4")))
        assertEquals(NhgBmiCategory.NORMAL, NhgBmiCategory.classify(BigDecimal("18.5")))
        assertEquals(NhgBmiCategory.NORMAL, NhgBmiCategory.classify(BigDecimal("24.9")))
        assertEquals(NhgBmiCategory.OVERWEIGHT, NhgBmiCategory.classify(BigDecimal("25.0")))
        assertEquals(NhgBmiCategory.OVERWEIGHT, NhgBmiCategory.classify(BigDecimal("29.9")))
        assertEquals(NhgBmiCategory.OBESE, NhgBmiCategory.classify(BigDecimal("30.0")))
        assertEquals(NhgBmiCategory.OBESE, NhgBmiCategory.classify(BigDecimal("35.2")))
    }

    private fun bp(sys: Int, dia: Int) = NhgBloodPressureCategory.classify(BloodPressureReading(sys, dia))

    @Test
    fun `Blood Pressure three band boundaries`() {
        assertEquals(NhgBloodPressureCategory.NORMAL, bp(139, 89))
        assertEquals(NhgBloodPressureCategory.HIGH, bp(140, 80))
        assertEquals(NhgBloodPressureCategory.HIGH, bp(120, 90))
        assertEquals(NhgBloodPressureCategory.HIGH, bp(179, 109))
        assertEquals(NhgBloodPressureCategory.SERIOUSLY_RAISED, bp(180, 80))
        assertEquals(NhgBloodPressureCategory.SERIOUSLY_RAISED, bp(120, 110))
        assertEquals(NhgBloodPressureCategory.NORMAL, bp(90, 60))
    }

    @Test
    fun `Blood Pressure legacy names map to the new bands`() {
        val expected = mapOf(
            "OPTIMAL" to NhgBloodPressureCategory.NORMAL,
            "NORMAL" to NhgBloodPressureCategory.NORMAL,
            "HIGH_NORMAL" to NhgBloodPressureCategory.NORMAL,
            "HYPERTENSION_GRADE_1" to NhgBloodPressureCategory.HIGH,
            "HYPERTENSION_GRADE_2" to NhgBloodPressureCategory.HIGH,
            "HYPERTENSION_GRADE_3" to NhgBloodPressureCategory.SERIOUSLY_RAISED,
            "HIGH" to NhgBloodPressureCategory.HIGH,
            "SERIOUSLY_RAISED" to NhgBloodPressureCategory.SERIOUSLY_RAISED
        )
        expected.forEach { (name, band) -> assertEquals(name, band, NhgBloodPressureCategory.fromStoredName(name)) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Blood Pressure unknown stored name is rejected`() {
        NhgBloodPressureCategory.fromStoredName("LOW")
    }

    @Test
    fun `Old six-band rule plus legacy mapping equals the new rule`() {
        fun old(sys: Int, dia: Int) = when {
            sys >= 180 || dia >= 110 -> "HYPERTENSION_GRADE_3"
            sys >= 160 || dia >= 100 -> "HYPERTENSION_GRADE_2"
            sys >= 140 || dia >= 90 -> "HYPERTENSION_GRADE_1"
            (sys in 130..139) || (dia in 85..89) -> "HIGH_NORMAL"
            (sys in 120..129 && dia < 80) || (sys < 130 && dia in 80..84) -> "NORMAL"
            else -> "OPTIMAL"
        }
        for (sys in 70..230) for (dia in 40..minOf(sys - 1, 200)) {
            assertEquals("$sys/$dia", bp(sys, dia), NhgBloodPressureCategory.fromStoredName(old(sys, dia)))
        }
    }

    @Test
    fun `Glucose Fasting NHG classification scenarios`() {
        val hypo = GlucoseLevel(BigDecimal("3.4"))
        assertEquals(NhgGlucoseCategory.HYPOGLYCAEMIA, NhgGlucoseCategory.classify(hypo, GlucoseContext.FASTING))

        val normalLow = GlucoseLevel(BigDecimal("3.5"))
        assertEquals(NhgGlucoseCategory.NORMAL, NhgGlucoseCategory.classify(normalLow, GlucoseContext.FASTING))

        val normalHigh = GlucoseLevel(BigDecimal("6.0"))
        assertEquals(NhgGlucoseCategory.NORMAL, NhgGlucoseCategory.classify(normalHigh, GlucoseContext.FASTING))

        val impaired = GlucoseLevel(BigDecimal("6.5"))
        assertEquals(NhgGlucoseCategory.IMPAIRED_FASTING, NhgGlucoseCategory.classify(impaired, GlucoseContext.FASTING))

        val diabetes = GlucoseLevel(BigDecimal("7.0"))
        assertEquals(NhgGlucoseCategory.DIABETES_RANGE, NhgGlucoseCategory.classify(diabetes, GlucoseContext.FASTING))
    }

    @Test
    fun `Glucose Postprandial NHG classification scenarios`() {
        val hypo = GlucoseLevel(BigDecimal("3.2"))
        assertEquals(NhgGlucoseCategory.HYPOGLYCAEMIA, NhgGlucoseCategory.classify(hypo, GlucoseContext.POSTPRANDIAL))

        val normal = GlucoseLevel(BigDecimal("7.5"))
        assertEquals(NhgGlucoseCategory.NORMAL, NhgGlucoseCategory.classify(normal, GlucoseContext.POSTPRANDIAL))

        val impairedLow = GlucoseLevel(BigDecimal("7.8"))
        assertEquals(NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE, NhgGlucoseCategory.classify(impairedLow, GlucoseContext.POSTPRANDIAL))

        val impairedHigh = GlucoseLevel(BigDecimal("11.0"))
        assertEquals(NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE, NhgGlucoseCategory.classify(impairedHigh, GlucoseContext.POSTPRANDIAL))

        val diabetes = GlucoseLevel(BigDecimal("11.1"))
        assertEquals(NhgGlucoseCategory.DIABETES_RANGE, NhgGlucoseCategory.classify(diabetes, GlucoseContext.POSTPRANDIAL))
    }
}
