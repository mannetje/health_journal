package nl.healthjournal.domain.metrics.port

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.model.ActivitySession

/** Storage of activity entries. One port per metric, so a metric never depends on the others. */
interface ActivityRepositoryPort {
    suspend fun saveActivity(session: ActivitySession)
    suspend fun getActivityHistory(profileId: ProfileId): List<ActivitySession>
    fun observeActivityHistory(profileId: ProfileId): Flow<List<ActivitySession>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateActivity(session: ActivitySession): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteActivity(id: MeasurementId): Boolean
}
