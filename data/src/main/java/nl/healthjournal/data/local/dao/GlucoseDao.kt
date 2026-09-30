package nl.healthjournal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nl.healthjournal.data.local.entity.GlucoseEntity

@Dao
interface GlucoseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: GlucoseEntity)

    @Update
    suspend fun update(entry: GlucoseEntity): Int

    @Query("DELETE FROM glucoses WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM glucoses WHERE profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getByProfileId(profileId: String): List<GlucoseEntity>

    @Query("SELECT * FROM glucoses WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun observeByProfileId(profileId: String): Flow<List<GlucoseEntity>>
}
