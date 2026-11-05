package nl.healthjournal.domain.command

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.model.common.ImportResult
import nl.healthjournal.domain.port.secondary.MigraineExportPort
import nl.healthjournal.domain.port.secondary.MigraineImportPort

/** Exports every entry of one kind for a profile as CSV text. */
data class ExportCsvQuery(val profileId: ProfileId, val kind: EntryKind) : Query<String>

/** Imports CSV text into one kind. [libra] reads a weight file in the Libra app format. */
data class ImportCsvCommand(
    val profileId: ProfileId,
    val kind: EntryKind,
    val csv: String,
    val libra: Boolean = false
) : Command<ImportResult>

/**
 * Maps the string keys the UI and file pickers use (`"bp"`, `"spo2"`, ...) to an [EntryKind], once, at the edge.
 * Returns null for an unknown key.
 */
fun entryKindFromKey(key: String): EntryKind? = when (key.lowercase().replace("-", "_")) {
    "weight", "libra", "libra_weight" -> EntryKind.WEIGHT
    "blood_pressure", "bloodpressure", "bp" -> EntryKind.BLOOD_PRESSURE
    "glucose" -> EntryKind.GLUCOSE
    "activity" -> EntryKind.ACTIVITY
    "waist_circumference", "waist" -> EntryKind.WAIST_CIRCUMFERENCE
    "oxygen_saturation", "oxygen", "spo2" -> EntryKind.OXYGEN_SATURATION
    "migraine" -> EntryKind.MIGRAINE
    else -> null
}

/** True for the keys that mean a Libra weight file. */
fun isLibraKey(key: String): Boolean = key.lowercase().replace("-", "_").let { it == "libra" || it == "libra_weight" }

class ExportCsvHandler(
    private val export: DataExportPort,
    private val migraineExport: MigraineExportPort
) : QueryHandler<ExportCsvQuery, String> {
    override suspend fun handle(query: ExportCsvQuery): DomainResult<String> = domainCatching {
        when (query.kind) {
            EntryKind.WEIGHT -> export.exportWeightCsv(query.profileId)
            EntryKind.BLOOD_PRESSURE -> export.exportBloodPressureCsv(query.profileId)
            EntryKind.GLUCOSE -> export.exportGlucoseCsv(query.profileId)
            EntryKind.ACTIVITY -> export.exportActivityCsv(query.profileId)
            EntryKind.WAIST_CIRCUMFERENCE -> export.exportWaistCircumferenceCsv(query.profileId)
            EntryKind.OXYGEN_SATURATION -> export.exportOxygenSaturationCsv(query.profileId)
            EntryKind.MIGRAINE -> migraineExport.exportMigraineCsv(query.profileId)
        }
    }
}

class ImportCsvHandler(
    private val import: DataImportPort,
    private val migraineImport: MigraineImportPort
) : CommandHandler<ImportCsvCommand, ImportResult> {
    override suspend fun handle(command: ImportCsvCommand): DomainResult<ImportResult> = domainCatching {
        if (command.kind == EntryKind.MIGRAINE) {
            migraineImport.importMigraineCsv(command.profileId, command.csv)
        } else {
            import.importCsv(command.profileId, command.kind.importKey(command.libra), command.csv)
        }
    }
}

// The import port still takes the metric as a string; this is the only place that spells it out.
private fun EntryKind.importKey(libra: Boolean): String = when (this) {
    EntryKind.WEIGHT -> if (libra) "libra" else "weight"
    EntryKind.BLOOD_PRESSURE -> "blood_pressure"
    EntryKind.GLUCOSE -> "glucose"
    EntryKind.ACTIVITY -> "activity"
    EntryKind.WAIST_CIRCUMFERENCE -> "waist_circumference"
    EntryKind.OXYGEN_SATURATION -> "oxygen_saturation"
    EntryKind.MIGRAINE -> "migraine"
}
