package nl.healthjournal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nl.healthjournal.data.local.entity.OxygenSaturationEntity

@Dao
interface OxygenSaturationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: OxygenSaturationEntity)

    /** Insert-or-ignore for backup restore: returns -1 when the id already exists, and never overwrites. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entry: OxygenSaturationEntity): Long

    @Update
    suspend fun update(entry: OxygenSaturationEntity): Int

    @Query("DELETE FROM oxygen_saturations WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM oxygen_saturations WHERE profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getByProfileId(profileId: String): List<OxygenSaturationEntity>

    @Query("SELECT * FROM oxygen_saturations WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun observeByProfileId(profileId: String): Flow<List<OxygenSaturationEntity>>
}
