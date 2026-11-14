package nl.healthjournal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nl.healthjournal.data.local.entity.FluidIntakeEntity
import nl.healthjournal.data.local.entity.FluidSettingsEntity

@Dao
interface FluidIntakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FluidIntakeEntity)

    /** Insert-or-ignore for backup restore: returns -1 when the id already exists, and never overwrites. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entry: FluidIntakeEntity): Long

    @Update
    suspend fun update(entry: FluidIntakeEntity): Int

    @Query("DELETE FROM fluid_intakes WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM fluid_intakes WHERE profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getByProfileId(profileId: String): List<FluidIntakeEntity>

    @Query("SELECT * FROM fluid_intakes WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun observeByProfileId(profileId: String): Flow<List<FluidIntakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: FluidSettingsEntity)

    @Query("SELECT * FROM fluid_settings WHERE profileId = :profileId")
    suspend fun getSettings(profileId: String): FluidSettingsEntity?

    @Query("SELECT * FROM fluid_settings WHERE profileId = :profileId")
    fun observeSettings(profileId: String): Flow<FluidSettingsEntity?>
}
