package nl.healthjournal.domain.metrics.port

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.model.WeightEntry

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
