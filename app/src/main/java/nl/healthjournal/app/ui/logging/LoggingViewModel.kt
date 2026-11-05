package nl.healthjournal.app.ui.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.app.settings.MetricDisplayOrderPreference
import nl.healthjournal.app.settings.MetricSlot
import nl.healthjournal.app.settings.UnitPreference
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.domain.metrics.model.GlucoseContext
import nl.healthjournal.domain.metrics.model.OxygenContext
import nl.healthjournal.domain.metrics.model.OxygenSite
import nl.healthjournal.domain.profile.command.GetActiveProfileQuery
import nl.healthjournal.domain.shared.command.Dispatcher
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.GetHistoryQuery
import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.shared.model.ProfileId

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
        val state = _uiState.value
        val preview = weightPreview(input, units(), state.activeProfile, state.chosen)
        _uiState.value = state.copy(previewBmi = preview?.bmi, previewBmiCategory = preview?.category)
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
        val state = _uiState.value
        _uiState.value = state.copy(previewBpCategory = bloodPressurePreview(sysStr, diaStr, state.chosenPulse(), state.chosen))
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
        val state = _uiState.value
        _uiState.value = state.copy(previewWaistCategory = waistPreview(input, state.activeProfile, state.chosen))
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
        _uiState.value = _uiState.value.copy(previewGlucoseCategory = glucosePreview(input, context, units()))
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
        val command = weightCommand(profileId, state, units()).orReturn { return it }
        val result = dispatcher.dispatch(command)
        return result.reportOr {
            _uiState.value = _uiState.value.copy(
                previewBmi = null,
                previewBmiCategory = null,
                successMessage = UiText.Res(R.string.log_msg_weight_saved)
            )
        }
    }

    private suspend fun saveBloodPressure(profileId: ProfileId, state: LoggingUiState): UiText? {
        val command = bloodPressureCommand(profileId, state).orReturn { return it }
        val result = dispatcher.dispatch(command)
        return result.reportOr {
            _uiState.value = _uiState.value.copy(successMessage = UiText.Res(R.string.log_msg_bp_saved))
        }
    }

    private suspend fun saveGlucose(profileId: ProfileId, state: LoggingUiState): UiText? {
        val command = glucoseCommand(profileId, state, units()).orReturn { return it }
        return dispatcher.dispatch(command).reportOr {
            _uiState.value = _uiState.value.copy(
                previewGlucoseCategory = null,
                successMessage = UiText.Res(R.string.log_msg_glucose_saved)
            )
        }
    }

    private suspend fun saveActivity(profileId: ProfileId, state: LoggingUiState): UiText? {
        val command = activityCommand(profileId, state, units(), Instant.now()).orReturn { return it }
        val result = dispatcher.dispatch(command)
        return result.reportOr {
            _uiState.value = _uiState.value.copy(
                activityDurationInput = "",
                activityDistanceInput = "",
                successMessage = UiText.Res(R.string.log_msg_activity_saved)
            )
        }
    }

    private suspend fun saveWaist(profileId: ProfileId, state: LoggingUiState): UiText? {
        val command = waistCommand(profileId, state).orReturn { return it }
        val result = dispatcher.dispatch(command)
        return result.reportOr {
            _uiState.value = _uiState.value.copy(
                previewWaistCategory = null,
                successMessage = UiText.Res(R.string.log_msg_waist_saved)
            )
        }
    }

    private suspend fun saveOxygen(profileId: ProfileId, state: LoggingUiState): UiText? {
        val result = dispatcher.dispatch(oxygenCommand(profileId, state))
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
