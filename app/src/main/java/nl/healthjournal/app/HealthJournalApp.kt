package nl.healthjournal.app

import android.app.Application
import nl.healthjournal.app.ui.history.EntryUseCases
import nl.healthjournal.data.DataModule
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.*

class HealthJournalApp : Application() {

    private lateinit var dataModule: DataModule

    val profileRepository: ProfileRepositoryPort
        get() = dataModule.profileRepository

    val healthLogRepository: HealthLogRepositoryPort
        get() = dataModule.healthLogRepository

    val dataExportAdapter: DataExportPort
        get() = dataModule.dataExportPort

    val dataImportAdapter: DataImportPort
        get() = dataModule.dataImportPort

    // Use cases
    lateinit var createProfileUseCase: CreateProfileUseCase
        private set
    lateinit var recordWeightUseCase: RecordWeightUseCase
        private set
    lateinit var recordBloodPressureUseCase: RecordBloodPressureUseCase
        private set
    lateinit var recordGlucoseUseCase: RecordGlucoseUseCase
        private set
    lateinit var recordActivityUseCase: RecordActivityUseCase
    lateinit var entryUseCases: EntryUseCases
        private set
    lateinit var getHealthHistoryUseCase: GetHealthHistoryUseCase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        dataModule = DataModule(this)

        createProfileUseCase = CreateProfileUseCase(profileRepository)
        recordWeightUseCase = RecordWeightUseCase(healthLogRepository, profileRepository)
        recordBloodPressureUseCase = RecordBloodPressureUseCase(healthLogRepository)
        recordGlucoseUseCase = RecordGlucoseUseCase(healthLogRepository)
        recordActivityUseCase = RecordActivityUseCase(healthLogRepository)
        entryUseCases = EntryUseCases(
            updateWeight = UpdateWeightUseCase(healthLogRepository, profileRepository),
            updateBloodPressure = UpdateBloodPressureUseCase(healthLogRepository),
            updateGlucose = UpdateGlucoseUseCase(healthLogRepository),
            updateActivity = UpdateActivityUseCase(healthLogRepository),
            deleteWeight = DeleteWeightUseCase(healthLogRepository),
            deleteBloodPressure = DeleteBloodPressureUseCase(healthLogRepository),
            deleteGlucose = DeleteGlucoseUseCase(healthLogRepository),
            deleteActivity = DeleteActivityUseCase(healthLogRepository)
        )
        getHealthHistoryUseCase = GetHealthHistoryUseCase(healthLogRepository)
    }

    companion object {
        lateinit var instance: HealthJournalApp
            private set
    }
}
