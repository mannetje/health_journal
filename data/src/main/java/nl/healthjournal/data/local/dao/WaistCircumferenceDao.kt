package nl.healthjournal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nl.healthjournal.data.local.entity.WaistCircumferenceEntity

@Dao
interface WaistCircumferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WaistCircumferenceEntity)

    @Update
    suspend fun update(entry: WaistCircumferenceEntity): Int

    @Query("DELETE FROM waist_circumferences WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM waist_circumferences WHERE profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getByProfileId(profileId: String): List<WaistCircumferenceEntity>

    @Query("SELECT * FROM waist_circumferences WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun observeByProfileId(profileId: String): Flow<List<WaistCircumferenceEntity>>
}
