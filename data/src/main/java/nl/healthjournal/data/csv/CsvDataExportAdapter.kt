package nl.healthjournal.data.csv

import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.port.DataExportPort
import nl.healthjournal.domain.metrics.port.WeightRepositoryPort
import nl.healthjournal.domain.metrics.port.BloodPressureRepositoryPort
import nl.healthjournal.domain.metrics.port.GlucoseRepositoryPort
import nl.healthjournal.domain.metrics.port.ActivityRepositoryPort
import nl.healthjournal.domain.metrics.port.WaistCircumferenceRepositoryPort
import nl.healthjournal.domain.metrics.port.OxygenSaturationRepositoryPort

class CsvDataExportAdapter(
    private val weightRepository: WeightRepositoryPort,
    private val bloodPressureRepository: BloodPressureRepositoryPort,
    private val glucoseRepository: GlucoseRepositoryPort,
    private val activityRepository: ActivityRepositoryPort,
    private val waistCircumferenceRepository: WaistCircumferenceRepositoryPort,
    private val oxygenSaturationRepository: OxygenSaturationRepositoryPort
) : DataExportPort {

    override suspend fun exportWeightCsv(profileId: ProfileId): String {
        val history = weightRepository.getWeightHistory(profileId).sortedBy { it.timestamp }
        val sb = StringBuilder()
        sb.append("timestamp,weight_kg,bmi,comment\n")
        for (entry in history) {
            val bmiStr = entry.bmi?.toPlainString() ?: ""
            sb.append("${entry.timestamp},${entry.weight.value.toPlainString()},$bmiStr,${CsvQuoting.cell(entry.comment?.text)}\n")
        }
        return sb.toString()
    }

    override suspend fun exportBloodPressureCsv(profileId: ProfileId): String {
        val history = bloodPressureRepository.getBloodPressureHistory(profileId).sortedBy { it.timestamp }
        val sb = StringBuilder()
        sb.append("timestamp,systolic_mmhg,diastolic_mmhg,pulse_bpm,classification,comment\n")
        for (entry in history) {
            val pulseStr = entry.reading.pulse?.toString() ?: ""
            sb.append("${entry.timestamp},${entry.reading.systolic},${entry.reading.diastolic},$pulseStr,${entry.category.name},${CsvQuoting.cell(entry.comment?.text)}\n")
        }
        return sb.toString()
    }

    override suspend fun exportGlucoseCsv(profileId: ProfileId): String {
        val history = glucoseRepository.getGlucoseHistory(profileId).sortedBy { it.timestamp }
        val sb = StringBuilder()
        sb.append("timestamp,glucose_mmol_l,context,classification,comment\n")
        for (entry in history) {
            sb.append("${entry.timestamp},${entry.glucose.valueInMmolL.toPlainString()},${entry.context.name},${entry.category.name},${CsvQuoting.cell(entry.comment?.text)}\n")
        }
        return sb.toString()
    }

    override suspend fun exportActivityCsv(profileId: ProfileId): String {
        val history = activityRepository.getActivityHistory(profileId).sortedBy { it.startTime }
        val sb = StringBuilder()
        sb.append("start_timestamp,end_timestamp,distance_m,duration_s,comment\n")
        for (session in history) {
            sb.append("${session.startTime},${session.endTime},${session.distanceInMeters},${session.durationInSeconds},${CsvQuoting.cell(session.comment?.text)}\n")
        }
        return sb.toString()
    }

    override suspend fun exportWaistCircumferenceCsv(profileId: ProfileId): String {
        val history = waistCircumferenceRepository.getWaistCircumferenceHistory(profileId).sortedBy { it.timestamp }
        val sb = StringBuilder()
        sb.append("timestamp,waist_cm,classification,comment\n")
        for (entry in history) {
            val categoryStr = entry.category?.name ?: ""
            sb.append("${entry.timestamp},${entry.waist.value},$categoryStr,${CsvQuoting.cell(entry.comment?.text)}\n")
        }
        return sb.toString()
    }

    override suspend fun exportOxygenSaturationCsv(profileId: ProfileId): String {
        val history = oxygenSaturationRepository.getOxygenSaturationHistory(profileId).sortedBy { it.timestamp }
        val sb = StringBuilder()
        sb.append("timestamp,spo2_percent,pulse_bpm,context,site,oxygen_given,comment\n")
        for (entry in history) {
            val pulseStr = entry.pulse?.toString() ?: ""
            sb.append("${entry.timestamp},${entry.saturation.value},$pulseStr,${entry.context?.name ?: ""},${entry.site?.name ?: ""},${entry.oxygenGiven},${CsvQuoting.cell(entry.comment?.text)}\n")
        }
        return sb.toString()
    }
}
