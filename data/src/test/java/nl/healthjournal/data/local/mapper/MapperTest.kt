package nl.healthjournal.data.local.mapper

import nl.healthjournal.data.local.entity.*
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.*
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.model.profile.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class MapperTest {

    @Test
    fun `ProfileMapper maps to entity and back`() {
        val profile = Profile.create(
            name = "Jane Doe",
            dateOfBirth = LocalDate.of(1990, 5, 15),
            height = HeightCm(175),
            sex = Sex.FEMALE
        )

        val entity = ProfileMapper.toEntity(profile, isActive = true)
        assertEquals(profile.id.value.toString(), entity.id)
        assertEquals("Jane Doe", entity.name)
        assertEquals("1990-05-15", entity.dateOfBirth)
        assertEquals(175, entity.heightCm)
        assertEquals("FEMALE", entity.sex)
        assertEquals(true, entity.isActive)

        val reconstructed = ProfileMapper.toDomain(entity)
        assertEquals(profile.id, reconstructed.id)
        assertEquals(profile.name, reconstructed.name)
        assertEquals(profile.dateOfBirth, reconstructed.dateOfBirth)
        assertEquals(profile.height, reconstructed.height)
        assertEquals(Sex.FEMALE, reconstructed.sex)
    }

    @Test
    fun `ProfileMapper maps null sex to entity and back`() {
        val profile = Profile.create(
            name = "No Sex Set",
            dateOfBirth = LocalDate.of(1990, 5, 15)
        )

        val entity = ProfileMapper.toEntity(profile)
        assertNull(entity.sex)

        val reconstructed = ProfileMapper.toDomain(entity)
        assertNull(reconstructed.sex)
    }

    @Test
    fun `WeightEntry maps to entity and back`() {
        val profileId = ProfileId.generate()
        val entry = WeightEntry(
            id = MeasurementId.generate(),
            profileId = profileId,
            timestamp = Instant.parse("2026-09-27T10:00:00Z"),
            weight = WeightKg(BigDecimal("75.5")),
            bmi = BigDecimal("24.7")
        )

        val entity = HealthLogMapper.toEntity(entry)
        assertEquals(entry.id.value.toString(), entity.id)
        assertEquals(profileId.value.toString(), entity.profileId)
        assertEquals(75.5, entity.weightKg, 0.01)
        assertEquals(24.7, entity.bmi!!, 0.01)

        val reconstructed = HealthLogMapper.toDomain(entity)
        assertEquals(entry.id, reconstructed.id)
        assertEquals(entry.profileId, reconstructed.profileId)
        assertEquals(entry.timestamp, reconstructed.timestamp)
        assertEquals(entry.weight.value.toDouble(), reconstructed.weight.value.toDouble(), 0.01)
        assertEquals(entry.bmi!!.toDouble(), reconstructed.bmi!!.toDouble(), 0.01)
    }

    @Test
    fun `BloodPressureEntry maps to entity and back`() {
        val profileId = ProfileId.generate()
        val entry = BloodPressureEntry(
            id = MeasurementId.generate(),
            profileId = profileId,
            timestamp = Instant.parse("2026-09-27T10:00:00Z"),
            reading = BloodPressureReading(120, 80),
            category = NhgBloodPressureCategory.NORMAL
        )

        val entity = HealthLogMapper.toEntity(entry)
        assertEquals(120, entity.systolic)
        assertEquals(80, entity.diastolic)
        assertEquals("NORMAL", entity.category)

        val reconstructed = HealthLogMapper.toDomain(entity)
        assertEquals(entry.id, reconstructed.id)
        assertEquals(entry.reading.systolic, reconstructed.reading.systolic)
        assertEquals(entry.reading.diastolic, reconstructed.reading.diastolic)
        assertEquals(NhgBloodPressureCategory.NORMAL, reconstructed.category)
    }

    @Test
    fun `legacy blood pressure rows are read as the new bands`() {
        val legacy = mapOf(
            "OPTIMAL" to NhgBloodPressureCategory.NORMAL,
            "HIGH_NORMAL" to NhgBloodPressureCategory.NORMAL,
            "HYPERTENSION_GRADE_1" to NhgBloodPressureCategory.HIGH,
            "HYPERTENSION_GRADE_2" to NhgBloodPressureCategory.HIGH,
            "HYPERTENSION_GRADE_3" to NhgBloodPressureCategory.SERIOUSLY_RAISED
        )
        val base = HealthLogMapper.toEntity(
            BloodPressureEntry(
                id = MeasurementId.generate(),
                profileId = ProfileId.generate(),
                timestamp = Instant.parse("2026-09-27T10:00:00Z"),
                reading = BloodPressureReading(120, 80),
                category = NhgBloodPressureCategory.NORMAL
            )
        )
        legacy.forEach { (stored, expected) ->
            assertEquals(stored, expected, HealthLogMapper.toDomain(base.copy(category = stored)).category)
        }
    }

    @Test
    fun `GlucoseEntry maps to entity and back`() {
        val profileId = ProfileId.generate()
        val entry = GlucoseEntry(
            id = MeasurementId.generate(),
            profileId = profileId,
            timestamp = Instant.parse("2026-09-27T10:00:00Z"),
            glucose = GlucoseLevel(BigDecimal("5.4")),
            context = GlucoseContext.FASTING,
            category = NhgGlucoseCategory.NORMAL
        )

        val entity = HealthLogMapper.toEntity(entry)
        assertEquals(5.4, entity.glucoseMmolL, 0.01)
        assertEquals("FASTING", entity.context)
        assertEquals("NORMAL", entity.category)

        val reconstructed = HealthLogMapper.toDomain(entity)
        assertEquals(entry.id, reconstructed.id)
        assertEquals(entry.glucose.valueInMmolL.toDouble(), reconstructed.glucose.valueInMmolL.toDouble(), 0.01)
        assertEquals(GlucoseContext.FASTING, reconstructed.context)
        assertEquals(NhgGlucoseCategory.NORMAL, reconstructed.category)
    }

    @Test
    fun `ActivitySession maps to entity and back`() {
        val profileId = ProfileId.generate()
        val start = Instant.parse("2026-09-27T10:00:00Z")
        val end = Instant.parse("2026-09-27T10:30:00Z")
        val session = ActivitySession(
            id = MeasurementId.generate(),
            profileId = profileId,
            startTime = start,
            endTime = end,
            distanceInMeters = 5200.0
        )

        val entity = HealthLogMapper.toEntity(session)
        assertEquals(start.toEpochMilli(), entity.startTime)
        assertEquals(end.toEpochMilli(), entity.endTime)
        assertEquals(5200.0, entity.distanceMeters, 0.01)

        val reconstructed = HealthLogMapper.toDomain(entity)
        assertEquals(session.id, reconstructed.id)
        assertEquals(session.startTime, reconstructed.startTime)
        assertEquals(session.endTime, reconstructed.endTime)
        assertEquals(session.distanceInMeters, reconstructed.distanceInMeters, 0.01)
        assertEquals(1800L, reconstructed.durationInSeconds)
    }

    @Test
    fun `WaistCircumferenceEntry maps to entity and back`() {
        val profileId = ProfileId.generate()
        val entry = WaistCircumferenceEntry(
            id = MeasurementId.generate(),
            profileId = profileId,
            timestamp = Instant.parse("2026-09-27T10:00:00Z"),
            waist = WaistCircumferenceCm(85.0),
            category = nl.healthjournal.domain.model.nhg.NhgWaistCircumferenceCategory.HEALTHY
        )

        val entity = HealthLogMapper.toEntity(entry)
        assertEquals(85.0, entity.waistCm, 0.01)
        assertEquals("HEALTHY", entity.category)

        val reconstructed = HealthLogMapper.toDomain(entity)
        assertEquals(entry.id, reconstructed.id)
        assertEquals(entry.waist.value, reconstructed.waist.value, 0.01)
        assertEquals(nl.healthjournal.domain.model.nhg.NhgWaistCircumferenceCategory.HEALTHY, reconstructed.category)
    }

    @Test
    fun `comments map to the entity and back for every entry type`() {
        val profileId = ProfileId.generate()
        val t = Instant.parse("2026-09-27T10:00:00Z")
        val comment = EntryComment("After a walk, \"calm\"")

        val weight = WeightEntry(MeasurementId.generate(), profileId, t, WeightKg(BigDecimal("75.5")), null, comment)
        assertEquals("After a walk, \"calm\"", HealthLogMapper.toEntity(weight).comment)
        assertEquals(comment, HealthLogMapper.toDomain(HealthLogMapper.toEntity(weight)).comment)

        val bp = BloodPressureEntry(MeasurementId.generate(), profileId, t, BloodPressureReading(120, 80), NhgBloodPressureCategory.NORMAL, comment)
        assertEquals(comment, HealthLogMapper.toDomain(HealthLogMapper.toEntity(bp)).comment)

        val glucose = GlucoseEntry(MeasurementId.generate(), profileId, t, GlucoseLevel(BigDecimal("5.4")), GlucoseContext.FASTING, NhgGlucoseCategory.NORMAL, comment)
        assertEquals(comment, HealthLogMapper.toDomain(HealthLogMapper.toEntity(glucose)).comment)

        val activity = ActivitySession(MeasurementId.generate(), profileId, t, t.plusSeconds(1800), 5200.0, comment)
        assertEquals(comment, HealthLogMapper.toDomain(HealthLogMapper.toEntity(activity)).comment)

        val waist = WaistCircumferenceEntry(MeasurementId.generate(), profileId, t, WaistCircumferenceCm(85.0), null, comment)
        assertEquals(comment, HealthLogMapper.toDomain(HealthLogMapper.toEntity(waist)).comment)
    }

    @Test
    fun `a missing comment stays null through the mapper`() {
        val entry = WeightEntry(MeasurementId.generate(), ProfileId.generate(), Instant.parse("2026-09-27T10:00:00Z"), WeightKg(BigDecimal("75.5")), null)
        assertNull(HealthLogMapper.toEntity(entry).comment)
        assertNull(HealthLogMapper.toDomain(HealthLogMapper.toEntity(entry)).comment)
    }
}
