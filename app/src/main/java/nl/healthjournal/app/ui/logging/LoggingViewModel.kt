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
import nl.healthjournal.app.settings.MetricSlot
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

/** The picker values the person has to set themselves when there is no earlier entry to start from. */
enum class LogField { WEIGHT, SYSTOLIC, DIASTOLIC, PULSE, WAIST }

data class LoggingUiState(
    val activeProfile: Profile? = null,
    val selectedMetric: MetricType = MetricType.WEIGHT,
    /** The metrics in the order the person chose, Migraine included. */
    val orderedMetrics: List<MetricSlot> = MetricSlot.DEFAULT_ORDER,

    // Weight inputs & feedback
    val weightValue: Double = START_WEIGHT_KG,
    val weightInput: String = START_WEIGHT_KG.toString(),
    val previewBmi: BigDecimal? = null,
    val previewBmiCategory: NhgBmiCategory? = null,

    // BP inputs & feedback
    val systolicValue: Int = START_SYSTOLIC,
    val systolicInput: String = START_SYSTOLIC.toString(),
    val diastolicValue: Int = START_DIASTOLIC,
    val diastolicInput: String = START_DIASTOLIC.toString(),
    val pulseValue: Int = START_PULSE,
    val previewBpCategory: NhgBloodPressureCategory? = null,

    // Glucose inputs & feedback
    val glucoseInput: String = "",
    val glucoseContext: GlucoseContext = GlucoseContext.FASTING,
    val previewGlucoseCategory: NhgGlucoseCategory? = null,

    // Activity inputs
    val activityDurationInput: String = "",
    val activityDistanceInput: String = "",

    // Waist inputs & feedback
    val waistValue: Double = START_WAIST_CM,
    val waistInput: String = START_WAIST_CM.toString(),
    val previewWaistCategory: NhgWaistCircumferenceCategory? = null,

    // Optional comment for the entry being logged (one draft, cleared after save and on tab change)
    val commentInput: String = "",

    /**
     * The picker values the person set or that come from their own latest entry. A picker without an earlier
     * entry only shows a starting position: it is not a value of theirs until they move it, and it is never
     * classified or recorded.
     */
    val chosen: Set<LogField> = emptySet(),

    val isSaving: Boolean = false,
    val successMessage: UiText? = null,
    val errorMessage: UiText? = null
) {
    /** Whether the selected tab holds a value the person set, so the entry can be recorded. */
    val canSave: Boolean
        get() = when (selectedMetric) {
            MetricType.WEIGHT -> LogField.WEIGHT in chosen
            MetricType.BLOOD_PRESSURE -> LogField.SYSTOLIC in chosen && LogField.DIASTOLIC in chosen
            MetricType.WAIST_CIRCUMFERENCE -> LogField.WAIST in chosen
            MetricType.GLUCOSE, MetricType.ACTIVITY -> true
        }
}

// Where a picker starts when there is no earlier entry. These are scroll positions, not targets or advice.
private const val START_WEIGHT_KG = 75.0
private const val START_SYSTOLIC = 120
private const val START_DIASTOLIC = 80
private const val START_PULSE = 70
private const val START_WAIST_CM = 90.0

