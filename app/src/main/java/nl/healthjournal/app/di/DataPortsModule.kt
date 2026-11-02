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
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
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
    fun healthLogRepository(data: DataModule): HealthLogRepositoryPort = data.healthLogRepository

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
