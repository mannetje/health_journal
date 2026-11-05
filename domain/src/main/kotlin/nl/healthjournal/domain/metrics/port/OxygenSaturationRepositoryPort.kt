package nl.healthjournal.domain.metrics.port

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.model.OxygenSaturationEntry

/** Storage of oxygen saturation entries. One port per metric, so a metric never depends on the others. */
interface OxygenSaturationRepositoryPort {
    suspend fun saveOxygenSaturation(entry: OxygenSaturationEntry)
    suspend fun getOxygenSaturationHistory(profileId: ProfileId): List<OxygenSaturationEntry>
    fun observeOxygenSaturationHistory(profileId: ProfileId): Flow<List<OxygenSaturationEntry>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateOxygenSaturation(entry: OxygenSaturationEntry): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteOxygenSaturation(id: MeasurementId): Boolean
}