class LoggingViewModel(
    private val profileRepository: ProfileRepositoryPort,
    private val recordWeightUseCase: RecordWeightUseCase,
    private val recordBloodPressureUseCase: RecordBloodPressureUseCase,
    private val recordGlucoseUseCase: RecordGlucoseUseCase,
    private val recordActivityUseCase: RecordActivityUseCase,
    private val recordWaistCircumferenceUseCase: RecordWaistCircumferenceUseCase,
    private val healthLogRepository: HealthLogRepositoryPort? = null,
    /** Reads the units currently chosen, so typed values are converted to metric before validation. */
    private val units: () -> DisplayUnits = { DisplayUnits.DEFAULT },
    /** The order of the metric tabs; the first tab with a form of its own is the one that opens selected. */
    private val metricOrder: StateFlow<List<MetricSlot>> = MutableStateFlow(MetricSlot.DEFAULT_ORDER)
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoggingUiState(
            selectedMetric = metricOrder.value.firstNotNullOf { it.metricType },
            orderedMetrics = metricOrder.value
        )
    )
    val uiState: StateFlow<LoggingUiState> = _uiState.asStateFlow()

    init {
        observeMetricOrder()
        loadProfile()
    }

    private fun observeMetricOrder() {
        viewModelScope.launch {
            metricOrder.collect { order -> _uiState.value = _uiState.value.copy(orderedMetrics = order) }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile()
            val history = profile?.let { healthLogRepository?.getWeightHistory(it.id) }
            val bpHistory = profile?.let { healthLogRepository?.getBloodPressureHistory(it.id) }
            val glucoseHistory = profile?.let { healthLogRepository?.getGlucoseHistory(it.id) }
            val waistHistory = profile?.let { healthLogRepository?.getWaistCircumferenceHistory(it.id) }

            // Start from the person's own latest entries; without one the picker keeps its starting position
            val latestWeight = history?.maxByOrNull { it.timestamp }
            val latestWaist = waistHistory?.maxByOrNull { it.timestamp }
            val latestBp = bpHistory?.maxByOrNull { it.timestamp }
            val latestGlucose = glucoseHistory?.maxByOrNull { it.timestamp }

            val start = LoggingUiState()
            val weightKg = latestWeight?.weight?.value?.toDouble() ?: start.weightValue
            val waistCm = latestWaist?.waist?.value ?: start.waistValue
            val systolic = latestBp?.reading?.systolic ?: start.systolicValue
            val diastolic = latestBp?.reading?.diastolic ?: start.diastolicValue
            val pulse = latestBp?.reading?.pulse ?: start.pulseValue
            val formattedWeight = String.format(Locale.US, "%.1f", weightKg)
            val formattedWaist = String.format(Locale.US, "%.1f", waistCm)
            val formattedGlucose = latestGlucose?.glucose?.valueInMmolL?.let { String.format(Locale.US, "%.1f", it.toDouble()) }.orEmpty()

            _uiState.value = _uiState.value.copy(
                activeProfile = profile,
                weightValue = weightKg,
                weightInput = formattedWeight,
                waistValue = waistCm,
                waistInput = formattedWaist,
                systolicValue = systolic,
                systolicInput = systolic.toString(),
                diastolicValue = diastolic,
                diastolicInput = diastolic.toString(),
                pulseValue = pulse,
                glucoseInput = formattedGlucose,
                chosen = buildSet {
                    if (latestWeight != null) add(LogField.WEIGHT)
                    if (latestWaist != null) add(LogField.WAIST)
                    if (latestBp != null) addAll(listOf(LogField.SYSTOLIC, LogField.DIASTOLIC))
                    if (latestBp?.reading?.pulse != null) add(LogField.PULSE)
                }
            )

            updateWeightPreview(formattedWeight)
            updateWaistPreview(formattedWaist)
            updateBpPreview(systolic.toString(), diastolic.toString())
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
        _uiState.value = _uiState.value.copy(weightValue = value, weightInput = str, chosen = _uiState.value.chosen + LogField.WEIGHT, errorMessage = null)
        updateWeightPreview(str)
    }

    fun onWeightChanged(input: String) {
        val parsed = input.toDoubleOrNull()
        _uiState.value = _uiState.value.copy(
            weightInput = input,
            weightValue = parsed ?: _uiState.value.weightValue,
            chosen = _uiState.value.chosen + LogField.WEIGHT,
            errorMessage = null
        )
        updateWeightPreview(input)
    }

    private fun updateWeightPreview(input: String) {
        val weightVal = input.parseDecimal()?.let { units().weightToKg(it) }
        val profile = _uiState.value.activeProfile
        if (weightVal != null && weightVal in 1.0..700.0 && profile != null && LogField.WEIGHT in _uiState.value.chosen) {
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
            chosen = _uiState.value.chosen + LogField.SYSTOLIC,
            errorMessage = null
        )
        updateBpPreview(value.toString(), _uiState.value.diastolicInput)
    }

    fun onDiastolicValueChanged(value: Int) {
        _uiState.value = _uiState.value.copy(
            diastolicValue = value,
            diastolicInput = value.toString(),
            chosen = _uiState.value.chosen + LogField.DIASTOLIC,
            errorMessage = null
        )
        updateBpPreview(_uiState.value.systolicInput, value.toString())
    }

    fun onPulseValueChanged(value: Int) {
        _uiState.value = _uiState.value.copy(pulseValue = value, chosen = _uiState.value.chosen + LogField.PULSE, errorMessage = null)
    }

    /** The pulse only counts when the person set it; the starting position is never recorded. */
    private fun chosenPulse(): Int? = _uiState.value.pulseValue.takeIf { LogField.PULSE in _uiState.value.chosen }

    fun onSystolicChanged(input: String) {
        val parsed = input.toIntOrNull()
        _uiState.value = _uiState.value.copy(
            systolicInput = input,
            systolicValue = parsed ?: _uiState.value.systolicValue,
            chosen = _uiState.value.chosen + LogField.SYSTOLIC,
            errorMessage = null
        )
        updateBpPreview(input, _uiState.value.diastolicInput)
    }

    fun onDiastolicChanged(input: String) {
        val parsed = input.toIntOrNull()
        _uiState.value = _uiState.value.copy(
            diastolicInput = input,
            diastolicValue = parsed ?: _uiState.value.diastolicValue,
            chosen = _uiState.value.chosen + LogField.DIASTOLIC,
            errorMessage = null
        )
        updateBpPreview(_uiState.value.systolicInput, input)
    }

    private fun updateBpPreview(sysStr: String, diaStr: String) {
        val sys = sysStr.toIntOrNull()
        val dia = diaStr.toIntOrNull()
        val chosen = _uiState.value.chosen
        if (sys != null && dia != null && sys in 40..300 && dia in 20..200 && sys > dia &&
            LogField.SYSTOLIC in chosen && LogField.DIASTOLIC in chosen
        ) {
            val reading = BloodPressureReading(systolic = sys, diastolic = dia, pulse = chosenPulse())
            _uiState.value = _uiState.value.copy(previewBpCategory = NhgBloodPressureCategory.classify(reading))
        } else {
            _uiState.value = _uiState.value.copy(previewBpCategory = null)
        }
    }

    fun onGlucoseChanged(input: String) {
        _uiState.value = _uiState.value.copy(
            glucoseInput = input,
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
        _uiState.value = _uiState.value.copy(waistValue = value, waistInput = str, chosen = _uiState.value.chosen + LogField.WAIST, errorMessage = null)
        updateWaistPreview(str)
    }

    fun onWaistChanged(input: String) {
        val parsed = input.toDoubleOrNull()
        _uiState.value = _uiState.value.copy(
            waistInput = input,
            waistValue = parsed ?: _uiState.value.waistValue,
            chosen = _uiState.value.chosen + LogField.WAIST,
            errorMessage = null
        )
        updateWaistPreview(input)
    }

    private fun updateWaistPreview(input: String) {
        val cm = input.toDoubleOrNull()
        val profile = _uiState.value.activeProfile
        if (cm != null && cm in WaistCircumferenceCm.MIN_CM..WaistCircumferenceCm.MAX_CM && profile != null &&
            LogField.WAIST in _uiState.value.chosen
        ) {
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

        if (!_uiState.value.canSave) {
            _uiState.value = _uiState.value.copy(errorMessage = UiText.Res(R.string.log_err_choose_value))
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
                        val pulse = chosenPulse()
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
        private val units: () -> DisplayUnits = { DisplayUnits.DEFAULT },
        private val metricOrder: StateFlow<List<MetricSlot>> = MutableStateFlow(MetricSlot.DEFAULT_ORDER)
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
                units,
                metricOrder
            ) as T
        }
    }
}
