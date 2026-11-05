package nl.healthjournal.domain.metrics.port

import nl.healthjournal.domain.shared.model.ImportResult
import nl.healthjournal.domain.shared.model.ProfileId

interface DataImportPort {
    suspend fun importCsv(profileId: ProfileId, metricType: String, csvContent: String): ImportResult
}
