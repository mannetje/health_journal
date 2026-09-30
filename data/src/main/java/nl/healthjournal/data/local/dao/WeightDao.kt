package nl.healthjournal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nl.healthjournal.data.local.entity.WeightEntity

@Dao
interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntity)

    @Update
    suspend fun update(entry: WeightEntity): Int

    @Query("DELETE FROM weights WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM weights WHERE profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getByProfileId(profileId: String): List<WeightEntity>

    @Query("SELECT * FROM weights WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun observeByProfileId(profileId: String): Flow<List<WeightEntity>>
}
