package nl.healthjournal.app.ui.logging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.common.asString
import nl.healthjournal.domain.model.common.GlucoseUnit
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.app.ui.logging.components.HorizontalRulerPicker
import nl.healthjournal.app.ui.logging.components.StackedBpPulsePicker
import nl.healthjournal.app.ui.nhg.RangeSourceNote
import nl.healthjournal.app.ui.nhg.label
import nl.healthjournal.app.ui.nhg.getBmiColor
import nl.healthjournal.app.ui.nhg.getBpColor
import nl.healthjournal.app.ui.nhg.getGlucoseColor
import nl.healthjournal.app.ui.nhg.getWaistColor
import nl.healthjournal.app.ui.theme.onSuccessContainerColor
import nl.healthjournal.app.ui.theme.successContainerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMetricScreen(
    viewModel: LoggingViewModel,
    modifier: Modifier = Modifier,
    onGlucoseUnitSelected: (GlucoseUnit) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val units = LocalDisplayUnits.current

    LaunchedEffect(units) { viewModel.onUnitsChanged() }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.log_title),
            style = MaterialTheme.typography.headlineSmall
        )

        if (state.activeProfile == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = stringResource(R.string.log_no_profile),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        state.errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        state.successMessage?.let { success ->
            Card(
                colors = CardDefaults.cardColors(containerColor = successContainerColor)
            ) {
                Text(
                    text = success.asString(),
                    color = onSuccessContainerColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Metric Selector Tabs
        PrimaryScrollableTabRow(selectedTabIndex = state.selectedMetric.ordinal, edgePadding = 0.dp) {
            Tab(
                selected = state.selectedMetric == MetricType.WEIGHT,
                onClick = { viewModel.selectMetric(MetricType.WEIGHT) },
                text = { Text(stringResource(R.string.log_tab_weight), maxLines = 1, softWrap = false) }
            )
            Tab(
                selected = state.selectedMetric == MetricType.BLOOD_PRESSURE,
                onClick = { viewModel.selectMetric(MetricType.BLOOD_PRESSURE) },
                text = { Text(stringResource(R.string.log_tab_blood_pressure), maxLines = 1, softWrap = false) }
            )
            Tab(
                selected = state.selectedMetric == MetricType.GLUCOSE,
                onClick = { viewModel.selectMetric(MetricType.GLUCOSE) },
                text = { Text(stringResource(R.string.log_tab_glucose), maxLines = 1, softWrap = false) }
            )
            Tab(
                selected = state.selectedMetric == MetricType.ACTIVITY,
                onClick = { viewModel.selectMetric(MetricType.ACTIVITY) },
                text = { Text(stringResource(R.string.log_tab_activity), maxLines = 1, softWrap = false) }
            )
            Tab(
                selected = state.selectedMetric == MetricType.WAIST_CIRCUMFERENCE,
                onClick = { viewModel.selectMetric(MetricType.WAIST_CIRCUMFERENCE) },
                text = { Text(stringResource(R.string.log_tab_waist_circumference), maxLines = 1, softWrap = false) }
            )
        }

        when (state.selectedMetric) {
            MetricType.WEIGHT -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.log_weight_label, units.weightSymbol) + ": ${state.weightInput}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalRulerPicker(
                        value = state.weightValue,
                        onValueChange = { viewModel.onWeightValueChanged(it) },
                        range = 20.0..250.0,
                        step = 0.5,
                        unitLabel = units.weightSymbol
                    )
                }

                if (state.previewBmi != null && state.previewBmiCategory != null) {
                    CategoryBadge(
                        label = stringResource(
                            R.string.log_bmi_badge,
                            state.previewBmi.toString(),
                            state.previewBmiCategory!!.label()
                        ),
                        color = getBmiColor(state.previewBmiCategory!!)
                    )
                }

                OutlinedTextField(
                    value = state.weightWaistInput,
                    onValueChange = { viewModel.onWeightWaistChanged(it) },
                    label = { Text(stringResource(R.string.log_waist_optional_label)) },
                    placeholder = { Text(stringResource(R.string.log_waist_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.previewWeightWaistCategory != null) {
                    CategoryBadge(
                        label = stringResource(
                            R.string.log_waist_badge,
                            state.weightWaistInput,
                            state.previewWeightWaistCategory!!.label(state.activeProfile?.sex)
                        ),
                        color = getWaistColor(state.previewWeightWaistCategory!!)
                    )
                }
            }

            MetricType.BLOOD_PRESSURE -> {
                StackedBpPulsePicker(
                    systolic = state.systolicValue,
                    onSystolicChange = { viewModel.onSystolicValueChanged(it) },
                    diastolic = state.diastolicValue,
                    onDiastolicChange = { viewModel.onDiastolicValueChanged(it) },
                    pulse = state.pulseValue,
                    onPulseChange = { viewModel.onPulseValueChanged(it) },
                    systolicLabel = stringResource(R.string.log_systolic_label),
                    diastolicLabel = stringResource(R.string.log_diastolic_label),
                    pulseLabel = stringResource(R.string.log_pulse_label)
                )

                if (state.previewBpCategory != null) {
                    CategoryBadge(
                        label = state.previewBpCategory!!.label(),
                        color = getBpColor(state.previewBpCategory!!)
                    )
                }
            }

            MetricType.GLUCOSE -> {
                // Context selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.glucoseContext == GlucoseContext.FASTING,
                        onClick = { viewModel.setGlucoseContext(GlucoseContext.FASTING) },
                        label = { Text(stringResource(R.string.log_glucose_context_fasting)) }
                    )
                    FilterChip(
                        selected = state.glucoseContext == GlucoseContext.POSTPRANDIAL,
                        onClick = { viewModel.setGlucoseContext(GlucoseContext.POSTPRANDIAL) },
                        label = { Text(stringResource(R.string.log_glucose_context_postprandial)) }
                    )
                }

                // Unit toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.log_unit_label), style = MaterialTheme.typography.bodyMedium)
                    FilterChip(
                        selected = units.glucose == GlucoseUnit.MMOL_PER_L,
                        onClick = { onGlucoseUnitSelected(GlucoseUnit.MMOL_PER_L) },
                        label = { Text(stringResource(R.string.log_unit_mmol)) }
                    )
                    FilterChip(
                        selected = units.glucose == GlucoseUnit.MG_PER_DL,
                        onClick = { onGlucoseUnitSelected(GlucoseUnit.MG_PER_DL) },
                        label = { Text(stringResource(R.string.log_unit_mgdl)) }
                    )
                }

                OutlinedTextField(
                    value = state.glucoseInput,
                    onValueChange = { viewModel.onGlucoseChanged(it) },
                    label = { Text(stringResource(R.string.log_glucose_label, units.glucoseSymbol)) },
                    placeholder = {
                        Text(
                            stringResource(
                                if (units.glucose == GlucoseUnit.MG_PER_DL) R.string.log_glucose_placeholder_mgdl else R.string.log_glucose_placeholder_mmol
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.previewGlucoseCategory != null) {
                    CategoryBadge(
                        label = state.previewGlucoseCategory!!.label(state.glucoseContext),
                        color = getGlucoseColor(state.previewGlucoseCategory!!)
                    )
                }
            }

            MetricType.ACTIVITY -> {
                OutlinedTextField(
                    value = state.activityDurationInput,
                    onValueChange = { viewModel.onActivityDurationChanged(it) },
                    label = { Text(stringResource(R.string.log_duration_label)) },
                    placeholder = { Text(stringResource(R.string.log_duration_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.activityDistanceInput,
                    onValueChange = { viewModel.onActivityDistanceChanged(it) },
                    label = { Text(stringResource(R.string.log_distance_label, units.distanceSymbol)) },
                    placeholder = {
                        Text(stringResource(if (units.isImperial) R.string.log_distance_placeholder_imperial else R.string.log_distance_placeholder))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            MetricType.WAIST_CIRCUMFERENCE -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.log_waist_label) + ": ${state.waistInput} cm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalRulerPicker(
                        value = state.waistValue,
                        onValueChange = { viewModel.onWaistValueChanged(it) },
                        range = 40.0..200.0,
                        step = 0.5,
                        unitLabel = "cm"
                    )
                }

                if (state.previewWaistCategory != null) {
                    CategoryBadge(
                        label = stringResource(
                            R.string.log_waist_badge,
                            state.waistInput,
                            state.previewWaistCategory!!.label(state.activeProfile?.sex)
                        ),
                        color = getWaistColor(state.previewWaistCategory!!)
                    )
                }
            }
        }

        if (state.selectedMetric != MetricType.ACTIVITY) {
            RangeSourceNote()
        }

        Button(
            onClick = { viewModel.saveCurrentMetric() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving && state.activeProfile != null
        ) {
            Text(stringResource(if (state.isSaving) R.string.log_saving else R.string.log_record_entry))
        }
    }
}

@Composable
fun CategoryBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
