package nl.healthjournal.data.fhir

import nl.healthjournal.domain.metrics.model.FhirConstants
import nl.healthjournal.domain.metrics.model.FhirExportData
import nl.healthjournal.domain.metrics.model.BloodPressureEntry
import nl.healthjournal.domain.metrics.model.BloodPressureReading
import nl.healthjournal.domain.metrics.model.NhgBloodPressureCategory
import nl.healthjournal.domain.metrics.model.WeightEntry
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.model.Sex
import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.shared.model.WeightKg
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FhirBasedBundleWriterTest {

    private val profileId = ProfileId.generate()
    private val t1 = Instant.parse("2026-11-10T08:30:00Z")
    private val t2 = Instant.parse("2026-11-15T09:00:00Z")
    private val exportTime = Instant.parse("2026-11-27T16:00:00Z")
    private val zoneAmsterdam = ZoneId.of("Europe/Amsterdam") // UTC+1 in November

    private val profile = Profile.create(
        name = "Secret Patient Name",
        dateOfBirth = LocalDate.of(1990, 1, 15),
        sex = Sex.FEMALE,
        id = profileId
    )

    private val weightEntry = WeightEntry(
        id = MeasurementId.generate(),
        profileId = profileId,
        timestamp = t1,
        weight = WeightKg(BigDecimal("68.5")),
        comment = EntryComment("Morning weight before breakfast")
    )

    private val bpEntry = BloodPressureEntry(
        id = MeasurementId.generate(),
        profileId = profileId,
        timestamp = t2,
        reading = BloodPressureReading(systolic = 125, diastolic = 82, pulse = 65),
        category = NhgBloodPressureCategory.NORMAL,
        comment = EntryComment("Rested 5 min")
    )

    @Test
    fun `bundle root structure, timestamp and source marker`() {
        val data = FhirExportData(profile, listOf(weightEntry), listOf(bpEntry))
        val jsonStr = FhirBasedBundleWriter.write(
            data = data,
            appVersion = "1.7.7",
            exportTime = exportTime,
            zoneId = zoneAmsterdam
        )
        val bundle = JSONObject(jsonStr)

        assertEquals("Bundle", bundle.getString("resourceType"))
        assertEquals("collection", bundle.getString("type"))
        assertEquals("2026-11-27T17:00:00+01:00", bundle.getString("timestamp"))

        val meta = bundle.getJSONObject("meta")
        val source = meta.getString("source")
        assertTrue("Source marker contains app version", source.contains("Health Journal 1.7.7"))
        assertTrue("Source marker contains package name and version", source.contains("package nictiz.fhir.nl.r4.zib2020 0.12.1-beta.1"))

        // Ensure no claim/conformance words in source
        val bannedWords = listOf("compatible", "conforms", "certified", "interoperable", "ehr", "care")
        for (banned in bannedWords) {
            assertFalse("Source marker must not contain '$banned'", source.lowercase().contains(banned))
        }
    }

    @Test
    fun `entry order is Patient first, then Observations sorted oldest first`() {
        val data = FhirExportData(profile, listOf(weightEntry), listOf(bpEntry))
        val jsonStr = FhirBasedBundleWriter.write(
            data = data,
            appVersion = "1.7.7",
            exportTime = exportTime,
            zoneId = zoneAmsterdam
        )
        val bundle = JSONObject(jsonStr)
        val entries = bundle.getJSONArray("entry")

        // 1 patient + 1 weight + 1 bp + 1 pulse = 4 entries
        assertEquals(4, entries.length())

        val patient = entries.getJSONObject(0).getJSONObject("resource")
        assertEquals("Patient", patient.getString("resourceType"))

        val obs1 = entries.getJSONObject(1).getJSONObject("resource")
        assertEquals("Observation", obs1.getString("resourceType"))
        assertEquals("2026-11-10T09:30:00+01:00", obs1.getString("effectiveDateTime")) // t1 is weight

        val obs2 = entries.getJSONObject(2).getJSONObject("resource")
        assertEquals("Observation", obs2.getString("resourceType"))
        assertEquals("2026-11-15T10:00:00+01:00", obs2.getString("effectiveDateTime")) // t2 is BP

        val obs3 = entries.getJSONObject(3).getJSONObject("resource")
        assertEquals("Observation", obs3.getString("resourceType"))
        assertEquals("2026-11-15T10:00:00+01:00", obs3.getString("effectiveDateTime")) // t2 is pulse
    }

    @Test
    fun `weight observation has correct profile, codes, units and note`() {
        val data = FhirExportData(profile, listOf(weightEntry), emptyList())
        val jsonStr = FhirBasedBundleWriter.write(data, "1.7.7", exportTime, zoneAmsterdam)
        val bundle = JSONObject(jsonStr)
        val entries = bundle.getJSONArray("entry")
        val obs = entries.getJSONObject(1).getJSONObject("resource")

        val profiles = obs.getJSONObject("meta").getJSONArray("profile")
        assertEquals(FhirConstants.PROFILE_BODY_WEIGHT, profiles.getString(0))

        val status = obs.getString("status")
        assertEquals("final", status)

        val categoryCoding = obs.getJSONArray("category").getJSONObject(0).getJSONArray("coding").getJSONObject(0)
        assertEquals(FhirConstants.SYSTEM_OBSERVATION_CATEGORY, categoryCoding.getString("system"))
        assertEquals("vital-signs", categoryCoding.getString("code"))

        val codeCoding = obs.getJSONObject("code").getJSONArray("coding").getJSONObject(0)
        assertEquals(FhirConstants.SYSTEM_LOINC, codeCoding.getString("system"))
        assertEquals(FhirConstants.LOINC_BODY_WEIGHT, codeCoding.getString("code"))

        val vq = obs.getJSONObject("valueQuantity")
        assertEquals(68.5, vq.getDouble("value"), 0.001)
        assertEquals("kg", vq.getString("unit"))
        assertEquals(FhirConstants.SYSTEM_UCUM, vq.getString("system"))
        assertEquals("kg", vq.getString("code"))

        val note = obs.getJSONArray("note").getJSONObject(0).getString("text")
        assertEquals("Morning weight before breakfast", note)

        val idSystem = obs.getJSONArray("identifier").getJSONObject(0).getString("system")
        val idVal = obs.getJSONArray("identifier").getJSONObject(0).getString("value")
        assertEquals(FhirConstants.SYSTEM_ENTRY_ID, idSystem)
        assertEquals(weightEntry.id.value.toString(), idVal)
    }

    @Test
    fun `blood pressure observation has components, no classification, and separate unlinked pulse observation`() {
        val data = FhirExportData(profile, emptyList(), listOf(bpEntry))
        val jsonStr = FhirBasedBundleWriter.write(data, "1.7.7", exportTime, zoneAmsterdam)
        val bundle = JSONObject(jsonStr)
        val entries = bundle.getJSONArray("entry")

        val bpObs = entries.getJSONObject(1).getJSONObject("resource")
        val pulseObs = entries.getJSONObject(2).getJSONObject("resource")

        // Check BP Observation
        val bpProfiles = bpObs.getJSONObject("meta").getJSONArray("profile")
        assertEquals(FhirConstants.PROFILE_BLOOD_PRESSURE, bpProfiles.getString(0))

        val bpCode = bpObs.getJSONObject("code").getJSONArray("coding").getJSONObject(0).getString("code")
        assertEquals(FhirConstants.LOINC_BLOOD_PRESSURE, bpCode)

        val components = bpObs.getJSONArray("component")
        assertEquals(2, components.length())

        val sys = components.getJSONObject(0)
        assertEquals(FhirConstants.LOINC_SYSTOLIC, sys.getJSONObject("code").getJSONArray("coding").getJSONObject(0).getString("code"))
        assertEquals(125, sys.getJSONObject("valueQuantity").getInt("value"))
        assertEquals("mm[Hg]", sys.getJSONObject("valueQuantity").getString("unit"))

        val dia = components.getJSONObject(1)
        assertEquals(FhirConstants.LOINC_DIASTOLIC, dia.getJSONObject("code").getJSONArray("coding").getJSONObject(0).getString("code"))
        assertEquals(82, dia.getJSONObject("valueQuantity").getInt("value"))
        assertEquals("mm[Hg]", dia.getJSONObject("valueQuantity").getString("unit"))

        // Check HeartRate Observation (pulse)
        val pulseProfiles = pulseObs.getJSONObject("meta").getJSONArray("profile")
        assertEquals(FhirConstants.PROFILE_HEART_RATE, pulseProfiles.getString(0))

        val pulseCode = pulseObs.getJSONObject("code").getJSONArray("coding").getJSONObject(0).getString("code")
        assertEquals(FhirConstants.LOINC_HEART_RATE, pulseCode)

        val pulseVq = pulseObs.getJSONObject("valueQuantity")
        assertEquals(65, pulseVq.getInt("value"))
        assertEquals("/min", pulseVq.getString("unit"))
        assertEquals("/min", pulseVq.getString("code"))

        // Effective times match
        assertEquals(bpObs.getString("effectiveDateTime"), pulseObs.getString("effectiveDateTime"))

        // Pulse Observation must NOT link to BP Observation
        assertFalse(pulseObs.has("derivedFrom"))
        assertFalse(pulseObs.has("hasMember"))
        assertFalse(bpObs.has("derivedFrom"))
        assertFalse(bpObs.has("hasMember"))
    }

    @Test
    fun `idempotent export produces identical deterministic UUIDs`() {
        val data = FhirExportData(profile, listOf(weightEntry), listOf(bpEntry))
        val json1 = FhirBasedBundleWriter.write(data, "1.7.7", exportTime, zoneAmsterdam)
        val json2 = FhirBasedBundleWriter.write(data, "1.7.7", exportTime, zoneAmsterdam)

        val b1 = JSONObject(json1)
        val b2 = JSONObject(json2)

        val entries1 = b1.getJSONArray("entry")
        val entries2 = b2.getJSONArray("entry")

        for (i in 0 until entries1.length()) {
            val r1 = entries1.getJSONObject(i).getJSONObject("resource")
            val r2 = entries2.getJSONObject(i).getJSONObject("resource")
            assertEquals("Resource ID at index $i is deterministic", r1.getString("id"), r2.getString("id"))
            assertEquals("fullUrl at index $i is deterministic", entries1.getJSONObject(i).getString("fullUrl"), entries2.getJSONObject(i).getString("fullUrl"))
        }
    }

    // --- Task 1.3: Privacy & Guardrail assertions ---

    @Test
    fun `bundle never contains patient name, BSN, or un-opted demographics`() {
        val data = FhirExportData(profile, listOf(weightEntry), listOf(bpEntry))
        val jsonStr = FhirBasedBundleWriter.write(
            data = data,
            appVersion = "1.7.7",
            exportTime = exportTime,
            zoneId = zoneAmsterdam,
            includeBirthDate = false,
            includeSex = false
        )

        assertFalse("Bundle must never contain person's name", jsonStr.contains("Secret Patient Name"))
        assertFalse("Bundle must never contain BSN", jsonStr.lowercase().contains("bsn"))

        val bundle = JSONObject(jsonStr)
        val patient = bundle.getJSONArray("entry").getJSONObject(0).getJSONObject("resource")

        assertFalse("Patient has no name element", patient.has("name"))
        assertFalse("Patient has no identifier element", patient.has("identifier"))
        assertFalse("Patient has no birthDate by default", patient.has("birthDate"))
        assertFalse("Patient has no gender by default", patient.has("gender"))
    }

    @Test
    fun `opt-in demographics only include birthDate and gender, never name or identifier`() {
        val data = FhirExportData(profile, listOf(weightEntry), listOf(bpEntry))
        val jsonStr = FhirBasedBundleWriter.write(
            data = data,
            appVersion = "1.7.7",
            exportTime = exportTime,
            zoneId = zoneAmsterdam,
            includeBirthDate = true,
            includeSex = true
        )

        val bundle = JSONObject(jsonStr)
        val patient = bundle.getJSONArray("entry").getJSONObject(0).getJSONObject("resource")

        assertTrue("Patient has birthDate when opted-in", patient.has("birthDate"))
        assertEquals("1990-01-15", patient.getString("birthDate"))

        assertTrue("Patient has gender when opted-in", patient.has("gender"))
        assertEquals("female", patient.getString("gender"))

        assertFalse("Patient still has no name", patient.has("name"))
        assertFalse("Patient still has no identifier", patient.has("identifier"))
    }

    @Test
    fun `observations never contain interpretation, reference range, or foreign identifiers`() {
        val data = FhirExportData(profile, listOf(weightEntry), listOf(bpEntry))
        val jsonStr = FhirBasedBundleWriter.write(data, "1.7.7", exportTime, zoneAmsterdam)
        val bundle = JSONObject(jsonStr)
        val entries = bundle.getJSONArray("entry")

        for (i in 1 until entries.length()) {
            val obs = entries.getJSONObject(i).getJSONObject("resource")

            assertFalse("Observation must not have interpretation", obs.has("interpretation"))
            assertFalse("Observation must not have referenceRange", obs.has("referenceRange"))
            assertFalse("Observation must not have performer", obs.has("performer"))

            val identifiers = obs.getJSONArray("identifier")
            assertEquals("Observation has exactly one identifier", 1, identifiers.length())
            val ident = identifiers.getJSONObject(0)
            assertEquals("Identifier system must be urn:health-journal:entry", FhirConstants.SYSTEM_ENTRY_ID, ident.getString("system"))
        }
    }
}
