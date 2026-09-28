package nl.healthjournal.app.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.healthjournal.app.ui.history.HistoryFilter
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.logging.MetricType
import nl.healthjournal.app.ui.profile.ProfileViewModel
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.HeightCm
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgBmiCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ImportResult
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.CreateProfileUseCase
import nl.healthjournal.domain.usecase.GetHealthHistoryUseCase
import nl.healthjournal.domain.usecase.RecordActivityUseCase
import nl.healthjournal.domain.usecase.RecordBloodPressureUseCase
import nl.healthjournal.domain.usecase.RecordGlucoseUseCase
import nl.healthjournal.domain.usecase.RecordWeightUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeProfileRepo : ProfileRepositoryPort {
        val profiles = mutableMapOf<ProfileId, Profile>()
        var activeId: ProfileId? = null
        override suspend fun save(profile: Profile) {
            profiles[profile.id] = profile
            if (activeId == null) activeId = profile.id
        }
        override suspend fun getById(id: ProfileId): Profile? = profiles[id]
        override suspend fun getAll(): List<Profile> = profiles.values.toList()
        override suspend fun getActiveProfile(): Profile? = activeId?.let { profiles[it] }
        override suspend fun setActiveProfile(id: ProfileId) { activeId = id }
    }

    private class FakeHealthLogRepo : HealthLogRepositoryPort {
        val weights = mutableListOf<WeightEntry>()
        val bps = mutableListOf<BloodPressureEntry>()
        val glucoses = mutableListOf<GlucoseEntry>()
        val activities = mutableListOf<ActivitySession>()

        override suspend fun saveWeight(entry: WeightEntry) { weights.add(entry) }
        override suspend fun getWeightHistory(profileId: ProfileId): List<WeightEntry> = weights.filter { it.profileId == profileId }
        override fun observeWeightHistory(profileId: ProfileId): Flow<List<WeightEntry>> = emptyFlow()

        override suspend fun saveBloodPressure(entry: BloodPressureEntry) { bps.add(entry) }
        override suspend fun getBloodPressureHistory(profileId: ProfileId): List<BloodPressureEntry> = bps.filter { it.profileId == profileId }
        override fun observeBloodPressureHistory(profileId: ProfileId): Flow<List<BloodPressureEntry>> = emptyFlow()

        override suspend fun saveGlucose(entry: GlucoseEntry) { glucoses.add(entry) }
        override suspend fun getGlucoseHistory(profileId: ProfileId): List<GlucoseEntry> = glucoses.filter { it.profileId == profileId }
        override fun observeGlucoseHistory(profileId: ProfileId): Flow<List<GlucoseEntry>> = emptyFlow()

        override suspend fun saveActivity(session: ActivitySession) { activities.add(session) }
        override suspend fun getActivityHistory(profileId: ProfileId): List<ActivitySession> = activities.filter { it.profileId == profileId }
        override fun observeActivityHistory(profileId: ProfileId): Flow<List<ActivitySession>> = emptyFlow()
    }

    private class FakeExportAdapter : DataExportPort {
        override suspend fun exportWeightCsv(profileId: ProfileId): String = "timestamp,weight_kg,bmi\n"
        override suspend fun exportBloodPressureCsv(profileId: ProfileId): String = "timestamp,systolic_mmhg,diastolic_mmhg,classification\n"
        override suspend fun exportGlucoseCsv(profileId: ProfileId): String = "timestamp,glucose_mmol_l,context,classification\n"
        override suspend fun exportActivityCsv(profileId: ProfileId): String = "start_timestamp,end_timestamp,distance_m,duration_s\n"
    }

    private class FakeImportAdapter : DataImportPort {
        override suspend fun importCsv(profileId: ProfileId, metricType: String, csvContent: String): ImportResult =
            ImportResult(importedCount = 1, skippedRows = emptyList())
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ProfileViewModel creates and loads active profile`() = runTest {
        val profileRepo = FakeProfileRepo()
        val createUseCase = CreateProfileUseCase(profileRepo)
        val vm = ProfileViewModel(profileRepo, createUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.saveProfile("John Doe", LocalDate.of(1985, 4, 12), 182)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.activeProfile)
        assertEquals("John Doe", state.activeProfile?.name)
        assertEquals(HeightCm(182), state.activeProfile?.height)
    }

    @Test
    fun `LoggingViewModel previews and records weight with BMI`() = runTest {
        val profileRepo = FakeProfileRepo()
        val healthLogRepo = FakeHealthLogRepo()
        val profile = Profile.create("Alice", LocalDate.of(1990, 1, 1), HeightCm(170))
        profileRepo.save(profile)

        val vm = LoggingViewModel(
            profileRepo,
            RecordWeightUseCase(healthLogRepo, profileRepo),
            RecordBloodPressureUseCase(healthLogRepo),
            RecordGlucoseUseCase(healthLogRepo),
            RecordActivityUseCase(healthLogRepo)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectMetric(MetricType.WEIGHT)
        vm.onWeightChanged("70.0")

        assertEquals(BigDecimal("24.2"), vm.uiState.value.previewBmi)
        assertEquals(NhgBmiCategory.NORMAL, vm.uiState.value.previewBmiCategory)

        vm.saveCurrentMetric()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, healthLogRepo.weights.size)
        assertEquals(70.0, healthLogRepo.weights[0].weight.value.toDouble(), 0.01)
    }

    @Test
    fun `LoggingViewModel previews and records blood pressure with NHG category`() = runTest {
        val profileRepo = FakeProfileRepo()
        val healthLogRepo = FakeHealthLogRepo()
        val profile = Profile.create("Alice", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val vm = LoggingViewModel(
            profileRepo,
            RecordWeightUseCase(healthLogRepo, profileRepo),
            RecordBloodPressureUseCase(healthLogRepo),
            RecordGlucoseUseCase(healthLogRepo),
            RecordActivityUseCase(healthLogRepo)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectMetric(MetricType.BLOOD_PRESSURE)
        vm.onSystolicChanged("120")
        vm.onDiastolicChanged("80")

        assertEquals(NhgBloodPressureCategory.NORMAL, vm.uiState.value.previewBpCategory)

        vm.saveCurrentMetric()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, healthLogRepo.bps.size)
        assertEquals(120, healthLogRepo.bps[0].reading.systolic)
    }

    @Test
    fun `LoggingViewModel previews and records glucose with conversion`() = runTest {
        val profileRepo = FakeProfileRepo()
        val healthLogRepo = FakeHealthLogRepo()
        val profile = Profile.create("Alice", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val vm = LoggingViewModel(
            profileRepo,
            RecordWeightUseCase(healthLogRepo, profileRepo),
            RecordBloodPressureUseCase(healthLogRepo),
            RecordGlucoseUseCase(healthLogRepo),
            RecordActivityUseCase(healthLogRepo)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectMetric(MetricType.GLUCOSE)
        vm.setGlucoseContext(GlucoseContext.FASTING)
        vm.onGlucoseChanged("5.5")

        assertEquals(NhgGlucoseCategory.NORMAL, vm.uiState.value.previewGlucoseCategory)

        vm.saveCurrentMetric()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, healthLogRepo.glucoses.size)
        assertEquals(5.5, healthLogRepo.glucoses[0].glucose.valueInMmolL.toDouble(), 0.01)
    }

    @Test
    fun `HistoryViewModel filters and triggers CSV export and import`() = runTest {
        val profileRepo = FakeProfileRepo()
        val healthLogRepo = FakeHealthLogRepo()
        val profile = Profile.create("Alice", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)

        val vm = HistoryViewModel(
            profileRepo,
            GetHealthHistoryUseCase(healthLogRepo),
            FakeExportAdapter(),
            FakeImportAdapter()
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setFilter(HistoryFilter.WEIGHT)
        assertEquals(HistoryFilter.WEIGHT, vm.uiState.value.selectedFilter)

        vm.exportCsv("weight")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("timestamp,weight_kg,bmi\n", vm.uiState.value.exportedCsvContent)

        vm.importCsv("weight", "timestamp,weight_kg,bmi\n2026-09-20T08:00:00Z,70.0,22.0")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.uiState.value.importResult?.importedCount)
    }
}
