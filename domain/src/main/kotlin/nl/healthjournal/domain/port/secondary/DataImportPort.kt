package nl.healthjournal.domain.port.secondary

import nl.healthjournal.domain.model.common.ProfileId

data class SkippedRow(
    val lineNumber: Int,
    val reason: String
)

data class ImportResult(
    val importedCount: Int,
    val skippedRows: List<SkippedRow>
)

interface DataImportPort {
    suspend fun importCsv(profileId: ProfileId, metricType: String, csvContent: String): ImportResult
}
