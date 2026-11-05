package nl.healthjournal.app.ui.history

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.MetricDisplayOrderPreference
import nl.healthjournal.app.settings.MetricSlot
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.app.ui.history.charts.TrendDateRange
import nl.healthjournal.domain.command.DeleteEntryCommand
import nl.healthjournal.domain.command.Dispatcher
import nl.healthjournal.domain.command.DomainError
import nl.healthjournal.domain.command.DomainResult
import nl.healthjournal.domain.command.EntryKind
import nl.healthjournal.domain.command.ExportCsvQuery
import nl.healthjournal.domain.command.GetHistoryQuery
import nl.healthjournal.domain.command.ImportCsvCommand
import nl.healthjournal.domain.command.entryKindFromKey
import nl.healthjournal.domain.command.isLibraKey
import nl.healthjournal.domain.metrics.command.UpdateActivityCommand
import nl.healthjournal.domain.metrics.command.UpdateBloodPressureCommand
import nl.healthjournal.domain.metrics.command.UpdateGlucoseCommand
import nl.healthjournal.domain.metrics.command.UpdateOxygenSaturationCommand
import nl.healthjournal.domain.metrics.command.UpdateWaistCircumferenceCommand
import nl.healthjournal.domain.metrics.command.UpdateWeightCommand
import nl.healthjournal.domain.migraine.command.EndMigraineCommand
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.OxygenContext
import nl.healthjournal.domain.model.metrics.OxygenSaturationEntry
import nl.healthjournal.domain.model.metrics.OxygenSite
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.model.common.ImportResult
import nl.healthjournal.domain.profile.command.GetActiveProfileQuery
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

enum class HistoryFilter {
    ALL,
    WEIGHT,
    BLOOD_PRESSURE,
    GLUCOSE,
    ACTIVITY,
    WAIST_CIRCUMFERENCE,
    MIGRAINE,
    OXYGEN_SATURATION
}

/** A reference to one History entry, used to track which entry is being edited or deleted. */
sealed interface EntryRef {
    val id: MeasurementId
    data class Weight(val entry: WeightEntry) : DialogEntryRef { override val id get() = entry.id }
    data class BloodPressure(val entry: BloodPressureEntry) : DialogEntryRef { override val id get() = entry.id }
    data class Glucose(val entry: GlucoseEntry) : DialogEntryRef { override val id get() = entry.id }
    data class Activity(val session: ActivitySession) : DialogEntryRef { override val id get() = session.id }
    data class WaistCircumference(val entry: WaistCircumferenceEntry) : DialogEntryRef { override val id get() = entry.id }
    data class OxygenSaturation(val entry: OxygenSaturationEntry) : DialogEntryRef { override val id get() = entry.id }
    data class Migraine(val entry: MigraineEntry) : EntryRef { override val id get() = entry.id }
}

/** The entries edited in the History edit dialog; a migraine is edited in its own step form instead. */
sealed interface DialogEntryRef : EntryRef

data class HistoryUiState(
    val activeProfile: Profile? = null,
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    /** The metric filters in the order the person chose; "All" always comes first. */
    val orderedMetrics: List<MetricSlot> = MetricSlot.DEFAULT_ORDER,
    val selectedDateRange: TrendDateRange = TrendDateRange.THIRTY_DAYS,
    /** Set when the Migraine calendar opened a day: History then lists only that day's migraines. */
    val migraineDay: LocalDate? = null,
    val weights: List<WeightEntry> = emptyList(),
    val bloodPressures: List<BloodPressureEntry> = emptyList(),
    val glucoses: List<GlucoseEntry> = emptyList(),
    val activities: List<ActivitySession> = emptyList(),
    val waistCircumferences: List<WaistCircumferenceEntry> = emptyList(),
    val oxygenSaturations: List<OxygenSaturationEntry> = emptyList(),
    val migraines: List<MigraineEntry> = emptyList(),
    val isLoading: Boolean = false,
    val exportedCsvContent: String? = null,
    val importResult: ImportResult? = null,
    val errorMessage: UiText? = null,
    val infoMessage: UiText? = null,
    val editing: DialogEntryRef? = null,
    val editError: UiText? = null,
    val pendingDelete: EntryRef? = null
)

