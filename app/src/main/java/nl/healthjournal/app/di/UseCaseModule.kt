package nl.healthjournal.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nl.healthjournal.domain.port.secondary.BackupCodecPort
import nl.healthjournal.domain.port.secondary.BackupSnapshotPort
import nl.healthjournal.domain.port.secondary.WeightRepositoryPort
import nl.healthjournal.domain.port.secondary.BloodPressureRepositoryPort
import nl.healthjournal.domain.port.secondary.GlucoseRepositoryPort
import nl.healthjournal.domain.port.secondary.ActivityRepositoryPort
import nl.healthjournal.domain.port.secondary.WaistCircumferenceRepositoryPort
import nl.healthjournal.domain.port.secondary.OxygenSaturationRepositoryPort
import nl.healthjournal.domain.usecase.*
import java.time.Clock

/** The domain has no DI annotations, so each use case is built here from its ports. */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun clock(): Clock = Clock.systemUTC()

    @Provides
    fun getHealthHistory(
        weight: WeightRepositoryPort,
        bloodPressure: BloodPressureRepositoryPort,
        glucose: GlucoseRepositoryPort,
        activity: ActivityRepositoryPort,
        waist: WaistCircumferenceRepositoryPort,
        oxygen: OxygenSaturationRepositoryPort
    ) = GetHealthHistoryUseCase(weight, bloodPressure, glucose, activity, waist, oxygen)

    @Provides
    fun createBackup(snapshot: BackupSnapshotPort, codec: BackupCodecPort) = CreateBackupUseCase(snapshot, codec)

    @Provides
    fun restoreBackup(snapshot: BackupSnapshotPort, codec: BackupCodecPort) = RestoreBackupUseCase(snapshot, codec)
}
