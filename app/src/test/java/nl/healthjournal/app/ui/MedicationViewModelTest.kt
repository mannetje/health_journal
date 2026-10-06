package nl.healthjournal.app.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.medication.DayStatus
import nl.healthjournal.app.ui.medication.MedicationDraft
import nl.healthjournal.app.ui.medication.MedicationUseCases
import nl.healthjournal.app.ui.medication.MedicationViewModel
import nl.healthjournal.app.ui.medication.TimeOfDay
import nl.healthjournal.app.ui.medication.sameRhythm
import nl.healthjournal.app.ui.medication.weekStatusOf
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeId
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.PillboxDay
import nl.healthjournal.domain.model.medication.PlannedStatus
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.metrics.HeightCm
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.ArchiveMedicationUseCase
import nl.healthjournal.domain.usecase.ChangeMedicationScheduleUseCase
import nl.healthjournal.domain.usecase.DeleteMedicationUseCase
import nl.healthjournal.domain.usecase.GetPillboxDayUseCase
import nl.healthjournal.domain.usecase.RecordIntakeUseCase
import nl.healthjournal.domain.usecase.RecordSlotIntakesUseCase
import nl.healthjournal.domain.usecase.SaveMedicationUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 9, 1)
    private val clock = Clock.fixed(today.atTime(10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private class FakeMedicationRepo : MedicationRepositoryPort {
        val medications = linkedMapOf<MedicationId, Medication>()
        val intakes = linkedMapOf<IntakeId, Intake>()

        override suspend fun saveMedication(medication: Medication) { medications[medication.id] = medication }
        override suspend fun getMedication(id: MedicationId): Medication? = medications[id]
        override suspend fun getMedications(profileId: ProfileId, includeArchived: Boolean): List<Medication> =
            medications.values.filter { it.profileId == profileId && (includeArchived || it.archivedFrom == null) }
        override suspend fun deleteMedication(id: MedicationId) {
            medications.remove(id)
            intakes.values.removeAll { it.medicationId == id }
        }
        override suspend fun saveIntake(intake: Intake) {
            intakes.values.removeAll { it.planned != null && it.medicationId == intake.medicationId && it.planned == intake.planned }
            intakes[intake.id] = intake
        }
        override suspend fun getIntake(medicationId: MedicationId, planned: LocalDateTime): Intake? =
            intakes.values.firstOrNull { it.medicationId == medicationId && it.planned == planned }
        override suspend fun deleteIntake(id: IntakeId) { intakes.remove(id) }
        override suspend fun getAllIntakes(profileId: ProfileId): List<Intake> {
            val ids = medications.values.filter { it.profileId == profileId }.map { it.id }.toSet()
            return intakes.values.filter { it.medicationId in ids }
        }
        override suspend fun getIntakesForDay(profileId: ProfileId, date: LocalDate, zone: ZoneId): List<Intake> =
            getAllIntakes(profileId).filter { (it.planned?.toLocalDate() ?: it.takenAt!!.atZone(zone).toLocalDate()) == date }
    }

    private class FakeProfileRepo(val profile: Profile?) : ProfileRepositoryPort {
        override suspend fun save(profile: Profile) {}
        override suspend fun getById(id: ProfileId): Profile? = profile?.takeIf { it.id == id }
        override suspend fun getAll(): List<Profile> = listOfNotNull(profile)
        override suspend fun getActiveProfile(): Profile? = profile
        override suspend fun setActiveProfile(id: ProfileId) {}
    }

    private fun viewModel(repo: FakeMedicationRepo, profile: Profile? = Profile.create("Test", LocalDate.of(1990, 1, 1), HeightCm(180))): MedicationViewModel {
        val recordIntake = RecordIntakeUseCase(repo)
        val useCases = MedicationUseCases(
            getDay = GetPillboxDayUseCase(repo, clock),
            recordIntake = recordIntake,
            recordSlot = RecordSlotIntakesUseCase(recordIntake),
            save = SaveMedicationUseCase(repo),
            changeSchedule = ChangeMedicationScheduleUseCase(repo),
            archive = ArchiveMedicationUseCase(repo),
            delete = DeleteMedicationUseCase(repo)
        )
        return MedicationViewModel(FakeProfileRepo(profile), repo, useCases, clock)
    }

    private fun draft(name: String = "Medication A") = MedicationDraft(name = name)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun idle() = testDispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `save adds a medication and shows the saved message`() = runTest {
        val repo = FakeMedicationRepo()
        val vm = viewModel(repo)
        idle()
        var done = false
        vm.saveMedication(draft()) { done = true }
        idle()

        assertEquals(1, repo.medications.size)
        assertTrue(done)
        assertEquals(UiText.Res(R.string.medication_msg_saved, emptyList()), vm.uiState.value.successMessage)
        assertEquals(1, vm.uiState.value.medications.size)
    }

    @Test
    fun `save rejects a blank name and a bad dose without storing anything`() = runTest {
        val repo = FakeMedicationRepo()
        val vm = viewModel(repo)
        idle()

        vm.saveMedication(draft(name = " "))
        idle()
        assertEquals(UiText.Res(R.string.medication_err_name_required, emptyList()), vm.uiState.value.errorMessage)

        vm.saveMedication(draft().copy(doseText = "0"))
        idle()
        assertEquals(UiText.Res(R.string.medication_err_dose_invalid, emptyList()), vm.uiState.value.errorMessage)

        vm.saveMedication(draft().copy(everyNDays = true, intervalText = "400"))
        idle()
        assertEquals(UiText.Res(R.string.medication_err_interval_invalid, emptyList()), vm.uiState.value.errorMessage)

        vm.saveMedication(draft().copy(weekdays = emptySet()))
        idle()
        assertEquals(UiText.Res(R.string.medication_err_weekday_required, emptyList()), vm.uiState.value.errorMessage)

        assertTrue(repo.medications.isEmpty())
    }

    @Test
    fun `save without a profile reports that a profile is needed`() = runTest {
        val repo = FakeMedicationRepo()
        val vm = viewModel(repo, profile = null)
        idle()
        vm.saveMedication(draft())
        idle()
        assertEquals(UiText.Res(R.string.medication_err_no_profile, emptyList()), vm.uiState.value.errorMessage)
        assertTrue(repo.medications.isEmpty())
    }

    @Test
    fun `editing without changing the rhythm keeps one schedule version`() = runTest {
        val repo = FakeMedicationRepo()
        val vm = viewModel(repo)
        idle()
        vm.saveMedication(draft())
        idle()
        val saved = repo.medications.values.single()

        vm.saveMedication(MedicationDraft.from(saved).copy(name = "Medication B"))
        idle()

        val updated = repo.medications.getValue(saved.id)
        assertEquals("Medication B", updated.name.value)
        assertEquals(1, updated.schedules.size)
    }

    @Test
    fun `editing the times adds a schedule version from the chosen date`() = runTest {
        val repo = FakeMedicationRepo()
        val vm = viewModel(repo)
        idle()
        vm.saveMedication(draft())
        idle()
        val saved = repo.medications.values.single()

        val from = today.plusDays(3)
        vm.saveMedication(
            MedicationDraft.from(saved).copy(times = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)), applyFrom = from)
        )
        idle()

        val updated = repo.medications.getValue(saved.id)
        assertEquals(2, updated.schedules.size)
        assertEquals(from, updated.schedules.last().effectiveFrom)
    }

    @Test
    fun `takeSlot records every open row as taken`() = runTest {
        val repo = FakeMedicationRepo()
        val vm = viewModel(repo)
        idle()
        vm.saveMedication(draft("Medication A"))
        vm.saveMedication(draft("Medication B"))
        idle()
        val slot = vm.uiState.value.day!!.slots.single()
        assertEquals(2, slot.openItems.size)

        vm.takeSlot(slot.time)
        idle()

        val after = vm.uiState.value.day!!.slots.single()
        assertTrue(after.items.all { it.status == PlannedStatus.TAKEN })
        assertEquals(2, repo.intakes.size)
    }

    @Test
    fun `a single row can be skipped and then corrected to taken`() = runTest {
        val repo = FakeMedicationRepo()
        val vm = viewModel(repo)
        idle()
        vm.saveMedication(draft())
        idle()
        val item = vm.uiState.value.day!!.slots.single().items.single()

        vm.recordIntake(item.medication.id, item.planned, IntakeStatus.SKIPPED)
        idle()
        assertEquals(PlannedStatus.SKIPPED, vm.uiState.value.day!!.slots.single().items.single().status)

        vm.recordIntake(item.medication.id, item.planned, IntakeStatus.TAKEN)
        idle()
        assertEquals(PlannedStatus.TAKEN, vm.uiState.value.day!!.slots.single().items.single().status)
        assertEquals(1, repo.intakes.size)
    }

    @Test
    fun `as needed doses are logged without a planned time`() = runTest {
        val repo = FakeMedicationRepo()
        val vm = viewModel(repo)
        idle()
        vm.saveMedication(draft().copy(asNeeded = true))
        idle()
        val id = repo.medications.keys.single()

        vm.logAsNeeded(id)
        idle()

        assertNull(repo.intakes.values.single().planned)
        assertEquals(1, vm.uiState.value.day!!.logged.size)
    }

    @Test
    fun `archive keeps the medication and delete removes it`() = runTest {
        val repo = FakeMedicationRepo()
        val vm = viewModel(repo)
        idle()
        vm.saveMedication(draft())
        idle()
        val id = repo.medications.keys.single()

        vm.archive(id)
        idle()
        assertNotNull(repo.medications.getValue(id).archivedFrom)
        assertEquals(UiText.Res(R.string.medication_msg_archived, emptyList()), vm.uiState.value.successMessage)

        vm.delete(id)
        idle()
        assertTrue(repo.medications.isEmpty())
        assertEquals(UiText.Res(R.string.medication_msg_deleted, emptyList()), vm.uiState.value.successMessage)
    }

    @Test
    fun `clearMessages removes both banners`() = runTest {
        val repo = FakeMedicationRepo()
        val vm = viewModel(repo)
        idle()
        vm.saveMedication(draft(name = ""))
        idle()
        assertNotNull(vm.uiState.value.errorMessage)
        vm.clearMessages()
        assertNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.successMessage)
    }

    @Test
    fun `the week strip covers the last seven days ending today`() = runTest {
        val vm = viewModel(FakeMedicationRepo())
        idle()
        val week = vm.uiState.value.week
        assertEquals(7, week.size)
        assertEquals(today, week.last().date)
        assertEquals(today.minusDays(6), week.first().date)
    }

    @Test
    fun `time of day groups follow the design boundaries`() {
        assertEquals(TimeOfDay.MORNING, TimeOfDay.of(LocalTime.of(11, 59)))
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.of(LocalTime.of(12, 0)))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.of(LocalTime.of(18, 0)))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.of(LocalTime.of(22, 0)))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.of(LocalTime.of(0, 0)))
    }

    @Test
    fun `sameRhythm ignores the start date but not times or days`() {
        val days = DayPattern.Weekdays(setOf(DayOfWeek.MONDAY))
        val a = Schedule.Recurring.of(listOf(LocalTime.of(8, 0)), days, today)
        val sameLater = Schedule.Recurring.of(listOf(LocalTime.of(8, 0)), days, today.plusDays(5))
        val otherTime = Schedule.Recurring.of(listOf(LocalTime.of(9, 0)), days, today)
        assertTrue(sameRhythm(a, sameLater))
        assertFalse(sameRhythm(a, otherTime))
        assertFalse(sameRhythm(Schedule.AsNeeded, a))
        assertTrue(sameRhythm(Schedule.AsNeeded, Schedule.AsNeeded))
    }

    @Test
    fun `weekStatusOf is empty for a day with nothing planned`() {
        assertEquals(DayStatus.EMPTY, weekStatusOf(PillboxDay(today, emptyList(), emptyList())))
    }
}
