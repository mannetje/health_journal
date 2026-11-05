package nl.healthjournal.domain.port.secondary

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.GlucoseEntry

/** Storage of glucose entries. One port per metric, so a metric never depends on the others. */
interface GlucoseRepositoryPort {
    suspend fun saveGlucose(entry: GlucoseEntry)
    suspend fun getGlucoseHistory(profileId: ProfileId): List<GlucoseEntry>
    fun observeGlucoseHistory(profileId: ProfileId): Flow<List<GlucoseEntry>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateGlucose(entry: GlucoseEntry): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteGlucose(id: MeasurementId): Boolean
}
