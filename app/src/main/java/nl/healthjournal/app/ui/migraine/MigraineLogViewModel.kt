package nl.healthjournal.app.ui.migraine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.migraine.DailyActivities
import nl.healthjournal.domain.model.migraine.HeadArea
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineSymptom
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.model.migraine.MigraineTagKind
import nl.healthjournal.domain.model.migraine.MigraineTrigger
import nl.healthjournal.domain.model.migraine.MigraineType
import nl.healthjournal.domain.model.migraine.PainCharacter
import nl.healthjournal.domain.model.migraine.ReliefMethod
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.AddMigraineTagUseCase
import nl.healthjournal.domain.usecase.AddTagResult
import nl.healthjournal.domain.usecase.ArchiveMigraineTagUseCase
import nl.healthjournal.domain.usecase.RecordMigraineUseCase
import nl.healthjournal.domain.usecase.RenameMigraineTagUseCase
import nl.healthjournal.domain.usecase.RenameTagResult
import nl.healthjournal.domain.usecase.UpdateMigraineUseCase
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

/** The nine steps of the migraine form, in the order they are shown. */
enum class MigraineStep { TIME, INTENSITY, WHERE, TYPE, SYMPTOMS, TRIGGERS, MEDICATION, RELIEF, COMMENT }

/**
 * Everything the person has filled in so far. Times are local; they become instants on save. [end] null means
 * the migraine is still going.
 */
data class MigraineDraft(
    val editingId: MeasurementId? = null,
    val start: LocalDateTime,
    val wokeUpWithIt: Boolean = false,
    val end: LocalDateTime? = null,
    val intensity: Int? = null,
    val areas: Set<HeadArea> = emptySet(),
    val painCharacters: Set<PainCharacter> = emptySet(),
    val type: MigraineType? = null,
    val symptoms: Set<MigraineSymptom> = emptySet(),
    val triggers: Set<MigraineTrigger> = emptySet(),
    val customTriggerIds: Set<MigraineTagId> = emptySet(),
    val tookSomething: Boolean = false,
    val medicationText: String = "",
    val secondDose: Boolean = false,
    val intensityAfter2Hours: Int? = null,
    val medicationHelpedMe: Boolean = false,
    val reliefMethods: Set<ReliefMethod> = emptySet(),
    val customReliefIds: Set<MigraineTagId> = emptySet(),
    val reliefHelpedMe: Boolean = false,
    val activities: DailyActivities? = null,
    val comment: String = ""
) {
    val hasRelief: Boolean get() = reliefMethods.isNotEmpty() || customReliefIds.isNotEmpty()
}

/** The dialog for the person's own tags: adding one, or renaming or removing an existing one. */
data class TagDialogState(
    val kind: MigraineTagKind,
    val tag: MigraineTag? = null,
    val input: String = "",
    val error: UiText? = null
)

data class MigraineUiState(
    val activeProfile: Profile? = null,
    /** The newest entry, for the card on the Log tab. */
    val latest: MigraineEntry? = null,
    val open: Boolean = false,
    val step: MigraineStep = MigraineStep.TIME,
    val draft: MigraineDraft? = null,
    /** All personal tags of the profile, archived ones included so old entries keep their label. */
    val tags: List<MigraineTag> = emptyList(),
    val tagDialog: TagDialogState? = null,
    val confirmDiscard: Boolean = false,
    val saving: Boolean = false,
    val successMessage: UiText? = null,
    val errorMessage: UiText? = null
) {
    fun activeTags(kind: MigraineTagKind): List<MigraineTag> =
        tags.filter { it.kind == kind && !it.archived }.sortedBy { it.label.lowercase() }
}

