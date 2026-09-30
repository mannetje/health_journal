package nl.healthjournal.app.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.history.EntryRef
import nl.healthjournal.app.ui.history.EntryUseCases
import nl.healthjournal.app.ui.history.HistoryFilter
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.logging.MetricType
import nl.healthjournal.app.ui.profile.ProfileViewModel
import nl.healthjournal.domain.model.common.GlucoseUnit
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.common.UnitSystem
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.HeightCm
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.metrics.WeightKg
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgBmiCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.model.profile.Sex
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ImportResult
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.CreateProfileUseCase
import nl.healthjournal.domain.usecase.DeleteActivityUseCase
import nl.healthjournal.domain.usecase.DeleteBloodPressureUseCase
import nl.healthjournal.domain.usecase.DeleteGlucoseUseCase
import nl.healthjournal.domain.usecase.DeleteWeightUseCase
import nl.healthjournal.domain.usecase.GetHealthHistoryUseCase
import nl.healthjournal.domain.usecase.UpdateActivityUseCase
import nl.healthjournal.domain.usecase.UpdateBloodPressureUseCase
import nl.healthjournal.domain.usecase.UpdateGlucoseUseCase
import nl.healthjournal.domain.usecase.UpdateWeightUseCase
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
import java.time.Instant
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
        override suspend fun updateWeight(entry: WeightEntry): Boolean {
            val i = weights.indexOfFirst { it.id == entry.id }
            if (i < 0) return false
            weights[i] = entry
            return true
        }
        override suspend fun deleteWeight(id: MeasurementId): Boolean = weights.removeAll { it.id == id }
        override suspend fun getWeightHistory(profileId: ProfileId): List<WeightEntry> = weights.filter { it.profileId == profileId }
        override fun observeWeightHistory(profileId: ProfileId): Flow<List<WeightEntry>> = emptyFlow()

        override suspend fun saveBloodPressure(entry: BloodPressureEntry) { bps.add(entry) }
        override suspend fun updateBloodPressure(entry: BloodPressureEntry): Boolean {
            val i = bps.indexOfFirst { it.id == entry.id }
            if (i < 0) return false
            bps[i] = entry
            return true
        }
        override suspend fun deleteBloodPressure(id: MeasurementId): Boolean = bps.removeAll { it.id == id }
        override suspend fun getBloodPressureHistory(profileId: ProfileId): List<BloodPressureEntry> = bps.filter { it.profileId == profileId }
        override fun observeBloodPressureHistory(profileId: ProfileId): Flow<List<BloodPressureEntry>> = emptyFlow()

        override suspend fun saveGlucose(entry: GlucoseEntry) { glucoses.add(entry) }
        override suspend fun updateGlucose(entry: GlucoseEntry): Boolean {
            val i = glucoses.indexOfFirst { it.id == entry.id }
            if (i < 0) return false
            glucoses[i] = entry
            return true
        }
        override suspend fun deleteGlucose(id: MeasurementId): Boolean = glucoses.removeAll { it.id == id }
        override suspend fun getGlucoseHistory(profileId: ProfileId): List<GlucoseEntry> = glucoses.filter { it.profileId == profileId }
        override fun observeGlucoseHistory(profileId: ProfileId): Flow<List<GlucoseEntry>> = emptyFlow()

        override suspend fun saveActivity(session: ActivitySession) { activities.add(session) }
        override suspend fun updateActivity(session: ActivitySession): Boolean {
            val i = activities.indexOfFirst { it.id == session.id }
            if (i < 0) return false
            activities[i] = session
            return true
        }
        override suspend fun deleteActivity(id: MeasurementId): Boolean = activities.removeAll { it.id == id }
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
    fun `ProfileViewModel update edits the active profile instead of adding another`() = runTest {
        val profileRepo = FakeProfileRepo()
        val vm = ProfileViewModel(profileRepo, CreateProfileUseCase(profileRepo))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.saveProfile("John Doe", LocalDate.of(1985, 4, 12), 178)
        testDispatcher.scheduler.advanceUntilIdle()
        val originalId = vm.uiState.value.activeProfile?.id

        vm.saveProfile("John D.", LocalDate.of(1985, 4, 12), 180, Sex.MALE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, profileRepo.profiles.size)
        assertEquals(originalId, state.activeProfile?.id)
        assertEquals("John D.", state.activeProfile?.name)
        assertEquals(HeightCm(180), state.activeProfile?.height)
        assertEquals(Sex.MALE, state.activeProfile?.sex)
        assertEquals(UiText.Res(R.string.profile_msg_saved, emptyList()), state.successMessage)
    }

    @Test
    fun `LoggingViewModel reports success and validation errors as resources, not English text`() = runTest {
        val profileRepo = FakeProfileRepo()
        val healthLogRepo = FakeHealthLogRepo()
        profileRepo.save(Profile.create("Alice", LocalDate.of(1990, 1, 1)))
        val vm = loggingViewModel(profileRepo, healthLogRepo, DisplayUnits.DEFAULT)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectMetric(MetricType.WEIGHT)
        vm.saveCurrentMetric()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiText.Res(R.string.log_err_weight, listOf("kg")), vm.uiState.value.errorMessage)

        vm.onWeightChanged("80")
        vm.saveCurrentMetric()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiText.Res(R.string.log_msg_weight_saved, emptyList()), vm.uiState.value.successMessage)
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

    private val imperialUnits = DisplayUnits(UnitSystem.IMPERIAL, GlucoseUnit.MG_PER_DL)

    private fun loggingViewModel(profileRepo: FakeProfileRepo, healthLogRepo: FakeHealthLogRepo, units: DisplayUnits) =
        LoggingViewModel(
            profileRepo,
            RecordWeightUseCase(healthLogRepo, profileRepo),
            RecordBloodPressureUseCase(healthLogRepo),
            RecordGlucoseUseCase(healthLogRepo),
            RecordActivityUseCase(healthLogRepo),
            units = { units }
        )

    @Test
    fun `LoggingViewModel stores imperial weight as metric kilograms`() = runTest {
        val profileRepo = FakeProfileRepo()
        val healthLogRepo = FakeHealthLogRepo()
        profileRepo.save(Profile.create("Alice", LocalDate.of(1990, 1, 1), HeightCm(170)))
        val vm = loggingViewModel(profileRepo, healthLogRepo, imperialUnits)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectMetric(MetricType.WEIGHT)
        vm.onWeightChanged("165")
        vm.saveCurrentMetric()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, healthLogRepo.weights.size)
        assertEquals(BigDecimal("74.84"), healthLogRepo.weights[0].weight.value)
    }

    @Test
    fun `LoggingViewModel stores mg per dL glucose as mmol per L`() = runTest {
        val profileRepo = FakeProfileRepo()
        val healthLogRepo = FakeHealthLogRepo()
        profileRepo.save(Profile.create("Alice", LocalDate.of(1990, 1, 1)))
        val vm = loggingViewModel(profileRepo, healthLogRepo, imperialUnits)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectMetric(MetricType.GLUCOSE)
        vm.setGlucoseContext(GlucoseContext.FASTING)
        vm.onGlucoseChanged("100")
        vm.saveCurrentMetric()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, healthLogRepo.glucoses.size)
        assertEquals(5.55, healthLogRepo.glucoses[0].glucose.valueInMmolL.toDouble(), 0.01)
    }

    @Test
    fun `LoggingViewModel stores miles as metric meters and kilometres as meters`() = runTest {
        val profileRepo = FakeProfileRepo()
        val healthLogRepo = FakeHealthLogRepo()
        profileRepo.save(Profile.create("Alice", LocalDate.of(1990, 1, 1)))

        for ((units, input, expectedMeters) in listOf(
            Triple(imperialUnits, "3", 4828.03),
            Triple(DisplayUnits.DEFAULT, "5", 5000.0)
        )) {
            healthLogRepo.activities.clear()
            val vm = loggingViewModel(profileRepo, healthLogRepo, units)
            testDispatcher.scheduler.advanceUntilIdle()
            vm.selectMetric(MetricType.ACTIVITY)
            vm.onActivityDurationChanged("30")
            vm.onActivityDistanceChanged(input)
            vm.saveCurrentMetric()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, healthLogRepo.activities.size)
            assertEquals(expectedMeters, healthLogRepo.activities[0].distanceInMeters, 0.5)
        }
    }

    private fun entryUseCases(health: HealthLogRepositoryPort, profiles: ProfileRepositoryPort) = EntryUseCases(
        updateWeight = UpdateWeightUseCase(health, profiles),
        updateBloodPressure = UpdateBloodPressureUseCase(health),
        updateGlucose = UpdateGlucoseUseCase(health),
        updateActivity = UpdateActivityUseCase(health),
        deleteWeight = DeleteWeightUseCase(health),
        deleteBloodPressure = DeleteBloodPressureUseCase(health),
        deleteGlucose = DeleteGlucoseUseCase(health),
        deleteActivity = DeleteActivityUseCase(health)
    )

    @Test
    fun `HistoryViewModel refreshes state after editing and deleting an entry`() = runTest {
        val profileRepo = FakeProfileRepo()
        val healthLogRepo = FakeHealthLogRepo()
        val profile = Profile.create("Alice", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)
        val older = WeightEntry(MeasurementId.generate(), profile.id, Instant.parse("2026-01-01T08:00:00Z"), WeightKg(BigDecimal("80.0")), null)
        val newest = WeightEntry(MeasurementId.generate(), profile.id, Instant.parse("2026-02-01T08:00:00Z"), WeightKg(BigDecimal("79.0")), null)
        healthLogRepo.weights.addAll(listOf(older, newest))

        val vm = HistoryViewModel(
            profileRepo,
            GetHealthHistoryUseCase(healthLogRepo),
            FakeExportAdapter(),
            FakeImportAdapter(),
            entryUseCases(healthLogRepo, profileRepo)
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, vm.uiState.value.weights.size)

        vm.startEdit(EntryRef.Weight(older))
        vm.updateWeight(older, BigDecimal("85.5"), older.timestamp)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, vm.uiState.value.editing)
        assertEquals(BigDecimal("85.5"), vm.uiState.value.weights.first { it.id == older.id }.weight.value)

        vm.requestDelete(EntryRef.Weight(newest))
        vm.cancelDelete()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, vm.uiState.value.weights.size)

        vm.requestDelete(EntryRef.Weight(newest))
        vm.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, vm.uiState.value.pendingDelete)
        assertEquals(listOf(older.id), vm.uiState.value.weights.map { it.id })
    }

    @Test
    fun `HistoryViewModel keeps the edit dialog open with an error when the update fails`() = runTest {
        val profileRepo = FakeProfileRepo()
        val healthLogRepo = FakeHealthLogRepo()
        val profile = Profile.create("Alice", LocalDate.of(1990, 1, 1))
        profileRepo.save(profile)
        val entry = WeightEntry(MeasurementId.generate(), profile.id, Instant.parse("2026-01-01T08:00:00Z"), WeightKg(BigDecimal("80.0")), null)
        healthLogRepo.weights.add(entry)
        val vm = HistoryViewModel(
            profileRepo,
            GetHealthHistoryUseCase(healthLogRepo),
            FakeExportAdapter(),
            FakeImportAdapter(),
            entryUseCases(healthLogRepo, profileRepo)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startEdit(EntryRef.Weight(entry))
        vm.updateWeight(entry, BigDecimal("-1"), entry.timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.editing)
        assertNotNull(vm.uiState.value.editError)
        assertEquals(BigDecimal("80.0"), vm.uiState.value.weights.single().weight.value)
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
            FakeImportAdapter(),
            entryUseCases(healthLogRepo, profileRepo)
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
