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
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Factor for converting pounds to kilograms (1 lb = 0.45359237 kg). */
private val LBS_TO_KG = BigDecimal("0.45359237")

/** Physiological weight bounds used for Libra row validation. */
private const val WEIGHT_MIN_KG = 1.0
private const val WEIGHT_MAX_KG = 700.0

/**
 * Returns true if the given CSV content looks like a Libra export.
 * Libra files start with metadata comment lines (#Version:, #Units:) or a
 * semicolon-delimited header beginning with '#date' or 'date'.
 */
private fun isLibraFormat(lines: List<String>): Boolean {
    return lines.any { line ->
        line.startsWith("#Version:") ||
            line.startsWith("#Units:") ||
            line.startsWith("#date;") ||
            line.startsWith("date;")
    }
}

class CsvDataImportAdapter(
    private val healthLogRepository: HealthLogRepositoryPort,
    private val profileRepository: ProfileRepositoryPort
) : DataImportPort {

    override suspend fun importCsv(profileId: ProfileId, metricType: String, csvContent: String): ImportResult {
        if (csvContent.isBlank()) {
            return ImportResult(0, emptyList())
        }

        // Keep the physical line number of every non-blank line, so skipped rows point at the real file line.
        val numbered = csvContent.lines().mapIndexed { i, l -> (i + 1) to l.trim() }.filter { it.second.isNotEmpty() }
        val allLines = numbered.map { it.second }
        val lineNumbers = numbered.map { it.first }
        if (allLines.isEmpty()) {
            return ImportResult(0, emptyList())
        }

        // Auto-detect Libra format when metricType is weight or explicitly libra
        val normalizedType = metricType.lowercase().replace("-", "_")
        val isLibra = normalizedType == "libra" ||
            normalizedType == "libra_weight" ||
            (normalizedType == "weight" && isLibraFormat(allLines))

        if (isLibra) {
            return importLibraCsv(profileId, allLines, lineNumbers)
        }

        // Standard comma-separated import path
        val lines = allLines
        if (lines.size == 1) {
            return ImportResult(0, emptyList())
        }

        val profile = profileRepository.getById(profileId)
        val dataLines = lines.drop(1)
        val skippedRows = mutableListOf<SkippedRow>()
        var importedCount = 0

        for ((index, line) in dataLines.withIndex()) {
            val lineNumber = lineNumbers[index + 1] // physical line; the header is the first non-blank line
            val parts = line.split(",").map { it.trim() }

            try {
                when (normalizedType) {
                    "weight" -> {
                        if (parts.size < 2) {
                            skippedRows.add(SkippedRow(lineNumber, "Expected at least 2 columns (timestamp, weight_kg), got ${parts.size}"))
                            continue
                        }
                        val timestamp = parseInstant(parts[0])
                        val weightKg = WeightKg(BigDecimal(parts[1]))
                        // BMI is derived data: always recomputed from the profile height, the file's value is ignored.
                        val bmi = profile?.calculateBmi(weightKg)?.bmi
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
                        val category = NhgBloodPressureCategory.classify(reading)
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
                        val category = NhgGlucoseCategory.classify(glucoseLevel, context)
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

    /**
     * Imports weight measurements from a Libra CSV export.
     *
     * Libra export format:
     * ```
     * #Version: 6
     * #Units: kg
     * #date;weight;weight trend;body fat;body fat trend;muscle mass;body water;bone mass;comments
     * 2026-09-20T08:00:00.000+02:00;74.5;;;;;;;
     * ```
     */
    private suspend fun importLibraCsv(profileId: ProfileId, allLines: List<String>, lineNumbers: List<Int>): ImportResult {
        val profile = profileRepository.getById(profileId)
        val skippedRows = mutableListOf<SkippedRow>()
        var importedCount = 0

        // Extract unit from metadata; default to kg if absent
        // Only kg and lbs are known; any other unit skips the data rows instead of guessing a conversion.
        var useKg = true
        var unsupportedUnit: String? = null
        for (line in allLines) {
            if (line.startsWith("#Units:")) {
                val unit = line.removePrefix("#Units:").trim().lowercase()
                when (unit) {
                    "kg" -> useKg = true
                    "lbs" -> useKg = false
                    else -> unsupportedUnit = line.removePrefix("#Units:").trim()
                }
                break
            }
        }

        // Find the column header line (starts with '#date;' or 'date;') and all data lines after it
        var headerFound = false
        var currentLineNumber: Int

        for ((lineIndex, line) in allLines.withIndex()) {
            currentLineNumber = lineNumbers[lineIndex]

            // Skip all comment/metadata lines
            if (line.startsWith("#")) {
                if (line.startsWith("#date;") || line.startsWith("#date,")) {
                    headerFound = true
                }
                continue
            }

            // Non-comment, non-header line: treat as potential data
            if (!headerFound) {
                // Could be undecorated 'date;...' header
                if (line.startsWith("date;") || line.startsWith("date,")) {
                    headerFound = true
                    continue
                }
            }

            // Data row
            if (unsupportedUnit != null) {
                skippedRows.add(SkippedRow(currentLineNumber, "Unsupported Libra unit: $unsupportedUnit (expected kg or lbs)"))
                continue
            }
            val parts = line.split(";").map { it.trim().removeSurrounding("\"") }

            if (parts.size < 2) {
                skippedRows.add(SkippedRow(currentLineNumber, "Expected at least 2 semicolon-separated columns (date, weight), got ${parts.size}"))
                continue
            }

            try {
                val timestamp = parseLibraInstant(parts[0])

                // Normalize decimal: replace comma with period, strip surrounding quotes
                val weightRaw = parts[1].replace(',', '.')
                if (weightRaw.isBlank()) {
                    skippedRows.add(SkippedRow(currentLineNumber, "Weight value is blank"))
                    continue
                }
                val weightValue = BigDecimal(weightRaw)
                val weightKg: BigDecimal = if (useKg) {
                    weightValue
                } else {
                    weightValue.multiply(LBS_TO_KG).setScale(2, RoundingMode.HALF_UP)
                }

                // Physiological range check
                val weightDouble = weightKg.toDouble()
                if (weightDouble < WEIGHT_MIN_KG || weightDouble > WEIGHT_MAX_KG) {
                    skippedRows.add(SkippedRow(currentLineNumber, "Weight $weightKg kg is outside physiological range [$WEIGHT_MIN_KG, $WEIGHT_MAX_KG] kg"))
                    continue
                }

                val weight = WeightKg(weightKg)
                val bmi = profile?.calculateBmi(weight)?.bmi

                val entry = WeightEntry(
                    id = MeasurementId.generate(),
                    profileId = profileId,
                    timestamp = timestamp,
                    weight = weight,
                    bmi = bmi
                )
                healthLogRepository.saveWeight(entry)
                importedCount++
            } catch (e: Exception) {
                skippedRows.add(SkippedRow(currentLineNumber, e.message ?: "Invalid data format"))
            }
        }

        return ImportResult(importedCount = importedCount, skippedRows = skippedRows)
    }

    /**
     * Parses a standard ISO-8601 timestamp from a canonical Health Journal CSV column.
     * Falls back to epoch-millis for legacy numeric strings.
     */
    private fun parseInstant(str: String): Instant {
        return try {
            Instant.parse(str)
        } catch (e: Exception) {
            Instant.ofEpochMilli(str.toLong())
        }
    }

    /**
     * Parses a Libra timestamp, which can be:
     * - Full ISO-8601 with offset: `2026-09-20T08:00:00.000+02:00`
     * - UTC instant: `2026-09-20T08:00:00.000Z`
     * - Local date-time without offset: `2026-09-20T08:00:00`
     * - Date only: `2026-09-20`
     */
    private fun parseLibraInstant(str: String): Instant {
        return try {
            Instant.parse(str)
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(str).toInstant(ZoneOffset.UTC)
            } catch (_: Exception) {
                LocalDate.parse(str).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }
    }
}
