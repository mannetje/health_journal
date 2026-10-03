package nl.healthjournal.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.*
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class FakeProfileRepository : ProfileRepositoryPort {
    val profiles = mutableMapOf<ProfileId, Profile>()
    var activeId: ProfileId? = null

    override suspend fun save(profile: Profile) {
        profiles[profile.id] = profile
    }

    override suspend fun getById(id: ProfileId): Profile? = profiles[id]

    override suspend fun getAll(): List<Profile> = profiles.values.toList()

    override suspend fun getActiveProfile(): Profile? = activeId?.let { profiles[it] }

    override suspend fun setActiveProfile(id: ProfileId) {
        activeId = id
    }
}

class FakeHealthLogRepository : HealthLogRepositoryPort {
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
    override fun observeWeightHistory(profileId: ProfileId): Flow<List<WeightEntry>> =
        flow { emit(getWeightHistory(profileId)) }

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
    override fun observeBloodPressureHistory(profileId: ProfileId): Flow<List<BloodPressureEntry>> =
        flow { emit(getBloodPressureHistory(profileId)) }

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
    override fun observeGlucoseHistory(profileId: ProfileId): Flow<List<GlucoseEntry>> =
        flow { emit(getGlucoseHistory(profileId)) }

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
    override fun observeActivityHistory(profileId: ProfileId): Flow<List<ActivitySession>> =
        flow { emit(getActivityHistory(profileId)) }
}

class UseCasesTest {

    private lateinit var profileRepo: FakeProfileRepository
    private lateinit var healthRepo: FakeHealthLogRepository

    @Before
    fun setup() {
        profileRepo = FakeProfileRepository()
        healthRepo = FakeHealthLogRepository()
    }

    @Test
    fun `CreateProfileUseCase creates profile and sets active`() = runBlocking {
        val useCase = CreateProfileUseCase(profileRepo)
        val profileId = useCase(
            name = "Maria Jansen",
            dateOfBirth = LocalDate.of(1982, 7, 24),
            heightCm = 170
        )

        assertNotNull(profileId)
        val saved = profileRepo.getById(profileId)
        assertNotNull(saved)
        assertEquals("Maria Jansen", saved?.name)
        assertEquals(profileId, profileRepo.getActiveProfile()?.id)
    }

    @Test
    fun `RecordBloodPressureUseCase classifies according to NHG and saves`() = runBlocking {
        val profileUseCase = CreateProfileUseCase(profileRepo)
        val profileId = profileUseCase("Test", LocalDate.of(1990, 1, 1), 180)

        val bpUseCase = RecordBloodPressureUseCase(healthRepo)
        val entry = bpUseCase(
            profileId = profileId,
            systolic = 145,
            diastolic = 92
        )

        assertEquals(NhgBloodPressureCategory.HIGH, entry.category)
        assertEquals(1, healthRepo.bloodPressures.size)
    }

    @Test
    fun `RecordGlucoseUseCase with mg per dL converts and evaluates NHG`() = runBlocking {
        val profileUseCase = CreateProfileUseCase(profileRepo)
        val profileId = profileUseCase("Test", LocalDate.of(1990, 1, 1), 180)

        val glucoseUseCase = RecordGlucoseUseCase(healthRepo)
        // 100 mg/dL * 0.0555 = 5.55 mmol/L (Normal for fasting)
        val entry = glucoseUseCase(
            profileId = profileId,
            context = GlucoseContext.FASTING,
            valueInMgDl = BigDecimal("100.0")
        )

        assertEquals(BigDecimal("5.55"), entry.glucose.valueInMmolL)
        assertEquals(NhgGlucoseCategory.NORMAL, entry.category)
        assertEquals(1, healthRepo.glucoses.size)
    }

