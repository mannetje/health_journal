package nl.healthjournal.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.ImportResult
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.usecase.DeleteActivityUseCase
import nl.healthjournal.domain.usecase.DeleteBloodPressureUseCase
import nl.healthjournal.domain.usecase.DeleteGlucoseUseCase
import nl.healthjournal.domain.usecase.DeleteWaistCircumferenceUseCase
import nl.healthjournal.domain.usecase.DeleteWeightUseCase
import nl.healthjournal.domain.usecase.GetHealthHistoryUseCase
import nl.healthjournal.domain.usecase.UpdateActivityUseCase
import nl.healthjournal.domain.usecase.UpdateBloodPressureUseCase
import nl.healthjournal.domain.usecase.UpdateGlucoseUseCase
import nl.healthjournal.domain.usecase.UpdateWaistCircumferenceUseCase
import nl.healthjournal.domain.usecase.UpdateWeightUseCase
import java.math.BigDecimal
import java.time.Instant
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.app.ui.history.charts.TrendDateRange

enum class HistoryFilter {
    ALL,
    WEIGHT,
    BLOOD_PRESSURE,
    GLUCOSE,
    ACTIVITY,
    WAIST_CIRCUMFERENCE
}

/** The update and delete use cases the History screen needs, bundled to keep the constructor small. */
data class EntryUseCases(
    val updateWeight: UpdateWeightUseCase,
    val updateBloodPressure: UpdateBloodPressureUseCase,
    val updateGlucose: UpdateGlucoseUseCase,
    val updateActivity: UpdateActivityUseCase,
    val updateWaistCircumference: UpdateWaistCircumferenceUseCase,
    val deleteWeight: DeleteWeightUseCase,
    val deleteBloodPressure: DeleteBloodPressureUseCase,
    val deleteGlucose: DeleteGlucoseUseCase,
    val deleteActivity: DeleteActivityUseCase,
    val deleteWaistCircumference: DeleteWaistCircumferenceUseCase
)

/** A reference to one History entry, used to track which entry is being edited or deleted. */
sealed interface EntryRef {
    val id: MeasurementId
    data class Weight(val entry: WeightEntry) : EntryRef { override val id get() = entry.id }
    data class BloodPressure(val entry: BloodPressureEntry) : EntryRef { override val id get() = entry.id }
    data class Glucose(val entry: GlucoseEntry) : EntryRef { override val id get() = entry.id }
    data class Activity(val session: ActivitySession) : EntryRef { override val id get() = session.id }
    data class WaistCircumference(val entry: WaistCircumferenceEntry) : EntryRef { override val id get() = entry.id }
}

data class HistoryUiState(
    val activeProfile: Profile? = null,
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val selectedDateRange: TrendDateRange = TrendDateRange.THIRTY_DAYS,
    val weights: List<WeightEntry> = emptyList(),
    val bloodPressures: List<BloodPressureEntry> = emptyList(),
    val glucoses: List<GlucoseEntry> = emptyList(),
    val activities: List<ActivitySession> = emptyList(),
    val waistCircumferences: List<WaistCircumferenceEntry> = emptyList(),
    val isLoading: Boolean = false,
    val exportedCsvContent: String? = null,
    val importResult: ImportResult? = null,
    val errorMessage: UiText? = null,
    val infoMessage: UiText? = null,
    val editing: EntryRef? = null,
    val editError: UiText? = null,
    val pendingDelete: EntryRef? = null
)

