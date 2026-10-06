package nl.healthjournal.data.csv

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.port.secondary.MedicationCsvFiles
import nl.healthjournal.domain.port.secondary.MedicationExportPort
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort

internal const val MEDICATIONS_HEADER =
    "ref,name,form,strength_amount,strength_unit,dose_amount,dose_unit,as_needed,colour,shape,comment,archived"
internal const val SCHEDULES_HEADER = "ref,effective_from,local_time,days_of_week,interval_days,start_date,end_date"
internal const val INTAKES_HEADER = "ref,planned_local_datetime,status,actual_timestamp,actual_amount,comment"

/**
 * Writes medications, schedule versions and the intake log as three CSV files. Enum names, never translated
 * labels; amounts exactly as entered; planned times without a zone. `ref` is `1..n` and only links the files.
 */
class CsvMedicationExportAdapter(
    private val medicationRepository: MedicationRepositoryPort
) : MedicationExportPort {

    override suspend fun exportMedicationCsv(profileId: ProfileId): MedicationCsvFiles {
        val medications = medicationRepository.getMedications(profileId, includeArchived = true)
            .sortedWith(compareBy({ it.name.value.lowercase() }, { it.id.toString() }))
        val refs = medications.withIndex().associate { (i, m) -> m.id to (i + 1).toString() }

        val meds = StringBuilder(MEDICATIONS_HEADER).append('\n')
        val schedules = StringBuilder(SCHEDULES_HEADER).append('\n')
        for (m in medications) {
            val ref = refs.getValue(m.id)
            meds.append(
                listOf(
                    ref,
                    CsvQuoting.cell(m.name.value),
                    m.form?.name.orEmpty(),
                    m.dosage.strength?.amount?.toPlainString().orEmpty(),
                    m.dosage.strength?.unit?.name.orEmpty(),
                    m.dosage.amountPerIntake.toPlainString(),
                    m.dosage.doseUnit.name,
                    m.isAsNeeded.toString(),
                    m.appearance?.color?.name.orEmpty(),
                    m.appearance?.shape?.name.orEmpty(),
                    CsvQuoting.cell(m.comment?.text),
                    m.archivedFrom?.toString().orEmpty()
                ).joinToString(",")
            ).append('\n')

            for (version in m.schedules) {
                val from = version.effectiveFrom.toString()
                when (val schedule = version.schedule) {
                    // A version without daily times (as needed) is one row with an empty local_time.
                    Schedule.AsNeeded -> schedules.append("$ref,$from,,,,,\n")
                    is Schedule.Recurring -> {
                        val days = (schedule.days as? DayPattern.Weekdays)?.days?.sorted()
                            ?.joinToString("|") { it.name.take(3) }.orEmpty()
                        val interval = (schedule.days as? DayPattern.EveryNDays)?.interval?.toString().orEmpty()
                        for (time in schedule.times) {
                            schedules.append("$ref,$from,$time,$days,$interval,${schedule.start},${schedule.end ?: ""}\n")
                        }
                    }
                }
            }
        }

        val intakes = StringBuilder(INTAKES_HEADER).append('\n')
        medicationRepository.getAllIntakes(profileId)
            .filter { it.medicationId in refs }
            .sortedWith(compareBy({ refs.getValue(it.medicationId).toInt() }, { it.planned ?: java.time.LocalDateTime.MIN }, { it.takenAt }))
            .forEach {
                val status = if (it.planned == null) "AS_NEEDED" else it.status.name
                intakes.append(
                    "${refs.getValue(it.medicationId)},${it.planned ?: ""},$status,${it.takenAt ?: ""}," +
                        "${it.actualAmount?.toPlainString().orEmpty()},${CsvQuoting.cell(it.comment?.text)}\n"
                )
            }

        return MedicationCsvFiles(meds.toString(), schedules.toString(), intakes.toString())
    }
}
