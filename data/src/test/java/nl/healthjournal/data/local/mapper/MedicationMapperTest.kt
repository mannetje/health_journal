package nl.healthjournal.data.local.mapper

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Placeholder names only ("Medication A"). */
class MedicationMapperTest {

    private fun medication(vararg versions: ScheduleVersion, strength: Strength? = Strength(BigDecimal("2.50"), StrengthUnit.MG)) =
        Medication(
            id = MedicationId.generate(),
            profileId = ProfileId.generate(),
            name = MedicationName.of("Medication A"),
            form = MedicationForm.TABLET,
            dosage = Dosage(strength, BigDecimal("1.5"), DoseUnit.TABLETS),
            appearance = PillAppearance(PillColor.BLUE, PillShape.OVAL),
            schedules = versions.toList(),
            comment = EntryComment("with food"),
            archivedFrom = LocalDate.of(2026, 12, 1)
        )

    private fun roundTrip(medication: Medication): Medication {
        val rows = MedicationMapper.toRows(medication)
        return MedicationMapper.toDomain(rows.medication, rows.schedules, rows.times)
    }

    @Test
    fun `weekday schedule with several times round trips`() {
        val m = medication(
            ScheduleVersion(
                LocalDate.of(2026, 9, 1),
                Schedule.Recurring.of(
                    listOf(LocalTime.of(20, 0), LocalTime.of(8, 0)),
                    DayPattern.Weekdays(setOf(DayOfWeek.THURSDAY, DayOfWeek.MONDAY)),
                    LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31)
                )
            )
        )
        assertEquals(m, roundTrip(m))
    }

    @Test
    fun `interval schedule and amount scale round trip`() {
        val m = medication(
            ScheduleVersion(LocalDate.of(2026, 9, 1), Schedule.Recurring.of(listOf(LocalTime.of(9, 0)), DayPattern.EveryNDays(7), LocalDate.of(2026, 9, 1)))
        )
        val back = roundTrip(m)
        assertEquals(m, back)
        assertEquals(BigDecimal("2.50"), back.dosage.strength!!.amount)
    }

    @Test
    fun `schedule versions keep their own times and as-needed`() {
        val m = medication(
            ScheduleVersion(LocalDate.of(2026, 9, 1), Schedule.Recurring.of(listOf(LocalTime.of(8, 0)), DayPattern.EVERY_DAY, LocalDate.of(2026, 9, 1))),
            ScheduleVersion(LocalDate.of(2026, 10, 1), Schedule.Recurring.of(listOf(LocalTime.of(7, 0), LocalTime.of(19, 0)), DayPattern.EVERY_DAY, LocalDate.of(2026, 10, 1))),
            ScheduleVersion(LocalDate.of(2026, 11, 1), Schedule.AsNeeded)
        )
        val rows = MedicationMapper.toRows(m)
        assertEquals(3, rows.schedules.size)
        assertEquals(3, rows.times.size)
        assertEquals(m, roundTrip(m))
    }

    @Test
    fun `missing strength and appearance stay null`() {
        val m = medication(ScheduleVersion(LocalDate.of(2026, 9, 1), Schedule.AsNeeded), strength = null)
            .copy(appearance = null, form = null, comment = null, archivedFrom = null)
        val back = roundTrip(m)
        assertEquals(m, back)
        assertNull(back.dosage.strength)
        assertNull(back.appearance)
    }

    @Test
    fun `planned and as-needed intakes round trip`() {
        val medicationId = MedicationId.generate()
        val planned = Intake(
            IntakeId.generate(), medicationId, LocalDateTime.of(2026, 9, 1, 8, 0), IntakeStatus.TAKEN,
            Instant.ofEpochMilli(1_788_000_000_000), BigDecimal("1.0"), EntryComment("left arm")
        )
        val skipped = Intake(IntakeId.generate(), medicationId, LocalDateTime.of(2026, 9, 1, 20, 0), IntakeStatus.SKIPPED, null)
        val asNeeded = Intake(IntakeId.generate(), medicationId, null, IntakeStatus.TAKEN, Instant.ofEpochMilli(1_788_000_100_000))

        listOf(planned, skipped, asNeeded).forEach {
            assertEquals(it, MedicationMapper.toDomain(MedicationMapper.toEntity(it)))
        }
        assertNull(MedicationMapper.toEntity(asNeeded).planned)
    }
}
