package nl.healthjournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import nl.healthjournal.data.local.dao.*
import nl.healthjournal.data.local.entity.*

@Database(
    entities = [
        ProfileEntity::class,
        WeightEntity::class,
        BloodPressureEntity::class,
        GlucoseEntity::class,
        ActivityEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HealthJournalDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun glucoseDao(): GlucoseDao
    abstract fun activityDao(): ActivityDao

    companion object {
        const val DATABASE_NAME = "health_journal.db"

        fun createInMemory(context: Context): HealthJournalDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                HealthJournalDatabase::class.java
            ).allowMainThreadQueries().build()
        }

        fun create(context: Context): HealthJournalDatabase {
            return Room.databaseBuilder(
                context,
                HealthJournalDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}
