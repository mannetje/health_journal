package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activities",
    indices = [Index("profileId"), Index("startTime")]
)
data class ActivityEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double
)