class MigraineLogViewModel(
    private val profileRepository: ProfileRepositoryPort,
    private val repository: MigraineRepositoryPort,
    private val recordMigraine: RecordMigraineUseCase,
    private val updateMigraine: UpdateMigraineUseCase,
    private val addTag: AddMigraineTagUseCase,
    private val renameTag: RenameMigraineTagUseCase,
    private val archiveTag: ArchiveMigraineTagUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(MigraineUiState())
    val uiState: StateFlow<MigraineUiState> = _uiState.asStateFlow()

    private var observing: Job? = null

    init {
        loadProfile()
    }

    /** Also called after a backup restore or a profile switch, so the card and the tag list follow the profile. */
    fun loadProfile() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile()
            _uiState.value = _uiState.value.copy(activeProfile = profile)
            observing?.cancel()
            if (profile == null) {
                _uiState.value = _uiState.value.copy(latest = null, tags = emptyList())
                return@launch
            }
            observing = launch {
                launch {
                    repository.observeMigraineHistory(profile.id).collect { list ->
                        _uiState.value = _uiState.value.copy(latest = list.maxByOrNull { it.start })
                    }
                }
                launch {
                    repository.observeMigraineTags(profile.id).collect { tags ->
                        _uiState.value = _uiState.value.copy(tags = tags)
                    }
                }
            }
        }
    }

    // ---- opening and leaving the form

    fun startNew() {
        val now = LocalDateTime.now(clock.withZone(zone())).withSecond(0).withNano(0)
        _uiState.value = _uiState.value.copy(
            open = true, step = MigraineStep.TIME, draft = MigraineDraft(start = now),
            confirmDiscard = false, errorMessage = null, successMessage = null
        )
    }

    fun startEdit(entry: MigraineEntry) {
        val z = zone()
        val draft = MigraineDraft(
            editingId = entry.id,
            start = LocalDateTime.ofInstant(entry.start, z),
            wokeUpWithIt = entry.wokeUpWithIt,
            end = entry.end?.let { LocalDateTime.ofInstant(it, z) },
            intensity = entry.intensity,
            areas = entry.areas,
            painCharacters = entry.painCharacters,
            type = entry.type,
            symptoms = entry.symptoms,
            triggers = entry.triggers,
            customTriggerIds = entry.customTriggerIds,
            tookSomething = entry.tookSomething,
            medicationText = entry.medicationText.orEmpty(),
            secondDose = entry.secondDose,
            intensityAfter2Hours = entry.intensityAfter2Hours,
            medicationHelpedMe = entry.medicationHelpedMe,
            reliefMethods = entry.reliefMethods,
            customReliefIds = entry.customReliefIds,
            reliefHelpedMe = entry.reliefHelpedMe,
            activities = entry.activities,
            comment = entry.comment?.text.orEmpty()
        )
        _uiState.value = _uiState.value.copy(
            open = true, step = MigraineStep.TIME, draft = draft,
            confirmDiscard = false, errorMessage = null, successMessage = null
        )
    }

    /** Back on the first step, or the system back gesture, asks before throwing the draft away. */
    fun onBack() {
        val s = _uiState.value
        if (!s.open) return
        if (s.step == MigraineStep.TIME) {
            _uiState.value = s.copy(confirmDiscard = true)
        } else {
            _uiState.value = s.copy(step = MigraineStep.entries[s.step.ordinal - 1], errorMessage = null)
        }
    }

    fun next() {
        val s = _uiState.value
        if (!s.open || s.step == MigraineStep.COMMENT) return
        if (s.step == MigraineStep.TIME) timeProblem(s.draft)?.let {
            _uiState.value = s.copy(errorMessage = it)
            return
        }
        _uiState.value = s.copy(step = MigraineStep.entries[s.step.ordinal + 1], errorMessage = null)
    }

    /** Skip leaves the step as it is; every step but the first is optional. */
    fun skip() = next()

    fun dismissDiscardPrompt() {
        _uiState.value = _uiState.value.copy(confirmDiscard = false)
    }

    fun discard() {
        _uiState.value = _uiState.value.copy(
            open = false, draft = null, step = MigraineStep.TIME, confirmDiscard = false,
            tagDialog = null, errorMessage = null
        )
    }

    // ---- editing the draft

    private fun edit(change: (MigraineDraft) -> MigraineDraft) {
        val s = _uiState.value
        val d = s.draft ?: return
        _uiState.value = s.copy(draft = change(d), errorMessage = null)
    }

    fun setStart(value: LocalDateTime) = edit { it.copy(start = value) }

    fun setWokeUpWithIt(value: Boolean) = edit { it.copy(wokeUpWithIt = value) }

    /** [end] null means still going. */
    fun setEnd(value: LocalDateTime?) = edit { it.copy(end = value) }

    /** Off gives an end time to fill in, starting at now (or the start, when that is later). */
    fun setStillGoing(value: Boolean) = edit {
        if (value) it.copy(end = null)
        else {
            val now = LocalDateTime.now(clock.withZone(zone())).withSecond(0).withNano(0)
            it.copy(end = if (now.isBefore(it.start)) it.start else now)
        }
    }

    fun setIntensity(value: Int?) = edit { it.copy(intensity = value?.coerceIn(0, 10)) }

    fun toggleArea(area: HeadArea) = edit { it.copy(areas = it.areas.toggle(area)) }

    fun togglePainCharacter(value: PainCharacter) = edit { it.copy(painCharacters = it.painCharacters.toggle(value)) }

    /** The type is a single choice; choosing the selected one again clears it. */
    fun setType(value: MigraineType) = edit { it.copy(type = if (it.type == value) null else value) }

    fun toggleSymptom(value: MigraineSymptom) = edit { it.copy(symptoms = it.symptoms.toggle(value)) }

    fun toggleTrigger(value: MigraineTrigger) = edit { it.copy(triggers = it.triggers.toggle(value)) }

    fun toggleCustomTrigger(id: MigraineTagId) = edit { it.copy(customTriggerIds = it.customTriggerIds.toggle(id)) }

    /** Turning it off clears everything that only makes sense with it, so no hidden value is saved. */
    fun setTookSomething(value: Boolean) = edit {
        if (value) it.copy(tookSomething = true)
        else it.copy(
            tookSomething = false, medicationText = "", secondDose = false,
            intensityAfter2Hours = null, medicationHelpedMe = false
        )
    }

    fun setMedicationText(input: String) = edit {
        it.copy(medicationText = input.replace(Regex("[\r\n]+"), " ").take(MEDICATION_MAX))
    }

    fun setSecondDose(value: Boolean) = edit { it.copy(secondDose = value) }

    fun setIntensityAfter2Hours(value: Int?) = edit { it.copy(intensityAfter2Hours = value?.coerceIn(0, 10)) }

    fun setMedicationHelpedMe(value: Boolean) = edit { it.copy(medicationHelpedMe = value) }

    fun toggleRelief(value: ReliefMethod) = edit { fixRelief(it.copy(reliefMethods = it.reliefMethods.toggle(value))) }

    fun toggleCustomRelief(id: MigraineTagId) = edit { fixRelief(it.copy(customReliefIds = it.customReliefIds.toggle(id))) }

    fun setReliefHelpedMe(value: Boolean) = edit { it.copy(reliefHelpedMe = value && it.hasRelief) }

    fun setActivities(value: DailyActivities) = edit { it.copy(activities = if (it.activities == value) null else value) }

    fun setComment(input: String) = edit {
        it.copy(comment = input.replace(Regex("[\r\n]+"), " ").take(EntryComment.MAX_LENGTH))
    }

    /** The "this helped me" tick belongs to the chosen methods; with none left it goes too. */
    private fun fixRelief(d: MigraineDraft) = if (d.hasRelief) d else d.copy(reliefHelpedMe = false)

    // ---- saving

    fun save() {
        val s = _uiState.value
        val draft = s.draft ?: return
        val profile = s.activeProfile
        if (profile == null) {
            _uiState.value = s.copy(errorMessage = UiText.Res(R.string.migraine_err_no_profile))
            return
        }
        timeProblem(draft)?.let {
            _uiState.value = s.copy(step = MigraineStep.TIME, errorMessage = it)
            return
        }
        if (s.saving) return
        _uiState.value = s.copy(saving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val entry = toEntry(draft, profile)
                if (draft.editingId != null) {
                    updateMigraine(entry) ?: error("Unknown entry")
                } else {
                    recordMigraine(entry)
                }
                _uiState.value = _uiState.value.copy(
                    open = false, draft = null, step = MigraineStep.TIME, saving = false, tagDialog = null,
                    successMessage = UiText.Res(
                        if (draft.editingId != null) R.string.migraine_msg_updated else R.string.migraine_msg_saved
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    saving = false, errorMessage = e.toUiText(R.string.migraine_err_save_failed)
                )
            }
        }
    }

    private fun toEntry(d: MigraineDraft, profile: Profile): MigraineEntry {
        val z = zone()
        return MigraineEntry(
            id = d.editingId ?: MeasurementId.generate(),
            profileId = profile.id,
            start = d.start.atZone(z).toInstant(),
            wokeUpWithIt = d.wokeUpWithIt,
            end = d.end?.atZone(z)?.toInstant(),
            intensity = d.intensity,
            areas = d.areas,
            painCharacters = d.painCharacters,
            type = d.type,
            symptoms = d.symptoms,
            triggers = d.triggers,
            customTriggerIds = d.customTriggerIds,
            tookSomething = d.tookSomething,
            medicationText = if (d.tookSomething) MigraineEntry.normalizeMedicationText(d.medicationText) else null,
            secondDose = d.tookSomething && d.secondDose,
            intensityAfter2Hours = if (d.tookSomething) d.intensityAfter2Hours else null,
            medicationHelpedMe = d.tookSomething && d.medicationHelpedMe,
            reliefMethods = d.reliefMethods,
            customReliefIds = d.customReliefIds,
            reliefHelpedMe = d.hasRelief && d.reliefHelpedMe,
            activities = d.activities,
            comment = EntryComment.ofOrNull(EntryComment.normalize(d.comment))
        )
    }

    private fun timeProblem(d: MigraineDraft?): UiText? {
        d ?: return null
        val end = d.end ?: return null
        return if (end.isBefore(d.start)) UiText.Res(R.string.migraine_err_end_before_start) else null
    }

    // ---- personal tags

    fun openAddTag(kind: MigraineTagKind) {
        _uiState.value = _uiState.value.copy(tagDialog = TagDialogState(kind))
    }

    fun openTag(tag: MigraineTag) {
        _uiState.value = _uiState.value.copy(tagDialog = TagDialogState(tag.kind, tag, tag.label))
    }

    fun onTagInput(input: String) {
        val dialog = _uiState.value.tagDialog ?: return
        _uiState.value = _uiState.value.copy(
            tagDialog = dialog.copy(input = input.replace(Regex("[\r\n]+"), " ").take(MigraineTag.MAX_LABEL_LENGTH), error = null)
        )
    }

    fun closeTagDialog() {
        _uiState.value = _uiState.value.copy(tagDialog = null)
    }

    /** Adds a new tag, or renames the one being edited. A new tag is also selected on the entry. */
    fun confirmTag() {
        val s = _uiState.value
        val dialog = s.tagDialog ?: return
        val profile = s.activeProfile ?: return
        val label = MigraineTag.normalizeLabel(dialog.input)
        if (label.isEmpty()) {
            _uiState.value = s.copy(tagDialog = dialog.copy(error = UiText.Res(R.string.migraine_tag_err_empty)))
            return
        }
        viewModelScope.launch {
            val existing = dialog.tag
            if (existing == null) {
                when (val result = addTag(profile.id, dialog.kind, label)) {
                    is AddTagResult.Added -> {
                        val id = result.tag.id
                        _uiState.value = _uiState.value.copy(tagDialog = null)
                        edit {
                            if (dialog.kind == MigraineTagKind.TRIGGER) it.copy(customTriggerIds = it.customTriggerIds + id)
                            else fixRelief(it.copy(customReliefIds = it.customReliefIds + id))
                        }
                    }
                    AddTagResult.LimitReached -> _uiState.value = _uiState.value.copy(
                        tagDialog = dialog.copy(error = UiText.Res(R.string.migraine_tag_err_limit, MigraineTag.MAX_ACTIVE_PER_KIND))
                    )
                }
            } else {
                when (renameTag(existing.id, label)) {
                    is RenameTagResult.Renamed, RenameTagResult.UnknownTag ->
                        _uiState.value = _uiState.value.copy(tagDialog = null)
                    RenameTagResult.LabelTaken -> _uiState.value = _uiState.value.copy(
                        tagDialog = dialog.copy(error = UiText.Res(R.string.migraine_tag_err_taken))
                    )
                }
            }
        }
    }

    /** Removes the tag from the pick list. Older entries keep it, and it is dropped from the draft. */
    fun removeTag() {
        val s = _uiState.value
        val tag = s.tagDialog?.tag ?: return
        viewModelScope.launch {
            archiveTag(tag.id)
            _uiState.value = _uiState.value.copy(tagDialog = null)
            edit {
                if (tag.kind == MigraineTagKind.TRIGGER) it.copy(customTriggerIds = it.customTriggerIds - tag.id)
                else fixRelief(it.copy(customReliefIds = it.customReliefIds - tag.id))
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    class Factory(
        private val profileRepository: ProfileRepositoryPort,
        private val repository: MigraineRepositoryPort
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MigraineLogViewModel(
            profileRepository,
            repository,
            RecordMigraineUseCase(repository),
            UpdateMigraineUseCase(repository),
            AddMigraineTagUseCase(repository),
            RenameMigraineTagUseCase(repository),
            ArchiveMigraineTagUseCase(repository)
        ) as T
    }

    companion object {
        const val MEDICATION_MAX = 100
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
