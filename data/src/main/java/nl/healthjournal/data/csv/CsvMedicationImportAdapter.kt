package nl.healthjournal.data.csv

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.Dosage
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeId
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationForm
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.MedicationName
import nl.healthjournal.domain.model.medication.PillAppearance
import nl.healthjournal.domain.model.medication.PillColor
import nl.healthjournal.domain.model.medication.PillShape
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.ScheduleVersion
import nl.healthjournal.domain.model.medication.Strength
import nl.healthjournal.domain.model.medication.StrengthUnit
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.port.secondary.ImportResult
import nl.healthjournal.domain.port.secondary.MedicationCsvFiles
import nl.healthjournal.domain.port.secondary.MedicationImportPort
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.port.secondary.SkippedRow
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Imports the three medication CSV files into a profile. Rows link through `ref`; new identifiers are assigned.
 * A medication that matches one already in the profile (name, form, strength, dose) is not duplicated, and its
 * schedule rows are ignored, so importing the same files twice adds nothing. Invalid rows are skipped with the
 * physical line number of their file and a reason; nothing is guessed.
 */
class CsvMedicationImportAdapter(
    private val medicationRepository: MedicationRepositoryPort,
    private val profileRepository: ProfileRepositoryPort
) : MedicationImportPort {

    private class Row(val line: Int, val cells: Map<String, String>) {
        operator fun get(column: String): String = cells[column].orEmpty()
    }

    private class DraftMedication(
        val ref: String,
        val line: Int,
        val name: MedicationName,
        val form: MedicationForm?,
        val dosage: Dosage,
        val appearance: PillAppearance?,
        val comment: EntryComment?,
        val archivedFrom: LocalDate?,
        val asNeeded: Boolean
    )

    private class ScheduleRow(
        val line: Int,
        val time: LocalTime?,
        val days: DayPattern?,
        val start: LocalDate?,
        val end: LocalDate?
    )

    override suspend fun importMedicationCsv(profileId: ProfileId, files: MedicationCsvFiles): ImportResult {
        if (profileRepository.getById(profileId) == null) return ImportResult(0, emptyList())

        val skipped = mutableListOf<SkippedRow>()
        fun skip(label: String, line: Int, reason: String?) {
            skipped += SkippedRow(line, "$label: ${reason ?: "Invalid data format"}")
        }

        // 1. Medications
        val drafts = linkedMapOf<String, DraftMedication>()
        for (row in readTable(files.medications, "medications", MEDICATIONS_HEADER, skipped)) {
            try {
                val draft = parseMedication(row)
                if (draft.ref in drafts) throw IllegalArgumentException("Duplicate ref ${draft.ref}")
                drafts[draft.ref] = draft
            } catch (e: Exception) {
                skip("medications", row.line, e.message)
            }
        }

        // 2. Schedule versions, grouped by medication and effective-from date
        val groups = linkedMapOf<Pair<String, LocalDate>, MutableList<ScheduleRow>>()
        for (row in readTable(files.schedules, "schedule", SCHEDULES_HEADER, skipped)) {
            try {
                val ref = row["ref"]
                if (ref !in drafts) throw IllegalArgumentException("Unknown ref $ref")
                val from = LocalDate.parse(row["effective_from"])
                groups.getOrPut(ref to from) { mutableListOf() } += parseScheduleRow(row)
            } catch (e: Exception) {
                skip("schedule", row.line, e.message)
            }
        }
        val versionsByRef = mutableMapOf<String, MutableList<ScheduleVersion>>()
        for ((key, rows) in groups) {
            try {
                versionsByRef.getOrPut(key.first) { mutableListOf() } += ScheduleVersion(key.second, buildSchedule(rows, ::skip))
            } catch (e: Exception) {
                skip("schedule", rows.first().line, e.message)
            }
        }

        // 3. Create new medications, reuse matching ones
        val existing = medicationRepository.getMedications(profileId, includeArchived = true)
        val byKey = existing.associateBy { matchKey(it.name, it.form, it.dosage) }.toMutableMap()
        val idByRef = mutableMapOf<String, MedicationId>()
        var imported = 0
        for (draft in drafts.values) {
            val match = byKey[matchKey(draft.name, draft.form, draft.dosage)]
            if (match != null) {
                idByRef[draft.ref] = match.id
                continue
            }
            val versions = versionsByRef[draft.ref].orEmpty().sortedBy { it.effectiveFrom }.ifEmpty {
                if (draft.asNeeded) listOf(ScheduleVersion(LocalDate.now(), Schedule.AsNeeded)) else emptyList()
            }
            if (versions.isEmpty()) {
                skip("medications", draft.line, "No valid schedule")
                continue
            }
            val medication = Medication(
                id = MedicationId.generate(),
                profileId = profileId,
                name = draft.name,
                form = draft.form,
                dosage = draft.dosage,
                appearance = draft.appearance,
                schedules = versions,
                comment = draft.comment,
                archivedFrom = draft.archivedFrom
            )
            medicationRepository.saveMedication(medication)
            byKey[matchKey(medication.name, medication.form, medication.dosage)] = medication
            idByRef[draft.ref] = medication.id
            imported++
        }

        // 4. Intakes, skipping duplicates of recorded outcomes
        val known = medicationRepository.getAllIntakes(profileId)
        val plannedKeys = known.mapNotNull { i -> i.planned?.let { i.medicationId to it } }.toMutableSet()
        val asNeededKeys = known.filter { it.planned == null }.mapNotNull { i -> i.takenAt?.let { i.medicationId to it } }.toMutableSet()
        for (row in readTable(files.intakes, "intakes", INTAKES_HEADER, skipped)) {
            try {
                val medicationId = idByRef[row["ref"]] ?: throw IllegalArgumentException("Unknown ref ${row["ref"]}")
                val intake = parseIntake(row, medicationId)
                val isNew = if (intake.planned != null) {
                    plannedKeys.add(medicationId to intake.planned!!)
                } else {
                    asNeededKeys.add(medicationId to intake.takenAt!!)
                }
                // Already recorded: importing the same files again adds nothing and is not an error.
                if (!isNew) continue
                medicationRepository.saveIntake(intake)
                imported++
            } catch (e: Exception) {
                skip("intakes", row.line, e.message)
            }
        }

        return ImportResult(importedCount = imported, skippedRows = skipped)
    }

    /** Header-driven rows with the physical line number; a missing required column skips the whole file. */
    private fun readTable(text: String, label: String, header: String, skipped: MutableList<SkippedRow>): List<Row> {
        val lines = text.lines().mapIndexed { i, l -> (i + 1) to l.trim() }.filter { it.second.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        val columns = CsvQuoting.split(lines.first().second).map { it.lowercase() }
        val missing = header.split(",").filter { it !in columns && it != "comment" }
        if (missing.isNotEmpty()) {
            skipped += SkippedRow(lines.first().first, "$label: Missing column ${missing.joinToString()}")
            return emptyList()
        }
        return lines.drop(1).map { (number, line) ->
            val cells = CsvQuoting.split(line)
            Row(number, columns.withIndex().associate { (i, c) -> c to cells.getOrElse(i) { "" } })
        }
    }

    private fun parseMedication(row: Row): DraftMedication {
        val ref = row["ref"].also { require(it.isNotBlank()) { "Missing ref" } }
        val strengthAmount = row["strength_amount"]
        val strengthUnit = row["strength_unit"]
        require(strengthAmount.isBlank() == strengthUnit.isBlank()) { "Strength needs both an amount and a unit" }
        val strength = if (strengthAmount.isBlank()) null else Strength(BigDecimal(strengthAmount), enumOf<StrengthUnit>(strengthUnit, "strength unit"))
        val colour = row["colour"]
        val shape = row["shape"]
        require(colour.isBlank() == shape.isBlank()) { "Appearance needs both a colour and a shape" }
        return DraftMedication(
            ref = ref,
            line = row.line,
            name = MedicationName.of(row["name"]),
            form = row["form"].takeIf { it.isNotBlank() }?.let { enumOf<MedicationForm>(it, "form") },
            dosage = Dosage(strength, BigDecimal(row["dose_amount"]), enumOf<DoseUnit>(row["dose_unit"], "dose unit")),
            appearance = if (colour.isBlank()) null else PillAppearance(enumOf<PillColor>(colour, "colour"), enumOf<PillShape>(shape, "shape")),
            comment = parseComment(row["comment"]),
            archivedFrom = row["archived"].takeIf { it.isNotBlank() }?.let(LocalDate::parse),
            asNeeded = row["as_needed"].equals("true", ignoreCase = true)
        )
    }

    private fun parseScheduleRow(row: Row): ScheduleRow {
        val time = row["local_time"].takeIf { it.isNotBlank() }?.let(LocalTime::parse) ?: return ScheduleRow(row.line, null, null, null, null)
        val days = row["days_of_week"]
        val interval = row["interval_days"]
        val pattern = when {
            days.isNotBlank() -> DayPattern.Weekdays(days.split("|").map { code ->
                DayOfWeek.values().firstOrNull { it.name.take(3) == code.trim().uppercase() }
                    ?: throw IllegalArgumentException("Unknown weekday $code")
            }.toSet())
            interval.isNotBlank() -> DayPattern.EveryNDays(interval.toInt())
            else -> throw IllegalArgumentException("Weekdays or an interval is required")
        }
        return ScheduleRow(
            line = row.line,
            time = time,
            days = pattern,
            start = LocalDate.parse(row["start_date"]),
            end = row["end_date"].takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        )
    }

    private fun buildSchedule(rows: List<ScheduleRow>, skip: (String, Int, String?) -> Unit): Schedule {
        val timed = rows.filter { it.time != null }
        if (timed.isEmpty()) return Schedule.AsNeeded
        rows.filter { it.time == null }.forEach { skip("schedule", it.line, "A version cannot mix as needed and daily times") }
        val first = timed.first()
        val consistent = timed.filter { it.days == first.days && it.start == first.start && it.end == first.end }
        (timed - consistent.toSet()).forEach { skip("schedule", it.line, "Rows of one version must share days, start and end") }
        return Schedule.Recurring.of(consistent.map { it.time!! }, first.days!!, first.start!!, first.end)
    }

    private fun parseIntake(row: Row, medicationId: MedicationId): Intake {
        val plannedText = row["planned_local_datetime"]
        val takenAt = row["actual_timestamp"].takeIf { it.isNotBlank() }?.let(Instant::parse)
        val amount = row["actual_amount"].takeIf { it.isNotBlank() }?.let { BigDecimal(it) }
        val comment = parseComment(row["comment"])
        return when (val status = row["status"].uppercase()) {
            "AS_NEEDED" -> {
                require(plannedText.isBlank()) { "An as-needed dose has no planned time" }
                Intake(IntakeId.generate(), medicationId, null, IntakeStatus.TAKEN, takenAt, amount, comment)
            }
            "TAKEN", "SKIPPED" -> {
                require(plannedText.isNotBlank()) { "A planned intake needs a planned time" }
                Intake(IntakeId.generate(), medicationId, LocalDateTime.parse(plannedText), IntakeStatus.valueOf(status), takenAt, amount, comment)
            }
            else -> throw IllegalArgumentException("Unknown status $status")
        }
    }

    private fun parseComment(raw: String): EntryComment? {
        val text = EntryComment.normalize(raw)
        require(text.length <= EntryComment.MAX_LENGTH) { "Comment longer than ${EntryComment.MAX_LENGTH} characters" }
        return EntryComment.ofOrNull(text)
    }

    private inline fun <reified E : Enum<E>> enumOf(value: String, what: String): E =
        enumValues<E>().firstOrNull { it.name == value.uppercase() }
            ?: throw IllegalArgumentException("Unknown $what: $value")

    private fun matchKey(name: MedicationName, form: MedicationForm?, dosage: Dosage): String =
        listOf(
            name.value.lowercase(),
            form?.name,
            dosage.strength?.amount?.stripTrailingZeros()?.toPlainString(),
            dosage.strength?.unit?.name,
            dosage.amountPerIntake.stripTrailingZeros().toPlainString(),
            dosage.doseUnit.name
        ).joinToString("|")
}
