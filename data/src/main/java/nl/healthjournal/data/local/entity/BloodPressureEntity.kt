package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blood_pressures",
    indices = [Index("profileId"), Index("timestamp")]
)
data class BloodPressureEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val timestamp: Long,
    val systolic: Int,
    val diastolic: Int,
    val category: String
)
