package nl.healthjournal.domain.port.secondary

import nl.healthjournal.domain.model.common.ProfileId

/** The three CSV files of a medication export; rows link through the `ref` column. */
data class MedicationCsvFiles(
    val medications: String,
    val schedules: String,
    val intakes: String
)

interface MedicationExportPort {
    suspend fun exportMedicationCsv(profileId: ProfileId): MedicationCsvFiles
}

interface MedicationImportPort {
    /**
     * Imports the files into [profileId]. A blank file counts as empty. Skipped rows carry the physical line
     * number in their own file, with the file name at the start of the reason.
     */
    suspend fun importMedicationCsv(profileId: ProfileId, files: MedicationCsvFiles): ImportResult
}
