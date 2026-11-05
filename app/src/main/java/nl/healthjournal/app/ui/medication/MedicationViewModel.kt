package nl.healthjournal.app.ui.medication

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import nl.healthjournal.app.reminder.ReminderCoordinator
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.app.R
import nl.healthjournal.app.reminder.ReminderHooks
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.parseDecimal
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.medication.model.Adherence
import nl.healthjournal.domain.medication.model.AdherenceReport
import nl.healthjournal.domain.medication.model.DayPattern
import nl.healthjournal.domain.medication.model.DoseUnit
import nl.healthjournal.domain.medication.model.IntakeStatus
import nl.healthjournal.domain.medication.model.Medication
import nl.healthjournal.domain.medication.model.MedicationForm
import nl.healthjournal.domain.medication.model.MedicationId
import nl.healthjournal.domain.medication.model.PillAppearance
import nl.healthjournal.domain.medication.model.PillColor
import nl.healthjournal.domain.medication.model.PillShape
import nl.healthjournal.domain.medication.model.PillboxDay
import nl.healthjournal.domain.medication.model.PlannedStatus
import nl.healthjournal.domain.medication.model.Schedule
import nl.healthjournal.domain.medication.model.StrengthUnit
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.Dispatcher
import nl.healthjournal.domain.medication.command.ArchiveMedicationCommand
import nl.healthjournal.domain.medication.command.ChangeMedicationScheduleCommand
import nl.healthjournal.domain.medication.command.DeleteMedicationCommand
import nl.healthjournal.domain.medication.command.GetAdherenceQuery
import nl.healthjournal.domain.medication.command.GetMedicationQuery
import nl.healthjournal.domain.medication.command.GetPillboxDayQuery
import nl.healthjournal.domain.medication.command.ListMedicationsQuery
import nl.healthjournal.domain.medication.command.RecordIntakeCommand
import nl.healthjournal.domain.medication.command.RecordSlotIntakesCommand
import nl.healthjournal.domain.medication.command.SaveMedicationCommand
import nl.healthjournal.domain.profile.command.GetActiveProfileQuery
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** How one day of the week strip looks. Days with nothing planned, or nothing due yet, are [EMPTY]. */
enum class DayStatus { COMPLETE, PARTIAL, MISSED, EMPTY }

data class WeekDay(val date: LocalDate, val status: DayStatus)

/** Morning is before 12:00, afternoon 12:00 to 18:00, evening 18:00 to 22:00, night from 22:00. */
enum class TimeOfDay {
    MORNING, AFTERNOON, EVENING, NIGHT;

    companion object {
        fun of(time: LocalTime): TimeOfDay = when {
            time.hour < 12 -> MORNING
            time.hour < 18 -> AFTERNOON
            time.hour < 22 -> EVENING
            else -> NIGHT
        }
    }
}

/** Whether two schedules plan the same times on the same days; the start date alone is not a change. */
internal fun sameRhythm(a: Schedule?, b: Schedule): Boolean = when {
    a is Schedule.Recurring && b is Schedule.Recurring -> a.times == b.times && a.days == b.days
    else -> a == b
}

fun weekStatusOf(day: PillboxDay): DayStatus {
    val items = day.slots.flatMap { it.items }
    if (items.isEmpty()) return DayStatus.EMPTY
    val done = items.count { it.status == PlannedStatus.TAKEN || it.status == PlannedStatus.SKIPPED }
    return when {
        done == items.size -> DayStatus.COMPLETE
        items.any { it.status == PlannedStatus.TAKEN } -> DayStatus.PARTIAL
        items.any { it.status == PlannedStatus.MISSED } -> DayStatus.MISSED
        else -> DayStatus.EMPTY
    }
}

