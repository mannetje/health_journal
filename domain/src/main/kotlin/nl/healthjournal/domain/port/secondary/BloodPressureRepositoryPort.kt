package nl.healthjournal.domain.port.secondary

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.BloodPressureEntry

/** Storage of blood pressure entries. One port per metric, so a metric never depends on the others. */
interface BloodPressureRepositoryPort {
    suspend fun saveBloodPressure(entry: BloodPressureEntry)
    suspend fun getBloodPressureHistory(profileId: ProfileId): List<BloodPressureEntry>
    fun observeBloodPressureHistory(profileId: ProfileId): Flow<List<BloodPressureEntry>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateBloodPressure(entry: BloodPressureEntry): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteBloodPressure(id: MeasurementId): Boolean
}
