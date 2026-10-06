package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waist_circumferences",
    indices = [Index("profileId"), Index("timestamp")]
)
data class WaistCircumferenceEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val timestamp: Long, // Epoch millis
    val waistCm: Double,
    val category: String?,
    val comment: String? = null
)
