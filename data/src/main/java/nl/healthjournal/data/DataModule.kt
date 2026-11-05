package nl.healthjournal.data

import android.content.Context
import nl.healthjournal.data.backup.Argon2KeyDerivation
import nl.healthjournal.data.backup.BackupContainer
import nl.healthjournal.data.backup.HjBackupCodec
import nl.healthjournal.data.backup.RoomBackupSnapshot
import nl.healthjournal.data.csv.CsvDataExportAdapter
import nl.healthjournal.data.csv.CsvDataImportAdapter
import nl.healthjournal.data.csv.CsvMedicationExportAdapter
import nl.healthjournal.data.csv.CsvMedicationImportAdapter
import nl.healthjournal.data.csv.CsvMigraineExportAdapter
import nl.healthjournal.data.csv.CsvMigraineImportAdapter
import nl.healthjournal.data.local.HealthJournalDatabase
import nl.healthjournal.data.repository.RoomHealthLogRepository
import nl.healthjournal.data.repository.RoomMedicationRepository
import nl.healthjournal.data.repository.RoomMigraineRepository
import nl.healthjournal.data.repository.RoomProfileRepository
import nl.healthjournal.domain.port.secondary.BackupCodecPort
import nl.healthjournal.domain.port.secondary.BackupSnapshotPort
import nl.healthjournal.domain.port.secondary.MedicationExportPort
import nl.healthjournal.domain.port.secondary.MedicationImportPort
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.MigraineExportPort
import nl.healthjournal.domain.port.secondary.MigraineImportPort
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.WeightRepositoryPort
import nl.healthjournal.domain.port.secondary.BloodPressureRepositoryPort
import nl.healthjournal.domain.port.secondary.GlucoseRepositoryPort
import nl.healthjournal.domain.port.secondary.ActivityRepositoryPort
import nl.healthjournal.domain.port.secondary.WaistCircumferenceRepositoryPort
import nl.healthjournal.domain.port.secondary.OxygenSaturationRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort

class DataModule(context: Context) {
    private val database: HealthJournalDatabase = HealthJournalDatabase.create(context)

    val profileRepository: ProfileRepositoryPort = RoomProfileRepository(database.profileDao())

    private val healthLog = RoomHealthLogRepository(
        weightDao = database.weightDao(),
        bloodPressureDao = database.bloodPressureDao(),
        glucoseDao = database.glucoseDao(),
        activityDao = database.activityDao(),
        waistCircumferenceDao = database.waistCircumferenceDao(),
        oxygenSaturationDao = database.oxygenSaturationDao()
    )

    val weightRepository: WeightRepositoryPort = healthLog

    val bloodPressureRepository: BloodPressureRepositoryPort = healthLog

    val glucoseRepository: GlucoseRepositoryPort = healthLog

    val activityRepository: ActivityRepositoryPort = healthLog

    val waistCircumferenceRepository: WaistCircumferenceRepositoryPort = healthLog

    val oxygenSaturationRepository: OxygenSaturationRepositoryPort = healthLog

    val medicationRepository: MedicationRepositoryPort = RoomMedicationRepository(database.medicationDao())

    val migraineRepository: MigraineRepositoryPort = RoomMigraineRepository(database.migraineDao())

    val dataExportPort: DataExportPort = CsvDataExportAdapter(weightRepository, bloodPressureRepository, glucoseRepository, activityRepository, waistCircumferenceRepository, oxygenSaturationRepository)

    val dataImportPort: DataImportPort = CsvDataImportAdapter(weightRepository, bloodPressureRepository, glucoseRepository, activityRepository, waistCircumferenceRepository, oxygenSaturationRepository, profileRepository)

    val medicationExportPort: MedicationExportPort = CsvMedicationExportAdapter(medicationRepository)

    val medicationImportPort: MedicationImportPort = CsvMedicationImportAdapter(medicationRepository, profileRepository)

    val migraineExportPort: MigraineExportPort = CsvMigraineExportAdapter(migraineRepository)

    val migraineImportPort: MigraineImportPort = CsvMigraineImportAdapter(migraineRepository, profileRepository)

    val backupSnapshotPort: BackupSnapshotPort = RoomBackupSnapshot(database)

    val backupCodec: BackupCodecPort = HjBackupCodec(BackupContainer(Argon2KeyDerivation()))
}
