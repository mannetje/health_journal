package nl.healthjournal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nl.healthjournal.data.local.entity.BloodPressureEntity

@Dao
interface BloodPressureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BloodPressureEntity)

    @Query("SELECT * FROM blood_pressures WHERE profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getByProfileId(profileId: String): List<BloodPressureEntity>

    @Query("SELECT * FROM blood_pressures WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun observeByProfileId(profileId: String): Flow<List<BloodPressureEntity>>
}
