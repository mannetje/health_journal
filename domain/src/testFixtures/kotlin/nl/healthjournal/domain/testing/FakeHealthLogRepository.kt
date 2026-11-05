package nl.healthjournal.domain.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.model.ActivitySession
import nl.healthjournal.domain.metrics.model.BloodPressureEntry
import nl.healthjournal.domain.metrics.model.GlucoseEntry
import nl.healthjournal.domain.metrics.model.OxygenSaturationEntry
import nl.healthjournal.domain.metrics.model.WaistCircumferenceEntry
import nl.healthjournal.domain.metrics.model.WeightEntry
import nl.healthjournal.domain.metrics.port.WeightRepositoryPort
import nl.healthjournal.domain.metrics.port.BloodPressureRepositoryPort
import nl.healthjournal.domain.metrics.port.GlucoseRepositoryPort
import nl.healthjournal.domain.metrics.port.ActivityRepositoryPort
import nl.healthjournal.domain.metrics.port.WaistCircumferenceRepositoryPort
import nl.healthjournal.domain.metrics.port.OxygenSaturationRepositoryPort
import nl.healthjournal.domain.metrics.usecase.GetHealthHistoryUseCase

/** In-memory storage for all six metrics, shared by the domain, data and app tests. */
class FakeHealthLogRepository :
    WeightRepositoryPort,
    BloodPressureRepositoryPort,
    GlucoseRepositoryPort,
    ActivityRepositoryPort,
    WaistCircumferenceRepositoryPort,
    OxygenSaturationRepositoryPort {
    val weights = mutableListOf<WeightEntry>()
    val bloodPressures = mutableListOf<BloodPressureEntry>()
    val glucoses = mutableListOf<GlucoseEntry>()
    val activities = mutableListOf<ActivitySession>()
    val waistCircumferences = mutableListOf<WaistCircumferenceEntry>()
    val oxygenSaturations = mutableListOf<OxygenSaturationEntry>()

    override suspend fun saveOxygenSaturation(entry: OxygenSaturationEntry) { oxygenSaturations.add(entry) }
    override suspend fun updateOxygenSaturation(entry: OxygenSaturationEntry): Boolean {
        val i = oxygenSaturations.indexOfFirst { it.id == entry.id }
        if (i < 0) return false
        oxygenSaturations[i] = entry
        return true
    }
    override suspend fun deleteOxygenSaturation(id: MeasurementId): Boolean = oxygenSaturations.removeAll { it.id == id }
    override suspend fun getOxygenSaturationHistory(profileId: ProfileId): List<OxygenSaturationEntry> =
        oxygenSaturations.filter { it.profileId == profileId }.sortedByDescending { it.timestamp }
    override fun observeOxygenSaturationHistory(profileId: ProfileId): Flow<List<OxygenSaturationEntry>> =
        flow { emit(getOxygenSaturationHistory(profileId)) }

    override suspend fun saveWeight(entry: WeightEntry) { weights.add(entry) }
    override suspend fun updateWeight(entry: WeightEntry): Boolean {
        val i = weights.indexOfFirst { it.id == entry.id }
        if (i < 0) return false
        weights[i] = entry
        return true
    }
    override suspend fun deleteWeight(id: MeasurementId): Boolean = weights.removeAll { it.id == id }
    override suspend fun getWeightHistory(profileId: ProfileId): List<WeightEntry> =
        weights.filter { it.profileId == profileId }
    override fun observeWeightHistory(profileId: ProfileId): Flow<List<WeightEntry>> =
        flow { emit(getWeightHistory(profileId)) }

    override suspend fun saveBloodPressure(entry: BloodPressureEntry) { bloodPressures.add(entry) }
    override suspend fun updateBloodPressure(entry: BloodPressureEntry): Boolean {
        val i = bloodPressures.indexOfFirst { it.id == entry.id }
        if (i < 0) return false
        bloodPressures[i] = entry
        return true
    }
    override suspend fun deleteBloodPressure(id: MeasurementId): Boolean = bloodPressures.removeAll { it.id == id }
    override suspend fun getBloodPressureHistory(profileId: ProfileId): List<BloodPressureEntry> =
        bloodPressures.filter { it.profileId == profileId }
    override fun observeBloodPressureHistory(profileId: ProfileId): Flow<List<BloodPressureEntry>> =
        flow { emit(getBloodPressureHistory(profileId)) }

    override suspend fun saveGlucose(entry: GlucoseEntry) { glucoses.add(entry) }
    override suspend fun updateGlucose(entry: GlucoseEntry): Boolean {
        val i = glucoses.indexOfFirst { it.id == entry.id }
        if (i < 0) return false
        glucoses[i] = entry
        return true
    }
    override suspend fun deleteGlucose(id: MeasurementId): Boolean = glucoses.removeAll { it.id == id }
    override suspend fun getGlucoseHistory(profileId: ProfileId): List<GlucoseEntry> =
        glucoses.filter { it.profileId == profileId }
    override fun observeGlucoseHistory(profileId: ProfileId): Flow<List<GlucoseEntry>> =
        flow { emit(getGlucoseHistory(profileId)) }

    override suspend fun saveActivity(session: ActivitySession) { activities.add(session) }
    override suspend fun updateActivity(session: ActivitySession): Boolean {
        val i = activities.indexOfFirst { it.id == session.id }
        if (i < 0) return false
        activities[i] = session
        return true
    }
    override suspend fun deleteActivity(id: MeasurementId): Boolean = activities.removeAll { it.id == id }
    override suspend fun getActivityHistory(profileId: ProfileId): List<ActivitySession> =
        activities.filter { it.profileId == profileId }
    override fun observeActivityHistory(profileId: ProfileId): Flow<List<ActivitySession>> =
        flow { emit(getActivityHistory(profileId)) }

    override suspend fun saveWaistCircumference(entry: WaistCircumferenceEntry) { waistCircumferences.add(entry) }
    override suspend fun updateWaistCircumference(entry: WaistCircumferenceEntry): Boolean {
        val i = waistCircumferences.indexOfFirst { it.id == entry.id }
        if (i < 0) return false
        waistCircumferences[i] = entry
        return true
    }
    override suspend fun deleteWaistCircumference(id: MeasurementId): Boolean = waistCircumferences.removeAll { it.id == id }
    override suspend fun getWaistCircumferenceHistory(profileId: ProfileId): List<WaistCircumferenceEntry> =
        waistCircumferences.filter { it.profileId == profileId }
    override fun observeWaistCircumferenceHistory(profileId: ProfileId): Flow<List<WaistCircumferenceEntry>> =
        flow { emit(getWaistCircumferenceHistory(profileId)) }
}

fun FakeHealthLogRepository.healthHistoryUseCase() = GetHealthHistoryUseCase(this, this, this, this, this, this)