    @Test
    fun `RecordWeightUseCase calculates BMI from profile height`() = runBlocking {
        val profileUseCase = CreateProfileUseCase(profileRepo)
        val profileId = profileUseCase("Test", LocalDate.of(1990, 1, 1), 180)

        val weightUseCase = RecordWeightUseCase(healthRepo, profileRepo)
        val entry = weightUseCase(
            profileId = profileId,
            weightKg = BigDecimal("81.0")
        )

        assertEquals(BigDecimal("25.0"), entry.bmi)
        assertEquals(1, healthRepo.weights.size)
    }

    @Test
    fun `RecordActivityUseCase persists a valid session`() = runBlocking {
        val profileUseCase = CreateProfileUseCase(profileRepo)
        val profileId = profileUseCase("Test", LocalDate.of(1990, 1, 1), 180)

        val activityUseCase = RecordActivityUseCase(healthRepo)
        val start = Instant.parse("2026-01-01T10:00:00Z")
        val end = Instant.parse("2026-01-01T10:30:00Z")
        val session = activityUseCase(
            profileId = profileId,
            startTime = start,
            endTime = end,
            distanceInMeters = 5_000.0
        )

        assertEquals(1_800L, session.durationInSeconds)
        assertEquals(1, healthRepo.activities.size)
    }

