package nl.healthjournal.domain.port.secondary

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.WeightEntry

/** Storage of weight entries. One port per metric, so a metric never depends on the others. */
interface WeightRepositoryPort {
    suspend fun saveWeight(entry: WeightEntry)
    suspend fun getWeightHistory(profileId: ProfileId): List<WeightEntry>
    fun observeWeightHistory(profileId: ProfileId): Flow<List<WeightEntry>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateWeight(entry: WeightEntry): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteWeight(id: MeasurementId): Boolean
}
