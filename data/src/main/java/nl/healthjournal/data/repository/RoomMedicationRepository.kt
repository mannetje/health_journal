package nl.healthjournal.data.repository

import nl.healthjournal.data.local.dao.MedicationDao
import nl.healthjournal.data.local.mapper.MedicationMapper
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeId
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RoomMedicationRepository(
    private val dao: MedicationDao
) : MedicationRepositoryPort {

    override suspend fun saveMedication(medication: Medication) {
        val rows = MedicationMapper.toRows(medication)
        dao.saveMedication(rows.medication, rows.schedules, rows.times)
    }

    override suspend fun getMedication(id: MedicationId): Medication? {
        val entity = dao.getMedication(id.value.toString()) ?: return null
        return assemble(listOf(entity)).single()
    }

    override suspend fun getMedications(profileId: ProfileId, includeArchived: Boolean): List<Medication> {
        val entities = dao.getMedications(profileId.value.toString())
        return assemble(entities).filter { includeArchived || it.archivedFrom == null }
    }

    override suspend fun deleteMedication(id: MedicationId) {
        dao.deleteMedication(id.value.toString())
    }

    override suspend fun saveIntake(intake: Intake) {
        dao.insertIntake(MedicationMapper.toEntity(intake))
    }

    override suspend fun getIntake(medicationId: MedicationId, planned: LocalDateTime): Intake? =
        dao.getIntake(medicationId.value.toString(), planned.toString())?.let { MedicationMapper.toDomain(it) }

    override suspend fun deleteIntake(id: IntakeId) {
        dao.deleteIntake(id.value.toString())
    }

    override suspend fun getAllIntakes(profileId: ProfileId): List<Intake> =
        dao.getAllIntakes(profileId.value.toString()).map { MedicationMapper.toDomain(it) }

    override suspend fun getIntakesForDay(profileId: ProfileId, date: LocalDate, zone: ZoneId): List<Intake> {
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.getIntakesForDay(profileId.value.toString(), date.toString(), from, to)
            .map { MedicationMapper.toDomain(it) }
    }

    private suspend fun assemble(entities: List<nl.healthjournal.data.local.entity.MedicationEntity>): List<Medication> {
        if (entities.isEmpty()) return emptyList()
        val ids = entities.map { it.id }
        val schedules = dao.getSchedules(ids).groupBy { it.medicationId }
        val times = dao.getTimes(ids).groupBy { it.medicationId }
        return entities.map {
            MedicationMapper.toDomain(it, schedules[it.id].orEmpty(), times[it.id].orEmpty())
        }
    }
}
