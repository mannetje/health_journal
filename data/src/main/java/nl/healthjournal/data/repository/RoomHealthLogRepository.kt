package nl.healthjournal.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.healthjournal.data.local.dao.ActivityDao
import nl.healthjournal.data.local.dao.BloodPressureDao
import nl.healthjournal.data.local.dao.GlucoseDao
import nl.healthjournal.data.local.dao.WeightDao
import nl.healthjournal.data.local.mapper.HealthLogMapper
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort

class RoomHealthLogRepository(
    private val weightDao: WeightDao,
    private val bloodPressureDao: BloodPressureDao,
    private val glucoseDao: GlucoseDao,
    private val activityDao: ActivityDao
) : HealthLogRepositoryPort {

    // Weight
    override suspend fun saveWeight(entry: WeightEntry) {
        weightDao.insert(HealthLogMapper.toEntity(entry))
    }

    override suspend fun getWeightHistory(profileId: ProfileId): List<WeightEntry> {
        return weightDao.getByProfileId(profileId.value.toString()).map { HealthLogMapper.toDomain(it) }
    }

    override fun observeWeightHistory(profileId: ProfileId): Flow<List<WeightEntry>> {
        return weightDao.observeByProfileId(profileId.value.toString()).map { list ->
            list.map { HealthLogMapper.toDomain(it) }
        }
    }

    // Blood Pressure
    override suspend fun saveBloodPressure(entry: BloodPressureEntry) {
        bloodPressureDao.insert(HealthLogMapper.toEntity(entry))
    }

    override suspend fun getBloodPressureHistory(profileId: ProfileId): List<BloodPressureEntry> {
        return bloodPressureDao.getByProfileId(profileId.value.toString()).map { HealthLogMapper.toDomain(it) }
    }

    override fun observeBloodPressureHistory(profileId: ProfileId): Flow<List<BloodPressureEntry>> {
        return bloodPressureDao.observeByProfileId(profileId.value.toString()).map { list ->
            list.map { HealthLogMapper.toDomain(it) }
        }
    }

    // Glucose
    override suspend fun saveGlucose(entry: GlucoseEntry) {
        glucoseDao.insert(HealthLogMapper.toEntity(entry))
    }

    override suspend fun getGlucoseHistory(profileId: ProfileId): List<GlucoseEntry> {
        return glucoseDao.getByProfileId(profileId.value.toString()).map { HealthLogMapper.toDomain(it) }
    }

    override fun observeGlucoseHistory(profileId: ProfileId): Flow<List<GlucoseEntry>> {
        return glucoseDao.observeByProfileId(profileId.value.toString()).map { list ->
            list.map { HealthLogMapper.toDomain(it) }
        }
    }

    // Activity
    override suspend fun saveActivity(session: ActivitySession) {
        activityDao.insert(HealthLogMapper.toEntity(session))
    }

    override suspend fun getActivityHistory(profileId: ProfileId): List<ActivitySession> {
        return activityDao.getByProfileId(profileId.value.toString()).map { HealthLogMapper.toDomain(it) }
    }

    override fun observeActivityHistory(profileId: ProfileId): Flow<List<ActivitySession>> {
        return activityDao.observeByProfileId(profileId.value.toString()).map { list ->
            list.map { HealthLogMapper.toDomain(it) }
        }
    }
}
