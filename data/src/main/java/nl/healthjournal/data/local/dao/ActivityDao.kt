package nl.healthjournal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nl.healthjournal.data.local.entity.ActivityEntity

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ActivityEntity)

    /** Insert-or-ignore for backup restore: returns -1 when the id already exists, and never overwrites. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entry: ActivityEntity): Long

    @Update
    suspend fun update(entry: ActivityEntity): Int

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM activities WHERE profileId = :profileId ORDER BY startTime DESC")
    suspend fun getByProfileId(profileId: String): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE profileId = :profileId ORDER BY startTime DESC")
    fun observeByProfileId(profileId: String): Flow<List<ActivityEntity>>
}
