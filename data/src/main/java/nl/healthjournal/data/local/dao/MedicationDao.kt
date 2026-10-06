package nl.healthjournal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import nl.healthjournal.data.local.entity.IntakeEntity
import nl.healthjournal.data.local.entity.MedicationEntity
import nl.healthjournal.data.local.entity.MedicationScheduleEntity
import nl.healthjournal.data.local.entity.MedicationTimeEntity

@Dao
interface MedicationDao {
    /** Upsert, not REPLACE: replacing a parent row would cascade-delete its schedules and intakes. */
    @Upsert
    suspend fun upsertMedication(medication: MedicationEntity)

    @Insert
    suspend fun insertSchedules(schedules: List<MedicationScheduleEntity>)

    @Insert
    suspend fun insertTimes(times: List<MedicationTimeEntity>)

    /** Cascades to the times of each version. */
    @Query("DELETE FROM medication_schedules WHERE medicationId = :medicationId")
    suspend fun deleteSchedules(medicationId: String)

    @Transaction
    suspend fun saveMedication(
        medication: MedicationEntity,
        schedules: List<MedicationScheduleEntity>,
        times: List<MedicationTimeEntity>
    ) {
        upsertMedication(medication)
        deleteSchedules(medication.id)
        insertSchedules(schedules)
        insertTimes(times)
    }

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedication(id: String): MedicationEntity?

    @Query("SELECT * FROM medications WHERE profileId = :profileId ORDER BY name COLLATE NOCASE")
    suspend fun getMedications(profileId: String): List<MedicationEntity>

    @Query("SELECT * FROM medication_schedules WHERE medicationId IN (:medicationIds) ORDER BY effectiveFrom")
    suspend fun getSchedules(medicationIds: List<String>): List<MedicationScheduleEntity>

    @Query("SELECT * FROM medication_times WHERE medicationId IN (:medicationIds) ORDER BY localTime")
    suspend fun getTimes(medicationIds: List<String>): List<MedicationTimeEntity>

    /** Cascades to schedule versions, times and intakes. */
    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntake(intake: IntakeEntity)

    @Query("SELECT * FROM intakes WHERE medicationId = :medicationId AND planned = :planned")
    suspend fun getIntake(medicationId: String, planned: String): IntakeEntity?

    @Query("DELETE FROM intakes WHERE id = :id")
    suspend fun deleteIntake(id: String): Int

    /**
     * Outcomes planned on [plannedPrefix] (`yyyy-MM-dd`), plus as-needed doses taken in
     * [fromMillis, toMillis), for the medications of [profileId].
     */
    @Query(
        """
        SELECT i.* FROM intakes i JOIN medications m ON m.id = i.medicationId
        WHERE m.profileId = :profileId
          AND ((i.planned IS NOT NULL AND substr(i.planned, 1, 10) = :plannedPrefix)
            OR (i.planned IS NULL AND i.takenAt >= :fromMillis AND i.takenAt < :toMillis))
        """
    )
    suspend fun getIntakesForDay(profileId: String, plannedPrefix: String, fromMillis: Long, toMillis: Long): List<IntakeEntity>

    @Query("SELECT i.* FROM intakes i JOIN medications m ON m.id = i.medicationId WHERE m.profileId = :profileId")
    suspend fun getAllIntakes(profileId: String): List<IntakeEntity>
}
