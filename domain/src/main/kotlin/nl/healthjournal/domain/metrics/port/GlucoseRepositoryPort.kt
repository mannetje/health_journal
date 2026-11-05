package nl.healthjournal.domain.metrics.port

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.model.GlucoseEntry

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
