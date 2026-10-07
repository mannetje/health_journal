package nl.healthjournal.data.repository

import kotlinx.coroutines.runBlocking
import nl.healthjournal.data.local.dao.MedicationDao
import nl.healthjournal.data.local.entity.IntakeEntity
import nl.healthjournal.data.local.entity.MedicationEntity
import nl.healthjournal.data.local.entity.MedicationScheduleEntity
import nl.healthjournal.data.local.entity.MedicationTimeEntity
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.Dosage
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeId
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.MedicationName
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.ScheduleVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Runs the repository against an in-memory [MedicationDao] that mirrors the SQL of the real one. */
class RoomMedicationRepositoryTest {

    private class FakeMedicationDao : MedicationDao {
        val medications = mutableMapOf<String, MedicationEntity>()
        val schedules = mutableListOf<MedicationScheduleEntity>()
        val times = mutableListOf<MedicationTimeEntity>()
        val intakes = mutableListOf<IntakeEntity>()

        override suspend fun upsertMedication(medication: MedicationEntity) {
            medications[medication.id] = medication
        }

        override suspend fun insertSchedules(schedules: List<MedicationScheduleEntity>) {
            this.schedules += schedules
        }

        override suspend fun insertTimes(times: List<MedicationTimeEntity>) {
            this.times += times
        }

        override suspend fun deleteSchedules(medicationId: String) {
            schedules.removeAll { it.medicationId == medicationId }
            times.removeAll { it.medicationId == medicationId }
        }

        override suspend fun getMedication(id: String): MedicationEntity? = medications[id]

        override suspend fun getMedications(profileId: String): List<MedicationEntity> =
            medications.values.filter { it.profileId == profileId }.sortedBy { it.name.lowercase() }

        override suspend fun getSchedules(medicationIds: List<String>): List<MedicationScheduleEntity> =
            schedules.filter { it.medicationId in medicationIds }.sortedBy { it.effectiveFrom }

        override suspend fun getTimes(medicationIds: List<String>): List<MedicationTimeEntity> =
            times.filter { it.medicationId in medicationIds }.sortedBy { it.localTime }

        override suspend fun deleteMedication(id: String): Int {
            val removed = medications.remove(id) != null
            deleteSchedules(id)
            intakes.removeAll { it.medicationId == id }
            return if (removed) 1 else 0
        }

        override suspend fun insertIntake(intake: IntakeEntity) {
            intakes.removeAll {
                it.id == intake.id || (intake.planned != null && it.medicationId == intake.medicationId && it.planned == intake.planned)
            }
            intakes += intake
        }

        override suspend fun getIntake(medicationId: String, planned: String): IntakeEntity? =
            intakes.firstOrNull { it.medicationId == medicationId && it.planned == planned }

        override suspend fun deleteIntake(id: String): Int = if (intakes.removeAll { it.id == id }) 1 else 0

        override suspend fun getIntakesForDay(
            profileId: String,
            plannedPrefix: String,
            fromMillis: Long,
            toMillis: Long
        ): List<IntakeEntity> = getAllIntakes(profileId).filter {
            if (it.planned != null) it.planned.substring(0, 10) == plannedPrefix
            else it.takenAt!! in fromMillis until toMillis
        }

        override suspend fun getAllIntakes(profileId: String): List<IntakeEntity> =
            intakes.filter { medications[it.medicationId]?.profileId == profileId }
    }

    private val dao = FakeMedicationDao()
    private val repository = RoomMedicationRepository(dao)
    private val profile = ProfileId.generate()
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val day = LocalDate.of(2026, 5, 4)

