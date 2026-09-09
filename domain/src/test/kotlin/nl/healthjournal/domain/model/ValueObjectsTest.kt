package nl.healthjournal.domain.model

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class ValueObjectsTest {

    @Test
    fun `ProfileId generation and equality`() {
        val id1 = ProfileId.generate()
        val id2 = ProfileId.fromString(id1.value.toString())
        assertEquals(id1, id2)
    }

    @Test
    fun `WeightKg valid range and invalid bounds`() {
        val valid = WeightKg(BigDecimal("75.5"))
        assertEquals(BigDecimal("75.5"), valid.value)

        assertThrows(IllegalArgumentException::class.java) {
            WeightKg(BigDecimal("0.9"))
        }

        assertThrows(IllegalArgumentException::class.java) {
            WeightKg(BigDecimal("700.1"))
        }
    }

    @Test
    fun `HeightCm valid range and invalid bounds`() {
        val valid = HeightCm(180)
        assertEquals(180, valid.value)

        assertThrows(IllegalArgumentException::class.java) {
            HeightCm(49)
        }

        assertThrows(IllegalArgumentException::class.java) {
            HeightCm(301)
        }
    }

    @Test
    fun `BloodPressureReading valid and invalid invariants`() {
        val valid = BloodPressureReading(systolic = 120, diastolic = 80)
        assertEquals(120, valid.systolic)
        assertEquals(80, valid.diastolic)

        // Systolic must be greater than diastolic
        assertThrows(IllegalArgumentException::class.java) {
            BloodPressureReading(systolic = 80, diastolic = 80)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BloodPressureReading(systolic = 70, diastolic = 80)
        }

        // Out of bounds
        assertThrows(IllegalArgumentException::class.java) {
            BloodPressureReading(systolic = 35, diastolic = 30)
        }
    }

    @Test
    fun `GlucoseLevel stores mmol per L canonically and converts from mg per dL`() {
        val level = GlucoseLevel(BigDecimal("5.50"))
        assertEquals(BigDecimal("5.50"), level.valueInMmolL)

        // Conversion test: 100 mg/dL * 0.0555 = 5.55 mmol/L
        val converted = GlucoseLevel.fromMgDl(BigDecimal("100.0"))
        assertEquals(BigDecimal("5.55"), converted.valueInMmolL)

        assertThrows(IllegalArgumentException::class.java) {
            GlucoseLevel(BigDecimal("0.4"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            GlucoseLevel(BigDecimal("55.1"))
        }
    }

    @Test
    fun `ActivitySession enforces end after start and calculates duration`() {
        val start = Instant.parse("2026-09-09T10:00:00Z")
        val end = Instant.parse("2026-09-09T11:00:00Z")
        val session = ActivitySession(
            id = MeasurementId.generate(),
            profileId = ProfileId.generate(),
            startTime = start,
            endTime = end,
            distanceInMeters = 5000.0
        )

        assertEquals(3600L, session.durationInSeconds)

        assertThrows(IllegalArgumentException::class.java) {
            ActivitySession(
                id = MeasurementId.generate(),
                profileId = ProfileId.generate(),
                startTime = end,
                endTime = start,
                distanceInMeters = 5000.0
            )
        }
    }
}
