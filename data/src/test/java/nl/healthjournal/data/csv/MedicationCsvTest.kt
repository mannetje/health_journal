package nl.healthjournal.data.csv

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.Dosage
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeId
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationForm
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.MedicationName
import nl.healthjournal.domain.model.medication.PillAppearance
import nl.healthjournal.domain.model.medication.PillColor
import nl.healthjournal.domain.model.medication.PillShape
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.ScheduleVersion
import nl.healthjournal.domain.model.medication.Strength
import nl.healthjournal.domain.model.medication.StrengthUnit
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.metrics.HeightCm
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.MedicationCsvFiles
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Placeholder names only ("Medication A"); tests never use real medicine names. */
class MedicationCsvTest {

    private class FakeMedicationRepository : MedicationRepositoryPort {
        val medications = linkedMapOf<MedicationId, Medication>()
        val intakes = linkedMapOf<IntakeId, Intake>()

        override suspend fun saveMedication(medication: Medication) { medications[medication.id] = medication }
        override suspend fun getMedication(id: MedicationId): Medication? = medications[id]
        override suspend fun getMedications(profileId: ProfileId, includeArchived: Boolean): List<Medication> =
            medications.values.filter { it.profileId == profileId && (includeArchived || it.archivedFrom == null) }
        override suspend fun deleteMedication(id: MedicationId) {
            medications.remove(id)
            intakes.values.removeAll { it.medicationId == id }
        }

        override suspend fun saveIntake(intake: Intake) {
            // Mirrors the unique index: one outcome per planned time.
            intakes.values.removeAll { it.planned != null && it.medicationId == intake.medicationId && it.planned == intake.planned }
            intakes[intake.id] = intake
        }
        override suspend fun getIntake(medicationId: MedicationId, planned: LocalDateTime): Intake? =
            intakes.values.firstOrNull { it.medicationId == medicationId && it.planned == planned }
        override suspend fun deleteIntake(id: IntakeId) { intakes.remove(id) }
        override suspend fun getAllIntakes(profileId: ProfileId): List<Intake> {
            val ids = medications.values.filter { it.profileId == profileId }.map { it.id }.toSet()
            return intakes.values.filter { it.medicationId in ids }
        }
        override suspend fun getIntakesForDay(profileId: ProfileId, date: LocalDate, zone: ZoneId): List<Intake> =
            getAllIntakes(profileId).filter { (it.planned?.toLocalDate() ?: it.takenAt!!.atZone(zone).toLocalDate()) == date }
    }

    private class FakeProfileRepository : ProfileRepositoryPort {
        val profiles = mutableMapOf<ProfileId, Profile>()
        override suspend fun save(profile: Profile) { profiles[profile.id] = profile }
        override suspend fun getById(id: ProfileId): Profile? = profiles[id]
        override suspend fun getAll(): List<Profile> = profiles.values.toList()
        override suspend fun getActiveProfile(): Profile? = profiles.values.firstOrNull()
        override suspend fun setActiveProfile(id: ProfileId) {}
    }

    private val day = LocalDate.of(2026, 9, 1)

    private fun medication(
        profileId: ProfileId,
        name: String,
        schedule: Schedule,
        comment: String? = null,
        archivedFrom: LocalDate? = null
    ) = Medication(
        id = MedicationId.generate(),
        profileId = profileId,
        name = MedicationName.of(name),
        form = MedicationForm.TABLET,
        dosage = Dosage(Strength(BigDecimal("2.50"), StrengthUnit.MG), BigDecimal("1.5"), DoseUnit.TABLETS),
        appearance = PillAppearance(PillColor.WHITE, PillShape.ROUND),
        schedules = listOf(ScheduleVersion(day, schedule)),
        comment = EntryComment.ofOrNull(comment),
        archivedFrom = archivedFrom
    )

