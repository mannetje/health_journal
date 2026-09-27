package nl.healthjournal.data.csv

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.*
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ImportResult
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.port.secondary.SkippedRow
import java.math.BigDecimal
import java.time.Instant

class CsvDataImportAdapter(
    private val healthLogRepository: HealthLogRepositoryPort,
    private val profileRepository: ProfileRepositoryPort
) : DataImportPort {

    override suspend fun importCsv(profileId: ProfileId, metricType: String, csvContent: String): ImportResult {
        if (csvContent.isBlank()) {
            return ImportResult(0, emptyList())
        }

        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty() || lines.size == 1) {
            return ImportResult(0, emptyList())
        }

        val profile = profileRepository.getById(profileId)
        val dataLines = lines.drop(1)
        val skippedRows = mutableListOf<SkippedRow>()
        var importedCount = 0

        for ((index, line) in dataLines.withIndex()) {
            val lineNumber = index + 2 // 1-indexed, header is line 1
            val parts = line.split(",").map { it.trim() }

            try {
                when (metricType.lowercase().replace("-", "_")) {
                    "weight" -> {
                        if (parts.size < 2) {
                            skippedRows.add(SkippedRow(lineNumber, "Expected at least 2 columns (timestamp, weight_kg), got ${parts.size}"))
                            continue
                        }
                        val timestamp = parseInstant(parts[0])
                        val weightKg = WeightKg(BigDecimal(parts[1]))
                        val bmi = if (parts.size >= 3 && parts[2].isNotBlank()) {
                            BigDecimal(parts[2])
                        } else {
                            profile?.calculateBmi(weightKg)?.bmi
                        }
                        val entry = WeightEntry(
                            id = MeasurementId.generate(),
                            profileId = profileId,
                            timestamp = timestamp,
                            weight = weightKg,
                            bmi = bmi
                        )
                        healthLogRepository.saveWeight(entry)
                        importedCount++
                    }
                    "blood_pressure", "bloodpressure", "bp" -> {
                        if (parts.size < 3) {
                            skippedRows.add(SkippedRow(lineNumber, "Expected at least 3 columns (timestamp, systolic, diastolic), got ${parts.size}"))
                            continue
                        }
                        val timestamp = parseInstant(parts[0])
                        val systolic = parts[1].toInt()
                        val diastolic = parts[2].toInt()
                        val reading = BloodPressureReading(systolic, diastolic)
                        val category = if (parts.size >= 4 && parts[3].isNotBlank()) {
                            try {
                                NhgBloodPressureCategory.valueOf(parts[3].uppercase())
                            } catch (e: Exception) {
                                NhgBloodPressureCategory.classify(reading)
                            }
                        } else {
                            NhgBloodPressureCategory.classify(reading)
                        }
                        val entry = BloodPressureEntry(
                            id = MeasurementId.generate(),
                            profileId = profileId,
                            timestamp = timestamp,
                            reading = reading,
                            category = category
                        )
                        healthLogRepository.saveBloodPressure(entry)
                        importedCount++
                    }
                    "glucose" -> {
                        if (parts.size < 3) {
                            skippedRows.add(SkippedRow(lineNumber, "Expected at least 3 columns (timestamp, glucose_mmol_l, context), got ${parts.size}"))
                            continue
                        }
                        val timestamp = parseInstant(parts[0])
                        val glucoseLevel = GlucoseLevel(BigDecimal(parts[1]))
                        val context = GlucoseContext.valueOf(parts[2].uppercase())
                        val category = if (parts.size >= 4 && parts[3].isNotBlank()) {
                            try {
                                NhgGlucoseCategory.valueOf(parts[3].uppercase())
                            } catch (e: Exception) {
                                NhgGlucoseCategory.classify(glucoseLevel, context)
                            }
                        } else {
                            NhgGlucoseCategory.classify(glucoseLevel, context)
                        }
                        val entry = GlucoseEntry(
                            id = MeasurementId.generate(),
                            profileId = profileId,
                            timestamp = timestamp,
                            glucose = glucoseLevel,
                            context = context,
                            category = category
                        )
                        healthLogRepository.saveGlucose(entry)
                        importedCount++
                    }
                    "activity" -> {
                        if (parts.size < 3) {
                            skippedRows.add(SkippedRow(lineNumber, "Expected at least 3 columns (start_timestamp, end_timestamp, distance_m), got ${parts.size}"))
                            continue
                        }
                        val startTime = parseInstant(parts[0])
                        val endTime = parseInstant(parts[1])
                        val distanceMeters = parts[2].toDouble()
                        val session = ActivitySession(
                            id = MeasurementId.generate(),
                            profileId = profileId,
                            startTime = startTime,
                            endTime = endTime,
                            distanceInMeters = distanceMeters
                        )
                        healthLogRepository.saveActivity(session)
                        importedCount++
                    }
                    else -> {
                        skippedRows.add(SkippedRow(lineNumber, "Unsupported metric type: $metricType"))
                    }
                }
            } catch (e: Exception) {
                skippedRows.add(SkippedRow(lineNumber, e.message ?: "Invalid data format"))
            }
        }

        return ImportResult(importedCount = importedCount, skippedRows = skippedRows)
    }

    private fun parseInstant(str: String): Instant {
        return try {
            Instant.parse(str)
        } catch (e: Exception) {
            Instant.ofEpochMilli(str.toLong())
        }
    }
}
