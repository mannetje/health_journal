package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val dateOfBirth: String, // ISO-8601 YYYY-MM-DD
    val heightCm: Int?,
    val isActive: Boolean = false
)
