package nl.healthjournal.data.local

import kotlinx.coroutines.test.runTest
import nl.healthjournal.data.local.dao.MedicationDao
import nl.healthjournal.data.local.entity.IntakeEntity
import nl.healthjournal.data.local.entity.MedicationEntity
import nl.healthjournal.data.local.entity.MedicationScheduleEntity
import nl.healthjournal.data.local.entity.MedicationTimeEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The medication DAO against a real in-memory SQLite: edit, delete, cascades and the day query. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MedicationDaoTest {
    private lateinit var db: HealthJournalDatabase
    private lateinit var dao: MedicationDao

    @Before
    fun open() {
        db = HealthJournalDatabase.createInMemory(RuntimeEnvironment.getApplication())
        dao = db.medicationDao()
    }

    @After
    fun close() = db.close()

    private fun medication(id: String = "m1", profile: String = "p1", name: String = "Medication A") =
        MedicationEntity(id, profile, name, "TABLET", null, null, "1", "TABLET", null, null, null, null)

    private fun schedule(id: String = "m1", from: String = "2026-01-01") =
        MedicationScheduleEntity(id, from, false, "MON|TUE", null, null, null)

    private fun intake(id: String, medicationId: String = "m1", planned: String? = "2026-05-04T08:00", status: String = "TAKEN", takenAt: Long? = 1L) =
        IntakeEntity(id, medicationId, planned, status, takenAt, null, null)

    @Test
    fun saving_stores_the_medication_with_its_schedule_and_times() = runTest {
        dao.saveMedication(medication(), listOf(schedule()), listOf(MedicationTimeEntity("m1", "2026-01-01", "08:00")))

        assertEquals("Medication A", dao.getMedication("m1")?.name)
        assertEquals(1, dao.getSchedules(listOf("m1")).size)
        assertEquals(listOf("08:00"), dao.getTimes(listOf("m1")).map { it.localTime })
    }

    @Test
    fun editing_replaces_schedule_and_times_but_keeps_the_recorded_intakes() = runTest {
        dao.saveMedication(medication(), listOf(schedule()), listOf(MedicationTimeEntity("m1", "2026-01-01", "08:00")))
        dao.insertIntake(intake("i1"))

        dao.saveMedication(
            medication(name = "Medication B"),
            listOf(schedule(from = "2026-06-01")),
            listOf(MedicationTimeEntity("m1", "2026-06-01", "20:00"))
        )

        assertEquals("Medication B", dao.getMedication("m1")?.name)
        assertEquals(listOf("2026-06-01"), dao.getSchedules(listOf("m1")).map { it.effectiveFrom })
        assertEquals(listOf("20:00"), dao.getTimes(listOf("m1")).map { it.localTime })
        assertNotNull(dao.getIntake("m1", "2026-05-04T08:00"))
    }

    @Test
    fun deleting_a_medication_removes_its_schedules_times_and_intakes_only() = runTest {
        dao.saveMedication(medication("m1"), listOf(schedule("m1")), listOf(MedicationTimeEntity("m1", "2026-01-01", "08:00")))
        dao.saveMedication(medication("m2"), listOf(schedule("m2")), listOf(MedicationTimeEntity("m2", "2026-01-01", "09:00")))
        dao.insertIntake(intake("i1", "m1"))
        dao.insertIntake(intake("i2", "m2"))

        assertEquals(1, dao.deleteMedication("m1"))

        assertNull(dao.getMedication("m1"))
        assertEquals(listOf("m2"), dao.getSchedules(listOf("m1", "m2")).map { it.medicationId })
        assertEquals(listOf("m2"), dao.getTimes(listOf("m1", "m2")).map { it.medicationId })
        assertEquals(listOf("i2"), dao.getAllIntakes("p1").map { it.id })
    }

    @Test
    fun deleting_an_unknown_medication_changes_nothing() = runTest {
        dao.saveMedication(medication(), listOf(schedule()), emptyList())
        assertEquals(0, dao.deleteMedication("missing"))
        assertNotNull(dao.getMedication("m1"))
    }

    @Test
    fun recording_the_same_planned_time_again_replaces_the_outcome() = runTest {
        dao.saveMedication(medication(), listOf(schedule()), emptyList())
        dao.insertIntake(intake("i1", status = "TAKEN"))
        dao.insertIntake(intake("i2", status = "SKIPPED", takenAt = null))

        assertEquals(listOf("i2"), dao.getAllIntakes("p1").map { it.id })
        assertEquals("SKIPPED", dao.getIntake("m1", "2026-05-04T08:00")?.status)
    }

    @Test
    fun deleting_an_intake_removes_only_that_outcome() = runTest {
        dao.saveMedication(medication(), listOf(schedule()), emptyList())
        dao.insertIntake(intake("i1", planned = "2026-05-04T08:00"))
        dao.insertIntake(intake("i2", planned = "2026-05-05T08:00"))

        assertEquals(1, dao.deleteIntake("i1"))

        assertEquals(listOf("i2"), dao.getAllIntakes("p1").map { it.id })
    }

    @Test
    fun the_day_query_returns_planned_outcomes_of_that_day_and_as_needed_doses_in_the_window() = runTest {
        dao.saveMedication(medication("m1", "p1"), listOf(schedule("m1")), emptyList())
        dao.saveMedication(medication("m2", "p2"), listOf(schedule("m2")), emptyList())
        dao.insertIntake(intake("planned-today", "m1", "2026-05-04T08:00"))
        dao.insertIntake(intake("planned-other-day", "m1", "2026-05-05T08:00"))
        dao.insertIntake(intake("as-needed-in", "m1", planned = null, takenAt = 1_500L))
        dao.insertIntake(intake("as-needed-out", "m1", planned = null, takenAt = 3_000L))
        dao.insertIntake(intake("other-profile", "m2", "2026-05-04T08:00"))

        val ids = dao.getIntakesForDay("p1", "2026-05-04", 1_000L, 2_000L).map { it.id }.toSet()

        assertEquals(setOf("planned-today", "as-needed-in"), ids)
    }

    @Test
    fun medications_are_listed_per_profile_by_name_ignoring_case() = runTest {
        dao.saveMedication(medication("m1", "p1", "banana"), listOf(schedule("m1")), emptyList())
        dao.saveMedication(medication("m2", "p1", "Apple"), listOf(schedule("m2")), emptyList())
        dao.saveMedication(medication("m3", "p2", "Other"), listOf(schedule("m3")), emptyList())

        assertEquals(listOf("Apple", "banana"), dao.getMedications("p1").map { it.name })
    }
}
