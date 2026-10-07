package nl.healthjournal.app.ui.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.app.R
import nl.healthjournal.app.reminder.ReminderHooks
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.UiTextException
import nl.healthjournal.app.ui.common.parseDecimal
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.Adherence
import nl.healthjournal.domain.model.medication.AdherenceReport
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationForm
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.PillAppearance
import nl.healthjournal.domain.model.medication.PillColor
import nl.healthjournal.domain.model.medication.PillShape
import nl.healthjournal.domain.model.medication.PillboxDay
import nl.healthjournal.domain.model.medication.PlannedStatus
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.StrengthUnit
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.ArchiveMedicationUseCase
import nl.healthjournal.domain.usecase.ChangeMedicationScheduleUseCase
import nl.healthjournal.domain.usecase.DeleteMedicationUseCase
import nl.healthjournal.domain.usecase.GetAdherenceUseCase
import nl.healthjournal.domain.usecase.GetPillboxDayUseCase
import nl.healthjournal.domain.usecase.RecordIntakeUseCase
import nl.healthjournal.domain.usecase.RecordSlotIntakesUseCase
import nl.healthjournal.domain.usecase.SaveMedicationUseCase
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Everything the pillbox can do, bundled so the Factory and the Application stay readable. */
data class MedicationUseCases(
    val getDay: GetPillboxDayUseCase,
    val recordIntake: RecordIntakeUseCase,
    val recordSlot: RecordSlotIntakesUseCase,
    val save: SaveMedicationUseCase,
    val changeSchedule: ChangeMedicationScheduleUseCase,
    val archive: ArchiveMedicationUseCase,
    val delete: DeleteMedicationUseCase,
    val getAdherence: GetAdherenceUseCase
)

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