/** The medication form as typed: text fields stay strings until [MedicationViewModel.saveMedication] validates them. */
data class MedicationDraft(
    val id: MedicationId? = null,
    val name: String = "",
    val form: MedicationForm? = null,
    val strengthText: String = "",
    val strengthUnit: StrengthUnit = StrengthUnit.MG,
    val doseText: String = "1",
    val doseUnit: DoseUnit = DoseUnit.TABLETS,
    val color: PillColor? = null,
    val shape: PillShape? = null,
    val asNeeded: Boolean = false,
    val times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
    val everyNDays: Boolean = false,
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val intervalText: String = "2",
    val comment: String = "",
    /** The date a changed schedule applies from; today unless the user picks another. */
    val applyFrom: LocalDate? = null
) {
    companion object {
        fun from(medication: Medication): MedicationDraft {
            val current = medication.schedules.last().schedule
            val recurring = current as? Schedule.Recurring
            val days = recurring?.days
            return MedicationDraft(
                id = medication.id,
                name = medication.name.value,
                form = medication.form,
                strengthText = medication.dosage.strength?.amount?.toPlainString().orEmpty(),
                strengthUnit = medication.dosage.strength?.unit ?: StrengthUnit.MG,
                doseText = medication.dosage.amountPerIntake.toPlainString(),
                doseUnit = medication.dosage.doseUnit,
                color = medication.appearance?.color,
                shape = medication.appearance?.shape,
                asNeeded = current == Schedule.AsNeeded,
                times = recurring?.times ?: listOf(LocalTime.of(8, 0)),
                everyNDays = days is DayPattern.EveryNDays,
                weekdays = (days as? DayPattern.Weekdays)?.days ?: DayOfWeek.entries.toSet(),
                intervalText = (days as? DayPattern.EveryNDays)?.interval?.toString() ?: "2",
                comment = medication.comment?.text.orEmpty()
            )
        }
    }
}

data class MedicationUiState(
    val profileId: ProfileId? = null,
    val date: LocalDate,
    val today: LocalDate,
    val day: PillboxDay? = null,
    val week: List<WeekDay> = emptyList(),
    val medications: List<Medication> = emptyList(),
    /** The range of the Adherence view in days: 7, 30 or 90. */
    val adherenceDays: Int = 30,
    val adherence: AdherenceReport? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val successMessage: UiText? = null
)

