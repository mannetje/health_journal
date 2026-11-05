package nl.healthjournal.domain.port.secondary

import nl.healthjournal.domain.model.common.ImportResult
import nl.healthjournal.domain.model.common.ProfileId

interface DataImportPort {
    suspend fun importCsv(profileId: ProfileId, metricType: String, csvContent: String): ImportResult
}
