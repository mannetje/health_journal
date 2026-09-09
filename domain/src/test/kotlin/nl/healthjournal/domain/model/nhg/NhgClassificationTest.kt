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

    @Test
    fun `Blood Pressure NHG classification scenarios`() {
        // Optimal: <120 and <80
        assertEquals(
            NhgBloodPressureCategory.OPTIMAL,
            NhgBloodPressureCategory.classify(BloodPressureReading(118, 76))
        )

        // Normal: 120-129 and <80, or <130 and 80-84
        assertEquals(
            NhgBloodPressureCategory.NORMAL,
            NhgBloodPressureCategory.classify(BloodPressureReading(125, 78))
        )
        assertEquals(
            NhgBloodPressureCategory.NORMAL,
            NhgBloodPressureCategory.classify(BloodPressureReading(119, 82))
        )

        // High Normal: 130-139 or 85-89
        assertEquals(
            NhgBloodPressureCategory.HIGH_NORMAL,
            NhgBloodPressureCategory.classify(BloodPressureReading(135, 82))
        )
        assertEquals(
            NhgBloodPressureCategory.HIGH_NORMAL,
            NhgBloodPressureCategory.classify(BloodPressureReading(122, 87))
        )

        // Hypertension Grade 1: 140-159 or 90-99
        assertEquals(
            NhgBloodPressureCategory.HYPERTENSION_GRADE_1,
            NhgBloodPressureCategory.classify(BloodPressureReading(145, 85))
        )
        assertEquals(
            NhgBloodPressureCategory.HYPERTENSION_GRADE_1,
            NhgBloodPressureCategory.classify(BloodPressureReading(132, 94))
        )

        // Hypertension Grade 2: 160-179 or 100-109
        assertEquals(
            NhgBloodPressureCategory.HYPERTENSION_GRADE_2,
            NhgBloodPressureCategory.classify(BloodPressureReading(165, 95))
        )
        assertEquals(
            NhgBloodPressureCategory.HYPERTENSION_GRADE_2,
            NhgBloodPressureCategory.classify(BloodPressureReading(142, 104))
        )

        // Hypertension Grade 3: >=180 or >=110
        assertEquals(
            NhgBloodPressureCategory.HYPERTENSION_GRADE_3,
            NhgBloodPressureCategory.classify(BloodPressureReading(182, 90))
        )
        assertEquals(
            NhgBloodPressureCategory.HYPERTENSION_GRADE_3,
            NhgBloodPressureCategory.classify(BloodPressureReading(150, 112))
        )
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
