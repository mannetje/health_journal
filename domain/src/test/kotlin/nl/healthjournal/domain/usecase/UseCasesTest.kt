package nl.healthjournal.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
    override suspend fun getWeightHistory(profileId: ProfileId): List<WeightEntry> =
        weights.filter { it.profileId == profileId }
    override fun observeWeightHistory(profileId: ProfileId): Flow<List<WeightEntry>> =
        flow { emit(getWeightHistory(profileId)) }

    override suspend fun saveBloodPressure(entry: BloodPressureEntry) { bloodPressures.add(entry) }
    override suspend fun getBloodPressureHistory(profileId: ProfileId): List<BloodPressureEntry> =
        bloodPressures.filter { it.profileId == profileId }
    override fun observeBloodPressureHistory(profileId: ProfileId): Flow<List<BloodPressureEntry>> =
        flow { emit(getBloodPressureHistory(profileId)) }

    override suspend fun saveGlucose(entry: GlucoseEntry) { glucoses.add(entry) }
    override suspend fun getGlucoseHistory(profileId: ProfileId): List<GlucoseEntry> =
        glucoses.filter { it.profileId == profileId }
    override fun observeGlucoseHistory(profileId: ProfileId): Flow<List<GlucoseEntry>> =
        flow { emit(getGlucoseHistory(profileId)) }

    override suspend fun saveActivity(session: ActivitySession) { activities.add(session) }
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

        assertEquals(NhgBloodPressureCategory.HYPERTENSION_GRADE_1, entry.category)
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
}
