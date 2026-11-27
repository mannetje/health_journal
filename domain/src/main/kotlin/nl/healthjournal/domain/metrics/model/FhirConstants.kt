package nl.healthjournal.domain.metrics.model

/**
 * Shared constants for FHIR R4 resources shaped by the Dutch zib2020 profiles.
 * Used by both export writer and import reader to ensure consistency.
 */
object FhirConstants {
    /** Application identifier system for idempotent re-import */
    const val SYSTEM_ENTRY_ID = "urn:health-journal:entry"

    /** Standard coding systems */
    const val SYSTEM_LOINC = "http://loinc.org"
    const val SYSTEM_UCUM = "http://unitsofmeasure.org"
    const val SYSTEM_OBSERVATION_CATEGORY = "http://terminology.hl7.org/CodeSystem/observation-category"
    const val SYSTEM_ADMINISTRATIVE_GENDER = "http://hl7.org/fhir/administrative-gender"

    /** Observation category */
    const val CATEGORY_VITAL_SIGNS = "vital-signs"
    const val CATEGORY_VITAL_SIGNS_DISPLAY = "Vital Signs"

    /** Observation status */
    const val STATUS_FINAL = "final"

    /** Nictiz zib2020 package version */
    const val PACKAGE_VERSION = "0.12.1-beta.1"
    const val PACKAGE_NAME = "nictiz.fhir.nl.r4.zib2020"

    /** Nictiz draft profiles */
    const val PROFILE_BODY_WEIGHT = "http://nictiz.nl/fhir/StructureDefinition/zib-BodyWeight"
    const val PROFILE_BLOOD_PRESSURE = "http://nictiz.nl/fhir/StructureDefinition/zib-BloodPressure"
    const val PROFILE_HEART_RATE = "http://nictiz.nl/fhir/StructureDefinition/zib-HeartRate"
    const val PROFILE_PATIENT = "http://nictiz.nl/fhir/StructureDefinition/zib-Patient"

    /** LOINC codes */
    const val LOINC_BODY_WEIGHT = "29463-7"
    const val LOINC_BODY_WEIGHT_DISPLAY = "Body weight"

    const val LOINC_BLOOD_PRESSURE = "85354-9"
    const val LOINC_BLOOD_PRESSURE_DISPLAY = "Blood pressure panel with all children optional"

    const val LOINC_SYSTOLIC = "8480-6"
    const val LOINC_SYSTOLIC_DISPLAY = "Systolic blood pressure"

    const val LOINC_DIASTOLIC = "8462-4"
    const val LOINC_DIASTOLIC_DISPLAY = "Diastolic blood pressure"

    const val LOINC_HEART_RATE = "8867-4"
    const val LOINC_HEART_RATE_DISPLAY = "Heart rate"

    /** Units of measure */
    const val UNIT_KG = "kg"
    const val UNIT_MMHG = "mm[Hg]"
    const val UNIT_PER_MIN = "/min"
}
