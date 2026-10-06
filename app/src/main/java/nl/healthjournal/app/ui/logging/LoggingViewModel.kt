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
import nl.healthjournal.domain.model.profile.Sex
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.RecordActivityUseCase
import nl.healthjournal.domain.usecase.RecordBloodPressureUseCase
import nl.healthjournal.domain.usecase.RecordGlucoseUseCase
import nl.healthjournal.domain.usecase.RecordWaistCircumferenceUseCase
import nl.healthjournal.domain.usecase.RecordWeightUseCase
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

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
    val weightValue: Double = 75.0,
    val weightInput: String = "75.0",
    val previewBmi: BigDecimal? = null,
    val previewBmiCategory: NhgBmiCategory? = null,

    // BP inputs & feedback
    val systolicValue: Int = 120,
    val systolicInput: String = "120",
    val diastolicValue: Int = 80,
    val diastolicInput: String = "80",
    val pulseValue: Int = 70,
    val previewBpCategory: NhgBloodPressureCategory? = null,

    // Glucose inputs & feedback
    val glucoseValue: Double = 5.5,
    val glucoseInput: String = "5.5",
    val glucoseContext: GlucoseContext = GlucoseContext.FASTING,
    val previewGlucoseCategory: NhgGlucoseCategory? = null,

    // Activity inputs
    val activityDurationInput: String = "",
    val activityDistanceInput: String = "",

    // Waist inputs & feedback
    val waistValue: Double = 90.0,
    val waistInput: String = "90.0",
    val previewWaistCategory: NhgWaistCircumferenceCategory? = null,

    // Optional comment for the entry being logged (one draft, cleared after save and on tab change)
    val commentInput: String = "",

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
    private val healthLogRepository: HealthLogRepositoryPort? = null,
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
            val history = profile?.let { healthLogRepository?.getWeightHistory(it.id) }
            val bpHistory = profile?.let { healthLogRepository?.getBloodPressureHistory(it.id) }
            val glucoseHistory = profile?.let { healthLogRepository?.getGlucoseHistory(it.id) }
            val waistHistory = profile?.let { healthLogRepository?.getWaistCircumferenceHistory(it.id) }

            // Pre-fill Weight
            val latestWeight = history?.maxByOrNull { it.timestamp }
            val profileHeight = profile?.height
            val prefilledWeightKg = when {
                latestWeight != null -> latestWeight.weight.value.toDouble()
                profileHeight != null -> {
                    val hMeters = profileHeight.value / 100.0
                    22.5 * hMeters * hMeters
                }
                else -> 75.0
            }

            // Pre-fill Waist
            val latestWaist = waistHistory?.maxByOrNull { it.timestamp }
            val prefilledWaistCm = when {
                latestWaist != null -> latestWaist.waist.value
                profile?.sex == Sex.FEMALE -> 74.0
                profile?.sex == Sex.MALE -> 86.5
                else -> 90.0
            }

            // Pre-fill Blood Pressure & Pulse
            val latestBp = bpHistory?.maxByOrNull { it.timestamp }
            val prefilledSystolic = latestBp?.reading?.systolic ?: 120
            val prefilledDiastolic = latestBp?.reading?.diastolic ?: 80
            val prefilledPulse = latestBp?.reading?.pulse ?: 70

            // Pre-fill Glucose
            val latestGlucose = glucoseHistory?.maxByOrNull { it.timestamp }
            val prefilledGlucoseMmol = latestGlucose?.glucose?.valueInMmolL?.toDouble() ?: 5.5

            val formattedWeight = String.format(Locale.US, "%.1f", prefilledWeightKg)
            val formattedWaist = String.format(Locale.US, "%.1f", prefilledWaistCm)
            val formattedGlucose = String.format(Locale.US, "%.1f", prefilledGlucoseMmol)

            _uiState.value = _uiState.value.copy(
                activeProfile = profile,
                weightValue = prefilledWeightKg,
                weightInput = formattedWeight,
                waistValue = prefilledWaistCm,
                waistInput = formattedWaist,
                systolicValue = prefilledSystolic,
                systolicInput = prefilledSystolic.toString(),
                diastolicValue = prefilledDiastolic,
                diastolicInput = prefilledDiastolic.toString(),
                pulseValue = prefilledPulse,
                glucoseValue = prefilledGlucoseMmol,
                glucoseInput = formattedGlucose
            )

            updateWeightPreview(formattedWeight)
            updateWaistPreview(formattedWaist)
            updateBpPreview(prefilledSystolic.toString(), prefilledDiastolic.toString())
        }
    }

    fun selectMetric(metric: MetricType) {
        _uiState.value = _uiState.value.copy(
            selectedMetric = metric,
            commentInput = "",
            errorMessage = null,
            successMessage = null
        )
    }

    fun onWeightValueChanged(value: Double) {
        val str = String.format(Locale.US, "%.1f", value)
        _uiState.value = _uiState.value.copy(weightValue = value, weightInput = str, errorMessage = null)
        updateWeightPreview(str)
    }

    fun onWeightChanged(input: String) {
        val parsed = input.toDoubleOrNull()
        _uiState.value = _uiState.value.copy(
            weightInput = input,
            weightValue = parsed ?: _uiState.value.weightValue,
            errorMessage = null
        )
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

    fun onSystolicValueChanged(value: Int) {
        _uiState.value = _uiState.value.copy(
            systolicValue = value,
            systolicInput = value.toString(),
            errorMessage = null
        )
        updateBpPreview(value.toString(), _uiState.value.diastolicInput)
    }

    fun onDiastolicValueChanged(value: Int) {
        _uiState.value = _uiState.value.copy(
            diastolicValue = value,
            diastolicInput = value.toString(),
            errorMessage = null
        )
        updateBpPreview(_uiState.value.systolicInput, value.toString())
    }

    fun onPulseValueChanged(value: Int) {
        _uiState.value = _uiState.value.copy(pulseValue = value, errorMessage = null)
    }

    fun onSystolicChanged(input: String) {
        val parsed = input.toIntOrNull()
        _uiState.value = _uiState.value.copy(
            systolicInput = input,
            systolicValue = parsed ?: _uiState.value.systolicValue,
            errorMessage = null
        )
        updateBpPreview(input, _uiState.value.diastolicInput)
    }

    fun onDiastolicChanged(input: String) {
        val parsed = input.toIntOrNull()
        _uiState.value = _uiState.value.copy(
            diastolicInput = input,
            diastolicValue = parsed ?: _uiState.value.diastolicValue,
            errorMessage = null
        )
        updateBpPreview(_uiState.value.systolicInput, input)
    }

    private fun updateBpPreview(sysStr: String, diaStr: String) {
        val sys = sysStr.toIntOrNull()
        val dia = diaStr.toIntOrNull()
        if (sys != null && dia != null && sys in 40..300 && dia in 20..200 && sys > dia) {
            val reading = BloodPressureReading(systolic = sys, diastolic = dia, pulse = _uiState.value.pulseValue)
            _uiState.value = _uiState.value.copy(previewBpCategory = NhgBloodPressureCategory.classify(reading))
        } else {
            _uiState.value = _uiState.value.copy(previewBpCategory = null)
        }
    }

    fun onGlucoseChanged(input: String) {
        val parsed = input.toDoubleOrNull()
        _uiState.value = _uiState.value.copy(
            glucoseInput = input,
            glucoseValue = parsed ?: _uiState.value.glucoseValue,
            errorMessage = null
        )
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

    fun onWaistValueChanged(value: Double) {
        val str = String.format(Locale.US, "%.1f", value)
        _uiState.value = _uiState.value.copy(waistValue = value, waistInput = str, errorMessage = null)
        updateWaistPreview(str)
    }

    fun onWaistChanged(input: String) {
        val parsed = input.toDoubleOrNull()
        _uiState.value = _uiState.value.copy(
            waistInput = input,
            waistValue = parsed ?: _uiState.value.waistValue,
            errorMessage = null
        )
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
            val comment = _uiState.value.commentInput
            try {
                when (_uiState.value.selectedMetric) {
                    MetricType.WEIGHT -> {
                        val current = units()
                        val weightKg = _uiState.value.weightInput.parseDecimal()?.let { current.weightToKg(it) }
                            ?: throw UiTextException(UiText.Res(R.string.log_err_weight, current.weightSymbol))
                        val timestamp = Instant.now()
                        recordWeightUseCase(profile.id, UnitConversion.toStoredKg(weightKg), timestamp, comment = comment)
                        _uiState.value = _uiState.value.copy(
                            previewBmi = null,
                            previewBmiCategory = null,
                            successMessage = UiText.Res(R.string.log_msg_weight_saved)
                        )
                    }
                    MetricType.BLOOD_PRESSURE -> {
                        val sys = _uiState.value.systolicInput.toIntOrNull()
                            ?: throw UiTextException(UiText.Res(R.string.log_err_systolic))
                        val dia = _uiState.value.diastolicInput.toIntOrNull()
                            ?: throw UiTextException(UiText.Res(R.string.log_err_diastolic))
                        val pulse = _uiState.value.pulseValue
                        recordBloodPressureUseCase(profile.id, sys, dia, pulse, comment = comment)
                        _uiState.value = _uiState.value.copy(
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
                                valueInMgDl = BigDecimal.valueOf(gVal),
                                comment = comment
                            )
                        } else {
                            recordGlucoseUseCase(
                                profileId = profile.id,
                                context = _uiState.value.glucoseContext,
                                valueInMmolL = BigDecimal.valueOf(gVal),
                                comment = comment
                            )
                        }
                        _uiState.value = _uiState.value.copy(
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
                            distanceInMeters = distanceMeters,
                            comment = comment
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
                        recordWaistCircumferenceUseCase(profile.id, cm, comment = comment)
                        _uiState.value = _uiState.value.copy(
                            previewWaistCategory = null,
                            successMessage = UiText.Res(R.string.log_msg_waist_saved)
                        )
                    }
                }
                _uiState.value = _uiState.value.copy(commentInput = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.toUiText(R.string.log_err_save_failed))
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    /** Keeps the draft comment single-line and within the limit, so the field can never hold an invalid value. */
    fun onCommentChanged(input: String) {
        val cleaned = input.replace(Regex("[\r\n]+"), " ").take(EntryComment.MAX_LENGTH)
        _uiState.value = _uiState.value.copy(commentInput = cleaned, errorMessage = null)
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
        private val healthLogRepository: HealthLogRepositoryPort? = null,
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
                healthLogRepository,
                units
            ) as T
        }
    }
}
