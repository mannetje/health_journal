package nl.healthjournal.data.csv

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.*
import nl.healthjournal.domain.model.metrics.NhgBloodPressureCategory
import nl.healthjournal.domain.model.metrics.NhgGlucoseCategory
import nl.healthjournal.domain.model.metrics.NhgWaistCircumferenceCategory
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.testing.FakeHealthLogRepository
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import nl.healthjournal.domain.model.common.EntryComment
import nl.healthjournal.domain.model.common.HeightCm
import nl.healthjournal.domain.model.common.WeightKg

class CsvAdaptersTest {

    private class InMemoryProfileRepository : ProfileRepositoryPort {
        val profiles = mutableMapOf<ProfileId, Profile>()
        override suspend fun save(profile: Profile) { profiles[profile.id] = profile }
        override suspend fun getById(id: ProfileId): Profile? = profiles[id]
        override suspend fun getAll(): List<Profile> = profiles.values.toList()
        override suspend fun getActiveProfile(): Profile? = profiles.values.firstOrNull()
        override suspend fun setActiveProfile(id: ProfileId) {}
    }

    @Test
    fun `export and import weight CSV`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1), HeightCm(180))
        profileRepo.save(profile)

        val exportAdapter = CsvDataExportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        // Empty export produces header-only CSV
        val emptyCsv = exportAdapter.exportWeightCsv(profile.id)
        assertEquals("timestamp,weight_kg,bmi,comment\n", emptyCsv)

        // Add entries
        val entry1 = WeightEntry(
            MeasurementId.generate(), profile.id, Instant.parse("2026-09-20T08:00:00Z"),
            WeightKg(BigDecimal("75.0")), BigDecimal("23.1")
        )
        val entry2 = WeightEntry(
            MeasurementId.generate(), profile.id, Instant.parse("2026-09-21T08:00:00Z"),
            WeightKg(BigDecimal("74.5")), BigDecimal("23.0")
        )
        healthLogRepo.saveWeight(entry2)
        healthLogRepo.saveWeight(entry1)

        val csv = exportAdapter.exportWeightCsv(profile.id)
        assertTrue(csv.startsWith("timestamp,weight_kg,bmi,comment\n"))
        assertTrue(csv.contains("2026-09-20T08:00:00Z,75.0,23.1"))
        assertTrue(csv.contains("2026-09-21T08:00:00Z,74.5,23.0"))

        // Clear and import back
        healthLogRepo.weights.clear()
        val result = importAdapter.importCsv(profile.id, "weight", csv)
        assertEquals(2, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        assertEquals(2, healthLogRepo.weights.size)
    }

    @Test
    fun `export and import blood pressure CSV`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val exportAdapter = CsvDataExportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        val entry = BloodPressureEntry(
            MeasurementId.generate(), profile.id, Instant.parse("2026-09-20T08:00:00Z"),
            BloodPressureReading(120, 80), NhgBloodPressureCategory.NORMAL
        )
        healthLogRepo.saveBloodPressure(entry)

        val csv = exportAdapter.exportBloodPressureCsv(profile.id)
        assertTrue(csv.startsWith("timestamp,systolic_mmhg,diastolic_mmhg,pulse_bpm,classification,comment\n"))
        assertTrue(csv.contains("2026-09-20T08:00:00Z,120,80,,NORMAL"))

        healthLogRepo.bloodPressures.clear()
        val result = importAdapter.importCsv(profile.id, "blood_pressure", csv)
        assertEquals(1, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        assertEquals(1, healthLogRepo.bloodPressures.size)
    }

    @Test
    fun `export and import glucose CSV`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val exportAdapter = CsvDataExportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        val entry = GlucoseEntry(
            MeasurementId.generate(), profile.id, Instant.parse("2026-09-20T08:00:00Z"),
            GlucoseLevel(BigDecimal("5.4")), GlucoseContext.FASTING, NhgGlucoseCategory.NORMAL
        )
        healthLogRepo.saveGlucose(entry)

        val csv = exportAdapter.exportGlucoseCsv(profile.id)
        assertTrue(csv.startsWith("timestamp,glucose_mmol_l,context,classification,comment\n"))
        assertTrue(csv.contains("2026-09-20T08:00:00Z,5.4,FASTING,NORMAL"))

        healthLogRepo.glucoses.clear()
        val result = importAdapter.importCsv(profile.id, "glucose", csv)
        assertEquals(1, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        assertEquals(1, healthLogRepo.glucoses.size)
    }

    @Test
    fun `export and import activity CSV`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val exportAdapter = CsvDataExportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        val session = ActivitySession(
            MeasurementId.generate(), profile.id,
            Instant.parse("2026-09-20T08:00:00Z"),
            Instant.parse("2026-09-20T09:00:00Z"),
            5000.0
        )
        healthLogRepo.saveActivity(session)

        val csv = exportAdapter.exportActivityCsv(profile.id)
        assertTrue(csv.startsWith("start_timestamp,end_timestamp,distance_m,duration_s,comment\n"))
        assertTrue(csv.contains("2026-09-20T08:00:00Z,2026-09-20T09:00:00Z,5000.0,3600"))

        healthLogRepo.activities.clear()
        val result = importAdapter.importCsv(profile.id, "activity", csv)
        assertEquals(1, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        assertEquals(1, healthLogRepo.activities.size)
    }

    @Test
    fun `malformed CSV row skipped with error report`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        val malformedCsv = """
            timestamp,weight_kg,bmi
            2026-09-20T08:00:00Z,75.0,23.1
            invalid-timestamp,80.0,24.0
            2026-09-22T08:00:00Z,invalid-weight,25.0
            2026-09-23T08:00:00Z,-10.0,
            2026-09-24T08:00:00Z,70.0,22.0
        """.trimIndent()

        val result = importAdapter.importCsv(profile.id, "weight", malformedCsv)
        assertEquals(2, result.importedCount) // rows 2 and 6 valid
        assertEquals(3, result.skippedRows.size) // rows 3, 4, 5 skipped
        assertEquals(3, result.skippedRows[0].lineNumber)
        assertEquals(4, result.skippedRows[1].lineNumber)
        assertEquals(5, result.skippedRows[2].lineNumber)
    }

    @Test
    fun `duplicate timestamps are allowed on import`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        val csv = """
            timestamp,weight_kg,bmi
            2026-09-20T08:00:00Z,75.0,23.1
            2026-09-20T08:00:00Z,75.2,23.2
        """.trimIndent()

        val result = importAdapter.importCsv(profile.id, "weight", csv)
        assertEquals(2, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        assertEquals(2, healthLogRepo.weights.size)
    }

    // -------------------------------------------------------------------------
    // Libra CSV import tests
    // -------------------------------------------------------------------------

    @Test
    fun `libra CSV in kilograms imported correctly`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1), HeightCm(180))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        val libraCsv = """
            #Version: 6
            #Units: kg
            #date;weight;weight trend;body fat;body fat trend;muscle mass;body water;bone mass;comments
            2026-09-20T08:00:00.000Z;74.5;;;;;;;
            2026-09-21T08:00:00.000Z;74.2;;;;;;;
        """.trimIndent()

        val result = importAdapter.importCsv(profile.id, "weight", libraCsv)
        assertEquals(2, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        assertEquals(2, healthLogRepo.weights.size)
        assertEquals(BigDecimal("74.5"), healthLogRepo.weights[0].weight.value)
        assertEquals(BigDecimal("74.2"), healthLogRepo.weights[1].weight.value)
    }

    @Test
    fun `libra CSV with imperial lbs converted to kg`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        val libraCsv = """
            #Version: 6
            #Units: lbs
            #date;weight;weight trend;body fat;body fat trend;muscle mass;body water;bone mass;comments
            2026-09-20T08:00:00.000Z;164.0;;;;;;;
        """.trimIndent()

        // 164.0 lbs * 0.45359237 = 74.39 kg (rounded to 2dp)
        val result = importAdapter.importCsv(profile.id, "weight", libraCsv)
        assertEquals(1, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        val importedWeight = healthLogRepo.weights[0].weight.value
        assertEquals(BigDecimal("74.39"), importedWeight)
    }

    @Test
    fun `libra CSV metadata comments and optional columns handled`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        // File with extra metadata, optional trailing columns, and a comment line in the body
        val libraCsv = """
            #Version: 6
            #Units: kg
            #SomeOtherDirective: ignored
            #date;weight;weight trend;body fat;body fat trend;muscle mass;body water;bone mass;comments
            2026-09-20;74.5;74.3;18.5;;35.0;60.0;3.2;Morning weigh-in
            #This is a stray comment line - should be skipped
            2026-09-21;74.2;74.2;;;;;;
        """.trimIndent()

        val result = importAdapter.importCsv(profile.id, "weight", libraCsv)
        assertEquals(2, result.importedCount)
        assertEquals(0, result.skippedRows.size)
    }

    @Test
    fun `libra CSV malformed rows are skipped with error report`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        val libraCsv = """
            #Version: 6
            #Units: kg
            #date;weight;weight trend
            2026-09-20T08:00:00.000Z;74.5;
            invalid-date;74.0;
            2026-09-22T08:00:00.000Z;not-a-number;
            2026-09-23T08:00:00.000Z;75.0;
        """.trimIndent()

        val result = importAdapter.importCsv(profile.id, "weight", libraCsv)
        assertEquals(2, result.importedCount)     // rows 1 and 4 valid
        assertEquals(2, result.skippedRows.size)  // rows 2 and 3 skipped
    }

    @Test
    fun `libra CSV out-of-range weight is skipped`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        val libraCsv = """
            #Version: 6
            #Units: kg
            #date;weight;weight trend
            2026-09-20T08:00:00.000Z;0.5;
            2026-09-21T08:00:00.000Z;74.5;
            2026-09-22T08:00:00.000Z;800.0;
        """.trimIndent()

        val result = importAdapter.importCsv(profile.id, "weight", libraCsv)
        assertEquals(1, result.importedCount)     // only the 74.5 row
        assertEquals(2, result.skippedRows.size)  // 0.5 and 800.0 out of range
    }

    private fun importSetup(height: Int? = 180): Triple<FakeHealthLogRepository, Profile, CsvDataImportAdapter> {
        val logRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1), height?.let { HeightCm(it) })
        runBlocking { profileRepo.save(profile) }
        return Triple(logRepo, profile, CsvDataImportAdapter(logRepo, logRepo, logRepo, logRepo, logRepo, logRepo, profileRepo))
    }

    private fun lines(vararg l: String) = l.joinToString("\n")

    @Test
    fun `skipped row line numbers are physical file lines even with blank lines`() = runBlocking {
        val (_, profile, importer) = importSetup()
        val csv = lines("timestamp,weight_kg,bmi", "", "2026-09-20T08:00:00Z,75.0,", "", "", "bad-time,80.0,", "")
        val result = importer.importCsv(profile.id, "weight", csv)
        assertEquals(1, result.importedCount)
        assertEquals(6, result.skippedRows.single().lineNumber)
    }

    @Test
    fun `imported BMI is recomputed from the profile height, not trusted from the file`() = runBlocking {
        val (logRepo, profile, importer) = importSetup(height = 180)
        importer.importCsv(profile.id, "weight", lines("timestamp,weight_kg,bmi", "2026-09-20T08:00:00Z,81.0,99.9"))
        assertEquals(BigDecimal("25.0"), logRepo.weights.single().bmi)
    }

    @Test
    fun `imported categories are recomputed, not trusted from the file`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        importer.importCsv(profile.id, "blood_pressure", lines("timestamp,systolic,diastolic,classification", "2026-09-20T08:00:00Z,120,80,STAGE_2"))
        assertEquals(NhgBloodPressureCategory.classify(BloodPressureReading(120, 80)), logRepo.bloodPressures.single().category)
        importer.importCsv(profile.id, "glucose", lines("timestamp,glucose_mmol_l,context,category", "2026-09-20T08:00:00Z,5.4,FASTING,HIGH"))
        assertEquals(
            NhgGlucoseCategory.classify(GlucoseLevel(BigDecimal("5.4")), GlucoseContext.FASTING),
            logRepo.glucoses.single().category
        )
    }

    @Test
    fun `export writes the new blood pressure names and an old-name file still imports`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        logRepo.saveBloodPressure(
            BloodPressureEntry(
                MeasurementId.generate(), profile.id, Instant.parse("2026-09-20T08:00:00Z"),
                BloodPressureReading(185, 95), NhgBloodPressureCategory.SERIOUSLY_RAISED
            )
        )
        val csv = CsvDataExportAdapter(logRepo, logRepo, logRepo, logRepo, logRepo, logRepo).exportBloodPressureCsv(profile.id)
        assertTrue(csv.contains("2026-09-20T08:00:00Z,185,95,,SERIOUSLY_RAISED"))

        logRepo.bloodPressures.clear()
        val result = importer.importCsv(
            profile.id, "blood_pressure",
            lines("timestamp,systolic_mmhg,diastolic_mmhg,classification", "2026-09-20T08:00:00Z,150,95,HYPERTENSION_GRADE_1")
        )
        assertEquals(1, result.importedCount)
        assertEquals(NhgBloodPressureCategory.HIGH, logRepo.bloodPressures.single().category)
    }

    @Test
    fun `libra CSV with an unknown unit skips the rows instead of assuming kilograms`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        val csv = lines(
            "#Version: 6", "#Units: st", "#date;weight;comments",
            "2026-09-20T08:00:00.000Z;12.0;", "2026-09-21T08:00:00.000Z;11.9;"
        )
        val result = importer.importCsv(profile.id, "libra", csv)
        assertEquals(0, result.importedCount)
        assertEquals(2, result.skippedRows.size)
        assertEquals(4, result.skippedRows[0].lineNumber)
        assertTrue(result.skippedRows[0].reason.contains("st"))
        assertTrue(logRepo.weights.isEmpty())
    }

    @Test
    fun `libra CSV with explicit kg unit is imported`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        val csv = lines("#Version: 6", "#Units: KG", "#date;weight;comments", "2026-09-20T08:00:00.000Z;74.5;")
        assertEquals(1, importer.importCsv(profile.id, "libra", csv).importedCount)
        assertEquals(BigDecimal("74.5"), logRepo.weights.single().weight.value)
    }

    @Test
    fun `export and import waist circumference CSV round-trip`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val exportAdapter = CsvDataExportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        // Empty export produces header-only CSV
        val emptyCsv = exportAdapter.exportWaistCircumferenceCsv(profile.id)
        assertEquals("timestamp,waist_cm,classification,comment\n", emptyCsv)

        // Add entries with and without a category
        val entry1 = WaistCircumferenceEntry(
            MeasurementId.generate(), profile.id, Instant.parse("2026-09-20T08:00:00Z"),
            WaistCircumferenceCm(82.0), NhgWaistCircumferenceCategory.INCREASED_RISK
        )
        val entry2 = WaistCircumferenceEntry(
            MeasurementId.generate(), profile.id, Instant.parse("2026-09-21T08:00:00Z"),
            WaistCircumferenceCm(78.0), null
        )
        healthLogRepo.saveWaistCircumference(entry1)
        healthLogRepo.saveWaistCircumference(entry2)

        val csv = exportAdapter.exportWaistCircumferenceCsv(profile.id)
        assertTrue(csv.startsWith("timestamp,waist_cm,classification,comment\n"))
        assertTrue(csv.contains("2026-09-20T08:00:00Z,82.0,INCREASED_RISK"))
        assertTrue(csv.contains("2026-09-21T08:00:00Z,78.0,"))

        // Clear and import back — category is recomputed (null because profile has no sex)
        healthLogRepo.waistCircumferences.clear()
        val result = importAdapter.importCsv(profile.id, "waist_circumference", csv)
        assertEquals(2, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        assertEquals(2, healthLogRepo.waistCircumferences.size)
        // No sex set → category must be null for both imported entries
        assertTrue(healthLogRepo.waistCircumferences.all { it.category == null })
    }

    @Test
    fun `waist circumference import recomputes category from profile sex`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        // Create a female profile so classification fires
        val profile = Profile.create(
            "Test User", LocalDate.of(1990, 1, 1),
            sex = nl.healthjournal.domain.model.profile.Sex.FEMALE
        )
        profileRepo.save(profile)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        // File claims HEALTHY but the value (82 cm for female) should be INCREASED_RISK
        val csv = lines(
            "timestamp,waist_cm,classification",
            "2026-09-20T08:00:00Z,82,HEALTHY"
        )
        val result = importAdapter.importCsv(profile.id, "waist_circumference", csv)
        assertEquals(1, result.importedCount)
        assertEquals(NhgWaistCircumferenceCategory.INCREASED_RISK, healthLogRepo.waistCircumferences.single().category)
    }

    @Test
    fun `export and import oxygen saturation CSV round-trip`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)
        val exportAdapter = CsvDataExportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        assertEquals(
            "timestamp,spo2_percent,pulse_bpm,context,site,oxygen_given,comment\n",
            exportAdapter.exportOxygenSaturationCsv(profile.id)
        )

        val full = OxygenSaturationEntry(
            MeasurementId.generate(), profile.id, Instant.parse("2026-09-20T08:00:00Z"),
            nl.healthjournal.domain.model.metrics.OxygenSaturationPercent(93), 80,
            nl.healthjournal.domain.model.metrics.OxygenContext.AFTER_ACTIVITY,
            nl.healthjournal.domain.model.metrics.OxygenSite.TOE, true,
            nl.healthjournal.domain.model.common.EntryComment.ofOrNull("stairs, \"quick\"")
        )
        val plain = OxygenSaturationEntry(
            MeasurementId.generate(), profile.id, Instant.parse("2026-09-21T08:00:00Z"),
            nl.healthjournal.domain.model.metrics.OxygenSaturationPercent(98)
        )
        healthLogRepo.saveOxygenSaturation(full)
        healthLogRepo.saveOxygenSaturation(plain)

        val csv = exportAdapter.exportOxygenSaturationCsv(profile.id)
        assertTrue(csv.contains("2026-09-21T08:00:00Z,98,,,,false,"))
        assertTrue(csv.contains("2026-09-20T08:00:00Z,93,80,AFTER_ACTIVITY,TOE,true,"))

        healthLogRepo.oxygenSaturations.clear()
        val result = importAdapter.importCsv(profile.id, "oxygen_saturation", csv)
        assertEquals(2, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        val back = healthLogRepo.oxygenSaturations.associateBy { it.timestamp }
        assertEquals(full.copy(id = back.getValue(full.timestamp).id), back.getValue(full.timestamp))
        assertEquals(plain.copy(id = back.getValue(plain.timestamp).id), back.getValue(plain.timestamp))
    }

    @Test
    fun `oxygen saturation import skips rows with an impossible value, an unknown code or a bad flag`() = runBlocking {
        val healthLogRepo = FakeHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, healthLogRepo, profileRepo)

        val csv = lines(
            "timestamp,spo2_percent,pulse_bpm,context,site,oxygen_given,comment",
            "2026-09-20T08:00:00Z,49,,,,false,",
            "2026-09-20T09:00:00Z,101,,,,false,",
            "2026-09-20T10:00:00Z,96,300,,,false,",
            "2026-09-20T11:00:00Z,96,,RUNNING,,false,",
            "2026-09-20T12:00:00Z,96,,,HAND,false,",
            "2026-09-20T13:00:00Z,96,,,,maybe,",
            "2026-09-20T14:00:00Z,96,,,,,"
        )
        val result = importAdapter.importCsv(profile.id, "oxygen_saturation", csv)
        assertEquals(1, result.importedCount)
        assertEquals(6, result.skippedRows.size)
        assertEquals(96, healthLogRepo.oxygenSaturations.single().saturation.value)
    }

    // --- Comments ---

    @Test
    fun `comment with comma and quote survives export and import`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        val text = "dizzy, then \"fine\""
        logRepo.saveWeight(
            WeightEntry(
                MeasurementId.generate(), profile.id, Instant.parse("2026-09-20T08:00:00Z"),
                WeightKg(BigDecimal("75.0")), null, EntryComment(text)
            )
        )
        val csv = CsvDataExportAdapter(logRepo, logRepo, logRepo, logRepo, logRepo, logRepo).exportWeightCsv(profile.id)
        assertTrue(csv.contains(",\"dizzy, then \"\"fine\"\"\"\n"))

        logRepo.weights.clear()
        val result = importer.importCsv(profile.id, "weight", csv)
        assertEquals(1, result.importedCount)
        assertEquals(text, logRepo.weights.single().comment?.text)
    }

    @Test
    fun `comment round trips for every entry type`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        val export = CsvDataExportAdapter(logRepo, logRepo, logRepo, logRepo, logRepo, logRepo)
        val comment = EntryComment("after a walk")
        val at = Instant.parse("2026-09-20T08:00:00Z")
        logRepo.saveBloodPressure(
            BloodPressureEntry(MeasurementId.generate(), profile.id, at, BloodPressureReading(120, 80), NhgBloodPressureCategory.NORMAL, comment)
        )
        logRepo.saveGlucose(
            GlucoseEntry(MeasurementId.generate(), profile.id, at, GlucoseLevel(BigDecimal("5.4")), GlucoseContext.FASTING, NhgGlucoseCategory.NORMAL, comment)
        )
        logRepo.saveActivity(ActivitySession(MeasurementId.generate(), profile.id, at, at.plusSeconds(3600), 5000.0, comment))
        logRepo.saveWaistCircumference(
            WaistCircumferenceEntry(MeasurementId.generate(), profile.id, at, WaistCircumferenceCm(82.0), null, comment)
        )

        val bp = export.exportBloodPressureCsv(profile.id)
        val glucose = export.exportGlucoseCsv(profile.id)
        val activity = export.exportActivityCsv(profile.id)
        val waist = export.exportWaistCircumferenceCsv(profile.id)
        logRepo.bloodPressures.clear(); logRepo.glucoses.clear(); logRepo.activities.clear(); logRepo.waistCircumferences.clear()

        importer.importCsv(profile.id, "blood_pressure", bp)
        importer.importCsv(profile.id, "glucose", glucose)
        importer.importCsv(profile.id, "activity", activity)
        importer.importCsv(profile.id, "waist_circumference", waist)

        assertEquals(comment, logRepo.bloodPressures.single().comment)
        assertEquals(comment, logRepo.glucoses.single().comment)
        assertEquals(comment, logRepo.activities.single().comment)
        assertEquals(comment, logRepo.waistCircumferences.single().comment)
    }

    @Test
    fun `older file without comment column imports with no comment`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        val result = importer.importCsv(profile.id, "weight", lines("timestamp,weight_kg,bmi", "2026-09-20T08:00:00Z,75.0,23.1"))
        assertEquals(1, result.importedCount)
        assertNull(logRepo.weights.single().comment)
    }

    @Test
    fun `empty comment cell imports as no comment`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        importer.importCsv(profile.id, "weight", lines("timestamp,weight_kg,bmi,comment", "2026-09-20T08:00:00Z,75.0,23.1,"))
        assertNull(logRepo.weights.single().comment)
    }

    @Test
    fun `comment over the limit skips the row with a reason`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        val csv = lines(
            "timestamp,weight_kg,bmi,comment",
            "2026-09-20T08:00:00Z,75.0,23.1," + "a".repeat(201),
            "2026-09-21T08:00:00Z,75.0,23.1," + "a".repeat(200)
        )
        val result = importer.importCsv(profile.id, "weight", csv)
        assertEquals(1, result.importedCount)
        assertEquals("Comment longer than 200 characters", result.skippedRows.single().reason)
        assertEquals(2, result.skippedRows.single().lineNumber)
    }

    @Test
    fun `libra comments column becomes the entry comment`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        val csv = lines(
            "#Version: 6", "#Units: kg",
            "#date;weight;weight trend;body fat;body fat trend;muscle mass;body water;bone mass;comments",
            "2026-09-20T08:00:00.000Z;74.5;;;;;;;test comment"
        )
        assertEquals(1, importer.importCsv(profile.id, "libra", csv).importedCount)
        assertEquals("test comment", logRepo.weights.single().comment?.text)
    }

    @Test
    fun `libra comment over the limit is clipped instead of skipped`() = runBlocking {
        val (logRepo, profile, importer) = importSetup()
        val csv = lines(
            "#Version: 6", "#Units: kg",
            "#date;weight;weight trend;body fat;body fat trend;muscle mass;body water;bone mass;comments",
            "2026-09-20T08:00:00.000Z;74.5;;;;;;;" + "a".repeat(250)
        )
        val result = importer.importCsv(profile.id, "libra", csv)
        assertEquals(1, result.importedCount)
        assertTrue(result.skippedRows.isEmpty())
        assertEquals("a".repeat(200), logRepo.weights.single().comment?.text)
    }
}
