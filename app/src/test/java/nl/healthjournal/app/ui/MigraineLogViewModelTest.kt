package nl.healthjournal.app.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.migraine.MigraineLogViewModel
import nl.healthjournal.app.ui.migraine.MigraineStep
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.migraine.DailyActivities
import nl.healthjournal.domain.model.migraine.HeadArea
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.model.migraine.MigraineTagKind
import nl.healthjournal.domain.model.migraine.MigraineType
import nl.healthjournal.domain.model.migraine.ReliefMethod
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.AddMigraineTagUseCase
import nl.healthjournal.domain.usecase.ArchiveMigraineTagUseCase
import nl.healthjournal.domain.usecase.RecordMigraineUseCase
import nl.healthjournal.domain.usecase.RenameMigraineTagUseCase
import nl.healthjournal.domain.usecase.UpdateMigraineUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MigraineLogViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(LocalDateTime.of(2026, 10, 24, 9, 30).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private class FakeProfiles(private val profile: Profile?) : ProfileRepositoryPort {
        override suspend fun save(profile: Profile) {}
        override suspend fun getById(id: ProfileId): Profile? = profile?.takeIf { it.id == id }
        override suspend fun getAll(): List<Profile> = listOfNotNull(profile)
        override suspend fun getActiveProfile(): Profile? = profile
        override suspend fun setActiveProfile(id: ProfileId) {}
    }

    private val profile = Profile.create(name = "Sample", dateOfBirth = LocalDate.of(1990, 1, 1))
    private lateinit var repo: FakeMigraines

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeMigraines()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(withProfile: Boolean = true) = MigraineLogViewModel(
        FakeProfiles(profile.takeIf { withProfile }), repo,
        RecordMigraineUseCase(repo), UpdateMigraineUseCase(repo),
        AddMigraineTagUseCase(repo), RenameMigraineTagUseCase(repo), ArchiveMigraineTagUseCase(repo),
        clock, { ZoneOffset.UTC }
    )

    @Test
    fun `starting a new entry opens at the first step with the start set to now`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        val s = vm.uiState.value
        assertTrue(s.open)
        assertEquals(MigraineStep.TIME, s.step)
        assertEquals(LocalDateTime.of(2026, 10, 24, 9, 30), s.draft!!.start)
        assertNull(s.draft.end)
    }

    @Test
    fun `next and back walk the nine steps and back on the first asks before discarding`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        repeat(MigraineStep.entries.size - 1) { vm.next() }
        assertEquals(MigraineStep.COMMENT, vm.uiState.value.step)
        vm.next()
        assertEquals(MigraineStep.COMMENT, vm.uiState.value.step)
        repeat(MigraineStep.entries.size - 1) { vm.onBack() }
        assertEquals(MigraineStep.TIME, vm.uiState.value.step)
        assertFalse(vm.uiState.value.confirmDiscard)
        vm.onBack()
        assertTrue(vm.uiState.value.confirmDiscard)
        vm.dismissDiscardPrompt()
        assertTrue(vm.uiState.value.open)
        vm.onBack()
        vm.discard()
        assertFalse(vm.uiState.value.open)
        assertNull(vm.uiState.value.draft)
    }

    @Test
    fun `an end before the start blocks the next step and the save`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        vm.setEnd(LocalDateTime.of(2026, 10, 24, 8, 0))
        vm.next()
        assertEquals(MigraineStep.TIME, vm.uiState.value.step)
        assertEquals(UiText.Res(R.string.migraine_err_end_before_start), vm.uiState.value.errorMessage)
        vm.save()
        advanceUntilIdle()
        assertTrue(repo.entries.value.isEmpty())
        assertTrue(vm.uiState.value.open)
    }

    @Test
    fun `saving only the start gives an ongoing entry and closes the form`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        vm.save()
        advanceUntilIdle()
        val saved = repo.entries.value.single()
        assertTrue(saved.ongoing)
        assertEquals(LocalDateTime.of(2026, 10, 24, 9, 30).toInstant(ZoneOffset.UTC), saved.start)
        assertFalse(vm.uiState.value.open)
        assertEquals(UiText.Res(R.string.migraine_msg_saved), vm.uiState.value.successMessage)
        assertEquals(saved.id, vm.uiState.value.latest?.id)
    }

    @Test
    fun `turning took something off clears the dependent medication fields`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        vm.setTookSomething(true)
        vm.setMedicationText("  my usual  ")
        vm.setSecondDose(true)
        vm.setIntensityAfter2Hours(3)
        vm.setMedicationHelpedMe(true)
        vm.setTookSomething(false)
        val d = vm.uiState.value.draft!!
        assertEquals("", d.medicationText)
        assertFalse(d.secondDose)
        assertNull(d.intensityAfter2Hours)
        assertFalse(d.medicationHelpedMe)
    }

    @Test
    fun `the medication text is trimmed on save and stays in the 100 character limit`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        vm.setTookSomething(true)
        vm.setMedicationText("x".repeat(150))
        assertEquals(100, vm.uiState.value.draft!!.medicationText.length)
        vm.setMedicationText("  my usual  ")
        vm.setIntensityAfter2Hours(3)
        vm.save()
        advanceUntilIdle()
        val saved = repo.entries.value.single()
        assertEquals("my usual", saved.medicationText)
        assertEquals(3, saved.intensityAfter2Hours)
    }

    @Test
    fun `the relief helped me tick follows the chosen methods`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        vm.setReliefHelpedMe(true)
        assertFalse("no method chosen yet", vm.uiState.value.draft!!.reliefHelpedMe)
        vm.toggleRelief(ReliefMethod.HEAT)
        vm.setReliefHelpedMe(true)
        assertTrue(vm.uiState.value.draft!!.reliefHelpedMe)
        vm.toggleRelief(ReliefMethod.HEAT)
        assertFalse(vm.uiState.value.draft!!.reliefHelpedMe)
    }

    @Test
    fun `the type and the carry on choice are single choices that clear on a second tap`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        vm.setType(MigraineType.WITH_AURA)
        assertEquals(MigraineType.WITH_AURA, vm.uiState.value.draft!!.type)
        vm.setType(MigraineType.NOT_SURE)
        assertEquals(MigraineType.NOT_SURE, vm.uiState.value.draft!!.type)
        vm.setType(MigraineType.NOT_SURE)
        assertNull(vm.uiState.value.draft!!.type)
        vm.setActivities(DailyActivities.HAD_TO_STOP)
        assertEquals(DailyActivities.HAD_TO_STOP, vm.uiState.value.draft!!.activities)
        vm.setActivities(DailyActivities.HAD_TO_STOP)
        assertNull(vm.uiState.value.draft!!.activities)
    }

    @Test
    fun `editing loads the entry and saving replaces it`() = runTest(dispatcher) {
        val original = MigraineEntry(
            MeasurementId.generate(), profile.id, LocalDateTime.of(2026, 10, 20, 6, 30).toInstant(ZoneOffset.UTC),
            intensity = 7, areas = setOf(HeadArea.TEMPLES)
        )
        repo.entries.value = listOf(original)
        val vm = viewModel()
        advanceUntilIdle()
        vm.startEdit(original)
        assertEquals(LocalDateTime.of(2026, 10, 20, 6, 30), vm.uiState.value.draft!!.start)
        vm.setIntensity(4)
        vm.save()
        advanceUntilIdle()
        val saved = repo.entries.value.single()
        assertEquals(original.id, saved.id)
        assertEquals(4, saved.intensity)
        assertEquals(setOf(HeadArea.TEMPLES), saved.areas)
        assertEquals(UiText.Res(R.string.migraine_msg_updated), vm.uiState.value.successMessage)
    }

    @Test
    fun `a new personal tag is created and selected on the entry`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        vm.openAddTag(MigraineTagKind.TRIGGER)
        vm.onTagInput("  skipped lunch ")
        vm.confirmTag()
        advanceUntilIdle()
        val tag = repo.tags.value.single()
        assertEquals("skipped lunch", tag.label)
        assertEquals(setOf(tag.id), vm.uiState.value.draft!!.customTriggerIds)
        assertNull(vm.uiState.value.tagDialog)
    }

    @Test
    fun `an empty tag label stays in the dialog with a message`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        vm.openAddTag(MigraineTagKind.RELIEF)
        vm.onTagInput("   ")
        vm.confirmTag()
        advanceUntilIdle()
        assertTrue(repo.tags.value.isEmpty())
        assertNotNull(vm.uiState.value.tagDialog?.error)
    }

    @Test
    fun `renaming to a label already in use is refused and removing hides the tag from the pick list`() = runTest(dispatcher) {
        val a = MigraineTag(MigraineTagId.generate(), profile.id, MigraineTagKind.TRIGGER, "noise")
        val b = MigraineTag(MigraineTagId.generate(), profile.id, MigraineTagKind.TRIGGER, "heat")
        repo.tags.value = listOf(a, b)
        val vm = viewModel()
        advanceUntilIdle()
        vm.startNew()
        vm.toggleCustomTrigger(b.id)

        vm.openTag(b)
        vm.onTagInput("Noise")
        vm.confirmTag()
        advanceUntilIdle()
        assertEquals(UiText.Res(R.string.migraine_tag_err_taken), vm.uiState.value.tagDialog?.error)

        vm.onTagInput("warmth")
        vm.confirmTag()
        advanceUntilIdle()
        assertEquals("warmth", repo.tags.value.first { it.id == b.id }.label)
        assertNull(vm.uiState.value.tagDialog)

        vm.openTag(b)
        vm.removeTag()
        advanceUntilIdle()
        assertTrue(repo.tags.value.first { it.id == b.id }.archived)
        assertEquals(listOf("noise"), vm.uiState.value.activeTags(MigraineTagKind.TRIGGER).map { it.label })
        assertFalse(b.id in vm.uiState.value.draft!!.customTriggerIds)
    }

    @Test
    fun `without a profile saving shows a message and writes nothing`() = runTest(dispatcher) {
        val vm = viewModel(withProfile = false)
        advanceUntilIdle()
        vm.startNew()
        vm.save()
        advanceUntilIdle()
        assertTrue(repo.entries.value.isEmpty())
        assertEquals(UiText.Res(R.string.migraine_err_no_profile), vm.uiState.value.errorMessage)
    }
}
