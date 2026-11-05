package nl.healthjournal.domain.port.secondary

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.OxygenSaturationEntry

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
