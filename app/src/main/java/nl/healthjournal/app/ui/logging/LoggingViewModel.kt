package nl.healthjournal.app.ui.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.domain.model.metrics.*
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgBmiCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.RecordActivityUseCase
import nl.healthjournal.domain.usecase.RecordBloodPressureUseCase
import nl.healthjournal.domain.usecase.RecordGlucoseUseCase
import nl.healthjournal.domain.usecase.RecordWeightUseCase
import java.math.BigDecimal
import java.time.Instant

enum class MetricType {
    WEIGHT,
    BLOOD_PRESSURE,
    GLUCOSE,
    ACTIVITY
}

data class LoggingUiState(
    val activeProfile: Profile? = null,
    val selectedMetric: MetricType = MetricType.WEIGHT,

    // Weight inputs & feedback
    val weightInput: String = "",
    val previewBmi: BigDecimal? = null,
    val previewBmiCategory: NhgBmiCategory? = null,

    // BP inputs & feedback
    val systolicInput: String = "",
    val diastolicInput: String = "",
    val previewBpCategory: NhgBloodPressureCategory? = null,

    // Glucose inputs & feedback
    val glucoseInput: String = "",
    val isGlucoseMgDl: Boolean = false,
    val glucoseContext: GlucoseContext = GlucoseContext.FASTING,
    val previewGlucoseCategory: NhgGlucoseCategory? = null,

    // Activity inputs
    val activityDurationInput: String = "",
    val activityDistanceInput: String = "",

    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class LoggingViewModel(
    private val profileRepository: ProfileRepositoryPort,
    private val recordWeightUseCase: RecordWeightUseCase,
    private val recordBloodPressureUseCase: RecordBloodPressureUseCase,
    private val recordGlucoseUseCase: RecordGlucoseUseCase,
    private val recordActivityUseCase: RecordActivityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoggingUiState())
    val uiState: StateFlow<LoggingUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile()
            _uiState.value = _uiState.value.copy(activeProfile = profile)
            updateWeightPreview(_uiState.value.weightInput)
        }
    }

    fun selectMetric(metric: MetricType) {
        _uiState.value = _uiState.value.copy(
            selectedMetric = metric,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onWeightChanged(input: String) {
        _uiState.value = _uiState.value.copy(weightInput = input, errorMessage = null)
        updateWeightPreview(input)
    }

    private fun updateWeightPreview(input: String) {
        val weightVal = input.toDoubleOrNull()
        val profile = _uiState.value.activeProfile
        if (weightVal != null && weightVal in 1.0..700.0 && profile != null) {
            val weight = WeightKg(weightVal)
            val bmiResult = profile.calculateBmi(weight)
            if (bmiResult != null) {
                _uiState.value = _uiState.value.copy(
                    previewBmi = bmiResult.bmi,
                    previewBmiCategory = NhgBmiCategory.classify(bmiResult.bmi)
                )
                return
            }
        }
        _uiState.value = _uiState.value.copy(previewBmi = null, previewBmiCategory = null)
    }

    fun onSystolicChanged(input: String) {
        _uiState.value = _uiState.value.copy(systolicInput = input, errorMessage = null)
        updateBpPreview(input, _uiState.value.diastolicInput)
    }

    fun onDiastolicChanged(input: String) {
        _uiState.value = _uiState.value.copy(diastolicInput = input, errorMessage = null)
        updateBpPreview(_uiState.value.systolicInput, input)
    }

    private fun updateBpPreview(sysStr: String, diaStr: String) {
        val sys = sysStr.toIntOrNull()
        val dia = diaStr.toIntOrNull()
        if (sys != null && dia != null && sys in 40..300 && dia in 20..200 && sys > dia) {
            val reading = BloodPressureReading(sys, dia)
            _uiState.value = _uiState.value.copy(previewBpCategory = NhgBloodPressureCategory.classify(reading))
        } else {
            _uiState.value = _uiState.value.copy(previewBpCategory = null)
        }
    }

    fun onGlucoseChanged(input: String) {
        _uiState.value = _uiState.value.copy(glucoseInput = input, errorMessage = null)
        updateGlucosePreview(input, _uiState.value.isGlucoseMgDl, _uiState.value.glucoseContext)
    }

    fun toggleGlucoseUnit(isMgDl: Boolean) {
        _uiState.value = _uiState.value.copy(isGlucoseMgDl = isMgDl)
        updateGlucosePreview(_uiState.value.glucoseInput, isMgDl, _uiState.value.glucoseContext)
    }

    fun onActivityDurationChanged(input: String) {
        _uiState.value = _uiState.value.copy(activityDurationInput = input, errorMessage = null)
    }

    fun onActivityDistanceChanged(input: String) {
        _uiState.value = _uiState.value.copy(activityDistanceInput = input, errorMessage = null)
    }

    fun setGlucoseContext(context: GlucoseContext) {
        _uiState.value = _uiState.value.copy(glucoseContext = context)
        updateGlucosePreview(_uiState.value.glucoseInput, _uiState.value.isGlucoseMgDl, context)
    }

    private fun updateGlucosePreview(input: String, isMgDl: Boolean, context: GlucoseContext) {
        val value = input.toDoubleOrNull()
        if (value != null && value > 0) {
            try {
                val level = if (isMgDl) GlucoseLevel.fromMgDl(value) else GlucoseLevel(value)
                _uiState.value = _uiState.value.copy(
                    previewGlucoseCategory = NhgGlucoseCategory.classify(level, context)
                )
                return
            } catch (e: Exception) {
                // Ignore out-of-range for live preview
            }
        }
        _uiState.value = _uiState.value.copy(previewGlucoseCategory = null)
    }

    fun saveCurrentMetric() {
        val profile = _uiState.value.activeProfile
        if (profile == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please create a profile first.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)
            try {
                when (_uiState.value.selectedMetric) {
                    MetricType.WEIGHT -> {
                        val weightVal = _uiState.value.weightInput.toDoubleOrNull()
                            ?: throw IllegalArgumentException("Please enter a valid weight in kg")
                        recordWeightUseCase(profile.id, BigDecimal.valueOf(weightVal))
                        _uiState.value = _uiState.value.copy(
                            weightInput = "",
                            previewBmi = null,
                            previewBmiCategory = null,
                            successMessage = "Weight entry recorded successfully!"
                        )
                    }
                    MetricType.BLOOD_PRESSURE -> {
                        val sys = _uiState.value.systolicInput.toIntOrNull()
                            ?: throw IllegalArgumentException("Please enter systolic value (mmHg)")
                        val dia = _uiState.value.diastolicInput.toIntOrNull()
                            ?: throw IllegalArgumentException("Please enter diastolic value (mmHg)")
                        recordBloodPressureUseCase(profile.id, sys, dia)
                        _uiState.value = _uiState.value.copy(
                            systolicInput = "",
                            diastolicInput = "",
                            previewBpCategory = null,
                            successMessage = "Blood pressure reading recorded!"
                        )
                    }
                    MetricType.GLUCOSE -> {
                        val gVal = _uiState.value.glucoseInput.toDoubleOrNull()
                            ?: throw IllegalArgumentException("Please enter glucose value")
                        if (_uiState.value.isGlucoseMgDl) {
                            recordGlucoseUseCase(
                                profileId = profile.id,
                                context = _uiState.value.glucoseContext,
                                valueInMgDl = BigDecimal.valueOf(gVal)
                            )
                        } else {
                            recordGlucoseUseCase(
                                profileId = profile.id,
                                context = _uiState.value.glucoseContext,
                                valueInMmolL = BigDecimal.valueOf(gVal)
                            )
                        }
                        _uiState.value = _uiState.value.copy(
                            glucoseInput = "",
                            previewGlucoseCategory = null,
                            successMessage = "Blood glucose entry recorded!"
                        )
                    }
                    MetricType.ACTIVITY -> {
                        val durationMinutes = _uiState.value.activityDurationInput.toDoubleOrNull()
                            ?.takeIf { it > 0 }
                            ?: throw IllegalArgumentException("Please enter a valid duration in minutes")
                        val distanceKm = _uiState.value.activityDistanceInput.toDoubleOrNull()
                            ?.takeIf { it >= 0 }
                            ?: throw IllegalArgumentException("Please enter a valid distance in km")
                        val endTime = Instant.now()
                        val startTime = endTime.minusSeconds((durationMinutes * 60).toLong())
                        recordActivityUseCase(
                            profileId = profile.id,
                            startTime = startTime,
                            endTime = endTime,
                            distanceInMeters = distanceKm * 1000.0
                        )
                        _uiState.value = _uiState.value.copy(
                            activityDurationInput = "",
                            activityDistanceInput = "",
                            successMessage = "Activity session recorded!"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to save entry")
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    class Factory(
        private val profileRepository: ProfileRepositoryPort,
        private val recordWeightUseCase: RecordWeightUseCase,
        private val recordBloodPressureUseCase: RecordBloodPressureUseCase,
        private val recordGlucoseUseCase: RecordGlucoseUseCase,
        private val recordActivityUseCase: RecordActivityUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoggingViewModel(
                profileRepository,
                recordWeightUseCase,
                recordBloodPressureUseCase,
                recordGlucoseUseCase,
                recordActivityUseCase
            ) as T
        }
    }
}
