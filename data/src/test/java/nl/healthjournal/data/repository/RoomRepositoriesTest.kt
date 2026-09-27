package nl.healthjournal.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.healthjournal.data.local.dao.*
import nl.healthjournal.data.local.entity.*
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.*
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.model.profile.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class RoomRepositoriesTest {

    private class FakeProfileDao : ProfileDao {
        private val profiles = mutableMapOf<String, ProfileEntity>()
        override suspend fun insert(profile: ProfileEntity) {
            profiles[profile.id] = profile
        }
        override suspend fun getById(id: String): ProfileEntity? = profiles[id]
        override suspend fun getAll(): List<ProfileEntity> = profiles.values.toList()
        override suspend fun getActive(): ProfileEntity? = profiles.values.firstOrNull { it.isActive }
        override suspend fun clearActive() {
            profiles.forEach { (k, v) -> profiles[k] = v.copy(isActive = false) }
        }
        override suspend fun markActive(id: String) {
            profiles[id]?.let { profiles[id] = it.copy(isActive = true) }
        }
    }

    private class FakeWeightDao : WeightDao {
        private val list = mutableListOf<WeightEntity>()
        private val flow = MutableStateFlow<List<WeightEntity>>(emptyList())

        override suspend fun insert(entry: WeightEntity) {
            list.removeAll { it.id == entry.id }
            list.add(entry)
            flow.value = list.sortedByDescending { it.timestamp }
        }
        override suspend fun getByProfileId(profileId: String): List<WeightEntity> =
            list.filter { it.profileId == profileId }.sortedByDescending { it.timestamp }

        override fun observeByProfileId(profileId: String): Flow<List<WeightEntity>> = flow
    }

    private class FakeBloodPressureDao : BloodPressureDao {
        private val list = mutableListOf<BloodPressureEntity>()
        private val flow = MutableStateFlow<List<BloodPressureEntity>>(emptyList())

        override suspend fun insert(entry: BloodPressureEntity) {
            list.removeAll { it.id == entry.id }
            list.add(entry)
            flow.value = list.sortedByDescending { it.timestamp }
        }
        override suspend fun getByProfileId(profileId: String): List<BloodPressureEntity> =
            list.filter { it.profileId == profileId }.sortedByDescending { it.timestamp }

        override fun observeByProfileId(profileId: String): Flow<List<BloodPressureEntity>> = flow
    }

    private class FakeGlucoseDao : GlucoseDao {
        private val list = mutableListOf<GlucoseEntity>()
        private val flow = MutableStateFlow<List<GlucoseEntity>>(emptyList())

        override suspend fun insert(entry: GlucoseEntity) {
            list.removeAll { it.id == entry.id }
            list.add(entry)
            flow.value = list.sortedByDescending { it.timestamp }
        }
        override suspend fun getByProfileId(profileId: String): List<GlucoseEntity> =
            list.filter { it.profileId == profileId }.sortedByDescending { it.timestamp }

        override fun observeByProfileId(profileId: String): Flow<List<GlucoseEntity>> = flow
    }

    private class FakeActivityDao : ActivityDao {
        private val list = mutableListOf<ActivityEntity>()
        private val flow = MutableStateFlow<List<ActivityEntity>>(emptyList())

        override suspend fun insert(entry: ActivityEntity) {
            list.removeAll { it.id == entry.id }
            list.add(entry)
            flow.value = list.sortedByDescending { it.startTime }
        }
        override suspend fun getByProfileId(profileId: String): List<ActivityEntity> =
            list.filter { it.profileId == profileId }.sortedByDescending { it.startTime }

        override fun observeByProfileId(profileId: String): Flow<List<ActivityEntity>> = flow
    }

    @Test
    fun `RoomProfileRepository saves and retrieves active profile`() = runBlocking {
        val dao = FakeProfileDao()
        val repo = RoomProfileRepository(dao)

        val profile1 = Profile.create("Alice", LocalDate.of(1995, 3, 10), HeightCm(168))
        val profile2 = Profile.create("Bob", LocalDate.of(1992, 7, 20), HeightCm(180))

        repo.save(profile1)
        repo.save(profile2)

        val active = repo.getActiveProfile()
        assertNotNull(active)
        assertEquals(profile1.id, active?.id)

        repo.setActiveProfile(profile2.id)
        val newActive = repo.getActiveProfile()
        assertEquals(profile2.id, newActive?.id)
    }

    @Test
    fun `RoomHealthLogRepository handles all metric operations`() = runBlocking {
        val weightDao = FakeWeightDao()
        val bpDao = FakeBloodPressureDao()
        val glucoseDao = FakeGlucoseDao()
        val activityDao = FakeActivityDao()

        val repo = RoomHealthLogRepository(weightDao, bpDao, glucoseDao, activityDao)
        val profileId = ProfileId.generate()

        // Weight
        val weightEntry = WeightEntry(
            id = MeasurementId.generate(),
            profileId = profileId,
            timestamp = Instant.now(),
            weight = WeightKg(BigDecimal("80.0")),
            bmi = BigDecimal("24.0")
        )
        repo.saveWeight(weightEntry)
        val weights = repo.getWeightHistory(profileId)
        assertEquals(1, weights.size)
        assertEquals(80.0, weights[0].weight.value.toDouble(), 0.01)

        // Blood Pressure
        val bpEntry = BloodPressureEntry(
            id = MeasurementId.generate(),
            profileId = profileId,
            timestamp = Instant.now(),
            reading = BloodPressureReading(120, 80),
            category = NhgBloodPressureCategory.NORMAL
        )
        repo.saveBloodPressure(bpEntry)
        val bps = repo.getBloodPressureHistory(profileId)
        assertEquals(1, bps.size)
        assertEquals(120, bps[0].reading.systolic)

        // Glucose
        val glucoseEntry = GlucoseEntry(
            id = MeasurementId.generate(),
            profileId = profileId,
            timestamp = Instant.now(),
            glucose = GlucoseLevel(BigDecimal("5.5")),
            context = GlucoseContext.FASTING,
            category = NhgGlucoseCategory.NORMAL
        )
        repo.saveGlucose(glucoseEntry)
        val glucoses = repo.getGlucoseHistory(profileId)
        assertEquals(1, glucoses.size)
        assertEquals(5.5, glucoses[0].glucose.valueInMmolL.toDouble(), 0.01)

        // Activity
        val now = Instant.now()
        val session = ActivitySession(
            id = MeasurementId.generate(),
            profileId = profileId,
            startTime = now,
            endTime = now.plusSeconds(3600),
            distanceInMeters = 10000.0
        )
        repo.saveActivity(session)
        val activities = repo.getActivityHistory(profileId)
        assertEquals(1, activities.size)
        assertEquals(10000.0, activities[0].distanceInMeters, 0.01)
    }
}
