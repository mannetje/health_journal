package nl.healthjournal.data.fhir

import nl.healthjournal.domain.metrics.model.FhirConstants
import nl.healthjournal.domain.metrics.model.FhirExportData
import nl.healthjournal.domain.metrics.model.BloodPressureEntry
import nl.healthjournal.domain.metrics.model.WeightEntry
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.model.Sex
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Serializes Health Journal domain readings to a FHIR R4 Bundle of type collection,
 * following the Nictiz zib2020 draft profiles (v0.12.1-beta.1) where the stored data allows.
 *
 * Privacy and compliance guarantees:
 * - Patient resource never contains a name, BSN, or identifier other than the generated id.
 * - Observations never contain an interpretation, reference range, or clinical flag.
 * - The only identifier on Observations is the entry ID under system urn:health-journal:entry.
 * - The pulse that belongs to a blood pressure entry is a separate HeartRate Observation with no link to BP.
 */
object FhirBasedBundleWriter {

    fun write(
        data: FhirExportData,
        appVersion: String,
        exportTime: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        includeBirthDate: Boolean = false,
        includeSex: Boolean = false
    ): String {
        val root = JSONObject()
        root.put("resourceType", "Bundle")
        root.put("type", "collection")
        root.put("timestamp", formatOffsetDateTime(exportTime, zoneId))

        val meta = JSONObject()
        meta.put("source", "Health Journal $appVersion, package ${FhirConstants.PACKAGE_NAME} ${FhirConstants.PACKAGE_VERSION}")
        root.put("meta", meta)

        val entryArray = JSONArray()

        // 1. Patient resource first
        val profile = data.profile
        val patientId = derivePatientId(profile)
        val patientResource = JSONObject()
        patientResource.put("resourceType", "Patient")
        patientResource.put("id", patientId)

        val patientMeta = JSONObject()
        patientMeta.put("profile", JSONArray().put(FhirConstants.PROFILE_PATIENT))
        patientResource.put("meta", patientMeta)

        if (includeBirthDate && profile != null) {
            patientResource.put("birthDate", profile.dateOfBirth.toString())
        }

        val sex = profile?.sex
        if (includeSex && sex != null) {
            val gender = when (sex) {
                Sex.MALE -> "male"
                Sex.FEMALE -> "female"
            }
            patientResource.put("gender", gender)
        }

        entryArray.put(
            JSONObject()
                .put("fullUrl", "urn:uuid:$patientId")
                .put("resource", patientResource)
        )

        // 2. Observations oldest first
        val items = mutableListOf<ExportItem>()
        data.weights.forEach { items.add(ExportItem.Weight(it)) }
        data.bloodPressures.forEach { items.add(ExportItem.BloodPressure(it)) }
        items.sortBy { it.timestamp }

        for (item in items) {
            when (item) {
                is ExportItem.Weight -> {
                    val obs = createWeightObservation(item.entry, patientId, zoneId)
                    entryArray.put(
                        JSONObject()
                            .put("fullUrl", "urn:uuid:${obs.getString("id")}")
                            .put("resource", obs)
                    )
                }
                is ExportItem.BloodPressure -> {
                    val bpObs = createBloodPressureObservation(item.entry, patientId, zoneId)
                    entryArray.put(
                        JSONObject()
                            .put("fullUrl", "urn:uuid:${bpObs.getString("id")}")
                            .put("resource", bpObs)
                    )
                    if (item.entry.reading.pulse != null) {
                        val pulseObs = createPulseObservation(item.entry, patientId, zoneId)
                        entryArray.put(
                            JSONObject()
                                .put("fullUrl", "urn:uuid:${pulseObs.getString("id")}")
                                .put("resource", pulseObs)
                        )
                    }
                }
            }
        }

        root.put("entry", entryArray)
        return root.toString(2)
    }

    private fun derivePatientId(profile: Profile?): String {
        val key = if (profile != null) "urn:health-journal:profile:${profile.id.value}" else "urn:health-journal:patient:anonymous"
        return UUID.nameUUIDFromBytes(key.toByteArray(StandardCharsets.UTF_8)).toString()
    }