    private class Fixture {
        val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1), HeightCm(180))
        val profiles = FakeProfileRepository().also { runBlocking { it.save(profile) } }
        val repo = FakeMedicationRepository()
        val export = CsvMedicationExportAdapter(repo)
        val import = CsvMedicationImportAdapter(repo, profiles)
    }

    private val weekdaysSchedule = Schedule.Recurring.of(
        listOf(LocalTime.of(20, 0), LocalTime.of(8, 0)),
        DayPattern.Weekdays(setOf(DayOfWeek.THURSDAY, DayOfWeek.MONDAY)),
        day, LocalDate.of(2026, 12, 31)
    )

    @Test
    fun `empty export has only headers`() = runBlocking {
        val f = Fixture()
        val files = f.export.exportMedicationCsv(f.profile.id)
        assertEquals(MEDICATIONS_HEADER + "\n", files.medications)
        assertEquals(SCHEDULES_HEADER + "\n", files.schedules)
        assertEquals(INTAKES_HEADER + "\n", files.intakes)
    }

    @Test
    fun `round trip keeps medications, schedule versions and intakes`() = runBlocking {
        val source = Fixture()
        val a = medication(source.profile.id, "Medication A", weekdaysSchedule, comment = "with, a comma")
        val b = medication(source.profile.id, "Medication B", Schedule.Recurring.of(listOf(LocalTime.of(9, 0)), DayPattern.EveryNDays(7), day))
            .withScheduleFrom(LocalDate.of(2026, 10, 1), Schedule.AsNeeded)
        val c = medication(source.profile.id, "Medication C", Schedule.AsNeeded, archivedFrom = LocalDate.of(2026, 11, 1))
        listOf(a, b, c).forEach { source.repo.saveMedication(it) }
        source.repo.saveIntake(Intake(IntakeId.generate(), a.id, LocalDateTime.of(2026, 9, 1, 8, 0), IntakeStatus.TAKEN, Instant.parse("2026-09-01T06:05:00Z"), BigDecimal("1.0"), EntryComment("left arm")))
        source.repo.saveIntake(Intake(IntakeId.generate(), a.id, LocalDateTime.of(2026, 9, 1, 20, 0), IntakeStatus.SKIPPED, null))
        source.repo.saveIntake(Intake(IntakeId.generate(), c.id, null, IntakeStatus.TAKEN, Instant.parse("2026-09-02T10:00:00Z")))

        val files = source.export.exportMedicationCsv(source.profile.id)

        val target = Fixture()
        val result = target.import.importMedicationCsv(target.profile.id, files)

        assertEquals(emptyList<Any>(), result.skippedRows)
        assertEquals(6, result.importedCount)
        val imported = target.repo.getMedications(target.profile.id, includeArchived = true).associateBy { it.name.value }
        assertEquals(setOf("Medication A", "Medication B", "Medication C"), imported.keys)
        assertEquals(a.copy(id = imported.getValue("Medication A").id, profileId = target.profile.id), imported.getValue("Medication A"))
        assertEquals(b.copy(id = imported.getValue("Medication B").id, profileId = target.profile.id), imported.getValue("Medication B"))
        assertEquals(c.copy(id = imported.getValue("Medication C").id, profileId = target.profile.id), imported.getValue("Medication C"))
        assertEquals(BigDecimal("2.50"), imported.getValue("Medication A").dosage.strength!!.amount)
        assertEquals(3, target.repo.getAllIntakes(target.profile.id).size)
    }

    @Test
    fun `importing the same files twice adds nothing`() = runBlocking {
        val f = Fixture()
        val a = medication(f.profile.id, "Medication A", weekdaysSchedule)
        val c = medication(f.profile.id, "Medication C", Schedule.AsNeeded)
        f.repo.saveMedication(a)
        f.repo.saveMedication(c)
        f.repo.saveIntake(Intake(IntakeId.generate(), a.id, LocalDateTime.of(2026, 9, 1, 8, 0), IntakeStatus.TAKEN, Instant.parse("2026-09-01T06:05:00Z")))
        f.repo.saveIntake(Intake(IntakeId.generate(), c.id, null, IntakeStatus.TAKEN, Instant.parse("2026-09-02T10:00:00Z")))
        val files = f.export.exportMedicationCsv(f.profile.id)

        val first = f.import.importMedicationCsv(f.profile.id, files)
        val second = f.import.importMedicationCsv(f.profile.id, files)

        assertEquals(0, first.importedCount)
        assertEquals(0, second.importedCount)
        assertEquals(emptyList<Any>(), second.skippedRows)
        assertEquals(2, f.repo.getMedications(f.profile.id, includeArchived = true).size)
        assertEquals(2, f.repo.getAllIntakes(f.profile.id).size)
    }

    @Test
    fun `a row with an unknown ref is skipped with its line number`() = runBlocking {
        val f = Fixture()
        val files = MedicationCsvFiles(
            medications = "$MEDICATIONS_HEADER\n1,Medication A,TABLET,10,MG,1,TABLETS,false,,,,\n",
            schedules = "$SCHEDULES_HEADER\n1,2026-09-01,08:00,MON,,2026-09-01,\n9,2026-09-01,08:00,MON,,2026-09-01,\n",
            intakes = "$INTAKES_HEADER\n7,2026-09-01T08:00,TAKEN,2026-09-01T06:00:00Z,,\n"
        )

        val result = f.import.importMedicationCsv(f.profile.id, files)

        assertEquals(1, result.importedCount)
        assertEquals(listOf(3, 2), result.skippedRows.map { it.lineNumber })
        assertTrue(result.skippedRows.all { "Unknown ref" in it.reason })
    }

    @Test
    fun `an unknown enum value skips the row`() = runBlocking {
        val f = Fixture()
        val files = MedicationCsvFiles(
            medications = "$MEDICATIONS_HEADER\n1,Medication A,POWDER,10,MG,1,TABLETS,false,,,,\n2,Medication B,TABLET,10,MG,1,TABLETS,true,,,,\n",
            schedules = "$SCHEDULES_HEADER\n2,2026-09-01,,,,,\n",
            intakes = INTAKES_HEADER + "\n"
        )

        val result = f.import.importMedicationCsv(f.profile.id, files)

        assertEquals(1, result.importedCount)
        assertEquals(1, result.skippedRows.size)
        assertEquals(2, result.skippedRows.single().lineNumber)
        assertTrue("Unknown form" in result.skippedRows.single().reason)
        assertEquals(listOf("Medication B"), f.repo.medications.values.map { it.name.value })
    }

    @Test
    fun `an over-long comment skips the row`() = runBlocking {
        val f = Fixture()
        val long = "x".repeat(EntryComment.MAX_LENGTH + 1)
        val files = MedicationCsvFiles(
            medications = "$MEDICATIONS_HEADER\n1,Medication A,TABLET,10,MG,1,TABLETS,true,,,$long,\n",
            schedules = "$SCHEDULES_HEADER\n1,2026-09-01,,,,,\n",
            intakes = INTAKES_HEADER + "\n"
        )

        val result = f.import.importMedicationCsv(f.profile.id, files)

        assertEquals(0, result.importedCount)
        assertTrue(result.skippedRows.any { it.lineNumber == 2 && it.reason.startsWith("medications:") })
        assertTrue(f.repo.medications.isEmpty())
    }

    @Test
    fun `a medication without a valid schedule is skipped`() = runBlocking {
        val f = Fixture()
        val files = MedicationCsvFiles(
            medications = "$MEDICATIONS_HEADER\n1,Medication A,TABLET,10,MG,1,TABLETS,false,,,,\n",
            schedules = SCHEDULES_HEADER + "\n",
            intakes = INTAKES_HEADER + "\n"
        )

        val result = f.import.importMedicationCsv(f.profile.id, files)

        assertEquals(0, result.importedCount)
        assertEquals("medications: No valid schedule", result.skippedRows.single().reason)
    }

    @Test
    fun `an unknown profile imports nothing`() = runBlocking {
        val f = Fixture()
        val result = f.import.importMedicationCsv(ProfileId.generate(), MedicationCsvFiles("", "", ""))
        assertEquals(0, result.importedCount)
        assertTrue(result.skippedRows.isEmpty())
    }
}
