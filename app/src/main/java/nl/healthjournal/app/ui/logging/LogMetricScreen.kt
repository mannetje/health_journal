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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgBmiCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory

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
            text = "Log Health Metric",
            style = MaterialTheme.typography.headlineSmall
        )

        if (state.activeProfile == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "No profile found. Please set up your profile first.",
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Text(
                    text = success,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Metric Selector Tabs
        TabRow(selectedTabIndex = state.selectedMetric.ordinal) {
            Tab(
                selected = state.selectedMetric == MetricType.WEIGHT,
                onClick = { viewModel.selectMetric(MetricType.WEIGHT) },
                text = { Text("Weight") }
            )
            Tab(
                selected = state.selectedMetric == MetricType.BLOOD_PRESSURE,
                onClick = { viewModel.selectMetric(MetricType.BLOOD_PRESSURE) },
                text = { Text("Blood Pressure") }
            )
            Tab(
                selected = state.selectedMetric == MetricType.GLUCOSE,
                onClick = { viewModel.selectMetric(MetricType.GLUCOSE) },
                text = { Text("Glucose") }
            )
        }

        when (state.selectedMetric) {
            MetricType.WEIGHT -> {
                OutlinedTextField(
                    value = state.weightInput,
                    onValueChange = { viewModel.onWeightChanged(it) },
                    label = { Text("Weight (kg)") },
                    placeholder = { Text("e.g. 74.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.previewBmi != null && state.previewBmiCategory != null) {
                    CategoryBadge(
                        label = "BMI: ${state.previewBmi} — ${state.previewBmiCategory?.name?.replace('_', ' ')}",
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
                        label = { Text("Systolic (mmHg)") },
                        placeholder = { Text("120") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.diastolicInput,
                        onValueChange = { viewModel.onDiastolicChanged(it) },
                        label = { Text("Diastolic (mmHg)") },
                        placeholder = { Text("80") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (state.previewBpCategory != null) {
                    CategoryBadge(
                        label = "NHG: ${state.previewBpCategory?.name?.replace('_', ' ')}",
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
                        label = { Text("Fasting (Nuchter)") }
                    )
                    FilterChip(
                        selected = state.glucoseContext == GlucoseContext.POSTPRANDIAL,
                        onClick = { viewModel.setGlucoseContext(GlucoseContext.POSTPRANDIAL) },
                        label = { Text("Postprandial (Na de maaltijd)") }
                    )
                }

                // Unit toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Unit:", style = MaterialTheme.typography.bodyMedium)
                    FilterChip(
                        selected = !state.isGlucoseMgDl,
                        onClick = { viewModel.toggleGlucoseUnit(false) },
                        label = { Text("mmol/L (NHG)") }
                    )
                    FilterChip(
                        selected = state.isGlucoseMgDl,
                        onClick = { viewModel.toggleGlucoseUnit(true) },
                        label = { Text("mg/dL") }
                    )
                }

                OutlinedTextField(
                    value = state.glucoseInput,
                    onValueChange = { viewModel.onGlucoseChanged(it) },
                    label = { Text(if (state.isGlucoseMgDl) "Glucose (mg/dL)" else "Glucose (mmol/L)") },
                    placeholder = { Text(if (state.isGlucoseMgDl) "e.g. 100" else "e.g. 5.4") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.previewGlucoseCategory != null) {
                    CategoryBadge(
                        label = "NHG: ${state.previewGlucoseCategory?.name?.replace('_', ' ')}",
                        color = getGlucoseColor(state.previewGlucoseCategory!!)
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.saveCurrentMetric() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving && state.activeProfile != null
        ) {
            Text(if (state.isSaving) "Saving..." else "Record Entry")
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

private fun getBmiColor(category: NhgBmiCategory): Color = when (category) {
    NhgBmiCategory.NORMAL -> Color(0xFF2E7D32)
    NhgBmiCategory.UNDERWEIGHT -> Color(0xFFF9A825)
    NhgBmiCategory.OVERWEIGHT -> Color(0xFFEF6C00)
    NhgBmiCategory.OBESE -> Color(0xFFC62828)
}

private fun getBpColor(category: NhgBloodPressureCategory): Color = when (category) {
    NhgBloodPressureCategory.OPTIMAL -> Color(0xFF2E7D32)
    NhgBloodPressureCategory.NORMAL -> Color(0xFF43A047)
    NhgBloodPressureCategory.HIGH_NORMAL -> Color(0xFFF9A825)
    NhgBloodPressureCategory.HYPERTENSION_GRADE_1 -> Color(0xFFEF6C00)
    NhgBloodPressureCategory.HYPERTENSION_GRADE_2 -> Color(0xFFD84315)
    NhgBloodPressureCategory.HYPERTENSION_GRADE_3 -> Color(0xFFB71C1C)
}

private fun getGlucoseColor(category: NhgGlucoseCategory): Color = when (category) {
    NhgGlucoseCategory.NORMAL -> Color(0xFF2E7D32)
    NhgGlucoseCategory.IMPAIRED_FASTING, NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE -> Color(0xFFEF6C00)
    NhgGlucoseCategory.HYPOGLYCAEMIA, NhgGlucoseCategory.DIABETES_RANGE -> Color(0xFFC62828)
}