@HiltViewModel
class HistoryViewModel(
    private val dispatcher: Dispatcher,
    private val metricOrder: StateFlow<List<MetricSlot>> = MutableStateFlow(MetricSlot.DEFAULT_ORDER)
) : ViewModel() {

    /** The constructor Hilt uses; tests pass the display order directly. */
    @Inject
    constructor(dispatcher: Dispatcher, metricDisplayOrder: MetricDisplayOrderPreference) :
        this(dispatcher, metricDisplayOrder.order)

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true, orderedMetrics = metricOrder.value))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeMetricOrder()
        loadHistory()
    }

    private fun observeMetricOrder() {
        viewModelScope.launch {
            metricOrder.collect { order -> _uiState.value = _uiState.value.copy(orderedMetrics = order) }
        }
    }

    fun setFilter(filter: HistoryFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter, migraineDay = null)
    }

    /** Opens one day of the Migraine calendar: the Migraine filter, all dates, only that day's entries. */
    fun showMigraineDay(day: LocalDate) {
        _uiState.value = _uiState.value.copy(
            selectedFilter = HistoryFilter.MIGRAINE,
            selectedDateRange = TrendDateRange.ALL,
            migraineDay = day
        )
    }

    fun clearMigraineDay() {
        _uiState.value = _uiState.value.copy(migraineDay = null)
    }

    fun setDateRange(range: TrendDateRange) {
        _uiState.value = _uiState.value.copy(selectedDateRange = range)
    }

    /** [clearError] is false when the caller has just set an error that the reload must not wipe. */
    fun loadHistory(clearError: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = if (clearError) null else _uiState.value.errorMessage
            )
            val profile = when (val result = dispatcher.query(GetActiveProfileQuery)) {
                is DomainResult.Success -> result.value
                is DomainResult.Failure -> return@launch loadFailed(result.error)
            }
            if (profile == null) {
                _uiState.value = _uiState.value.copy(activeProfile = null, isLoading = false)
                return@launch
            }
            when (val result = dispatcher.query(GetHistoryQuery(profile.id))) {
                is DomainResult.Success -> {
                    val history = result.value.health
                    _uiState.value = _uiState.value.copy(
                        activeProfile = profile,
                        weights = history.weights,
                        bloodPressures = history.bloodPressures,
                        glucoses = history.glucoses,
                        activities = history.activities,
                        waistCircumferences = history.waistCircumferences,
                        oxygenSaturations = history.oxygenSaturations,
                        migraines = result.value.migraines,
                        isLoading = false
                    )
                }
                is DomainResult.Failure -> loadFailed(result.error)
            }
        }
    }

    private fun loadFailed(error: DomainError) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = error.text(R.string.history_err_load_failed)
        )
    }

    fun exportCsv(metric: String) {
        val profile = _uiState.value.activeProfile ?: return
        val kind = entryKindFromKey(metric)
        viewModelScope.launch {
            // An unknown key exports an empty file, as it always has.
            val result = if (kind == null) {
                DomainResult.success("")
            } else {
                dispatcher.query(ExportCsvQuery(profile.id, kind))
            }
            _uiState.value = when (result) {
                is DomainResult.Success -> _uiState.value.copy(
                    exportedCsvContent = result.value,
                    infoMessage = UiText.Res(R.string.history_msg_export_done, metric)
                )
                is DomainResult.Failure -> _uiState.value.copy(
                    errorMessage = UiText.Res(R.string.history_err_export, result.error.detail())
                )
            }
        }
    }

    fun importCsv(metricType: String, csvContent: String) {
        val profile = _uiState.value.activeProfile ?: return
        val kind = entryKindFromKey(metricType)
        viewModelScope.launch {
            if (kind == null) {
                _uiState.value = _uiState.value.copy(errorMessage = UiText.Res(R.string.history_err_import, metricType))
                return@launch
            }
            val libra = isLibraKey(metricType)
            when (val result = dispatcher.dispatch(ImportCsvCommand(profile.id, kind, csvContent, libra))) {
                is DomainResult.Success -> {
                    val imported = result.value
                    _uiState.value = _uiState.value.copy(
                        importResult = imported,
                        infoMessage = UiText.Res(
                            R.string.history_msg_import_done, imported.importedCount, imported.skippedRows.size
                        )
                    )
                    loadHistory()
                }
                is DomainResult.Failure -> {
                    val message = if (libra) {
                        UiText.Res(R.string.history_err_import_libra, result.error.text(R.string.history_err_unknown))
                    } else {
                        UiText.Res(R.string.history_err_import, result.error.detail())
                    }
                    _uiState.value = _uiState.value.copy(errorMessage = message)
                }
            }
        }
    }

    fun startEdit(entry: DialogEntryRef) {
        _uiState.value = _uiState.value.copy(editing = entry, editError = null)
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(editing = null, editError = null)
    }

    fun requestDelete(entry: EntryRef) {
        _uiState.value = _uiState.value.copy(pendingDelete = entry)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(pendingDelete = null)
    }

    fun confirmDelete() {
        val target = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            when (val result = dispatcher.dispatch(DeleteEntryCommand(target.kind(), target.id))) {
                is DomainResult.Success -> {
                    _uiState.value = _uiState.value.copy(pendingDelete = null, errorMessage = null)
                    loadHistory()
                }
                is DomainResult.Failure -> {
                    val error = result.error
                    val message = if (error is DomainError.Storage) {
                        UiText.Res(R.string.history_err_delete, error.detail())
                    } else {
                        error.toUiText()
                    }
                    _uiState.value = _uiState.value.copy(pendingDelete = null, errorMessage = message)
                    // The entry may be gone already: reload the list, but keep the message.
                    if (error is DomainError.NotFound) loadHistory(clearError = false)
                }
            }
        }
    }

    fun updateWeight(entry: WeightEntry, weightKg: BigDecimal, timestamp: Instant, comment: String?) =
        runUpdate { dispatcher.dispatch(UpdateWeightCommand(entry.id, entry.profileId, weightKg, timestamp, comment)) }

    fun updateBloodPressure(entry: BloodPressureEntry, systolic: Int, diastolic: Int, timestamp: Instant, comment: String?) =
        runUpdate {
            dispatcher.dispatch(
                UpdateBloodPressureCommand(entry.id, entry.profileId, systolic, diastolic, timestamp, comment = comment)
            )
        }

    fun updateGlucose(entry: GlucoseEntry, context: GlucoseContext, valueInMmolL: BigDecimal, timestamp: Instant, comment: String?) =
        runUpdate {
            dispatcher.dispatch(UpdateGlucoseCommand(entry.id, entry.profileId, context, valueInMmolL, timestamp, comment))
        }

    fun updateWaistCircumference(entry: WaistCircumferenceEntry, waistCm: Double, timestamp: Instant, comment: String?) =
        runUpdate {
            dispatcher.dispatch(UpdateWaistCircumferenceCommand(entry.id, entry.profileId, waistCm, timestamp, comment))
        }

    fun updateOxygenSaturation(
        entry: OxygenSaturationEntry,
        percent: Int,
        pulse: Int?,
        context: OxygenContext?,
        site: OxygenSite?,
        oxygenGiven: Boolean,
        timestamp: Instant,
        comment: String?
    ) = runUpdate {
        dispatcher.dispatch(
            UpdateOxygenSaturationCommand(
                entry.id, entry.profileId, percent, pulse, context, site, oxygenGiven, timestamp, comment
            )
        )
    }

    fun updateActivity(session: ActivitySession, durationSeconds: Long, distanceInMeters: Double, comment: String?) =
        runUpdate {
            dispatcher.dispatch(
                UpdateActivityCommand(
                    session.id,
                    session.profileId,
                    session.startTime,
                    session.startTime.plusSeconds(durationSeconds),
                    distanceInMeters,
                    comment
                )
            )
        }

    /** Ends an ongoing migraine now; the form's end field is the way to give another time. */
    fun endMigraine(entry: MigraineEntry) {
        viewModelScope.launch {
            when (val result = dispatcher.dispatch(EndMigraineCommand(entry.id))) {
                is DomainResult.Success -> loadHistory()
                is DomainResult.Failure -> _uiState.value =
                    _uiState.value.copy(errorMessage = result.error.text(R.string.history_err_update_failed))
            }
        }
    }

    /** Runs an update; on success closes the edit dialog and reloads so list and charts refresh. */
    private fun runUpdate(block: suspend () -> DomainResult<*>) {
        viewModelScope.launch {
            when (val result = block()) {
                is DomainResult.Success -> {
                    _uiState.value = _uiState.value.copy(editing = null, editError = null)
                    loadHistory()
                }
                is DomainResult.Failure -> _uiState.value =
                    _uiState.value.copy(editError = result.error.text(R.string.history_err_update_failed))
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            exportedCsvContent = null,
            importResult = null,
            errorMessage = null,
            infoMessage = null
        )
    }
}

private fun EntryRef.kind(): EntryKind = when (this) {
    is EntryRef.Weight -> EntryKind.WEIGHT
    is EntryRef.BloodPressure -> EntryKind.BLOOD_PRESSURE
    is EntryRef.Glucose -> EntryKind.GLUCOSE
    is EntryRef.Activity -> EntryKind.ACTIVITY
    is EntryRef.WaistCircumference -> EntryKind.WAIST_CIRCUMFERENCE
    is EntryRef.OxygenSaturation -> EntryKind.OXYGEN_SATURATION
    is EntryRef.Migraine -> EntryKind.MIGRAINE
}

/** A storage failure keeps the exception's own message, else the screen's fallback; other errors have their own text. */
private fun DomainError.text(@StringRes fallback: Int): UiText =
    if (this is DomainError.Storage) cause?.toUiText(fallback) ?: UiText.Res(fallback) else toUiText()

private fun DomainError.detail(): String = (this as? DomainError.Storage)?.cause?.message.orEmpty()
