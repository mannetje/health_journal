package nl.healthjournal.app.ui.history

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.history.charts.BloodPressureTrendSection
import nl.healthjournal.app.ui.history.charts.DateRangeSelector
import nl.healthjournal.app.ui.history.charts.GlucoseTrendSection
import nl.healthjournal.app.ui.history.charts.WeightTrendSection
import nl.healthjournal.app.ui.history.charts.filterByDateRange
import nl.healthjournal.app.ui.nhg.label
import nl.healthjournal.app.ui.theme.infoContainerColor
import nl.healthjournal.app.ui.theme.onInfoContainerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importMetricType by remember { mutableStateOf("weight") }
    var importCsvText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val pickFileError = stringResource(R.string.history_pick_file_error)
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.getOrNull()
            if (text != null) importCsvText = text else Toast.makeText(context, pickFileError, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showImportDialog = true }) {
                    Text(stringResource(R.string.history_import_button))
                }
                Button(onClick = { showExportDialog = true }) {
                    Text(stringResource(R.string.history_export_button))
                }
            }
        }

        state.infoMessage?.let { info ->
            Card(colors = CardDefaults.cardColors(containerColor = infoContainerColor)) {
                Text(
                    text = info,
                    color = onInfoContainerColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        state.errorMessage?.let { error ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedFilter == HistoryFilter.ALL,
                onClick = { viewModel.setFilter(HistoryFilter.ALL) },
                label = { Text(stringResource(R.string.history_filter_all)) }
            )
            FilterChip(
                selected = state.selectedFilter == HistoryFilter.WEIGHT,
                onClick = { viewModel.setFilter(HistoryFilter.WEIGHT) },
                label = { Text(stringResource(R.string.history_filter_weight)) }
            )
            FilterChip(
                selected = state.selectedFilter == HistoryFilter.BLOOD_PRESSURE,
                onClick = { viewModel.setFilter(HistoryFilter.BLOOD_PRESSURE) },
                label = { Text(stringResource(R.string.history_filter_bp)) }
            )
            FilterChip(
                selected = state.selectedFilter == HistoryFilter.GLUCOSE,
                onClick = { viewModel.setFilter(HistoryFilter.GLUCOSE) },
                label = { Text(stringResource(R.string.history_filter_glucose)) }
            )
            FilterChip(
                selected = state.selectedFilter == HistoryFilter.ACTIVITY,
                onClick = { viewModel.setFilter(HistoryFilter.ACTIVITY) },
                label = { Text(stringResource(R.string.history_filter_activity)) }
            )
        }

        // Under a single-metric filter the entries follow the selected date range, like the chart;
        // under "All" the full list is shown.
        val ranged = state.selectedFilter != HistoryFilter.ALL
        val weights = if (ranged) state.weights.filterByDateRange(state.selectedDateRange) { it.timestamp } else state.weights
        val bloodPressures = if (ranged) state.bloodPressures.filterByDateRange(state.selectedDateRange) { it.timestamp } else state.bloodPressures
        val glucoses = if (ranged) state.glucoses.filterByDateRange(state.selectedDateRange) { it.timestamp } else state.glucoses

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Trend chart scrolls together with the entries below it, so the entries stay reachable.
                if (state.selectedFilter != HistoryFilter.ALL && state.selectedFilter != HistoryFilter.ACTIVITY) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateRangeSelector(
                                selected = state.selectedDateRange,
                                onSelect = { viewModel.setDateRange(it) }
                            )
                            when (state.selectedFilter) {
                                HistoryFilter.WEIGHT -> WeightTrendSection(entries = weights)
                                HistoryFilter.BLOOD_PRESSURE -> BloodPressureTrendSection(entries = bloodPressures)
                                HistoryFilter.GLUCOSE -> GlucoseTrendSection(entries = glucoses)
                                else -> Unit
                            }
                        }
                    }
                }

                // One chronological list (newest first) so no metric hides below a long run of another.
                val filter = state.selectedFilter
                val rows = buildList<Pair<java.time.Instant, @Composable () -> Unit>> {
                    if (filter == HistoryFilter.ALL || filter == HistoryFilter.WEIGHT) {
                        weights.forEach { w ->
                            add(w.timestamp to {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.history_weight_line, w.weight.value.toString()), style = MaterialTheme.typography.titleMedium)
                                        w.bmi?.let { Text(stringResource(R.string.history_bmi_line, it.toString())) }
                                        Text(stringResource(R.string.history_time_line, w.timestamp.toString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            })
                        }
                    }
                    if (filter == HistoryFilter.ALL || filter == HistoryFilter.BLOOD_PRESSURE) {
                        bloodPressures.forEach { bp ->
                            add(bp.timestamp to {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.history_bp_line, bp.reading.systolic.toString(), bp.reading.diastolic.toString()), style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(R.string.history_category_line, bp.category.label()))
                                        Text(stringResource(R.string.history_time_line, bp.timestamp.toString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            })
                        }
                    }
                    if (filter == HistoryFilter.ALL || filter == HistoryFilter.GLUCOSE) {
                        glucoses.forEach { g ->
                            add(g.timestamp to {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.history_glucose_line, g.glucose.valueInMmolL.toString(), g.context.label()), style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(R.string.history_category_line, g.category.label()))
                                        Text(stringResource(R.string.history_time_line, g.timestamp.toString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            })
                        }
                    }
                    if (filter == HistoryFilter.ALL || filter == HistoryFilter.ACTIVITY) {
                        state.activities.forEach { a ->
                            add(a.startTime to {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.history_activity_line, a.distanceInMeters.toString(), a.durationInSeconds.toString()), style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(R.string.history_start_line, a.startTime.toString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            })
                        }
                    }
                }.sortedByDescending { it.first }
                items(rows) { row -> row.second() }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.history_export_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.exportCsv("weight") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.history_export_weight))
                    }
                    Button(onClick = { viewModel.exportCsv("blood_pressure") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.history_export_bp))
                    }
                    Button(onClick = { viewModel.exportCsv("glucose") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.history_export_glucose))
                    }
                    state.exportedCsvContent?.let { content ->
                        HorizontalDivider()
                        Text(stringResource(R.string.history_preview_label), style = MaterialTheme.typography.labelMedium)
                        Text(content, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.history_import_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = importMetricType == "weight",
                            onClick = { importMetricType = "weight" },
                            label = { Text(stringResource(R.string.history_import_type_weight)) }
                        )
                        FilterChip(
                            selected = importMetricType == "blood_pressure",
                            onClick = { importMetricType = "blood_pressure" },
                            label = { Text(stringResource(R.string.history_import_type_bp)) }
                        )
                        FilterChip(
                            selected = importMetricType == "glucose",
                            onClick = { importMetricType = "glucose" },
                            label = { Text(stringResource(R.string.history_import_type_glucose)) }
                        )
                        FilterChip(
                            selected = importMetricType == "libra",
                            onClick = { importMetricType = "libra" },
                            label = { Text(stringResource(R.string.history_import_type_libra)) }
                        )
                    }
                    OutlinedButton(onClick = { filePicker.launch(arrayOf("text/*", "application/octet-stream")) }) {
                        Text(stringResource(R.string.history_pick_file_button))
                    }
                    OutlinedTextField(
                        value = importCsvText,
                        onValueChange = { importCsvText = it },
                        label = { Text(stringResource(R.string.history_paste_csv_label)) },
                        placeholder = {
                            if (importMetricType == "libra") {
                                Text(stringResource(R.string.history_libra_placeholder))
                            } else {
                                Text(stringResource(R.string.history_csv_placeholder))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importCsv(importMetricType, importCsvText)
                    showImportDialog = false
                    importCsvText = ""
                }) {
                    Text(stringResource(R.string.common_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
