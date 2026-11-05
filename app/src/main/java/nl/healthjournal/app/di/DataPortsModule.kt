package nl.healthjournal.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import nl.healthjournal.data.DataModule
import nl.healthjournal.domain.backup.port.BackupCodecPort
import nl.healthjournal.domain.backup.port.BackupSnapshotPort
import nl.healthjournal.domain.metrics.port.DataExportPort
import nl.healthjournal.domain.metrics.port.DataImportPort
import nl.healthjournal.domain.metrics.port.WeightRepositoryPort
import nl.healthjournal.domain.metrics.port.BloodPressureRepositoryPort
import nl.healthjournal.domain.metrics.port.GlucoseRepositoryPort
import nl.healthjournal.domain.metrics.port.ActivityRepositoryPort
import nl.healthjournal.domain.metrics.port.WaistCircumferenceRepositoryPort
import nl.healthjournal.domain.metrics.port.OxygenSaturationRepositoryPort
import nl.healthjournal.domain.medication.port.MedicationRepositoryPort
import nl.healthjournal.domain.migraine.port.MigraineExportPort
import nl.healthjournal.domain.migraine.port.MigraineImportPort
import nl.healthjournal.domain.migraine.port.MigraineRepositoryPort
import nl.healthjournal.domain.profile.port.ProfileRepositoryPort
import javax.inject.Singleton

/** `DataModule` stays one singleton, so the database is opened once and every port shares it. */
@Module
@InstallIn(SingletonComponent::class)
object DataPortsModule {

    @Provides
    @Singleton
    fun dataModule(@ApplicationContext context: Context): DataModule = DataModule(context)

    @Provides
    fun profileRepository(data: DataModule): ProfileRepositoryPort = data.profileRepository

    @Provides
    fun weightRepository(data: DataModule): WeightRepositoryPort = data.weightRepository

    @Provides
    fun bloodPressureRepository(data: DataModule): BloodPressureRepositoryPort = data.bloodPressureRepository

    @Provides
    fun glucoseRepository(data: DataModule): GlucoseRepositoryPort = data.glucoseRepository

    @Provides
    fun activityRepository(data: DataModule): ActivityRepositoryPort = data.activityRepository

    @Provides
    fun waistCircumferenceRepository(data: DataModule): WaistCircumferenceRepositoryPort = data.waistCircumferenceRepository

    @Provides
    fun oxygenSaturationRepository(data: DataModule): OxygenSaturationRepositoryPort = data.oxygenSaturationRepository

    @Provides
    fun medicationRepository(data: DataModule): MedicationRepositoryPort = data.medicationRepository

    @Provides
    fun migraineRepository(data: DataModule): MigraineRepositoryPort = data.migraineRepository

    @Provides
    fun dataExport(data: DataModule): DataExportPort = data.dataExportPort

    @Provides
    fun dataImport(data: DataModule): DataImportPort = data.dataImportPort

    @Provides
    fun migraineExport(data: DataModule): MigraineExportPort = data.migraineExportPort

    @Provides
    fun migraineImport(data: DataModule): MigraineImportPort = data.migraineImportPort

    @Provides
    fun backupCodec(data: DataModule): BackupCodecPort = data.backupCodec

    @Provides
    fun backupSnapshot(data: DataModule): BackupSnapshotPort = data.backupSnapshotPort
}
