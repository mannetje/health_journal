package nl.healthjournal.domain.port.secondary

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry

/** Storage of waist circumference entries. One port per metric, so a metric never depends on the others. */
interface WaistCircumferenceRepositoryPort {
    suspend fun saveWaistCircumference(entry: WaistCircumferenceEntry)
    suspend fun getWaistCircumferenceHistory(profileId: ProfileId): List<WaistCircumferenceEntry>
    fun observeWaistCircumferenceHistory(profileId: ProfileId): Flow<List<WaistCircumferenceEntry>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateWaistCircumference(entry: WaistCircumferenceEntry): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteWaistCircumference(id: MeasurementId): Boolean
}
