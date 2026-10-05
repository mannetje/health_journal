package nl.healthjournal.data.csv

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort

class CsvDataExportAdapter(
    private val healthLogRepository: HealthLogRepositoryPort
) : DataExportPort {

    override suspend fun exportWeightCsv(profileId: ProfileId): String {
        val history = healthLogRepository.getWeightHistory(profileId).sortedBy { it.timestamp }
        val sb = StringBuilder()
        sb.append("timestamp,weight_kg,bmi\n")
        for (entry in history) {
            val bmiStr = entry.bmi?.toPlainString() ?: ""
            sb.append("${entry.timestamp},${entry.weight.value.toPlainString()},$bmiStr\n")
        }
        return sb.toString()
    }

    override suspend fun exportBloodPressureCsv(profileId: ProfileId): String {
        val history = healthLogRepository.getBloodPressureHistory(profileId).sortedBy { it.timestamp }
        val sb = StringBuilder()
        sb.append("timestamp,systolic_mmhg,diastolic_mmhg,classification\n")
        for (entry in history) {
            sb.append("${entry.timestamp},${entry.reading.systolic},${entry.reading.diastolic},${entry.category.name}\n")
        }
        return sb.toString()
    }

    override suspend fun exportGlucoseCsv(profileId: ProfileId): String {
        val history = healthLogRepository.getGlucoseHistory(profileId).sortedBy { it.timestamp }
        val sb = StringBuilder()
        sb.append("timestamp,glucose_mmol_l,context,classification\n")
        for (entry in history) {
            sb.append("${entry.timestamp},${entry.glucose.valueInMmolL.toPlainString()},${entry.context.name},${entry.category.name}\n")
        }
        return sb.toString()
    }

    override suspend fun exportActivityCsv(profileId: ProfileId): String {
        val history = healthLogRepository.getActivityHistory(profileId).sortedBy { it.startTime }
        val sb = StringBuilder()
        sb.append("start_timestamp,end_timestamp,distance_m,duration_s\n")
        for (session in history) {
            sb.append("${session.startTime},${session.endTime},${session.distanceInMeters},${session.durationInSeconds}\n")
        }
        return sb.toString()
    }

    override suspend fun exportWaistCircumferenceCsv(profileId: ProfileId): String {
        val history = healthLogRepository.getWaistCircumferenceHistory(profileId).sortedBy { it.timestamp }
        val sb = StringBuilder()
        sb.append("timestamp,waist_cm,classification\n")
        for (entry in history) {
            val categoryStr = entry.category?.name ?: ""
            sb.append("${entry.timestamp},${entry.waist.value},$categoryStr\n")
        }
        return sb.toString()
    }
}
