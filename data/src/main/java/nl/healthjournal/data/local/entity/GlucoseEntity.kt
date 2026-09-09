package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "glucoses",
    indices = [Index("profileId"), Index("timestamp")]
)
data class GlucoseEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val timestamp: Long,
    val glucoseMmolL: Double,
    val context: String,
    val category: String
)
