package nl.healthjournal.data.local.mapper

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
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The rows that make up one medication. */
data class MedicationRows(
    val medication: MedicationEntity,
    val schedules: List<MedicationScheduleEntity>,
    val times: List<MedicationTimeEntity>
)

object MedicationMapper {

    fun toRows(domain: Medication): MedicationRows {
        val id = domain.id.value.toString()
        val schedules = mutableListOf<MedicationScheduleEntity>()
        val times = mutableListOf<MedicationTimeEntity>()
        for (version in domain.schedules) {
            val from = version.effectiveFrom.toString()
            when (val schedule = version.schedule) {
                Schedule.AsNeeded -> schedules += MedicationScheduleEntity(id, from, true, null, null, null, null)
                is Schedule.Recurring -> {
                    val pattern = schedule.days
                    schedules += MedicationScheduleEntity(
                        medicationId = id,
                        effectiveFrom = from,
                        asNeeded = false,
                        daysOfWeek = (pattern as? DayPattern.Weekdays)?.days?.sorted()?.joinToString("|") { it.name.take(3) },
                        intervalDays = (pattern as? DayPattern.EveryNDays)?.interval,
                        startDate = schedule.start.toString(),
                        endDate = schedule.end?.toString()
                    )
                    schedule.times.forEach { times += MedicationTimeEntity(id, from, it.toString()) }
                }
            }
        }
        val entity = MedicationEntity(
            id = id,
            profileId = domain.profileId.value.toString(),
            name = domain.name.value,
            form = domain.form?.name,
            strengthAmount = domain.dosage.strength?.amount?.toPlainString(),
            strengthUnit = domain.dosage.strength?.unit?.name,
            doseAmount = domain.dosage.amountPerIntake.toPlainString(),
            doseUnit = domain.dosage.doseUnit.name,
            color = domain.appearance?.color?.name,
            shape = domain.appearance?.shape?.name,
            comment = domain.comment?.text,
            archivedFrom = domain.archivedFrom?.toString()
        )
        return MedicationRows(entity, schedules, times)
    }

    fun toDomain(
        entity: MedicationEntity,
        schedules: List<MedicationScheduleEntity>,
        times: List<MedicationTimeEntity>
    ): Medication {
        val versions = schedules.sortedBy { it.effectiveFrom }.map { row ->
            val schedule = if (row.asNeeded) {
                Schedule.AsNeeded
            } else {
                val pattern = row.intervalDays?.let { DayPattern.EveryNDays(it) }
                    ?: DayPattern.Weekdays(row.daysOfWeek.orEmpty().split("|").filter { it.isNotEmpty() }.map(::parseDay).toSet())
                Schedule.Recurring.of(
                    times = times.filter { it.effectiveFrom == row.effectiveFrom }.map { LocalTime.parse(it.localTime) },
                    days = pattern,
                    start = LocalDate.parse(requireNotNull(row.startDate)),
                    end = row.endDate?.let(LocalDate::parse)
                )
            }
            ScheduleVersion(LocalDate.parse(row.effectiveFrom), schedule)
        }
        val strength = if (entity.strengthAmount != null && entity.strengthUnit != null) {
            Strength(BigDecimal(entity.strengthAmount), StrengthUnit.valueOf(entity.strengthUnit))
        } else {
            null
        }
        val appearance = if (entity.color != null && entity.shape != null) {
            PillAppearance(PillColor.valueOf(entity.color), PillShape.valueOf(entity.shape))
        } else {
            null
        }
        return Medication(
            id = MedicationId.fromString(entity.id),
            profileId = ProfileId.fromString(entity.profileId),
            name = MedicationName(entity.name),
            form = entity.form?.let { MedicationForm.valueOf(it) },
            dosage = Dosage(strength, BigDecimal(entity.doseAmount), DoseUnit.valueOf(entity.doseUnit)),
            appearance = appearance,
            schedules = versions,
            comment = EntryComment.ofOrNull(entity.comment),
            archivedFrom = entity.archivedFrom?.let(LocalDate::parse)
        )
    }

    fun toEntity(domain: Intake): IntakeEntity = IntakeEntity(
        id = domain.id.value.toString(),
        medicationId = domain.medicationId.value.toString(),
        planned = domain.planned?.toString(),
        status = domain.status.name,
        takenAt = domain.takenAt?.toEpochMilli(),
        actualAmount = domain.actualAmount?.toPlainString(),
        comment = domain.comment?.text
    )

    fun toDomain(entity: IntakeEntity): Intake = Intake(
        id = IntakeId.fromString(entity.id),
        medicationId = MedicationId.fromString(entity.medicationId),
        planned = entity.planned?.let(LocalDateTime::parse),
        status = IntakeStatus.valueOf(entity.status),
        takenAt = entity.takenAt?.let(Instant::ofEpochMilli),
        actualAmount = entity.actualAmount?.let(::BigDecimal),
        comment = EntryComment.ofOrNull(entity.comment)
    )

    private fun parseDay(code: String): DayOfWeek =
        DayOfWeek.values().first { it.name.startsWith(code) }
}
