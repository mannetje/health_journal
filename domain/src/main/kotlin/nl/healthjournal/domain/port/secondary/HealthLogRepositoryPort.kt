package nl.healthjournal.domain.port.secondary

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.WeightEntry
import kotlinx.coroutines.flow.Flow

interface HealthLogRepositoryPort {
    // Weight
    suspend fun saveWeight(entry: WeightEntry)
    suspend fun getWeightHistory(profileId: ProfileId): List<WeightEntry>
    fun observeWeightHistory(profileId: ProfileId): Flow<List<WeightEntry>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateWeight(entry: WeightEntry): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteWeight(id: MeasurementId): Boolean

    // Blood Pressure
    suspend fun saveBloodPressure(entry: BloodPressureEntry)
    suspend fun getBloodPressureHistory(profileId: ProfileId): List<BloodPressureEntry>
    fun observeBloodPressureHistory(profileId: ProfileId): Flow<List<BloodPressureEntry>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateBloodPressure(entry: BloodPressureEntry): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteBloodPressure(id: MeasurementId): Boolean

    // Glucose
    suspend fun saveGlucose(entry: GlucoseEntry)
    suspend fun getGlucoseHistory(profileId: ProfileId): List<GlucoseEntry>
    fun observeGlucoseHistory(profileId: ProfileId): Flow<List<GlucoseEntry>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateGlucose(entry: GlucoseEntry): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteGlucose(id: MeasurementId): Boolean

    // Activity
    suspend fun saveActivity(session: ActivitySession)
    suspend fun getActivityHistory(profileId: ProfileId): List<ActivitySession>
    fun observeActivityHistory(profileId: ProfileId): Flow<List<ActivitySession>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateActivity(session: ActivitySession): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteActivity(id: MeasurementId): Boolean
}
