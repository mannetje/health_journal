package nl.healthjournal.domain.port.secondary

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

    // Blood Pressure
    suspend fun saveBloodPressure(entry: BloodPressureEntry)
    suspend fun getBloodPressureHistory(profileId: ProfileId): List<BloodPressureEntry>
    fun observeBloodPressureHistory(profileId: ProfileId): Flow<List<BloodPressureEntry>>

    // Glucose
    suspend fun saveGlucose(entry: GlucoseEntry)
    suspend fun getGlucoseHistory(profileId: ProfileId): List<GlucoseEntry>
    fun observeGlucoseHistory(profileId: ProfileId): Flow<List<GlucoseEntry>>

    // Activity
    suspend fun saveActivity(session: ActivitySession)
    suspend fun getActivityHistory(profileId: ProfileId): List<ActivitySession>
    fun observeActivityHistory(profileId: ProfileId): Flow<List<ActivitySession>>
}
