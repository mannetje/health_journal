package nl.healthjournal.domain.port.secondary

import nl.healthjournal.domain.model.common.ProfileId

interface MigraineExportPort {
    /** One CSV file with a row per migraine of the profile, sorted ascending by start. */
    suspend fun exportMigraineCsv(profileId: ProfileId): String
}

interface MigraineImportPort {
    /**
     * Imports the file into [profileId]. A rejected row imports nothing and creates no personal tag; skipped rows
     * carry the physical line number. A personal tag that does not exist yet is created in the profile list.
     */
    suspend fun importMigraineCsv(profileId: ProfileId, csvContent: String): ImportResult
}
