package nl.healthjournal.app.ui.logging

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import nl.healthjournal.app.settings.MetricDisplayOrderPreference
import nl.healthjournal.app.settings.UnitPreference
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.app.settings.MetricSlot
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.app.ui.common.distanceToMeters
import nl.healthjournal.app.ui.common.glucoseToMmol
import nl.healthjournal.app.ui.common.parseDecimal
import nl.healthjournal.app.ui.common.weightToKg
import nl.healthjournal.domain.model.common.GlucoseUnit
import nl.healthjournal.domain.model.common.UnitConversion
import nl.healthjournal.domain.model.metrics.*
import nl.healthjournal.domain.model.metrics.NhgBloodPressureCategory
import nl.healthjournal.domain.model.metrics.NhgBmiCategory
import nl.healthjournal.domain.model.metrics.NhgGlucoseCategory
import nl.healthjournal.domain.model.metrics.NhgWaistCircumferenceCategory
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.command.DomainResult
import nl.healthjournal.domain.command.Dispatcher
import nl.healthjournal.domain.command.GetHistoryQuery
import nl.healthjournal.domain.metrics.command.RecordActivityCommand
import nl.healthjournal.domain.metrics.command.RecordBloodPressureCommand
import nl.healthjournal.domain.metrics.command.RecordGlucoseCommand
import nl.healthjournal.domain.metrics.command.RecordOxygenSaturationCommand
import nl.healthjournal.domain.metrics.command.RecordWaistCircumferenceCommand
import nl.healthjournal.domain.metrics.command.RecordWeightCommand
import nl.healthjournal.domain.profile.command.GetActiveProfileQuery
import nl.healthjournal.domain.model.common.ProfileId
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale
import nl.healthjournal.domain.model.common.EntryComment
import nl.healthjournal.domain.model.common.WeightKg

enum class MetricType {
    WEIGHT,
    BLOOD_PRESSURE,
    GLUCOSE,
    ACTIVITY,
    WAIST_CIRCUMFERENCE,
    OXYGEN_SATURATION
}

/** The picker values the person has to set themselves when there is no earlier entry to start from. */
enum class LogField { WEIGHT, SYSTOLIC, DIASTOLIC, PULSE, WAIST, OXYGEN }

data class LoggingUiState(
    val activeProfile: Profile? = null,
    /** False until the first read of the profile has finished, so "no profile" is not shown while loading. */
    val profileLoaded: Boolean = false,
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

    // Oxygen saturation inputs: a value that stays unset until the person picks it
    val oxygenPercentValue: Int = START_OXYGEN_PERCENT,
    val oxygenPulseInput: String = "",
    val oxygenContext: OxygenContext? = null,
    val oxygenSite: OxygenSite? = null,
    val oxygenGiven: Boolean = false,

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
            MetricType.OXYGEN_SATURATION -> LogField.OXYGEN in chosen
            MetricType.GLUCOSE, MetricType.ACTIVITY -> true
        }
}

// Where a picker starts when there is no earlier entry. These are scroll positions, not targets or advice.
private const val START_WEIGHT_KG = 75.0
private const val START_SYSTOLIC = 120
private const val START_DIASTOLIC = 80
private const val START_PULSE = 70
private const val START_WAIST_CM = 90.0
private const val START_OXYGEN_PERCENT = 96

