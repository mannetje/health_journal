package nl.healthjournal.data.csv

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.migraine.DailyActivities
import nl.healthjournal.domain.model.migraine.HeadArea
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineSymptom
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.model.migraine.MigraineTagKind
import nl.healthjournal.domain.model.migraine.MigraineTrigger
import nl.healthjournal.domain.model.migraine.MigraineType
import nl.healthjournal.domain.model.migraine.PainCharacter
import nl.healthjournal.domain.model.migraine.ReliefMethod
import nl.healthjournal.domain.port.secondary.ImportResult
import nl.healthjournal.domain.port.secondary.MigraineImportPort
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.port.secondary.SkippedRow
import nl.healthjournal.domain.usecase.AddMigraineTagUseCase
import nl.healthjournal.domain.usecase.AddTagResult
import java.time.Instant

/**
 * Imports the migraine file into a profile. Only `start_timestamp` is required; other columns may be missing or
 * empty. A row is validated completely before anything is written, so a rejected row leaves no entry and no tag.
 */
class CsvMigraineImportAdapter(
    private val repository: MigraineRepositoryPort,
    private val profileRepository: ProfileRepositoryPort
) : MigraineImportPort {

    private val addTag = AddMigraineTagUseCase(repository)

    override suspend fun importMigraineCsv(profileId: ProfileId, csvContent: String): ImportResult {
        if (profileRepository.getById(profileId) == null) return ImportResult(0, emptyList())
        val lines = csvContent.lines().mapIndexed { i, l -> (i + 1) to l.trim() }.filter { it.second.isNotEmpty() }
        if (lines.isEmpty()) return ImportResult(0, emptyList())

        val columns = CsvQuoting.split(lines.first().second).map { it.lowercase() }
        if ("start_timestamp" !in columns) {
            return ImportResult(0, listOf(SkippedRow(lines.first().first, "Missing column start_timestamp")))
        }

        val skipped = mutableListOf<SkippedRow>()
        var imported = 0
        for ((number, line) in lines.drop(1)) {
            val cells = CsvQuoting.split(line)
            val row = columns.withIndex().associate { (i, c) -> c to cells.getOrElse(i) { "" } }
            try {
                save(profileId, row)
                imported++
            } catch (e: Exception) {
                skipped += SkippedRow(number, e.message ?: "Invalid data format")
            }
        }
        return ImportResult(imported, skipped)
    }

    private suspend fun save(profileId: ProfileId, row: Map<String, String>) {
        fun cell(name: String) = row[name].orEmpty()
        val triggerLabels = labels(cell("custom_triggers"))
        val reliefLabels = labels(cell("custom_relief_methods"))

        fun entry(triggerIds: Set<MigraineTagId>, reliefIds: Set<MigraineTagId>) = MigraineEntry(
            id = MeasurementId.generate(),
            profileId = profileId,
            start = Instant.parse(cell("start_timestamp")),
            wokeUpWithIt = flag(cell("woke_up_with_it"), "woke_up_with_it"),
            end = cell("end_timestamp").takeIf { it.isNotBlank() }?.let(Instant::parse),
            intensity = cell("intensity").takeIf { it.isNotBlank() }?.toInt(),
            areas = codes<HeadArea>(cell("areas"), "area"),
            painCharacters = codes<PainCharacter>(cell("pain_character"), "pain character"),
            type = cell("type").takeIf { it.isNotBlank() }?.let { one<MigraineType>(it, "type") },
            symptoms = codes<MigraineSymptom>(cell("symptoms"), "symptom"),
            triggers = codes<MigraineTrigger>(cell("triggers"), "trigger"),
            customTriggerIds = triggerIds,
            tookSomething = flag(cell("took_something"), "took_something"),
            medicationText = MigraineEntry.normalizeMedicationText(cell("medication_text")),
            secondDose = flag(cell("second_dose"), "second_dose"),
            intensityAfter2Hours = cell("intensity_after_2h").takeIf { it.isNotBlank() }?.toInt(),
            medicationHelpedMe = flag(cell("medication_helped_me"), "medication_helped_me"),
            reliefMethods = codes<ReliefMethod>(cell("relief_methods"), "relief method"),
            customReliefIds = reliefIds,
            reliefHelpedMe = flag(cell("relief_helped_me"), "relief_helped_me"),
            activities = cell("activities").takeIf { it.isNotBlank() }?.let { one<DailyActivities>(it, "activities") },
            comment = comment(cell("comment"))
        )

        // Validate everything with throwaway ids first: no tag is created for a row that is then rejected.
        triggerLabels.forEach { MigraineTag(MigraineTagId.generate(), profileId, MigraineTagKind.TRIGGER, it) }
        reliefLabels.forEach { MigraineTag(MigraineTagId.generate(), profileId, MigraineTagKind.RELIEF, it) }
        entry(triggerLabels.map { MigraineTagId.generate() }.toSet(), reliefLabels.map { MigraineTagId.generate() }.toSet())

        val triggerIds = triggerLabels.map { resolve(profileId, MigraineTagKind.TRIGGER, it) }.toSet()
        val reliefIds = reliefLabels.map { resolve(profileId, MigraineTagKind.RELIEF, it) }.toSet()
        repository.saveMigraine(entry(triggerIds, reliefIds))
    }

    private suspend fun resolve(profileId: ProfileId, kind: MigraineTagKind, label: String): MigraineTagId =
        when (val result = addTag(profileId, kind, label)) {
            is AddTagResult.Added -> result.tag.id
            AddTagResult.LimitReached -> throw IllegalArgumentException("Too many personal tags (${MigraineTag.MAX_ACTIVE_PER_KIND})")
        }

    private fun labels(raw: String): List<String> =
        raw.split("|").map { MigraineTag.normalizeLabel(it) }.filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

    private fun flag(value: String, name: String): Boolean = when (value.lowercase()) {
        "", "false" -> false
        "true" -> true
        else -> throw IllegalArgumentException("$name must be true or false")
    }

    private fun comment(raw: String): EntryComment? {
        val text = EntryComment.normalize(raw)
        require(text.length <= EntryComment.MAX_LENGTH) { "Comment longer than ${EntryComment.MAX_LENGTH} characters" }
        return EntryComment.ofOrNull(text)
    }

    private inline fun <reified E : Enum<E>> one(value: String, what: String): E =
        enumValues<E>().firstOrNull { it.name == value.trim().uppercase() }
            ?: throw IllegalArgumentException("Unknown $what: $value")

    private inline fun <reified E : Enum<E>> codes(raw: String, what: String): Set<E> =
        raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }.map { one<E>(it, what) }.toSet()
}
