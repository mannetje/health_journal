package nl.healthjournal.app.ui.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.UiTextException
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.app.ui.common.distanceToMeters
import nl.healthjournal.app.ui.common.glucoseToMmol
import nl.healthjournal.app.ui.common.parseDecimal
import nl.healthjournal.app.ui.common.weightToKg
import nl.healthjournal.domain.model.common.GlucoseUnit
import nl.healthjournal.domain.model.common.UnitConversion
import nl.healthjournal.domain.model.metrics.*
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgBmiCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.model.nhg.NhgWaistCircumferenceCategory
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.RecordActivityUseCase
import nl.healthjournal.domain.usecase.RecordBloodPressureUseCase
import nl.healthjournal.domain.usecase.RecordGlucoseUseCase
import nl.healthjournal.domain.usecase.RecordWaistCircumferenceUseCase
import nl.healthjournal.domain.usecase.RecordWeightUseCase
import java.math.BigDecimal
import java.time.Instant

enum class MetricType {
    WEIGHT,
    BLOOD_PRESSURE,
    GLUCOSE,
    ACTIVITY,
    WAIST_CIRCUMFERENCE
}

data class LoggingUiState(
    val activeProfile: Profile? = null,
    val selectedMetric: MetricType = MetricType.WEIGHT,

    // Weight inputs & feedback
    val weightInput: String = "",
    val previewBmi: BigDecimal? = null,
    val previewBmiCategory: NhgBmiCategory? = null,
    val weightWaistInput: String = "",
    val previewWeightWaistCategory: NhgWaistCircumferenceCategory? = null,

    // BP inputs & feedback
    val systolicInput: String = "",
    val diastolicInput: String = "",
    val previewBpCategory: NhgBloodPressureCategory? = null,

    // Glucose inputs & feedback
    val glucoseInput: String = "",
    val glucoseContext: GlucoseContext = GlucoseContext.FASTING,
    val previewGlucoseCategory: NhgGlucoseCategory? = null,

    // Activity inputs
    val activityDurationInput: String = "",
    val activityDistanceInput: String = "",

    // Waist inputs & feedback
    val waistInput: String = "",
    val previewWaistCategory: NhgWaistCircumferenceCategory? = null,

    val isSaving: Boolean = false,
    val successMessage: UiText? = null,
    val errorMessage: UiText? = null
)