    private fun medication(name: String, archivedFrom: LocalDate? = null, owner: ProfileId = profile) = Medication(
        id = MedicationId.generate(),
        profileId = owner,
        name = MedicationName.of(name),
        form = null,
        dosage = Dosage(null, BigDecimal.ONE, DoseUnit.TABLETS),
        appearance = null,
        schedules = listOf(
            ScheduleVersion(
                day.minusDays(5),
                Schedule.Recurring(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), DayPattern.EVERY_DAY, day.minusDays(5))
            )
        ),
        archivedFrom = archivedFrom
    )

    private fun intake(medication: Medication, planned: java.time.LocalDateTime?, status: IntakeStatus = IntakeStatus.TAKEN, takenAt: Instant? = null) =
        Intake(
            id = IntakeId.generate(),
            medicationId = medication.id,
            planned = planned,
            status = status,
            takenAt = if (status == IntakeStatus.TAKEN) takenAt ?: planned?.atZone(zone)?.toInstant() else null
        )

    @Test
    fun `a saved medication is read back with its schedule and times`() = runBlocking {
        val saved = medication("Medication A")
        repository.saveMedication(saved)

        assertEquals(saved, repository.getMedication(saved.id))
    }

    @Test
    fun `an unknown medication is null`() = runBlocking {
        assertNull(repository.getMedication(MedicationId.generate()))
    }

    @Test
    fun `saving again replaces the schedule rows instead of adding to them`() = runBlocking {
        val saved = medication("Medication A")
        repository.saveMedication(saved)
        repository.saveMedication(saved.copy(name = MedicationName.of("Medication A2")))

        assertEquals(1, dao.schedules.size)
        assertEquals(2, dao.times.size)
        assertEquals("Medication A2", repository.getMedication(saved.id)?.name?.value)
    }

    @Test
    fun `medications are listed per profile, in name order, without archived ones by default`() = runBlocking {
        repository.saveMedication(medication("medication b"))
        repository.saveMedication(medication("Medication A"))
        repository.saveMedication(medication("Medication C", archivedFrom = day))
        repository.saveMedication(medication("Medication Z", owner = ProfileId.generate()))

        assertEquals(listOf("Medication A", "medication b"), repository.getMedications(profile).map { it.name.value })
        assertEquals(3, repository.getMedications(profile, includeArchived = true).size)
        assertTrue(repository.getMedications(ProfileId.generate()).isEmpty())
    }

    @Test
    fun `deleting a medication removes its schedule, times and intakes`() = runBlocking {
        val a = medication("Medication A")
        val b = medication("Medication B")
        repository.saveMedication(a)
        repository.saveMedication(b)
        repository.saveIntake(intake(a, day.atTime(8, 0)))
        repository.saveIntake(intake(b, day.atTime(8, 0)))

        repository.deleteMedication(a.id)

        assertNull(repository.getMedication(a.id))
        assertEquals(listOf(b.id), repository.getAllIntakes(profile).map { it.medicationId })
        assertTrue(dao.schedules.none { it.medicationId == a.id.value.toString() })
    }

    @Test
    fun `an intake is found by medication and planned time`() = runBlocking {
        val a = medication("Medication A")
        repository.saveMedication(a)
        val saved = intake(a, day.atTime(8, 0))
        repository.saveIntake(saved)

        assertEquals(saved, repository.getIntake(a.id, day.atTime(8, 0)))
        assertNull(repository.getIntake(a.id, day.atTime(20, 0)))
    }

    @Test
    fun `saving an outcome for the same planned time replaces it`() = runBlocking {
        val a = medication("Medication A")
        repository.saveMedication(a)
        repository.saveIntake(intake(a, day.atTime(8, 0), IntakeStatus.SKIPPED))
        repository.saveIntake(intake(a, day.atTime(8, 0), IntakeStatus.TAKEN))

        val all = repository.getAllIntakes(profile)
        assertEquals(1, all.size)
        assertEquals(IntakeStatus.TAKEN, all.single().status)
    }

    @Test
    fun `an intake can be deleted by id`() = runBlocking {
        val a = medication("Medication A")
        repository.saveMedication(a)
        val saved = intake(a, day.atTime(8, 0))
        repository.saveIntake(saved)

        repository.deleteIntake(saved.id)

        assertTrue(repository.getAllIntakes(profile).isEmpty())
    }

    @Test
    fun `a day holds planned outcomes of that date and as needed doses taken in the local day`() = runBlocking {
        val a = medication("Medication A")
        repository.saveMedication(a)
        repository.saveIntake(intake(a, day.atTime(8, 0)))
        repository.saveIntake(intake(a, day.plusDays(1).atTime(8, 0)))
        // 23:30 local on the day is inside it; 00:30 local the next day is outside it.
        repository.saveIntake(intake(a, null, takenAt = day.atTime(23, 30).atZone(zone).toInstant()))
        repository.saveIntake(intake(a, null, takenAt = day.plusDays(1).atTime(0, 30).atZone(zone).toInstant()))

        val result = repository.getIntakesForDay(profile, day, zone)

        assertEquals(2, result.size)
        assertNotNull(result.firstOrNull { it.planned == day.atTime(8, 0) })
        assertEquals(1, result.count { it.planned == null })
    }
}
