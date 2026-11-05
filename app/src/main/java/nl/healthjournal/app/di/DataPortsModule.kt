package nl.healthjournal.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import nl.healthjournal.data.DataModule
import nl.healthjournal.domain.port.secondary.BackupCodecPort
import nl.healthjournal.domain.port.secondary.BackupSnapshotPort
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.WeightRepositoryPort
import nl.healthjournal.domain.port.secondary.BloodPressureRepositoryPort
import nl.healthjournal.domain.port.secondary.GlucoseRepositoryPort
import nl.healthjournal.domain.port.secondary.ActivityRepositoryPort
import nl.healthjournal.domain.port.secondary.WaistCircumferenceRepositoryPort
import nl.healthjournal.domain.port.secondary.OxygenSaturationRepositoryPort
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.MigraineExportPort
import nl.healthjournal.domain.port.secondary.MigraineImportPort
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
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