class MedicationViewModel(
    private val profileRepository: ProfileRepositoryPort,
    private val medicationRepository: MedicationRepositoryPort,
    private val useCases: MedicationUseCases,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val reminders: ReminderHooks = ReminderHooks.None
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MedicationUiState(date = LocalDate.now(clock), today = LocalDate.now(clock), isLoading = true)
    )
    val uiState: StateFlow<MedicationUiState> = _uiState.asStateFlow()

    init {
        refresh()
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
        mutate(R.string.medication_err_save_intake) { useCases.recordSlot(ids, planned) }
    }

    /** Records or corrects one planned intake. */
    fun recordIntake(medicationId: MedicationId, planned: LocalDateTime, status: IntakeStatus) {
        mutate(R.string.medication_err_save_intake) { useCases.recordIntake(medicationId, planned, status) }
    }

    fun logAsNeeded(medicationId: MedicationId) {
        mutate(R.string.medication_err_save_intake) { useCases.recordIntake(medicationId, planned = null, takenAt = clock.instant()) }
    }

    fun saveMedication(draft: MedicationDraft, onDone: () -> Unit = {}) {
        val profileId = _uiState.value.profileId
        if (profileId == null) {
            fail(UiText.Res(R.string.medication_err_no_profile))
            return
        }
        viewModelScope.launch {
            try {
                val today = LocalDate.now(clock)
                val parsed = validate(draft, today)
                val existing = draft.id
                useCases.save(
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
                if (existing != null) {
                    val current = medicationRepository.getMedication(existing)?.schedules?.last()?.schedule
                    if (!sameRhythm(current, parsed.schedule)) {
                        useCases.changeSchedule(existing, parsed.schedule, draft.applyFrom ?: today)
                    }
                }
                reminders.changed()
                _uiState.value = _uiState.value.copy(
                    successMessage = UiText.Res(R.string.medication_msg_saved), errorMessage = null
                )
                load()
                onDone()
            } catch (e: Exception) {
                fail(e.toUiText(R.string.medication_err_save_failed))
            }
        }
    }

    fun archive(id: MedicationId) {
        mutate(R.string.medication_err_save_failed, R.string.medication_msg_archived) { useCases.archive(id) }
    }

    fun delete(id: MedicationId) {
        mutate(R.string.medication_err_save_failed, R.string.medication_msg_deleted) { useCases.delete(id) }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    private fun mutate(errorRes: Int, successRes: Int? = null, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                reminders.changed()
                _uiState.value = _uiState.value.copy(
                    errorMessage = null,
                    successMessage = successRes?.let { UiText.Res(it) }
                )
                load()
            } catch (e: Exception) {
                fail(e.toUiText(errorRes))
            }
        }
    }

    private fun fail(message: UiText) {
        _uiState.value = _uiState.value.copy(errorMessage = message, successMessage = null, isLoading = false)
    }

    private suspend fun load() {
        try {
            val profile = profileRepository.getActiveProfile()
            if (profile == null) {
                _uiState.value = _uiState.value.copy(profileId = null, day = null, week = emptyList(), medications = emptyList(), adherence = null, isLoading = false)
                return
            }
            val today = LocalDate.now(clock)
            val date = _uiState.value.date
            val day = useCases.getDay(profile.id, date)
            val week = (6 downTo 0).map { back ->
                val d = today.minusDays(back.toLong())
                WeekDay(d, if (d == date) weekStatusOf(day) else weekStatusOf(useCases.getDay(profile.id, d)))
            }
            val medications = medicationRepository.getMedications(profile.id, includeArchived = true)
                .sortedBy { it.name.value.lowercase() }
            val adherence = useCases.getAdherence(profile.id, _uiState.value.adherenceDays)
            _uiState.value = _uiState.value.copy(
                profileId = profile.id, today = today, day = day, week = week,
                medications = medications, adherence = adherence, isLoading = false
            )
        } catch (e: Exception) {
            fail(e.toUiText(R.string.medication_err_load_failed))
        }
    }

    private class Parsed(
        val strength: BigDecimal?,
        val dose: BigDecimal,
        val appearance: PillAppearance?,
        val schedule: Schedule
    )

    private fun validate(draft: MedicationDraft, today: LocalDate): Parsed {
        if (draft.name.isBlank()) throw UiTextException(UiText.Res(R.string.medication_err_name_required))
        if (draft.name.trim().length > nl.healthjournal.domain.model.medication.MedicationName.MAX_LENGTH) {
            throw UiTextException(UiText.Res(R.string.medication_err_name_too_long))
        }
        val strength = if (draft.strengthText.isBlank()) null else {
            draft.strengthText.parseDecimal()?.let { BigDecimal(it.toString()) }
                ?.takeIf { it > BigDecimal.ZERO && it <= BigDecimal(100_000) }
                ?: throw UiTextException(UiText.Res(R.string.medication_err_strength_invalid))
        }
        val dose = draft.doseText.parseDecimal()?.let { BigDecimal(it.toString()) }
            ?.takeIf { it > BigDecimal.ZERO && it <= BigDecimal(1000) }
            ?: throw UiTextException(UiText.Res(R.string.medication_err_dose_invalid))
        val appearance = if (draft.color != null && draft.shape != null) PillAppearance(draft.color, draft.shape) else null

        val schedule = if (draft.asNeeded) Schedule.AsNeeded else {
            if (draft.times.isEmpty()) throw UiTextException(UiText.Res(R.string.medication_err_time_required))
            val days = if (draft.everyNDays) {
                val n = draft.intervalText.trim().toIntOrNull()
                    ?.takeIf { it in 1..DayPattern.EveryNDays.MAX_INTERVAL }
                    ?: throw UiTextException(UiText.Res(R.string.medication_err_interval_invalid))
                DayPattern.EveryNDays(n)
            } else {
                if (draft.weekdays.isEmpty()) throw UiTextException(UiText.Res(R.string.medication_err_weekday_required))
                DayPattern.Weekdays(draft.weekdays)
            }
            Schedule.Recurring.of(draft.times, days, draft.applyFrom ?: today)
        }
        return Parsed(strength, dose, appearance, schedule)
    }

    class Factory(
        private val profileRepository: ProfileRepositoryPort,
        private val medicationRepository: MedicationRepositoryPort,
        private val useCases: MedicationUseCases,
        private val reminders: ReminderHooks = ReminderHooks.None
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MedicationViewModel(
                profileRepository, medicationRepository, useCases, reminders = reminders
            ) as T
    }
}
