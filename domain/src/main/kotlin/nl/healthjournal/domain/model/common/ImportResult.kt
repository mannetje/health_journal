package nl.healthjournal.domain.model.common

/** A CSV row that could not be imported, with the reason. */
data class SkippedRow(
    val lineNumber: Int,
    val reason: String
)

/** What a CSV import did: how many rows it stored and which it skipped. */
data class ImportResult(
    val importedCount: Int,
    val skippedRows: List<SkippedRow>
)