@HiltViewModel
class LoggingViewModel(
    private val dispatcher: Dispatcher,
    /** Reads the units currently chosen, so typed values are converted to metric before validation. */
    private val units: () -> DisplayUnits = { DisplayUnits.DEFAULT },
    /** The order of the metric tabs; the first tab with a form of its own is the one that opens selected. */
    private val metricOrder: StateFlow<List<MetricSlot>> = MutableStateFlow(MetricSlot.DEFAULT_ORDER)
) : ViewModel() {

    /** The constructor Hilt uses; tests pass the unit and order sources directly. */
    @Inject
    constructor(
        dispatcher: Dispatcher,
        unitPreference: UnitPreference,
        metricDisplayOrder: MetricDisplayOrderPreference
    ) : this(dispatcher, { unitPreference.resolve() }, metricDisplayOrder.order)

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
            val profile = dispatcher.query(GetActiveProfileQuery).getOrNull()
            val history = profile?.let { dispatcher.query(GetHistoryQuery(it.id)).getOrNull() }

            // Start from the person's own latest entries; without one the picker keeps its starting position
            val latestWeight = history?.health?.weights?.maxByOrNull { it.timestamp }
            val latestWaist = history?.health?.waistCircumferences?.maxByOrNull { it.timestamp }
            val latestBp = history?.health?.bloodPressures?.maxByOrNull { it.timestamp }
            val latestGlucose = history?.health?.glucoses?.maxByOrNull { it.timestamp }

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
                profileLoaded = true,
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
        if (weightVal != null && weightVal in WeightKg.MIN_WEIGHT_KG.toDouble()..WeightKg.MAX_WEIGHT_KG.toDouble() && profile != null && LogField.WEIGHT in _uiState.value.chosen) {
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
        if (sys != null && dia != null && sys in BloodPressureReading.MIN_SYSTOLIC..BloodPressureReading.MAX_SYSTOLIC &&
            dia in BloodPressureReading.MIN_DIASTOLIC..BloodPressureReading.MAX_DIASTOLIC && sys > dia &&
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
                previewWaistCategory = WaistCircumferenceClassifier.classify(profile.sex, waist)
            )
        } else {
            _uiState.value = _uiState.value.copy(previewWaistCategory = null)
        }
    }

    fun onOxygenPercentChanged(value: Int) {
        _uiState.value = _uiState.value.copy(
            oxygenPercentValue = value,
            chosen = _uiState.value.chosen + LogField.OXYGEN,
            errorMessage = null
        )
    }

    /** Keeps only digits, so the optional pulse can never hold anything but a number. */
    fun onOxygenPulseChanged(input: String) {
        _uiState.value = _uiState.value.copy(oxygenPulseInput = input.filter { it.isDigit() }.take(3), errorMessage = null)
    }

    /** Choosing the selected context again clears it, since it is optional. */
    fun onOxygenContextSelected(context: OxygenContext) {
        val current = _uiState.value.oxygenContext
        _uiState.value = _uiState.value.copy(oxygenContext = if (current == context) null else context)
    }

    fun onOxygenSiteSelected(site: OxygenSite) {
        val current = _uiState.value.oxygenSite
        _uiState.value = _uiState.value.copy(oxygenSite = if (current == site) null else site)
    }

    fun onOxygenGivenChanged(given: Boolean) {
        _uiState.value = _uiState.value.copy(oxygenGiven = given)
    }

    fun setGlucoseContext(context: GlucoseContext) {
        _uiState.value = _uiState.value.copy(glucoseContext = context)
        updateGlucosePreview(_uiState.value.glucoseInput, context)
    }

    private fun updateGlucosePreview(input: String, context: GlucoseContext) {
        val value = input.parseDecimal()?.takeIf { it > 0 }
        // A value outside the physical range has no category; the preview just stays empty
        val level = value?.let {
            runCatching { if (units().glucose == GlucoseUnit.MG_PER_DL) GlucoseLevel.fromMgDl(it) else GlucoseLevel(it) }.getOrNull()
        }
        if (level != null) {
            _uiState.value = _uiState.value.copy(previewGlucoseCategory = NhgGlucoseCategory.classify(level, context))
            return
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
            val state = _uiState.value
            val error = when (state.selectedMetric) {
                MetricType.WEIGHT -> saveWeight(profile.id, state)
                MetricType.BLOOD_PRESSURE -> saveBloodPressure(profile.id, state)
                MetricType.GLUCOSE -> saveGlucose(profile.id, state)
                MetricType.ACTIVITY -> saveActivity(profile.id, state)
                MetricType.WAIST_CIRCUMFERENCE -> saveWaist(profile.id, state)
                MetricType.OXYGEN_SATURATION -> saveOxygen(profile.id, state)
            }
            _uiState.value = if (error == null) {
                _uiState.value.copy(commentInput = "", isSaving = false)
            } else {
                _uiState.value.copy(errorMessage = error, isSaving = false)
            }
        }
    }

    // Each saver parses its own inputs, dispatches the record command and returns the error to show, or null.
    // The rules (ranges, comment length) are the domain's; only unparsable text is rejected here.

    private suspend fun saveWeight(profileId: ProfileId, state: LoggingUiState): UiText? {
        val current = units()
        val weightKg = state.weightInput.parseDecimal()?.let { current.weightToKg(it) }
            ?: return UiText.Res(R.string.log_err_weight, current.weightSymbol)
        val result = dispatcher.dispatch(
            RecordWeightCommand(profileId, UnitConversion.toStoredKg(weightKg), comment = state.commentInput)
        )
        return result.reportOr {
            _uiState.value = _uiState.value.copy(
                previewBmi = null,
                previewBmiCategory = null,
                successMessage = UiText.Res(R.string.log_msg_weight_saved)
            )
        }
    }

    private suspend fun saveBloodPressure(profileId: ProfileId, state: LoggingUiState): UiText? {
        val systolic = state.systolicInput.toIntOrNull() ?: return UiText.Res(R.string.log_err_systolic)
        val diastolic = state.diastolicInput.toIntOrNull() ?: return UiText.Res(R.string.log_err_diastolic)
        val result = dispatcher.dispatch(
            RecordBloodPressureCommand(profileId, systolic, diastolic, chosenPulse(), comment = state.commentInput)
        )
        return result.reportOr {
            _uiState.value = _uiState.value.copy(successMessage = UiText.Res(R.string.log_msg_bp_saved))
        }
    }

    private suspend fun saveGlucose(profileId: ProfileId, state: LoggingUiState): UiText? {
        val value = state.glucoseInput.parseDecimal()?.let { BigDecimal.valueOf(it) }
            ?: return UiText.Res(R.string.log_err_glucose)
        val command = if (units().glucose == GlucoseUnit.MG_PER_DL) {
            RecordGlucoseCommand(profileId, state.glucoseContext, valueInMgDl = value, comment = state.commentInput)
        } else {
            RecordGlucoseCommand(profileId, state.glucoseContext, valueInMmolL = value, comment = state.commentInput)
        }
        return dispatcher.dispatch(command).reportOr {
            _uiState.value = _uiState.value.copy(
                previewGlucoseCategory = null,
                successMessage = UiText.Res(R.string.log_msg_glucose_saved)
            )
        }
    }

    private suspend fun saveActivity(profileId: ProfileId, state: LoggingUiState): UiText? {
        val durationMinutes = state.activityDurationInput.parseDecimal()?.takeIf { it > 0 }
            ?: return UiText.Res(R.string.log_err_duration)
        val distanceMeters = state.activityDistanceInput.parseDecimal()
            ?.takeIf { it >= 0 }
            ?.let { units().distanceToMeters(it) }
            ?: return UiText.Res(R.string.log_err_distance, units().distanceSymbol)
        val endTime = Instant.now()
        val startTime = endTime.minusSeconds((durationMinutes * 60).toLong())
        val result = dispatcher.dispatch(
            RecordActivityCommand(profileId, startTime, endTime, distanceMeters, comment = state.commentInput)
        )
        return result.reportOr {
            _uiState.value = _uiState.value.copy(
                activityDurationInput = "",
                activityDistanceInput = "",
                successMessage = UiText.Res(R.string.log_msg_activity_saved)
            )
        }
    }

    private suspend fun saveWaist(profileId: ProfileId, state: LoggingUiState): UiText? {
        val cm = state.waistInput.toDoubleOrNull() ?: return UiText.Res(R.string.log_err_waist)
        val result = dispatcher.dispatch(RecordWaistCircumferenceCommand(profileId, cm, comment = state.commentInput))
        return result.reportOr {
            _uiState.value = _uiState.value.copy(
                previewWaistCategory = null,
                successMessage = UiText.Res(R.string.log_msg_waist_saved)
            )
        }
    }

    private suspend fun saveOxygen(profileId: ProfileId, state: LoggingUiState): UiText? {
        val result = dispatcher.dispatch(
            RecordOxygenSaturationCommand(
                profileId = profileId,
                percent = state.oxygenPercentValue,
                pulse = state.oxygenPulseInput.takeIf { it.isNotEmpty() }?.toIntOrNull(),
                context = state.oxygenContext,
                site = state.oxygenSite,
                oxygenGiven = state.oxygenGiven,
                comment = state.commentInput
            )
        )
        return result.reportOr {
            _uiState.value = _uiState.value.copy(
                oxygenPulseInput = "",
                oxygenContext = null,
                oxygenSite = null,
                oxygenGiven = false,
                chosen = _uiState.value.chosen - LogField.OXYGEN,
                successMessage = UiText.Res(R.string.log_msg_oxygen_saved)
            )
        }
    }

    /** Runs [onSuccess] for a success and returns null; for a failure returns the text to show. */
    private inline fun DomainResult<*>.reportOr(onSuccess: () -> Unit): UiText? {
        val error = errorOrNull() ?: return run { onSuccess(); null }
        return error.toUiText(units())
    }

    /** Keeps the draft comment single-line and within the limit, so the field can never hold an invalid value. */
    fun onCommentChanged(input: String) {
        val cleaned = input.replace(Regex("[\r\n]+"), " ").take(EntryComment.MAX_LENGTH)
        _uiState.value = _uiState.value.copy(commentInput = cleaned, errorMessage = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
