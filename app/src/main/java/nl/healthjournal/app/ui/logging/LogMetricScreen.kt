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
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgBmiCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.app.ui.nhg.label
import nl.healthjournal.app.ui.theme.NhgNormalGreen
import nl.healthjournal.app.ui.theme.NhgNormalGreenDark
import nl.healthjournal.app.ui.theme.NhgOptimalGreen
import nl.healthjournal.app.ui.theme.NhgOptimalGreenDark
import nl.healthjournal.app.ui.theme.NhgOrange
import nl.healthjournal.app.ui.theme.NhgOrangeDark
import nl.healthjournal.app.ui.theme.NhgDeepOrange
import nl.healthjournal.app.ui.theme.NhgDeepOrangeDark
import nl.healthjournal.app.ui.theme.NhgRed
import nl.healthjournal.app.ui.theme.NhgRedDark
import nl.healthjournal.app.ui.theme.NhgSevereRed
import nl.healthjournal.app.ui.theme.NhgSevereRedDark
import nl.healthjournal.app.ui.theme.NhgWarningYellow
import nl.healthjournal.app.ui.theme.NhgWarningYellowDark
import nl.healthjournal.app.ui.theme.onSuccessContainerColor
import nl.healthjournal.app.ui.theme.successContainerColor
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMetricScreen(
    viewModel: LoggingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

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
                    text = error,
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
                    text = success,
                    color = onSuccessContainerColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Metric Selector Tabs
        TabRow(selectedTabIndex = state.selectedMetric.ordinal) {
            Tab(
                selected = state.selectedMetric == MetricType.WEIGHT,
                onClick = { viewModel.selectMetric(MetricType.WEIGHT) },
                text = { Text(stringResource(R.string.log_tab_weight)) }
            )
            Tab(
                selected = state.selectedMetric == MetricType.BLOOD_PRESSURE,
                onClick = { viewModel.selectMetric(MetricType.BLOOD_PRESSURE) },
                text = { Text(stringResource(R.string.log_tab_blood_pressure)) }
            )
            Tab(
                selected = state.selectedMetric == MetricType.GLUCOSE,
                onClick = { viewModel.selectMetric(MetricType.GLUCOSE) },
                text = { Text(stringResource(R.string.log_tab_glucose)) }
            )
            Tab(
                selected = state.selectedMetric == MetricType.ACTIVITY,
                onClick = { viewModel.selectMetric(MetricType.ACTIVITY) },
                text = { Text(stringResource(R.string.log_tab_activity)) }
            )
        }

        when (state.selectedMetric) {
            MetricType.WEIGHT -> {
                OutlinedTextField(
                    value = state.weightInput,
                    onValueChange = { viewModel.onWeightChanged(it) },
                    label = { Text(stringResource(R.string.log_weight_label)) },
                    placeholder = { Text(stringResource(R.string.log_weight_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

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
            }

            MetricType.BLOOD_PRESSURE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.systolicInput,
                        onValueChange = { viewModel.onSystolicChanged(it) },
                        label = { Text(stringResource(R.string.log_systolic_label)) },
                        placeholder = { Text(stringResource(R.string.log_systolic_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.diastolicInput,
                        onValueChange = { viewModel.onDiastolicChanged(it) },
                        label = { Text(stringResource(R.string.log_diastolic_label)) },
                        placeholder = { Text(stringResource(R.string.log_diastolic_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (state.previewBpCategory != null) {
                    CategoryBadge(
                        label = stringResource(R.string.log_nhg_badge, state.previewBpCategory!!.label()),
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
                        selected = !state.isGlucoseMgDl,
                        onClick = { viewModel.toggleGlucoseUnit(false) },
                        label = { Text(stringResource(R.string.log_unit_mmol)) }
                    )
                    FilterChip(
                        selected = state.isGlucoseMgDl,
                        onClick = { viewModel.toggleGlucoseUnit(true) },
                        label = { Text(stringResource(R.string.log_unit_mgdl)) }
                    )
                }

                OutlinedTextField(
                    value = state.glucoseInput,
                    onValueChange = { viewModel.onGlucoseChanged(it) },
                    label = {
                        Text(
                            stringResource(
                                if (state.isGlucoseMgDl) R.string.log_glucose_label_mgdl else R.string.log_glucose_label_mmol
                            )
                        )
                    },
                    placeholder = {
                        Text(
                            stringResource(
                                if (state.isGlucoseMgDl) R.string.log_glucose_placeholder_mgdl else R.string.log_glucose_placeholder_mmol
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.previewGlucoseCategory != null) {
                    CategoryBadge(
                        label = stringResource(R.string.log_nhg_badge, state.previewGlucoseCategory!!.label()),
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
                    label = { Text(stringResource(R.string.log_distance_label)) },
                    placeholder = { Text(stringResource(R.string.log_distance_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
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

@Composable
private fun getBmiColor(category: NhgBmiCategory): Color {
    val dark = isSystemInDarkTheme()
    return when (category) {
        NhgBmiCategory.NORMAL -> if (dark) NhgOptimalGreenDark else NhgOptimalGreen
        NhgBmiCategory.UNDERWEIGHT -> if (dark) NhgWarningYellowDark else NhgWarningYellow
        NhgBmiCategory.OVERWEIGHT -> if (dark) NhgOrangeDark else NhgOrange
        NhgBmiCategory.OBESE -> if (dark) NhgRedDark else NhgRed
    }
}

@Composable
private fun getBpColor(category: NhgBloodPressureCategory): Color {
    val dark = isSystemInDarkTheme()
    return when (category) {
        NhgBloodPressureCategory.OPTIMAL -> if (dark) NhgOptimalGreenDark else NhgOptimalGreen
        NhgBloodPressureCategory.NORMAL -> if (dark) NhgNormalGreenDark else NhgNormalGreen
        NhgBloodPressureCategory.HIGH_NORMAL -> if (dark) NhgWarningYellowDark else NhgWarningYellow
        NhgBloodPressureCategory.HYPERTENSION_GRADE_1 -> if (dark) NhgOrangeDark else NhgOrange
        NhgBloodPressureCategory.HYPERTENSION_GRADE_2 -> if (dark) NhgDeepOrangeDark else NhgDeepOrange
        NhgBloodPressureCategory.HYPERTENSION_GRADE_3 -> if (dark) NhgSevereRedDark else NhgSevereRed
    }
}

@Composable
private fun getGlucoseColor(category: NhgGlucoseCategory): Color {
    val dark = isSystemInDarkTheme()
    return when (category) {
        NhgGlucoseCategory.NORMAL -> if (dark) NhgOptimalGreenDark else NhgOptimalGreen
        NhgGlucoseCategory.IMPAIRED_FASTING, NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE -> if (dark) NhgOrangeDark else NhgOrange
        NhgGlucoseCategory.HYPOGLYCAEMIA, NhgGlucoseCategory.DIABETES_RANGE -> if (dark) NhgRedDark else NhgRed
    }
}
