package nl.healthjournal.app.ui.history.charts

import nl.healthjournal.domain.model.metrics.BloodPressureReading
import org.junit.Assert.assertEquals
import org.junit.Test

class BloodPressureAverageTest {
    @Test
    fun `average is rounded to the nearest mmHg, not truncated`() {
        // systolic (130 + 130 + 129 + 130 + 130) / 5 = 129.8, diastolic (80 + 80 + 80 + 80 + 79) / 5 = 79.8
        val readings = listOf(
            BloodPressureReading(130, 80), BloodPressureReading(130, 80), BloodPressureReading(129, 80),
            BloodPressureReading(130, 80), BloodPressureReading(130, 79)
        )
        val avg = averageBloodPressure(readings)
        assertEquals(130, avg.systolic)
        assertEquals(80, avg.diastolic)
    }

    @Test
    fun `half rounds up and a lower fraction rounds down`() {
        assertEquals(121, averageBloodPressure(listOf(BloodPressureReading(120, 80), BloodPressureReading(121, 81))).systolic)
        assertEquals(120, averageBloodPressure(listOf(BloodPressureReading(120, 80), BloodPressureReading(120, 81), BloodPressureReading(121, 81))).systolic)
    }
}
