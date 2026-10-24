package nl.healthjournal.data.backup

import androidx.room.withTransaction
import nl.healthjournal.data.local.HealthJournalDatabase
import nl.healthjournal.data.local.mapper.HealthLogMapper
import nl.healthjournal.data.local.mapper.MedicationMapper
import nl.healthjournal.data.local.mapper.MigraineMapper
import nl.healthjournal.data.local.mapper.ProfileMapper
import nl.healthjournal.domain.model.backup.BackupMedication
import nl.healthjournal.domain.model.backup.BackupProfile
import nl.healthjournal.domain.model.backup.MergeResult
import nl.healthjournal.domain.port.secondary.BackupSnapshotPort

/**
 * Reads and merges the whole database for a backup, each in one Room transaction. The merge only uses
 * insert-or-ignore, so nothing existing is overwritten and a failure rolls everything back.
 */
class RoomBackupSnapshot(
    private val db: HealthJournalDatabase,
    private val clock: () -> Long = System::currentTimeMillis
) : BackupSnapshotPort {

    override suspend fun snapshot(): List<BackupProfile> = db.withTransaction {
        val medicationDao = db.medicationDao()
        db.profileDao().getAll().map { entity ->
            val id = entity.id
            val medications = medicationDao.getMedications(id)
            val ids = medications.map { it.id }
            val schedules = if (ids.isEmpty()) emptyMap() else medicationDao.getSchedules(ids).groupBy { it.medicationId }
            val times = if (ids.isEmpty()) emptyMap() else medicationDao.getTimes(ids).groupBy { it.medicationId }
            val intakes = medicationDao.getAllIntakes(id).groupBy { it.medicationId }
            BackupProfile(
                profile = ProfileMapper.toDomain(entity),
                isActive = entity.isActive,
                weights = db.weightDao().getByProfileId(id).map(HealthLogMapper::toDomain),
                bloodPressures = db.bloodPressureDao().getByProfileId(id).map(HealthLogMapper::toDomain),
                glucoses = db.glucoseDao().getByProfileId(id).map(HealthLogMapper::toDomain),
                waistCircumferences = db.waistCircumferenceDao().getByProfileId(id).map(HealthLogMapper::toDomain),
                activities = db.activityDao().getByProfileId(id).map(HealthLogMapper::toDomain),
                migraines = db.migraineDao().getByProfileId(id).map(MigraineMapper::toDomain),
                migraineTags = db.migraineDao().getTags(id).mapNotNull(MigraineMapper::toDomain),
                medications = medications.map { med ->
                    BackupMedication(
                        medication = MedicationMapper.toDomain(med, schedules[med.id].orEmpty(), times[med.id].orEmpty()),
                        intakes = intakes[med.id].orEmpty().map(MedicationMapper::toDomain)
                    )
                }
            )
        }
    }

    override suspend fun merge(profiles: List<BackupProfile>): MergeResult = db.withTransaction {
        var profilesAdded = 0
        var entriesAdded = 0
        var medicationsAdded = 0
        var intakesAdded = 0
        var present = 0

        fun count(inserted: Long, onAdded: () -> Unit) {
            if (inserted == -1L) present++ else onAdded()
        }

        val medicationDao = db.medicationDao()
        for (item in profiles) {
            val profileId = item.profile.id.value.toString()
            count(db.profileDao().insertIgnore(ProfileMapper.toEntity(item.profile, isActive = false))) { profilesAdded++ }

            item.weights.forEach { count(db.weightDao().insertIgnore(HealthLogMapper.toEntity(it))) { entriesAdded++ } }
            item.bloodPressures.forEach { count(db.bloodPressureDao().insertIgnore(HealthLogMapper.toEntity(it))) { entriesAdded++ } }
            item.glucoses.forEach { count(db.glucoseDao().insertIgnore(HealthLogMapper.toEntity(it))) { entriesAdded++ } }
            item.waistCircumferences.forEach { count(db.waistCircumferenceDao().insertIgnore(HealthLogMapper.toEntity(it))) { entriesAdded++ } }
            item.activities.forEach { count(db.activityDao().insertIgnore(HealthLogMapper.toEntity(it))) { entriesAdded++ } }

            val migraineDao = db.migraineDao()
            item.migraines.forEach { count(migraineDao.insertIgnore(MigraineMapper.toEntity(it))) { entriesAdded++ } }
            // Tags are not counted: they are the person's own labels, not entries.
            item.migraineTags.forEach { migraineDao.insertTagIgnore(MigraineMapper.toEntity(it, clock())) }

            for (entry in item.medications) {
                val rows = MedicationMapper.toRows(entry.medication)
                val existing = medicationDao.getMedication(rows.medication.id)
                if (existing != null && existing.profileId != profileId) {
                    // The same id under another profile: never move or touch it.
                    present += 1 + entry.intakes.size
                    continue
                }
                count(medicationDao.insertMedicationIgnore(rows.medication)) { medicationsAdded++ }
                for (schedule in rows.schedules) {
                    if (medicationDao.insertScheduleIgnore(schedule) != -1L) {
                        rows.times
                            .filter { it.effectiveFrom == schedule.effectiveFrom }
                            .forEach { medicationDao.insertTimeIgnore(it) }
                    }
                }
                for (intake in entry.intakes) {
                    count(medicationDao.insertIntakeIgnore(MedicationMapper.toEntity(intake))) { intakesAdded++ }
                }
            }
        }

        // Keep the active profile if there is one; otherwise take the one the file marks.
        if (db.profileDao().getActive() == null) {
            profiles.firstOrNull { it.isActive }?.let { db.profileDao().markActive(it.profile.id.value.toString()) }
        }
        MergeResult(profilesAdded, entriesAdded, medicationsAdded, intakesAdded, present)
    }
}
