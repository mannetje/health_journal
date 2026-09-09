package nl.healthjournal.domain.port.secondary

import nl.healthjournal.domain.model.common.ProfileId

interface DataExportPort {
    suspend fun exportWeightCsv(profileId: ProfileId): String
    suspend fun exportBloodPressureCsv(profileId: ProfileId): String
    suspend fun exportGlucoseCsv(profileId: ProfileId): String
    suspend fun exportActivityCsv(profileId: ProfileId): String
}
