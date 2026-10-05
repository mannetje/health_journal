package nl.healthjournal.data

import android.content.Context
import nl.healthjournal.data.csv.CsvDataExportAdapter
import nl.healthjournal.data.csv.CsvDataImportAdapter
import nl.healthjournal.data.local.HealthJournalDatabase
import nl.healthjournal.data.repository.RoomHealthLogRepository
import nl.healthjournal.data.repository.RoomProfileRepository
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort

class DataModule(context: Context) {
    private val database: HealthJournalDatabase = HealthJournalDatabase.create(context)

    val profileRepository: ProfileRepositoryPort = RoomProfileRepository(database.profileDao())

    val healthLogRepository: HealthLogRepositoryPort = RoomHealthLogRepository(
        weightDao = database.weightDao(),
        bloodPressureDao = database.bloodPressureDao(),
        glucoseDao = database.glucoseDao(),
        activityDao = database.activityDao(),
        waistCircumferenceDao = database.waistCircumferenceDao()
    )

    val dataExportPort: DataExportPort = CsvDataExportAdapter(healthLogRepository)

    val dataImportPort: DataImportPort = CsvDataImportAdapter(healthLogRepository, profileRepository)
}
