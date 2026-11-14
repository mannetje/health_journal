package nl.healthjournal.app.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.healthjournal.app.command.DefaultDispatcher
import nl.healthjournal.app.ui.fluid.FluidLogViewModel
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.domain.metrics.command.ObserveFluidHistoryHandler
import nl.healthjournal.domain.metrics.command.ObserveFluidHistoryQuery
import nl.healthjournal.domain.metrics.command.ObserveFluidLimitsHandler
import nl.healthjournal.domain.metrics.command.ObserveFluidLimitsQuery
import nl.healthjournal.domain.metrics.command.RecordFluidIntakeCommand
import nl.healthjournal.domain.metrics.command.RecordFluidIntakeHandler
import nl.healthjournal.domain.metrics.command.SetFluidLimitsCommand
import nl.healthjournal.domain.metrics.command.SetFluidLimitsHandler
import nl.healthjournal.domain.metrics.model.FluidDrink
import nl.healthjournal.domain.metrics.port.DataExportPort
import nl.healthjournal.domain.metrics.port.DataImportPort
import nl.healthjournal.domain.shared.model.ImportResult
import nl.healthjournal.domain.metrics.model.FluidIntakeEntry
import nl.healthjournal.domain.metrics.model.FluidLimits
import nl.healthjournal.domain.metrics.model.FluidMl
import nl.healthjournal.domain.metrics.model.FluidPosition
import nl.healthjournal.domain.migraine.port.MigraineExportPort
import nl.healthjournal.domain.migraine.port.MigraineImportPort
import nl.healthjournal.domain.profile.command.GetActiveProfileHandler
import nl.healthjournal.domain.profile.command.GetActiveProfileQuery
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.port.ProfileRepositoryPort
import nl.healthjournal.domain.shared.command.CommandHandler
import nl.healthjournal.domain.shared.command.DeleteEntryCommand
import nl.healthjournal.domain.shared.command.DeleteEntryHandler
import nl.healthjournal.domain.shared.command.Dispatcher
import nl.healthjournal.domain.shared.command.ObserveQueryHandler
import nl.healthjournal.domain.shared.command.PortEntryGateway
import nl.healthjournal.domain.shared.command.QueryHandler
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.testing.FakeHealthLogRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class FluidLogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    /** A clock the test can move, so the day can roll over without waiting. */
    private class MovableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }

    private class FakeProfiles(private val profile: Profile?) : ProfileRepositoryPort {
        override suspend fun save(profile: Profile) {}
        override suspend fun getById(id: ProfileId): Profile? = profile?.takeIf { it.id == id }
        override suspend fun getAll(): List<Profile> = listOfNotNull(profile)
        override suspend fun getActiveProfile(): Profile? = profile
        override suspend fun setActiveProfile(id: ProfileId) {}
    }


    private class FakeExportAdapter : DataExportPort {
        override suspend fun exportWeightCsv(profileId: ProfileId): String = ""
        override suspend fun exportBloodPressureCsv(profileId: ProfileId): String = ""
        override suspend fun exportGlucoseCsv(profileId: ProfileId): String = ""
        override suspend fun exportActivityCsv(profileId: ProfileId): String = ""
        override suspend fun exportWaistCircumferenceCsv(profileId: ProfileId): String = ""
        override suspend fun exportOxygenSaturationCsv(profileId: ProfileId): String = ""
        override suspend fun exportFluidCsv(profileId: ProfileId): String = ""
    }

    private class FakeImportAdapter : DataImportPort {
        override suspend fun importCsv(profileId: ProfileId, metricType: String, csvContent: String): ImportResult =
            ImportResult(importedCount = 0, skippedRows = emptyList())
    }

    private class FakeMigraineExport : MigraineExportPort {
        override suspend fun exportMigraineCsv(profileId: ProfileId): String = ""
    }

    private class FakeMigraineImport : MigraineImportPort {
        override suspend fun importMigraineCsv(profileId: ProfileId, csvContent: String): ImportResult =
            ImportResult(importedCount = 0, skippedRows = emptyList())
    }

    private val profile = Profile.create("Sample", LocalDate.of(1990, 1, 1))
    private val clock = MovableClock(Instant.parse("2026-11-14T09:00:00Z"))
    private lateinit var health: FakeHealthLogRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        health = FakeHealthLogRepository()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun fluidDispatcher(profiles: ProfileRepositoryPort): Dispatcher = DefaultDispatcher(
        commands = mapOf<Class<*>, CommandHandler<*, *>>(
            RecordFluidIntakeCommand::class.java to RecordFluidIntakeHandler(health, clock),
            SetFluidLimitsCommand::class.java to SetFluidLimitsHandler(health),
            DeleteEntryCommand::class.java to DeleteEntryHandler(PortEntryGateway(health, health, health, health, health, health, FakeMigraines(), health))
        ),
        queries = mapOf<Class<*>, QueryHandler<*, *>>(
            GetActiveProfileQuery::class.java to GetActiveProfileHandler(profiles)
        ),
        observers = mapOf<Class<*>, ObserveQueryHandler<*, *>>(
            ObserveFluidHistoryQuery::class.java to ObserveFluidHistoryHandler(health),
            ObserveFluidLimitsQuery::class.java to ObserveFluidLimitsHandler(health)
        )
    )

    private fun viewModel(withProfile: Boolean = true) =
        FluidLogViewModel(fluidDispatcher(FakeProfiles(profile.takeIf { withProfile })), clock, { ZoneOffset.UTC })

    private fun entryAt(instant: String, ml: Int = 250) =
        FluidIntakeEntry(MeasurementId.generate(), profile.id, Instant.parse(instant), FluidDrink.WATER, FluidMl(ml))

    @Test
    fun `a tap stores one entry of the count times the drink's portion and resets the count`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.stepCount(FluidDrink.COFFEE, 1)
        assertEquals(2, vm.uiState.value.countOf(FluidDrink.COFFEE))
        vm.add(FluidDrink.COFFEE)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(1, health.fluidIntakes.size)
        assertEquals(FluidDrink.COFFEE, health.fluidIntakes.single().drink)
        assertEquals(250, health.fluidIntakes.single().amount.value)
        assertEquals(250, s.todayMl)
        assertEquals(1, s.todayEntries.size)
        assertEquals(1, s.countOf(FluidDrink.COFFEE))
        assertEquals(health.fluidIntakes.single().id, s.lastAdded?.id)
    }

    @Test
    fun `the count stays between one and ten`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        repeat(15) { vm.stepCount(FluidDrink.WATER, 1) }
        assertEquals(10, vm.uiState.value.countOf(FluidDrink.WATER))
        repeat(15) { vm.stepCount(FluidDrink.WATER, -1) }
        assertEquals(1, vm.uiState.value.countOf(FluidDrink.WATER))
    }

    @Test
    fun `without limits there is no position`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.add(FluidDrink.WATER)
        advanceUntilIdle()
        assertEquals(FluidPosition.NONE, vm.uiState.value.position)
        assertEquals(FluidLimits.NONE, vm.uiState.value.limits)
    }

    @Test
    fun `the position follows the person's own numbers and each tap`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.saveLimits(500, 600)
        vm.add(FluidDrink.WATER)
        advanceUntilIdle()
        assertEquals(250, vm.uiState.value.todayMl)
        assertEquals(FluidPosition.BELOW_MIN, vm.uiState.value.position)

        vm.add(FluidDrink.WATER)
        advanceUntilIdle()
        assertEquals(FluidPosition.WITHIN, vm.uiState.value.position)

        vm.add(FluidDrink.WATER)
        advanceUntilIdle()
        assertEquals(750, vm.uiState.value.todayMl)
        assertEquals(FluidPosition.ABOVE_MAX, vm.uiState.value.position)

        vm.saveLimits(null, null)
        advanceUntilIdle()
        assertEquals(FluidPosition.NONE, vm.uiState.value.position)
        assertEquals(3, health.fluidIntakes.size)
    }

    @Test
    fun `limits that break the rules are refused with a message and nothing is stored`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.saveLimits(3000, 1500)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.message)
        assertEquals(FluidLimits.NONE, vm.uiState.value.limits)

        vm.saveLimits(50, null)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.message)
        assertEquals(FluidLimits.NONE, health.getFluidLimits(profile.id))
    }

    @Test
    fun `only entries of today's local day count, and the day rolls over when the screen asks for a recount`() = runTest(testDispatcher) {
        health.fluidIntakes.add(entryAt("2026-11-13T23:30:00Z", 300))
        health.fluidIntakes.add(entryAt("2026-11-14T08:00:00Z", 250))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(250, vm.uiState.value.todayMl)
        assertEquals(1, vm.uiState.value.todayEntries.size)

        // Past midnight nothing is emitted, so without the recount the screen would still show yesterday's drinks.
        clock.now = Instant.parse("2026-11-15T00:05:00Z")
        vm.refreshToday()
        assertEquals(0, vm.uiState.value.todayMl)
        assertEquals(emptyList<FluidIntakeEntry>(), vm.uiState.value.todayEntries)
        assertEquals(FluidPosition.NONE, vm.uiState.value.position)
    }

    @Test
    fun `undo removes the last tap`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.add(FluidDrink.TEA)
        advanceUntilIdle()
        vm.undo()
        advanceUntilIdle()
        assertEquals(0, health.fluidIntakes.size)
        assertEquals(0, vm.uiState.value.todayMl)
        assertNull(vm.uiState.value.lastAdded)
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `an undo that fails tells the person instead of pretending the drink is gone`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.add(FluidDrink.TEA)
        advanceUntilIdle()
        health.fluidIntakes.clear() // already gone, so the delete finds nothing
        vm.undo()
        advanceUntilIdle()
        assertNull(vm.uiState.value.lastAdded)
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `without a profile nothing is recorded`() = runTest(testDispatcher) {
        val vm = viewModel(withProfile = false)
        advanceUntilIdle()
        vm.add(FluidDrink.WATER)
        vm.saveLimits(1000, 2000)
        advanceUntilIdle()
        assertEquals(0, health.fluidIntakes.size)
        assertEquals(FluidLimits.NONE, vm.uiState.value.limits)
    }

    @Test
    fun `History follows limits changed after it loaded`() = runTest(testDispatcher) {
        val profiles = FakeProfiles(profile)
        val history = HistoryViewModel(
            historyDispatcher(
                profiles, health, FakeMigraines(), FakeExportAdapter(), FakeImportAdapter(), FakeMigraineExport(), FakeMigraineImport(), clock
            )
        )
        advanceUntilIdle()
        assertEquals(FluidLimits.NONE, history.uiState.value.fluidLimits)

        health.saveFluidLimits(profile.id, FluidLimits(1500, 2500))
        advanceUntilIdle()
        assertEquals(FluidLimits(1500, 2500), history.uiState.value.fluidLimits)

        health.saveFluidLimits(profile.id, FluidLimits(null, 2000))
        advanceUntilIdle()
        assertEquals(FluidLimits(null, 2000), history.uiState.value.fluidLimits)
    }

    @Test
    fun `History lists the fluid entries and the person's limits for the active profile`() = runTest(testDispatcher) {
        health.fluidIntakes.add(entryAt("2026-11-14T08:00:00Z"))
        health.saveFluidLimits(profile.id, FluidLimits(1500, null))
        val history = HistoryViewModel(
            historyDispatcher(
                FakeProfiles(profile), health, FakeMigraines(), FakeExportAdapter(), FakeImportAdapter(), FakeMigraineExport(), FakeMigraineImport(), clock
            )
        )
        advanceUntilIdle()
        assertEquals(1, history.uiState.value.fluidIntakes.size)
        assertEquals(FluidLimits(1500, null), history.uiState.value.fluidLimits)

        history.showFluidDay(LocalDate.of(2026, 11, 14))
        assertEquals(LocalDate.of(2026, 11, 14), history.uiState.value.fluidDay)
        history.clearFluidDay()
        assertNull(history.uiState.value.fluidDay)
    }
}