class LoggingViewModel(
    private val profileRepository: ProfileRepositoryPort,
    private val recordWeightUseCase: RecordWeightUseCase,
    private val recordBloodPressureUseCase: RecordBloodPressureUseCase,
    private val recordGlucoseUseCase: RecordGlucoseUseCase,
    private val recordActivityUseCase: RecordActivityUseCase,
    private val recordWaistCircumferenceUseCase: RecordWaistCircumferenceUseCase,
    /** Reads the units currently chosen, so typed values are converted to metric before validation. */
    private val units: () -> DisplayUnits = { DisplayUnits.DEFAULT }
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
            updateWeightWaistPreview(_uiState.value.weightWaistInput)
            updateWaistPreview(_uiState.value.waistInput)
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
        val weightVal = input.parseDecimal()?.let { units().weightToKg(it) }
        val profile = _uiState.value.activeProfile
        if (weightVal != null && weightVal in 1.0..700.0 && profile != null) {
            val weight = WeightKg(UnitConversion.toStoredKg(weightVal))
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
        updateGlucosePreview(input, _uiState.value.glucoseContext)
    }

    /** Re-runs the live previews after the display units changed, since typed numbers mean something else now. */
    fun onUnitsChanged() {
        updateWeightPreview(_uiState.value.weightInput)
        updateGlucosePreview(_uiState.value.glucoseInput, _uiState.value.glucoseContext)
    }

    fun onActivityDurationChanged(input: String) {
        _uiState.value = _uiState.value.copy(activityDurationInput = input, errorMessage = null)
    }

    fun onActivityDistanceChanged(input: String) {
        _uiState.value = _uiState.value.copy(activityDistanceInput = input, errorMessage = null)
    }

    fun onWaistChanged(input: String) {
        _uiState.value = _uiState.value.copy(waistInput = input, errorMessage = null)
        updateWaistPreview(input)
    }

    private fun updateWaistPreview(input: String) {
        val cm = input.toDoubleOrNull()
        val profile = _uiState.value.activeProfile
        if (cm != null && cm in WaistCircumferenceCm.MIN_CM..WaistCircumferenceCm.MAX_CM && profile != null) {
            val waist = WaistCircumferenceCm(cm)
            _uiState.value = _uiState.value.copy(
                previewWaistCategory = profile.classifyWaistCircumference(waist)
            )
        } else {
            _uiState.value = _uiState.value.copy(previewWaistCategory = null)
        }
    }

    fun onWeightWaistChanged(input: String) {
        _uiState.value = _uiState.value.copy(weightWaistInput = input, errorMessage = null)
        updateWeightWaistPreview(input)
    }

    private fun updateWeightWaistPreview(input: String) {
        val cm = input.toDoubleOrNull()
        val profile = _uiState.value.activeProfile
        if (cm != null && cm in WaistCircumferenceCm.MIN_CM..WaistCircumferenceCm.MAX_CM && profile != null) {
            val waist = WaistCircumferenceCm(cm)
            _uiState.value = _uiState.value.copy(
                previewWeightWaistCategory = profile.classifyWaistCircumference(waist)
            )
        } else {
            _uiState.value = _uiState.value.copy(previewWeightWaistCategory = null)
        }
    }

    fun setGlucoseContext(context: GlucoseContext) {
        _uiState.value = _uiState.value.copy(glucoseContext = context)
        updateGlucosePreview(_uiState.value.glucoseInput, context)
    }

    private fun updateGlucosePreview(input: String, context: GlucoseContext) {
        val value = input.parseDecimal()
        if (value != null && value > 0) {
            try {
                val level = if (units().glucose == GlucoseUnit.MG_PER_DL) GlucoseLevel.fromMgDl(value) else GlucoseLevel(value)
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
            _uiState.value = _uiState.value.copy(errorMessage = UiText.Res(R.string.log_msg_no_profile_first))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)
            try {
                when (_uiState.value.selectedMetric) {
                    MetricType.WEIGHT -> {
                        val current = units()
                        val weightKg = _uiState.value.weightInput.parseDecimal()?.let { current.weightToKg(it) }
                            ?: throw UiTextException(UiText.Res(R.string.log_err_weight, current.weightSymbol))
                        val timestamp = Instant.now()
                        recordWeightUseCase(profile.id, UnitConversion.toStoredKg(weightKg), timestamp)
                        val waistInput = _uiState.value.weightWaistInput.trim()
                        if (waistInput.isNotEmpty()) {
                            val waistCm = waistInput.toDoubleOrNull()
                                ?.takeIf { it in WaistCircumferenceCm.MIN_CM..WaistCircumferenceCm.MAX_CM }
                                ?: throw UiTextException(UiText.Res(R.string.log_err_waist))
                            recordWaistCircumferenceUseCase(profile.id, waistCm, timestamp)
                        }
                        _uiState.value = _uiState.value.copy(
                            weightInput = "",
                            weightWaistInput = "",
                            previewBmi = null,
                            previewBmiCategory = null,
                            previewWeightWaistCategory = null,
                            successMessage = UiText.Res(R.string.log_msg_weight_saved)
                        )
                    }
                    MetricType.BLOOD_PRESSURE -> {
                        val sys = _uiState.value.systolicInput.toIntOrNull()
                            ?: throw UiTextException(UiText.Res(R.string.log_err_systolic))
                        val dia = _uiState.value.diastolicInput.toIntOrNull()
                            ?: throw UiTextException(UiText.Res(R.string.log_err_diastolic))
                        recordBloodPressureUseCase(profile.id, sys, dia)
                        _uiState.value = _uiState.value.copy(
                            systolicInput = "",
                            diastolicInput = "",
                            previewBpCategory = null,
                            successMessage = UiText.Res(R.string.log_msg_bp_saved)
                        )
                    }
                    MetricType.GLUCOSE -> {
                        val gVal = _uiState.value.glucoseInput.parseDecimal()
                            ?: throw UiTextException(UiText.Res(R.string.log_err_glucose))
                        if (units().glucose == GlucoseUnit.MG_PER_DL) {
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
                            successMessage = UiText.Res(R.string.log_msg_glucose_saved)
                        )
                    }
                    MetricType.ACTIVITY -> {
                        val durationMinutes = _uiState.value.activityDurationInput.parseDecimal()
                            ?.takeIf { it > 0 }
                            ?: throw UiTextException(UiText.Res(R.string.log_err_duration))
                        val distanceMeters = _uiState.value.activityDistanceInput.parseDecimal()
                            ?.takeIf { it >= 0 }
                            ?.let { units().distanceToMeters(it) }
                            ?: throw UiTextException(UiText.Res(R.string.log_err_distance, units().distanceSymbol))
                        val endTime = Instant.now()
                        val startTime = endTime.minusSeconds((durationMinutes * 60).toLong())
                        recordActivityUseCase(
                            profileId = profile.id,
                            startTime = startTime,
                            endTime = endTime,
                            distanceInMeters = distanceMeters
                        )
                        _uiState.value = _uiState.value.copy(
                            activityDurationInput = "",
                            activityDistanceInput = "",
                            successMessage = UiText.Res(R.string.log_msg_activity_saved)
                        )
                    }
                    MetricType.WAIST_CIRCUMFERENCE -> {
                        val cm = _uiState.value.waistInput.toDoubleOrNull()
                            ?.takeIf { it in WaistCircumferenceCm.MIN_CM..WaistCircumferenceCm.MAX_CM }
                            ?: throw UiTextException(UiText.Res(R.string.log_err_waist))
                        recordWaistCircumferenceUseCase(profile.id, cm)
                        _uiState.value = _uiState.value.copy(
                            waistInput = "",
                            previewWaistCategory = null,
                            successMessage = UiText.Res(R.string.log_msg_waist_saved)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.toUiText(R.string.log_err_save_failed))
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
        private val recordActivityUseCase: RecordActivityUseCase,
        private val recordWaistCircumferenceUseCase: RecordWaistCircumferenceUseCase,
        private val units: () -> DisplayUnits = { DisplayUnits.DEFAULT }
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoggingViewModel(
                profileRepository,
                recordWeightUseCase,
                recordBloodPressureUseCase,
                recordGlucoseUseCase,
                recordActivityUseCase,
                recordWaistCircumferenceUseCase,
                units
            ) as T
        }
    }
}
