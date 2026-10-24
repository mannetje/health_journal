package nl.healthjournal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nl.healthjournal.data.local.entity.MigraineEntity
import nl.healthjournal.data.local.entity.MigraineTagEntity

@Dao
interface MigraineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MigraineEntity)

    /** Insert-or-ignore for backup restore: returns -1 when the id already exists, and never overwrites. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entry: MigraineEntity): Long

    @Update
    suspend fun update(entry: MigraineEntity): Int

    @Query("DELETE FROM migraines WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM migraines WHERE id = :id")
    suspend fun getById(id: String): MigraineEntity?

    @Query("SELECT * FROM migraines WHERE profileId = :profileId ORDER BY startTime DESC")
    suspend fun getByProfileId(profileId: String): List<MigraineEntity>

    @Query("SELECT * FROM migraines WHERE profileId = :profileId ORDER BY startTime DESC")
    fun observeByProfileId(profileId: String): Flow<List<MigraineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: MigraineTagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagIgnore(tag: MigraineTagEntity): Long

    /** Rename or archive; keeps `createdAt`. */
    @Query("UPDATE migraine_tags SET label = :label, archived = :archived WHERE id = :id")
    suspend fun updateTag(id: String, label: String, archived: Boolean): Int

    @Query("SELECT * FROM migraine_tags WHERE id = :id")
    suspend fun getTag(id: String): MigraineTagEntity?

    @Query("SELECT * FROM migraine_tags WHERE profileId = :profileId ORDER BY createdAt, label")
    suspend fun getTags(profileId: String): List<MigraineTagEntity>

    @Query("SELECT * FROM migraine_tags WHERE profileId = :profileId ORDER BY createdAt, label")
    fun observeTags(profileId: String): Flow<List<MigraineTagEntity>>
}
