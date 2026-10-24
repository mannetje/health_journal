package nl.healthjournal.domain.port.secondary

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagId

interface MigraineRepositoryPort {
    suspend fun saveMigraine(entry: MigraineEntry)
    suspend fun getMigraine(id: MeasurementId): MigraineEntry?
    suspend fun getMigraineHistory(profileId: ProfileId): List<MigraineEntry>
    fun observeMigraineHistory(profileId: ProfileId): Flow<List<MigraineEntry>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateMigraine(entry: MigraineEntry): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteMigraine(id: MeasurementId): Boolean

    suspend fun saveMigraineTag(tag: MigraineTag)
    suspend fun getMigraineTag(id: MigraineTagId): MigraineTag?
    /** All tags of the profile, archived ones included (old entries still refer to them). */
    suspend fun getMigraineTags(profileId: ProfileId): List<MigraineTag>
    fun observeMigraineTags(profileId: ProfileId): Flow<List<MigraineTag>>
    /** Replaces an existing tag (rename or archive); returns false when the id is unknown. */
    suspend fun updateMigraineTag(tag: MigraineTag): Boolean
}