class HistoryViewModel(
    private val profileRepository: ProfileRepositoryPort,
    private val getHealthHistoryUseCase: GetHealthHistoryUseCase,
    private val dataExportPort: DataExportPort,
    private val dataImportPort: DataImportPort,
    private val entryUseCases: EntryUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun setFilter(filter: HistoryFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun setDateRange(range: TrendDateRange) {
        _uiState.value = _uiState.value.copy(selectedDateRange = range)
    }

    /** [clearError] is false when the caller has just set an error that the reload must not wipe. */
    fun loadHistory(clearError: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = if (clearError) null else _uiState.value.errorMessage)
            try {
                val profile = profileRepository.getActiveProfile()
                if (profile == null) {
                    _uiState.value = _uiState.value.copy(
                        activeProfile = null,
                        isLoading = false
                    )
                    return@launch
                }

                val history = getHealthHistoryUseCase(profile.id)
                _uiState.value = _uiState.value.copy(
                    activeProfile = profile,
                    weights = history.weights,
                    bloodPressures = history.bloodPressures,
                    glucoses = history.glucoses,
                    activities = history.activities,
                    waistCircumferences = history.waistCircumferences,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUiText(R.string.history_err_load_failed)
                )
            }
        }
    }

    fun exportCsv(metric: String) {
        val profile = _uiState.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                val csv = when (metric.lowercase()) {
                    "weight" -> dataExportPort.exportWeightCsv(profile.id)
                    "blood_pressure", "bp" -> dataExportPort.exportBloodPressureCsv(profile.id)
                    "glucose" -> dataExportPort.exportGlucoseCsv(profile.id)
                    "activity" -> dataExportPort.exportActivityCsv(profile.id)
                    "waist_circumference", "waist" -> dataExportPort.exportWaistCircumferenceCsv(profile.id)
                    else -> ""
                }
                _uiState.value = _uiState.value.copy(
                    exportedCsvContent = csv,
                    infoMessage = UiText.Res(R.string.history_msg_export_done, metric)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = UiText.Res(R.string.history_err_export, e.message.orEmpty()))
            }
        }
    }

    fun importCsv(metricType: String, csvContent: String) {
        val profile = _uiState.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                val result = dataImportPort.importCsv(profile.id, metricType, csvContent)
                _uiState.value = _uiState.value.copy(
                    importResult = result,
                    infoMessage = UiText.Res(R.string.history_msg_import_done, result.importedCount, result.skippedRows.size)
                )
                loadHistory()
            } catch (e: Exception) {
                val errorMessage = if (metricType.lowercase() == "libra") {
                    UiText.Res(R.string.history_err_import_libra, e.toUiText(R.string.history_err_unknown))
                } else {
                    UiText.Res(R.string.history_err_import, e.message.orEmpty())
                }
                _uiState.value = _uiState.value.copy(errorMessage = errorMessage)
            }
        }
    }

    fun startEdit(entry: EntryRef) {
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
            try {
                val deleted = when (target) {
                    is EntryRef.Weight -> entryUseCases.deleteWeight(target.id)
                    is EntryRef.BloodPressure -> entryUseCases.deleteBloodPressure(target.id)
                    is EntryRef.Glucose -> entryUseCases.deleteGlucose(target.id)
                    is EntryRef.Activity -> entryUseCases.deleteActivity(target.id)
                    is EntryRef.WaistCircumference -> entryUseCases.deleteWaistCircumference(target.id)
                }
                _uiState.value = _uiState.value.copy(
                    pendingDelete = null,
                    errorMessage = if (deleted) null else UiText.Res(R.string.history_err_not_found)
                )
                loadHistory(clearError = deleted)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(pendingDelete = null, errorMessage = UiText.Res(R.string.history_err_delete, e.message.orEmpty()))
            }
        }
    }

    fun updateWeight(entry: WeightEntry, weightKg: BigDecimal, timestamp: Instant, comment: String?) =
        runUpdate { entryUseCases.updateWeight(entry.id, entry.profileId, weightKg, timestamp, comment = comment) }

    fun updateBloodPressure(entry: BloodPressureEntry, systolic: Int, diastolic: Int, timestamp: Instant, comment: String?) =
        runUpdate { entryUseCases.updateBloodPressure(entry.id, entry.profileId, systolic, diastolic, timestamp, comment = comment) }

    fun updateGlucose(entry: GlucoseEntry, context: GlucoseContext, valueInMmolL: BigDecimal, timestamp: Instant, comment: String?) =
        runUpdate { entryUseCases.updateGlucose(entry.id, entry.profileId, context, valueInMmolL, timestamp, comment = comment) }

    fun updateWaistCircumference(entry: WaistCircumferenceEntry, waistCm: Double, timestamp: Instant, comment: String?) =
        runUpdate { entryUseCases.updateWaistCircumference(entry.id, entry.profileId, waistCm, timestamp, comment = comment) }

    fun updateActivity(session: ActivitySession, durationSeconds: Long, distanceInMeters: Double, comment: String?) =
        runUpdate {
            entryUseCases.updateActivity(
                session.id,
                session.profileId,
                session.startTime,
                session.startTime.plusSeconds(durationSeconds),
                distanceInMeters,
                comment = comment
            )
        }

    /** Runs an update; on success closes the edit dialog and reloads so list and charts refresh. */
    private fun runUpdate(block: suspend () -> Any?) {
        viewModelScope.launch {
            try {
                if (block() == null) {
                    _uiState.value = _uiState.value.copy(editError = UiText.Res(R.string.history_err_not_found))
                    return@launch
                }
                _uiState.value = _uiState.value.copy(editing = null, editError = null)
                loadHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(editError = e.toUiText(R.string.history_err_update_failed))
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

    class Factory(
        private val profileRepository: ProfileRepositoryPort,
        private val getHealthHistoryUseCase: GetHealthHistoryUseCase,
        private val dataExportPort: DataExportPort,
        private val dataImportPort: DataImportPort,
        private val entryUseCases: EntryUseCases
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(
                profileRepository,
                getHealthHistoryUseCase,
                dataExportPort,
                dataImportPort,
                entryUseCases
            ) as T
        }
    }
}
