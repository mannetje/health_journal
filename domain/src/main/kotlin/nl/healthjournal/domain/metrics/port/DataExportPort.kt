package nl.healthjournal.domain.metrics.port

import nl.healthjournal.domain.shared.model.ProfileId

interface DataExportPort {
    suspend fun exportWeightCsv(profileId: ProfileId): String
    suspend fun exportBloodPressureCsv(profileId: ProfileId): String
    suspend fun exportGlucoseCsv(profileId: ProfileId): String
    suspend fun exportActivityCsv(profileId: ProfileId): String
    suspend fun exportWaistCircumferenceCsv(profileId: ProfileId): String
    suspend fun exportOxygenSaturationCsv(profileId: ProfileId): String
}