@HiltViewModel
class MedicationViewModel(
    private val dispatcher: Dispatcher,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val reminders: ReminderHooks = ReminderHooks.None
) : ViewModel() {

    /** The constructor Hilt uses; tests pass the clock and the reminder hooks directly. */
    @Inject
    constructor(
        dispatcher: Dispatcher,
        reminders: ReminderCoordinator
    ) : this(dispatcher, reminders = reminders as ReminderHooks) // upcast: picks the primary constructor

    private val _uiState = MutableStateFlow(
        MedicationUiState(date = LocalDate.now(clock), today = LocalDate.now(clock), isLoading = true)
    )
    val uiState: StateFlow<MedicationUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch { reminders.outcomesRecorded.collect { load() } }
    }

    /** Reloads the profile, the medication list, the selected day and the week strip. */
    fun refresh() {
        viewModelScope.launch { load() }
    }

    fun selectAdherenceRange(days: Int) {
        if (days !in Adherence.RANGES_IN_DAYS) return
        _uiState.value = _uiState.value.copy(adherenceDays = days)
        refresh()
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
        refresh()
    }

    /** Records every open row of the slot as taken, all at the same moment. */
    fun takeSlot(planned: LocalDateTime) {
        val slot = _uiState.value.day?.slots?.firstOrNull { it.time == planned } ?: return
        val ids = slot.openItems.map { it.medication.id }
        if (ids.isEmpty()) return
        mutate(R.string.medication_err_save_intake) { dispatcher.dispatch(RecordSlotIntakesCommand(ids, planned)) }
    }

    /** Records or corrects one planned intake. */
    fun recordIntake(medicationId: MedicationId, planned: LocalDateTime, status: IntakeStatus) {
        mutate(R.string.medication_err_save_intake) { dispatcher.dispatch(RecordIntakeCommand(medicationId, planned, status)) }
    }

    fun logAsNeeded(medicationId: MedicationId) {
        mutate(R.string.medication_err_save_intake) {
            dispatcher.dispatch(RecordIntakeCommand(medicationId, planned = null, takenAt = clock.instant()))
        }
    }

    fun saveMedication(draft: MedicationDraft, onDone: () -> Unit = {}) {
        val profileId = _uiState.value.profileId
        if (profileId == null) {
            fail(UiText.Res(R.string.medication_err_no_profile))
            return
        }
        val today = LocalDate.now(clock)
        val parsed = when (val outcome = validate(draft, today)) {
            is Validated.Invalid -> return fail(outcome.message)
            is Validated.Valid -> outcome.parsed
        }
        viewModelScope.launch {
            val existing = draft.id
            val saved = dispatcher.dispatch(
                SaveMedicationCommand(
                    profileId = profileId,
                    name = draft.name,
                    form = draft.form,
                    strengthAmount = parsed.strength,
                    strengthUnit = if (parsed.strength != null) draft.strengthUnit else null,
                    amountPerIntake = parsed.dose,
                    doseUnit = draft.doseUnit,
                    appearance = parsed.appearance,
                    schedule = parsed.schedule,
                    comment = draft.comment.ifBlank { null },
                    effectiveFrom = today,
                    existingId = existing
                )
            )
            val changed = if (saved is DomainResult.Success && existing != null) {
                changeScheduleIfNeeded(existing, parsed.schedule, draft.applyFrom ?: today)
            } else {
                saved
            }
            when (changed) {
                is DomainResult.Failure -> fail(changed.error.toUiText())
                is DomainResult.Success -> {
                    reminders.changed()
                    _uiState.value = _uiState.value.copy(
                        successMessage = UiText.Res(R.string.medication_msg_saved), errorMessage = null
                    )
                    load()
                    onDone()
                }
            }
        }
    }

    /** An edit that keeps the rhythm only changes details; a new rhythm becomes a new schedule version. */
    private suspend fun changeScheduleIfNeeded(id: MedicationId, schedule: Schedule, from: LocalDate): DomainResult<*> {
        val current = when (val found = dispatcher.query(GetMedicationQuery(id))) {
            is DomainResult.Failure -> return found
            is DomainResult.Success -> found.value?.schedules?.last()?.schedule
        }
        if (sameRhythm(current, schedule)) return DomainResult.success(Unit)
        return dispatcher.dispatch(ChangeMedicationScheduleCommand(id, schedule, from))
    }

    fun archive(id: MedicationId) {
        mutate(R.string.medication_err_save_failed, R.string.medication_msg_archived) { dispatcher.dispatch(ArchiveMedicationCommand(id)) }
    }

    fun delete(id: MedicationId) {
        mutate(R.string.medication_err_save_failed, R.string.medication_msg_deleted) { dispatcher.dispatch(DeleteMedicationCommand(id)) }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    private fun mutate(errorRes: Int, successRes: Int? = null, action: suspend () -> DomainResult<*>) {
        viewModelScope.launch {
            when (val result = action()) {
                is DomainResult.Failure -> fail(result.error.text(errorRes))
                is DomainResult.Success -> {
                    reminders.changed()
                    _uiState.value = _uiState.value.copy(
                        errorMessage = null,
                        successMessage = successRes?.let { UiText.Res(it) }
                    )
                    load()
                }
            }
        }
    }

    private fun fail(message: UiText) {
        _uiState.value = _uiState.value.copy(errorMessage = message, successMessage = null, isLoading = false)
    }

    private suspend fun load() {
        val profile = when (val result = dispatcher.query(GetActiveProfileQuery)) {
            is DomainResult.Failure -> return fail(result.error.text(R.string.medication_err_load_failed))
            is DomainResult.Success -> result.value
        }
        if (profile == null) {
            _uiState.value = _uiState.value.copy(profileId = null, day = null, week = emptyList(), medications = emptyList(), adherence = null, isLoading = false)
            return
        }
        val today = LocalDate.now(clock)
        val date = _uiState.value.date
        val week = mutableListOf<WeekDay>()
        val days = mutableMapOf<LocalDate, PillboxDay>()
        for (back in 6 downTo 0) {
            val d = today.minusDays(back.toLong())
            val day = loadDay(profile.id, d) ?: return
            days[d] = day
            week += WeekDay(d, weekStatusOf(day))
        }
        // The selected day may lie outside the week strip, so it is read on its own
        val selectedDay = days[date] ?: loadDay(profile.id, date) ?: return
        val medications = when (val result = dispatcher.query(ListMedicationsQuery(profile.id, includeArchived = true))) {
            is DomainResult.Failure -> return fail(result.error.text(R.string.medication_err_load_failed))
            is DomainResult.Success -> result.value.sortedBy { it.name.value.lowercase() }
        }
        val adherence = when (val result = dispatcher.query(GetAdherenceQuery(profile.id, _uiState.value.adherenceDays))) {
            is DomainResult.Failure -> return fail(result.error.text(R.string.medication_err_load_failed))
            is DomainResult.Success -> result.value
        }
        _uiState.value = _uiState.value.copy(
            profileId = profile.id, today = today, day = selectedDay, week = week,
            medications = medications, adherence = adherence, isLoading = false
        )
    }

    /** One day of the pillbox, or null after reporting the failure. */
    private suspend fun loadDay(profileId: ProfileId, date: LocalDate): PillboxDay? =
        when (val result = dispatcher.query(GetPillboxDayQuery(profileId, date))) {
            is DomainResult.Failure -> { fail(result.error.text(R.string.medication_err_load_failed)); null }
            is DomainResult.Success -> result.value
        }

    private fun DomainError.text(@StringRes fallback: Int): UiText =
        if (this is DomainError.Storage) cause?.toUiText(fallback) ?: UiText.Res(fallback) else toUiText()

    private class Parsed(
        val strength: BigDecimal?,
        val dose: BigDecimal,
        val appearance: PillAppearance?,
        val schedule: Schedule
    )

    private sealed interface Validated {
        class Valid(val parsed: Parsed) : Validated
        class Invalid(val message: UiText) : Validated
    }

    private fun invalid(@StringRes res: Int) = Validated.Invalid(UiText.Res(res))

    private fun validate(draft: MedicationDraft, today: LocalDate): Validated {
        if (draft.name.isBlank()) return invalid(R.string.medication_err_name_required)
        if (draft.name.trim().length > nl.healthjournal.domain.medication.model.MedicationName.MAX_LENGTH) {
            return invalid(R.string.medication_err_name_too_long)
        }
        val strength = if (draft.strengthText.isBlank()) null else {
            draft.strengthText.parseDecimal()?.let { BigDecimal(it.toString()) }
                ?.takeIf { it > BigDecimal.ZERO && it <= BigDecimal(100_000) }
                ?: return invalid(R.string.medication_err_strength_invalid)
        }
        val dose = draft.doseText.parseDecimal()?.let { BigDecimal(it.toString()) }
            ?.takeIf { it > BigDecimal.ZERO && it <= BigDecimal(1000) }
            ?: return invalid(R.string.medication_err_dose_invalid)
        val appearance = if (draft.color != null && draft.shape != null) PillAppearance(draft.color, draft.shape) else null

        val schedule = if (draft.asNeeded) Schedule.AsNeeded else {
            if (draft.times.isEmpty()) return invalid(R.string.medication_err_time_required)
            val days = if (draft.everyNDays) {
                val n = draft.intervalText.trim().toIntOrNull()
                    ?.takeIf { it in 1..DayPattern.EveryNDays.MAX_INTERVAL }
                    ?: return invalid(R.string.medication_err_interval_invalid)
                DayPattern.EveryNDays(n)
            } else {
                if (draft.weekdays.isEmpty()) return invalid(R.string.medication_err_weekday_required)
                DayPattern.Weekdays(draft.weekdays)
            }
            Schedule.Recurring.of(draft.times, days, draft.applyFrom ?: today)
        }
        return Validated.Valid(Parsed(strength, dose, appearance, schedule))
    }
}