    private fun formatOffsetDateTime(instant: Instant, zoneId: ZoneId): String {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(zoneId))
    }

    private fun createWeightObservation(
        entry: WeightEntry,
        patientId: String,
        zoneId: ZoneId
    ): JSONObject {
        val obsId = UUID.nameUUIDFromBytes("urn:health-journal:entry:${entry.id.value}".toByteArray(StandardCharsets.UTF_8)).toString()
        val obs = JSONObject()
        obs.put("resourceType", "Observation")
        obs.put("id", obsId)

        val meta = JSONObject()
        meta.put("profile", JSONArray().put(FhirConstants.PROFILE_BODY_WEIGHT))
        obs.put("meta", meta)

        obs.put("identifier", JSONArray().put(
            JSONObject()
                .put("system", FhirConstants.SYSTEM_ENTRY_ID)
                .put("value", entry.id.value.toString())
        ))

        obs.put("status", FhirConstants.STATUS_FINAL)

        obs.put("category", JSONArray().put(
            JSONObject().put("coding", JSONArray().put(
                JSONObject()
                    .put("system", FhirConstants.SYSTEM_OBSERVATION_CATEGORY)
                    .put("code", FhirConstants.CATEGORY_VITAL_SIGNS)
                    .put("display", FhirConstants.CATEGORY_VITAL_SIGNS_DISPLAY)
            ))
        ))

        obs.put("code", JSONObject().put("coding", JSONArray().put(
            JSONObject()
                .put("system", FhirConstants.SYSTEM_LOINC)
                .put("code", FhirConstants.LOINC_BODY_WEIGHT)
                .put("display", FhirConstants.LOINC_BODY_WEIGHT_DISPLAY)
        )))

        obs.put("subject", JSONObject().put("reference", "urn:uuid:$patientId"))
        obs.put("effectiveDateTime", formatOffsetDateTime(entry.timestamp, zoneId))

        obs.put("valueQuantity", JSONObject()
            .put("value", entry.weight.value.toDouble())
            .put("unit", FhirConstants.UNIT_KG)
            .put("system", FhirConstants.SYSTEM_UCUM)
            .put("code", FhirConstants.UNIT_KG)
        )

        entry.comment?.text?.takeIf { it.isNotBlank() }?.let { commentText ->
            obs.put("note", JSONArray().put(JSONObject().put("text", commentText)))
        }

        return obs
    }

    private fun createBloodPressureObservation(
        entry: BloodPressureEntry,
        patientId: String,
        zoneId: ZoneId
    ): JSONObject {
        val obsId = UUID.nameUUIDFromBytes("urn:health-journal:entry:${entry.id.value}".toByteArray(StandardCharsets.UTF_8)).toString()
        val obs = JSONObject()
        obs.put("resourceType", "Observation")
        obs.put("id", obsId)

        val meta = JSONObject()
        meta.put("profile", JSONArray().put(FhirConstants.PROFILE_BLOOD_PRESSURE))
        obs.put("meta", meta)

        obs.put("identifier", JSONArray().put(
            JSONObject()
                .put("system", FhirConstants.SYSTEM_ENTRY_ID)
                .put("value", entry.id.value.toString())
        ))

        obs.put("status", FhirConstants.STATUS_FINAL)

        obs.put("category", JSONArray().put(
            JSONObject().put("coding", JSONArray().put(
                JSONObject()
                    .put("system", FhirConstants.SYSTEM_OBSERVATION_CATEGORY)
                    .put("code", FhirConstants.CATEGORY_VITAL_SIGNS)
                    .put("display", FhirConstants.CATEGORY_VITAL_SIGNS_DISPLAY)
            ))
        ))

        obs.put("code", JSONObject().put("coding", JSONArray().put(
            JSONObject()
                .put("system", FhirConstants.SYSTEM_LOINC)
                .put("code", FhirConstants.LOINC_BLOOD_PRESSURE)
                .put("display", FhirConstants.LOINC_BLOOD_PRESSURE_DISPLAY)
        )))

        obs.put("subject", JSONObject().put("reference", "urn:uuid:$patientId"))
        obs.put("effectiveDateTime", formatOffsetDateTime(entry.timestamp, zoneId))

        val components = JSONArray()
        components.put(
            JSONObject()
                .put("code", JSONObject().put("coding", JSONArray().put(
                    JSONObject()
                        .put("system", FhirConstants.SYSTEM_LOINC)
                        .put("code", FhirConstants.LOINC_SYSTOLIC)
                        .put("display", FhirConstants.LOINC_SYSTOLIC_DISPLAY)
                )))
                .put("valueQuantity", JSONObject()
                    .put("value", entry.reading.systolic)
                    .put("unit", FhirConstants.UNIT_MMHG)
                    .put("system", FhirConstants.SYSTEM_UCUM)
                    .put("code", FhirConstants.UNIT_MMHG)
                )
        )
        components.put(
            JSONObject()
                .put("code", JSONObject().put("coding", JSONArray().put(
                    JSONObject()
                        .put("system", FhirConstants.SYSTEM_LOINC)
                        .put("code", FhirConstants.LOINC_DIASTOLIC)
                        .put("display", FhirConstants.LOINC_DIASTOLIC_DISPLAY)
                )))
                .put("valueQuantity", JSONObject()
                    .put("value", entry.reading.diastolic)
                    .put("unit", FhirConstants.UNIT_MMHG)
                    .put("system", FhirConstants.SYSTEM_UCUM)
                    .put("code", FhirConstants.UNIT_MMHG)
                )
        )
        obs.put("component", components)

        entry.comment?.text?.takeIf { it.isNotBlank() }?.let { commentText ->
            obs.put("note", JSONArray().put(JSONObject().put("text", commentText)))
        }

        return obs
    }

    private fun createPulseObservation(
        entry: BloodPressureEntry,
        patientId: String,
        zoneId: ZoneId
    ): JSONObject {
        val obsId = UUID.nameUUIDFromBytes("urn:health-journal:entry:${entry.id.value}:pulse".toByteArray(StandardCharsets.UTF_8)).toString()
        val obs = JSONObject()
        obs.put("resourceType", "Observation")
        obs.put("id", obsId)

        val meta = JSONObject()
        meta.put("profile", JSONArray().put(FhirConstants.PROFILE_HEART_RATE))
        obs.put("meta", meta)

        obs.put("identifier", JSONArray().put(
            JSONObject()
                .put("system", FhirConstants.SYSTEM_ENTRY_ID)
                .put("value", entry.id.value.toString())
        ))

        obs.put("status", FhirConstants.STATUS_FINAL)

        obs.put("category", JSONArray().put(
            JSONObject().put("coding", JSONArray().put(
                JSONObject()
                    .put("system", FhirConstants.SYSTEM_OBSERVATION_CATEGORY)
                    .put("code", FhirConstants.CATEGORY_VITAL_SIGNS)
                    .put("display", FhirConstants.CATEGORY_VITAL_SIGNS_DISPLAY)
            ))
        ))

        obs.put("code", JSONObject().put("coding", JSONArray().put(
            JSONObject()
                .put("system", FhirConstants.SYSTEM_LOINC)
                .put("code", FhirConstants.LOINC_HEART_RATE)
                .put("display", FhirConstants.LOINC_HEART_RATE_DISPLAY)
        )))

        obs.put("subject", JSONObject().put("reference", "urn:uuid:$patientId"))
        obs.put("effectiveDateTime", formatOffsetDateTime(entry.timestamp, zoneId))

        obs.put("valueQuantity", JSONObject()
            .put("value", entry.reading.pulse!!)
            .put("unit", FhirConstants.UNIT_PER_MIN)
            .put("system", FhirConstants.SYSTEM_UCUM)
            .put("code", FhirConstants.UNIT_PER_MIN)
        )

        entry.comment?.text?.takeIf { it.isNotBlank() }?.let { commentText ->
            obs.put("note", JSONArray().put(JSONObject().put("text", commentText)))
        }

        return obs
    }

    private sealed interface ExportItem {
        val timestamp: Instant
        data class Weight(val entry: WeightEntry) : ExportItem {
            override val timestamp: Instant get() = entry.timestamp
        }
        data class BloodPressure(val entry: BloodPressureEntry) : ExportItem {
            override val timestamp: Instant get() = entry.timestamp
        }
    }
}
