package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weights",
    indices = [Index("profileId"), Index("timestamp")]
)
data class WeightEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val timestamp: Long, // Epoch millis
    val weightKg: Double,
    val bmi: Double?
)
