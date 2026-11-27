# FHIR-based Export

This document describes the FHIR-based export feature of Health Journal (`add-fhir-based-export`).

## Overview

Health Journal offers an export option in the History export dialog: **"Export (based on FHIR)"** ("Exporteren (op FHIR gebaseerd)").
It exports weight, blood pressure, and pulse readings as a single JSON file formatted as a FHIR R4 `Bundle` of type `collection`.

> **Notice:** The export is based on FHIR and the Dutch zib (Zorginformatiebouwstenen) formats. It has not been checked or approved by anyone, is not intended for clinical use, and is not encrypted. It is used at the person's own risk. The person is responsible for the data entered and exported; the makers of the app are not responsible for the data or for how the file is used.
>
> HL7, FHIR and the FHIR [FLAME DESIGN] are the registered trademarks of Health Level Seven International and their use does not constitute endorsement by HL7.

## Offset and Timestamp Rule (Task 1.0)

Health Journal entries store timestamps as UTC `Instant` (persisted as UTC epoch milliseconds in SQLite via Room). No time zone identifier or offset is stored with individual entries in the database.

When exporting entries to FHIR JSON:
- The `effectiveDateTime` of each `Observation` is formatted in ISO-8601 with the device's current zone offset at export time (e.g., `2026-11-27T16:40:00+01:00`).
- If a person recorded an entry in one time zone and later exports the data in another time zone, the exported timestamp reflects the current device zone offset at the moment of export.
- The `Bundle.timestamp` reflects the UTC export time formatted with the device's current zone offset.

## Target Profiles

Profiles follow Nictiz package `nictiz.fhir.nl.r4.zib2020` version `0.12.1-beta.1`:
- Weight: `http://nictiz.nl/fhir/StructureDefinition/zib-BodyWeight` (LOINC `29463-7`, UCUM `kg`)
- Blood pressure: `http://nictiz.nl/fhir/StructureDefinition/zib-BloodPressure` (LOINC `85354-9`, systolic `8480-6`, diastolic `8462-4`, UCUM `mm[Hg]`)
- Pulse: `http://nictiz.nl/fhir/StructureDefinition/zib-HeartRate` (LOINC `8867-4`, UCUM `/min`)
- Patient: `http://nictiz.nl/fhir/StructureDefinition/zib-Patient`

## Identity & Privacy Rules

1. **Default:** Minimal `Patient` resource containing only a generated UUID identifier. No name, no BSN, no national identifier.
2. **Opt-in demographics:** Date of birth and administrative sex can be included only if explicitly checked by the user in the export dialog.
3. **No Clinical Interpretations:** No `interpretation`, `referenceRange`, or flags are written.
4. **Traceable ID:** Each `Observation` includes an `identifier` with system `urn:health-journal:entry` containing the measurement UUID to allow idempotent re-import.
