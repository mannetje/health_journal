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
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.ImportResult
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.GetHealthHistoryUseCase

enum class HistoryFilter {
    ALL,
    WEIGHT,
    BLOOD_PRESSURE,
    GLUCOSE,
    ACTIVITY
}

data class HistoryUiState(
    val activeProfile: Profile? = null,
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val weights: List<WeightEntry> = emptyList(),
    val bloodPressures: List<BloodPressureEntry> = emptyList(),
    val glucoses: List<GlucoseEntry> = emptyList(),
    val activities: List<ActivitySession> = emptyList(),
    val isLoading: Boolean = false,
    val exportedCsvContent: String? = null,
    val importResult: ImportResult? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class HistoryViewModel(
    private val profileRepository: ProfileRepositoryPort,
    private val getHealthHistoryUseCase: GetHealthHistoryUseCase,
    private val dataExportPort: DataExportPort,
    private val dataImportPort: DataImportPort
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun setFilter(filter: HistoryFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
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
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load history"
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
                    else -> ""
                }
                _uiState.value = _uiState.value.copy(
                    exportedCsvContent = csv,
                    infoMessage = "CSV Export generated ($metric)!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Export failed: ${e.message}")
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
                    infoMessage = "Imported ${result.importedCount} rows (${result.skippedRows.size} skipped)"
                )
                loadHistory()
            } catch (e: Exception) {
                val errorMessage = if (metricType.lowercase() == "libra") {
                    "Libra import failed: ${e.message ?: "Unknown error"}. " +
                        "Ensure the file starts with #Version:, #Units:, and a #date;weight;... header."
                } else {
                    "Import failed: ${e.message}"
                }
                _uiState.value = _uiState.value.copy(errorMessage = errorMessage)
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
        private val dataImportPort: DataImportPort
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(
                profileRepository,
                getHealthHistoryUseCase,
                dataExportPort,
                dataImportPort
            ) as T
        }
    }
}
