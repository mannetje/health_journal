package nl.healthjournal.data.csv

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.*
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class CsvAdaptersTest {

    private class InMemoryHealthLogRepository : HealthLogRepositoryPort {
        val weights = mutableListOf<WeightEntry>()
        val bloodPressures = mutableListOf<BloodPressureEntry>()
        val glucoses = mutableListOf<GlucoseEntry>()
        val activities = mutableListOf<ActivitySession>()

        override suspend fun saveWeight(entry: WeightEntry) { weights.add(entry) }
        override suspend fun updateWeight(entry: WeightEntry): Boolean {
            val i = weights.indexOfFirst { it.id == entry.id }
            if (i < 0) return false
            weights[i] = entry
            return true
        }
        override suspend fun deleteWeight(id: MeasurementId): Boolean = weights.removeAll { it.id == id }

        override suspend fun getWeightHistory(profileId: ProfileId): List<WeightEntry> =
            weights.filter { it.profileId == profileId }
        override fun observeWeightHistory(profileId: ProfileId): Flow<List<WeightEntry>> = emptyFlow()

        override suspend fun saveBloodPressure(entry: BloodPressureEntry) { bloodPressures.add(entry) }
        override suspend fun updateBloodPressure(entry: BloodPressureEntry): Boolean {
            val i = bloodPressures.indexOfFirst { it.id == entry.id }
            if (i < 0) return false
            bloodPressures[i] = entry
            return true
        }
        override suspend fun deleteBloodPressure(id: MeasurementId): Boolean = bloodPressures.removeAll { it.id == id }

        override suspend fun getBloodPressureHistory(profileId: ProfileId): List<BloodPressureEntry> =
            bloodPressures.filter { it.profileId == profileId }
        override fun observeBloodPressureHistory(profileId: ProfileId): Flow<List<BloodPressureEntry>> = emptyFlow()

        override suspend fun saveGlucose(entry: GlucoseEntry) { glucoses.add(entry) }
        override suspend fun updateGlucose(entry: GlucoseEntry): Boolean {
            val i = glucoses.indexOfFirst { it.id == entry.id }
            if (i < 0) return false
            glucoses[i] = entry
            return true
        }
        override suspend fun deleteGlucose(id: MeasurementId): Boolean = glucoses.removeAll { it.id == id }

        override suspend fun getGlucoseHistory(profileId: ProfileId): List<GlucoseEntry> =
            glucoses.filter { it.profileId == profileId }
        override fun observeGlucoseHistory(profileId: ProfileId): Flow<List<GlucoseEntry>> = emptyFlow()

        override suspend fun saveActivity(session: ActivitySession) { activities.add(session) }
        override suspend fun updateActivity(session: ActivitySession): Boolean {
            val i = activities.indexOfFirst { it.id == session.id }
            if (i < 0) return false
            activities[i] = session
            return true
        }
        override suspend fun deleteActivity(id: MeasurementId): Boolean = activities.removeAll { it.id == id }

        override suspend fun getActivityHistory(profileId: ProfileId): List<ActivitySession> =
            activities.filter { it.profileId == profileId }
        override fun observeActivityHistory(profileId: ProfileId): Flow<List<ActivitySession>> = emptyFlow()
    }

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
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1), HeightCm(180))
        profileRepo.save(profile)

        val exportAdapter = CsvDataExportAdapter(healthLogRepo)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

        // Empty export produces header-only CSV
        val emptyCsv = exportAdapter.exportWeightCsv(profile.id)
        assertEquals("timestamp,weight_kg,bmi\n", emptyCsv)

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
        assertTrue(csv.startsWith("timestamp,weight_kg,bmi\n"))
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
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val exportAdapter = CsvDataExportAdapter(healthLogRepo)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

        val entry = BloodPressureEntry(
            MeasurementId.generate(), profile.id, Instant.parse("2026-09-20T08:00:00Z"),
            BloodPressureReading(120, 80), NhgBloodPressureCategory.NORMAL
        )
        healthLogRepo.saveBloodPressure(entry)

        val csv = exportAdapter.exportBloodPressureCsv(profile.id)
        assertTrue(csv.startsWith("timestamp,systolic_mmhg,diastolic_mmhg,classification\n"))
        assertTrue(csv.contains("2026-09-20T08:00:00Z,120,80,NORMAL"))

        healthLogRepo.bloodPressures.clear()
        val result = importAdapter.importCsv(profile.id, "blood_pressure", csv)
        assertEquals(1, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        assertEquals(1, healthLogRepo.bloodPressures.size)
    }

    @Test
    fun `export and import glucose CSV`() = runBlocking {
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val exportAdapter = CsvDataExportAdapter(healthLogRepo)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

        val entry = GlucoseEntry(
            MeasurementId.generate(), profile.id, Instant.parse("2026-09-20T08:00:00Z"),
            GlucoseLevel(BigDecimal("5.4")), GlucoseContext.FASTING, NhgGlucoseCategory.NORMAL
        )
        healthLogRepo.saveGlucose(entry)

        val csv = exportAdapter.exportGlucoseCsv(profile.id)
        assertTrue(csv.startsWith("timestamp,glucose_mmol_l,context,classification\n"))
        assertTrue(csv.contains("2026-09-20T08:00:00Z,5.4,FASTING,NORMAL"))

        healthLogRepo.glucoses.clear()
        val result = importAdapter.importCsv(profile.id, "glucose", csv)
        assertEquals(1, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        assertEquals(1, healthLogRepo.glucoses.size)
    }

    @Test
    fun `export and import activity CSV`() = runBlocking {
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val exportAdapter = CsvDataExportAdapter(healthLogRepo)
        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

        val session = ActivitySession(
            MeasurementId.generate(), profile.id,
            Instant.parse("2026-09-20T08:00:00Z"),
            Instant.parse("2026-09-20T09:00:00Z"),
            5000.0
        )
        healthLogRepo.saveActivity(session)

        val csv = exportAdapter.exportActivityCsv(profile.id)
        assertTrue(csv.startsWith("start_timestamp,end_timestamp,distance_m,duration_s\n"))
        assertTrue(csv.contains("2026-09-20T08:00:00Z,2026-09-20T09:00:00Z,5000.0,3600"))

        healthLogRepo.activities.clear()
        val result = importAdapter.importCsv(profile.id, "activity", csv)
        assertEquals(1, result.importedCount)
        assertEquals(0, result.skippedRows.size)
        assertEquals(1, healthLogRepo.activities.size)
    }

    @Test
    fun `malformed CSV row skipped with error report`() = runBlocking {
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

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
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

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
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1), HeightCm(180))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

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
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

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
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

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
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

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
        val healthLogRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val importAdapter = CsvDataImportAdapter(healthLogRepo, profileRepo)

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

    private fun importSetup(height: Int? = 180): Triple<InMemoryHealthLogRepository, Profile, CsvDataImportAdapter> {
        val logRepo = InMemoryHealthLogRepository()
        val profileRepo = InMemoryProfileRepository()
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1), height?.let { HeightCm(it) })
        runBlocking { profileRepo.save(profile) }
        return Triple(logRepo, profile, CsvDataImportAdapter(logRepo, profileRepo))
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
        val csv = CsvDataExportAdapter(logRepo).exportBloodPressureCsv(profile.id)
        assertTrue(csv.contains("2026-09-20T08:00:00Z,185,95,SERIOUSLY_RAISED"))

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
}
