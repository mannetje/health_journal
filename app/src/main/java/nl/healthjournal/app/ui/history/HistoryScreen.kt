package nl.healthjournal.app.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
                text = "Health History",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showImportDialog = true }) {
                    Text("Import")
                }
                Button(onClick = { showExportDialog = true }) {
                    Text("Export")
                }
            }
        }

        state.infoMessage?.let { info ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Text(
                    text = info,
                    color = Color(0xFF1565C0),
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedFilter == HistoryFilter.ALL,
                onClick = { viewModel.setFilter(HistoryFilter.ALL) },
                label = { Text("All") }
            )
            FilterChip(
                selected = state.selectedFilter == HistoryFilter.WEIGHT,
                onClick = { viewModel.setFilter(HistoryFilter.WEIGHT) },
                label = { Text("Weight") }
            )
            FilterChip(
                selected = state.selectedFilter == HistoryFilter.BLOOD_PRESSURE,
                onClick = { viewModel.setFilter(HistoryFilter.BLOOD_PRESSURE) },
                label = { Text("BP") }
            )
            FilterChip(
                selected = state.selectedFilter == HistoryFilter.GLUCOSE,
                onClick = { viewModel.setFilter(HistoryFilter.GLUCOSE) },
                label = { Text("Glucose") }
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.selectedFilter == HistoryFilter.ALL || state.selectedFilter == HistoryFilter.WEIGHT) {
                    items(state.weights) { w ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Weight: ${w.weight.value} kg", style = MaterialTheme.typography.titleMedium)
                                w.bmi?.let { Text("BMI: $it") }
                                Text("Time: ${w.timestamp}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }

                if (state.selectedFilter == HistoryFilter.ALL || state.selectedFilter == HistoryFilter.BLOOD_PRESSURE) {
                    items(state.bloodPressures) { bp ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("BP: ${bp.reading.systolic}/${bp.reading.diastolic} mmHg", style = MaterialTheme.typography.titleMedium)
                                Text("Category: ${bp.category.name.replace('_', ' ')}")
                                Text("Time: ${bp.timestamp}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }

                if (state.selectedFilter == HistoryFilter.ALL || state.selectedFilter == HistoryFilter.GLUCOSE) {
                    items(state.glucoses) { g ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Glucose: ${g.glucose.valueInMmolL} mmol/L (${g.context.name})", style = MaterialTheme.typography.titleMedium)
                                Text("Category: ${g.category.name.replace('_', ' ')}")
                                Text("Time: ${g.timestamp}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }

                if (state.selectedFilter == HistoryFilter.ALL || state.selectedFilter == HistoryFilter.ACTIVITY) {
                    items(state.activities) { a ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Activity: ${a.distanceInMeters} m (${a.durationInSeconds} s)", style = MaterialTheme.typography.titleMedium)
                                Text("Start: ${a.startTime}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export CSV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.exportCsv("weight") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Export Weight CSV")
                    }
                    Button(onClick = { viewModel.exportCsv("blood_pressure") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Export Blood Pressure CSV")
                    }
                    Button(onClick = { viewModel.exportCsv("glucose") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Export Glucose CSV")
                    }
                    state.exportedCsvContent?.let { content ->
                        HorizontalDivider()
                        Text("Preview:", style = MaterialTheme.typography.labelMedium)
                        Text(content, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import CSV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = importMetricType == "weight",
                            onClick = { importMetricType = "weight" },
                            label = { Text("Weight") }
                        )
                        FilterChip(
                            selected = importMetricType == "blood_pressure",
                            onClick = { importMetricType = "blood_pressure" },
                            label = { Text("BP") }
                        )
                        FilterChip(
                            selected = importMetricType == "glucose",
                            onClick = { importMetricType = "glucose" },
                            label = { Text("Glucose") }
                        )
                        FilterChip(
                            selected = importMetricType == "libra",
                            onClick = { importMetricType = "libra" },
                            label = { Text("Libra (CSV)") }
                        )
                    }
                    OutlinedTextField(
                        value = importCsvText,
                        onValueChange = { importCsvText = it },
                        label = { Text("Paste CSV Content") },
                        placeholder = {
                            if (importMetricType == "libra") {
                                Text("#Version: 6\n#Units: kg\n#date;weight;...\n2026-09-20T08:00:00Z;74.5;...")
                            } else {
                                Text("timestamp,weight_kg,bmi\n...")
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
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