    @Test
    fun `RecordActivityUseCase rejects end before start`() = runBlocking {
        val activityUseCase = RecordActivityUseCase(healthRepo)
        val now = Instant.parse("2026-01-01T10:00:00Z")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                activityUseCase(
                    profileId = ProfileId.generate(),
                    startTime = now,
                    endTime = now.minusSeconds(60),
                    distanceInMeters = 1_000.0
                )
            }
        }
        Unit
    }

    @Test
    fun `GetHealthHistoryUseCase aggregates all metric entries`() = runBlocking {
        val profileUseCase = CreateProfileUseCase(profileRepo)
        val profileId = profileUseCase("Test", LocalDate.of(1990, 1, 1), 180)

        val bpUseCase = RecordBloodPressureUseCase(healthRepo)
        bpUseCase(profileId, 120, 80)

        val weightUseCase = RecordWeightUseCase(healthRepo, profileRepo)
        weightUseCase(profileId, BigDecimal("75.0"))

        val historyUseCase = GetHealthHistoryUseCase(healthRepo)
        val history = historyUseCase(profileId)

        assertEquals(1, history.bloodPressures.size)
        assertEquals(1, history.weights.size)
        assertEquals(0, history.glucoses.size)
    }

    @Test
    fun `UpdateWeightUseCase keeps id and profile and recomputes BMI`() = runBlocking {
        val profileId = CreateProfileUseCase(profileRepo)("Test", LocalDate.of(1990, 1, 1), 180)
        val original = RecordWeightUseCase(healthRepo, profileRepo)(profileId, BigDecimal("80.0"))
        val newTime = Instant.parse("2026-01-01T08:00:00Z")

        val updated = UpdateWeightUseCase(healthRepo, profileRepo)(original.id, profileId, BigDecimal("90.0"), newTime)

        assertNotNull(updated)
        assertEquals(1, healthRepo.weights.size)
        assertEquals(original.id, healthRepo.weights[0].id)
        assertEquals(BigDecimal("90.0"), healthRepo.weights[0].weight.value)
        assertEquals(newTime, healthRepo.weights[0].timestamp)
        assertNotEquals(original.bmi, healthRepo.weights[0].bmi)
    }

    @Test
    fun `UpdateWeightUseCase rejects invalid weight and leaves data unchanged`() = runBlocking {
        val profileId = CreateProfileUseCase(profileRepo)("Test", LocalDate.of(1990, 1, 1), 180)
        val original = RecordWeightUseCase(healthRepo, profileRepo)(profileId, BigDecimal("80.0"))

        try {
            UpdateWeightUseCase(healthRepo, profileRepo)(original.id, profileId, BigDecimal("-5"), Instant.now())
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // domain validation
        }
        assertEquals(BigDecimal("80.0"), healthRepo.weights[0].weight.value)
    }

    @Test
    fun `Update use cases return null for an unknown id and create nothing`() = runBlocking {
        val profileId = CreateProfileUseCase(profileRepo)("Test", LocalDate.of(1990, 1, 1), 180)
        val unknown = MeasurementId.generate()

        assertNull(UpdateWeightUseCase(healthRepo, profileRepo)(unknown, profileId, BigDecimal("70"), Instant.now()))
        assertNull(UpdateBloodPressureUseCase(healthRepo)(unknown, profileId, 120, 80, Instant.now()))
        assertNull(UpdateGlucoseUseCase(healthRepo)(unknown, profileId, GlucoseContext.FASTING, BigDecimal("5.5"), Instant.now()))
        val start = Instant.parse("2026-01-01T08:00:00Z")
        assertNull(UpdateActivityUseCase(healthRepo)(unknown, profileId, start, start.plusSeconds(600), 1000.0))

        assertTrue(healthRepo.weights.isEmpty() && healthRepo.bloodPressures.isEmpty())
        assertTrue(healthRepo.glucoses.isEmpty() && healthRepo.activities.isEmpty())
    }

    @Test
    fun `UpdateBloodPressureUseCase and UpdateGlucoseUseCase reclassify`() = runBlocking {
        val profileId = CreateProfileUseCase(profileRepo)("Test", LocalDate.of(1990, 1, 1), 180)
        val bp = RecordBloodPressureUseCase(healthRepo)(profileId, 120, 80)
        val updatedBp = UpdateBloodPressureUseCase(healthRepo)(bp.id, profileId, 145, 92, bp.timestamp)
        assertEquals(NhgBloodPressureCategory.HIGH, updatedBp?.category)
        assertEquals(1, healthRepo.bloodPressures.size)

        val g = RecordGlucoseUseCase(healthRepo)(profileId, GlucoseContext.FASTING, valueInMmolL = BigDecimal("5.0"))
        val updatedG = UpdateGlucoseUseCase(healthRepo)(g.id, profileId, GlucoseContext.POSTPRANDIAL, BigDecimal("12.0"), g.timestamp)
        assertEquals(GlucoseContext.POSTPRANDIAL, healthRepo.glucoses[0].context)
        assertNotEquals(g.category, updatedG?.category)
    }

    @Test
    fun `UpdateActivityUseCase rejects end before start and keeps the stored session`() = runBlocking {
        val profileId = CreateProfileUseCase(profileRepo)("Test", LocalDate.of(1990, 1, 1), 180)
        val start = Instant.parse("2026-01-01T08:00:00Z")
        val session = RecordActivityUseCase(healthRepo)(profileId, start, start.plusSeconds(1800), 5000.0)

        try {
            UpdateActivityUseCase(healthRepo)(session.id, profileId, start, start.minusSeconds(60), 1000.0)
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // end must be after start
        }
        assertEquals(5000.0, healthRepo.activities[0].distanceInMeters, 0.0)
    }

    @Test
    fun `Delete use cases remove only the target entry and report unknown ids`() = runBlocking {
        val profileId = CreateProfileUseCase(profileRepo)("Test", LocalDate.of(1990, 1, 1), 180)
        val record = RecordWeightUseCase(healthRepo, profileRepo)
        val a = record(profileId, BigDecimal("70"))
        val b = record(profileId, BigDecimal("71"))

        assertTrue(DeleteWeightUseCase(healthRepo)(a.id))
        assertEquals(listOf(b.id), healthRepo.weights.map { it.id })
        assertFalse(DeleteWeightUseCase(healthRepo)(a.id))

        val bp = RecordBloodPressureUseCase(healthRepo)(profileId, 120, 80)
        assertTrue(DeleteBloodPressureUseCase(healthRepo)(bp.id))
        assertTrue(healthRepo.bloodPressures.isEmpty())
        assertFalse(DeleteGlucoseUseCase(healthRepo)(MeasurementId.generate()))
        assertFalse(DeleteActivityUseCase(healthRepo)(MeasurementId.generate()))
    }
}
