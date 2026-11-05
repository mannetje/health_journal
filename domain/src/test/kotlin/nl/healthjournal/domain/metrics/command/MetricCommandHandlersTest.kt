package nl.healthjournal.domain.metrics.command

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.shared.command.DeleteEntryCommand
import nl.healthjournal.domain.shared.command.DeleteEntryHandler
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.EntryKind
import nl.healthjournal.domain.shared.command.PortEntryGateway
import nl.healthjournal.domain.shared.command.ValidationField
import nl.healthjournal.domain.shared.command.ValidationRule
import nl.healthjournal.domain.shared.model.HeightCm
import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.model.GlucoseContext
import nl.healthjournal.domain.metrics.model.NhgWaistCircumferenceCategory
import nl.healthjournal.domain.metrics.model.OxygenContext
import nl.healthjournal.domain.metrics.model.OxygenSite
import nl.healthjournal.domain.migraine.model.MigraineEntry
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.model.Sex
import nl.healthjournal.domain.profile.port.ProfileRepositoryPort
import nl.healthjournal.domain.testing.FakeHealthLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import nl.healthjournal.domain.testing.FakeMigraineRepository
import nl.healthjournal.domain.testing.FakeProfileRepository

class MetricCommandHandlersTest {
    private val now = Instant.parse("2026-09-20T10:15:00Z")
    private val explicit = Instant.parse("2026-01-02T03:04:05Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val health = FakeHealthLogRepository()
    private val profiles = FakeProfileRepository()
    private val profile = Profile.create(
        name = "Test", dateOfBirth = LocalDate.of(1980, 1, 1), height = HeightCm(180), sex = Sex.FEMALE
    ).also { profiles.profiles[it.id] = it }
    private val pid: ProfileId = profile.id

    private fun DomainResult<*>.invalid() = errorOrNull() as DomainError.Invalid

    // --- weight ---

    @Test fun `weight uses the clock and computes the bmi`() = runBlocking {
        val handler = RecordWeightHandler(health, profiles, clock)
        val entry = handler.handle(RecordWeightCommand(pid, BigDecimal("80"))).getOrNull()!!
        assertEquals(now, entry.timestamp)
        assertTrue(entry.bmi != null)
        assertEquals(listOf(entry), health.weights)
        assertEquals(explicit, handler.handle(RecordWeightCommand(pid, BigDecimal("80"), explicit)).getOrNull()!!.timestamp)
    }

    @Test fun `weight out of range and long comments are typed errors and nothing is saved`() = runBlocking {
        val handler = RecordWeightHandler(health, profiles, clock)
        assertEquals(ValidationField.WEIGHT, handler.handle(RecordWeightCommand(pid, BigDecimal("0.1"))).invalid().field)
        assertEquals(
            ValidationRule.TOO_LONG,
            handler.handle(RecordWeightCommand(pid, BigDecimal("80"), comment = "x".repeat(201))).invalid().rule
        )
        assertTrue(health.weights.isEmpty())
    }

    @Test fun `weight update replaces or reports not found`() = runBlocking {
        val saved = RecordWeightHandler(health, profiles, clock).handle(RecordWeightCommand(pid, BigDecimal("80"))).getOrNull()!!
        val update = UpdateWeightHandler(health, profiles)
        val changed = update.handle(UpdateWeightCommand(saved.id, pid, BigDecimal("75"), explicit, "after")).getOrNull()!!
        assertEquals(changed, health.weights.single())
        val missing = MeasurementId.generate()
        assertEquals(
            DomainError.NotFound("WEIGHT", missing.toString()),
            update.handle(UpdateWeightCommand(missing, pid, BigDecimal("75"), explicit)).errorOrNull()
        )
    }

    @Test fun `a comment is stored normalised on every entry type and replaced or cleared on update`() = runBlocking {
        val start = Instant.parse("2026-01-01T08:00:00Z")
        RecordWeightHandler(health, profiles, clock).handle(RecordWeightCommand(pid, BigDecimal("80"), comment = "  a\nb "))
        RecordBloodPressureHandler(health, clock).handle(RecordBloodPressureCommand(pid, 120, 80, 70, comment = "bp"))
        RecordGlucoseHandler(health, clock)
            .handle(RecordGlucoseCommand(pid, GlucoseContext.FASTING, valueInMmolL = BigDecimal("5.5"), comment = "glucose"))
        RecordWaistCircumferenceHandler(health, profiles, clock).handle(RecordWaistCircumferenceCommand(pid, 85.0, comment = "waist"))
        RecordActivityHandler(health).handle(RecordActivityCommand(pid, start, start.plusSeconds(600), 1000.0, comment = "activity"))
        assertEquals("a b", health.weights.single().comment?.text)
        assertEquals("bp", health.bloodPressures.single().comment?.text)
        assertEquals("glucose", health.glucoses.single().comment?.text)
        assertEquals("waist", health.waistCircumferences.single().comment?.text)
        assertEquals("activity", health.activities.single().comment?.text)

        val saved = health.weights.single()
        val update = UpdateWeightHandler(health, profiles)
        update.handle(UpdateWeightCommand(saved.id, pid, BigDecimal("80"), saved.timestamp, "after"))
        assertEquals("after", health.weights.single().comment?.text)
        update.handle(UpdateWeightCommand(saved.id, pid, BigDecimal("80"), saved.timestamp, null))
        assertNull(health.weights.single().comment)
    }

    @Test fun `a failing port is a storage error`() = runBlocking {
        val broken = object : ProfileRepositoryPort by FakeProfileRepository() {
            override suspend fun getById(id: ProfileId): Profile? = throw IllegalStateException("disk")
        }
        val result = RecordWeightHandler(health, broken, clock).handle(RecordWeightCommand(pid, BigDecimal("80")))
        assertTrue(result.errorOrNull() is DomainError.Storage)
    }

    // --- blood pressure ---

    @Test fun `blood pressure records, classifies and validates`() = runBlocking {
        val record = RecordBloodPressureHandler(health, clock)
        val entry = record.handle(RecordBloodPressureCommand(pid, 120, 80, pulse = 60)).getOrNull()!!
        assertEquals(now, entry.timestamp)
        assertEquals(explicit, record.handle(RecordBloodPressureCommand(pid, 120, 80, timestamp = explicit)).getOrNull()!!.timestamp)
        assertEquals(ValidationRule.SYSTOLIC_NOT_ABOVE_DIASTOLIC, record.handle(RecordBloodPressureCommand(pid, 80, 100)).invalid().rule)
        assertEquals(ValidationField.PULSE, record.handle(RecordBloodPressureCommand(pid, 120, 80, pulse = 5)).invalid().field)
        assertEquals(2, health.bloodPressures.size)
    }

    @Test fun `blood pressure update`() = runBlocking {
        val saved = RecordBloodPressureHandler(health, clock).handle(RecordBloodPressureCommand(pid, 120, 80)).getOrNull()!!
        val update = UpdateBloodPressureHandler(health)
        assertEquals(130, update.handle(UpdateBloodPressureCommand(saved.id, pid, 130, 85, explicit)).getOrNull()!!.reading.systolic)
        val missing = MeasurementId.generate()
        assertEquals(
            DomainError.NotFound("BLOOD_PRESSURE", missing.toString()),
            update.handle(UpdateBloodPressureCommand(missing, pid, 130, 85, explicit)).errorOrNull()
        )
    }

    // --- glucose ---

    @Test fun `glucose accepts mmol or mg per dl and needs one of them`() = runBlocking {
        val record = RecordGlucoseHandler(health, clock)
        val mmol = record.handle(RecordGlucoseCommand(pid, GlucoseContext.FASTING, valueInMmolL = BigDecimal("5.5"))).getOrNull()!!
        assertEquals(now, mmol.timestamp)
        val mg = record.handle(RecordGlucoseCommand(pid, GlucoseContext.FASTING, valueInMgDl = BigDecimal("100"), timestamp = explicit)).getOrNull()!!
        assertEquals(BigDecimal("5.55"), mg.glucose.valueInMmolL)
        assertEquals(ValidationRule.REQUIRED, record.handle(RecordGlucoseCommand(pid, GlucoseContext.FASTING)).invalid().rule)
        assertEquals(
            ValidationField.GLUCOSE,
            record.handle(RecordGlucoseCommand(pid, GlucoseContext.FASTING, valueInMmolL = BigDecimal("99"))).invalid().field
        )
    }

    @Test fun `glucose update`() = runBlocking {
        val saved = RecordGlucoseHandler(health, clock)
            .handle(RecordGlucoseCommand(pid, GlucoseContext.FASTING, valueInMmolL = BigDecimal("5.5"))).getOrNull()!!
        val update = UpdateGlucoseHandler(health)
        assertEquals(
            BigDecimal("6.1"),
            update.handle(UpdateGlucoseCommand(saved.id, pid, GlucoseContext.FASTING, BigDecimal("6.1"), explicit)).getOrNull()!!.glucose.valueInMmolL
        )
        val missing = MeasurementId.generate()
        assertEquals(
            DomainError.NotFound("GLUCOSE", missing.toString()),
            update.handle(UpdateGlucoseCommand(missing, pid, GlucoseContext.FASTING, BigDecimal("6.1"), explicit)).errorOrNull()
        )
    }

    // --- activity ---

    @Test fun `activity validates the time range and the distance`() = runBlocking {
        val record = RecordActivityHandler(health)
        val start = explicit
        assertTrue(record.handle(RecordActivityCommand(pid, start, start.plusSeconds(60), 100.0)).isSuccess)
        assertEquals(ValidationRule.END_NOT_AFTER_START, record.handle(RecordActivityCommand(pid, start, start, 1.0)).invalid().rule)
        assertEquals(ValidationField.DISTANCE, record.handle(RecordActivityCommand(pid, start, start.plusSeconds(1), -1.0)).invalid().field)
        assertEquals(1, health.activities.size)
    }

    @Test fun `activity update`() = runBlocking {
        val saved = RecordActivityHandler(health).handle(RecordActivityCommand(pid, explicit, explicit.plusSeconds(60), 100.0)).getOrNull()!!
        val update = UpdateActivityHandler(health)
        assertEquals(250.0, update.handle(UpdateActivityCommand(saved.id, pid, explicit, explicit.plusSeconds(90), 250.0)).getOrNull()!!.distanceInMeters, 0.0)
        val missing = MeasurementId.generate()
        assertEquals(
            DomainError.NotFound("ACTIVITY", missing.toString()),
            update.handle(UpdateActivityCommand(missing, pid, explicit, explicit.plusSeconds(90), 1.0)).errorOrNull()
        )
    }

    // --- waist circumference ---

    @Test fun `waist uses the clock and the profile's sex`() = runBlocking {
        val record = RecordWaistCircumferenceHandler(health, profiles, clock)
        val entry = record.handle(RecordWaistCircumferenceCommand(pid, 90.0)).getOrNull()!!
        assertEquals(now, entry.timestamp)
        assertEquals(NhgWaistCircumferenceCategory.HIGH_RISK, entry.category)
        val withoutProfile = record.handle(RecordWaistCircumferenceCommand(ProfileId.generate(), 90.0, explicit)).getOrNull()!!
        assertNull(withoutProfile.category)
        assertEquals(ValidationField.WAIST, record.handle(RecordWaistCircumferenceCommand(pid, 10.0)).invalid().field)
    }

    @Test fun `waist update recomputes the category`() = runBlocking {
        val saved = RecordWaistCircumferenceHandler(health, profiles, clock).handle(RecordWaistCircumferenceCommand(pid, 90.0)).getOrNull()!!
        val update = UpdateWaistCircumferenceHandler(health, profiles)
        assertEquals(
            NhgWaistCircumferenceCategory.HEALTHY,
            update.handle(UpdateWaistCircumferenceCommand(saved.id, pid, 70.0, explicit)).getOrNull()!!.category
        )
        val missing = MeasurementId.generate()
        assertEquals(
            DomainError.NotFound("WAIST_CIRCUMFERENCE", missing.toString()),
            update.handle(UpdateWaistCircumferenceCommand(missing, pid, 70.0, explicit)).errorOrNull()
        )
    }

    // --- oxygen saturation ---

    @Test fun `oxygen saturation validates percent and pulse`() = runBlocking {
        val record = RecordOxygenSaturationHandler(health, clock)
        assertEquals(now, record.handle(RecordOxygenSaturationCommand(pid, 97)).getOrNull()!!.timestamp)
        assertEquals(explicit, record.handle(RecordOxygenSaturationCommand(pid, 97, timestamp = explicit)).getOrNull()!!.timestamp)
        assertEquals(ValidationField.OXYGEN_SATURATION, record.handle(RecordOxygenSaturationCommand(pid, 20)).invalid().field)
        assertEquals(ValidationField.PULSE, record.handle(RecordOxygenSaturationCommand(pid, 97, pulse = 5)).invalid().field)
        assertEquals(2, health.oxygenSaturations.size)
    }

    @Test fun `oxygen saturation stores the notes and trims the comment`() = runBlocking {
        val record = RecordOxygenSaturationHandler(health, clock)
        val saved = record.handle(
            RecordOxygenSaturationCommand(
                pid, 96, pulse = 68, context = OxygenContext.AT_REST, site = OxygenSite.FINGER,
                oxygenGiven = true, comment = "  after the stairs "
            )
        ).getOrNull()!!
        assertEquals(68, saved.pulse)
        assertEquals(OxygenContext.AT_REST, saved.context)
        assertEquals(OxygenSite.FINGER, saved.site)
        assertTrue(saved.oxygenGiven)
        assertEquals(EntryComment("after the stairs"), saved.comment)

        val plain = record.handle(RecordOxygenSaturationCommand(pid, 97)).getOrNull()!!
        assertNull(plain.pulse)
        assertNull(plain.context)
        assertNull(plain.site)
        assertEquals(false, plain.oxygenGiven)
        assertNull(plain.comment)
    }

    @Test fun `oxygen saturation update keeps the given timestamp`() = runBlocking {
        val saved = RecordOxygenSaturationHandler(health, clock).handle(RecordOxygenSaturationCommand(pid, 97)).getOrNull()!!
        val update = UpdateOxygenSaturationHandler(health)
        val changed = update.handle(UpdateOxygenSaturationCommand(saved.id, pid, 95, null, null, null, true, explicit)).getOrNull()!!
        assertEquals(explicit, changed.timestamp)
        val missing = MeasurementId.generate()
        assertEquals(
            DomainError.NotFound("OXYGEN_SATURATION", missing.toString()),
            update.handle(UpdateOxygenSaturationCommand(missing, pid, 95, null, null, null, false, explicit)).errorOrNull()
        )
    }

    // --- delete, all seven kinds ---

    @Test fun `delete works for every kind and reports an unknown id`() = runBlocking {
        val migraines = FakeMigraineRepository()
        val handler = DeleteEntryHandler(PortEntryGateway(health, health, health, health, health, health, migraines))
        val ids = mapOf(
            EntryKind.WEIGHT to RecordWeightHandler(health, profiles, clock).handle(RecordWeightCommand(pid, BigDecimal("80"))).getOrNull()!!.id,
            EntryKind.BLOOD_PRESSURE to RecordBloodPressureHandler(health, clock).handle(RecordBloodPressureCommand(pid, 120, 80)).getOrNull()!!.id,
            EntryKind.GLUCOSE to RecordGlucoseHandler(health, clock).handle(RecordGlucoseCommand(pid, GlucoseContext.FASTING, valueInMmolL = BigDecimal("5"))).getOrNull()!!.id,
            EntryKind.ACTIVITY to RecordActivityHandler(health).handle(RecordActivityCommand(pid, explicit, explicit.plusSeconds(5), 1.0)).getOrNull()!!.id,
            EntryKind.WAIST_CIRCUMFERENCE to RecordWaistCircumferenceHandler(health, profiles, clock).handle(RecordWaistCircumferenceCommand(pid, 90.0)).getOrNull()!!.id,
            EntryKind.OXYGEN_SATURATION to RecordOxygenSaturationHandler(health, clock).handle(RecordOxygenSaturationCommand(pid, 97)).getOrNull()!!.id,
            EntryKind.MIGRAINE to MigraineEntry(id = MeasurementId.generate(), profileId = pid, start = explicit).also { migraines.saveMigraine(it) }.id
        )
        for ((kind, id) in ids) {
            assertTrue("$kind", handler.handle(DeleteEntryCommand(kind, id)).isSuccess)
            assertEquals("$kind", DomainError.NotFound(kind.name, id.toString()), handler.handle(DeleteEntryCommand(kind, id)).errorOrNull())
        }
        assertTrue(health.weights.isEmpty() && health.oxygenSaturations.isEmpty() && health.waistCircumferences.isEmpty())
    }
}
