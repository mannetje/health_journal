## 0. Decisions
- [x] 0.1 Technical conformance to the Nictiz zib2020 profiles where the data allows, labelled "based on FHIR" (decided 2026-11-16)
- [x] 0.2 No claim of interoperability or use in care; "use at your own risk" notice (decided 2026-11-16)
- [ ] 0.3 Spec review by the user before implementation starts
- [ ] 0.4 Open: identity defaults (generated id only; birth date and sex opt-in; never name or BSN)
- [ ] 0.5 Open: glucose in or out of the first version
- [ ] 0.6 Open: link from the pulse Observation to the blood pressure Observation
- [ ] 0.7 Open: accept `meta.profile` pointing to draft, beta profiles, with the package version stated
- [ ] 0.8 Open: validator run locally only, not in CI
- [ ] 0.9 Open: verify the EHDS text on EUR-Lex and the HL7 naming rules; final English and Dutch wording

## 1. Domain (`:domain`) and data (`:data`)
- [ ] 1.1 Read-only export port and use case (range, metrics); JVM tests
- [ ] 1.2 `FhirBasedBundleWriter` for weight, blood pressure and pulse; JVM tests for structure, codes, units, ids, offsets
- [ ] 1.3 Test that a bundle never contains a name, an identifier or an interpretation

## 2. App (`:app`)
- [ ] 2.1 Button in the History export dialog, the two identity options, the notice and the file-name suggestion
- [ ] 2.2 Strings in English and Dutch (the key parity test passes); scan test for banned phrases (care system, EHR, GP, compatible, certified, "FHIR export" without "based on")
- [ ] 2.3 Emulator pass: English and Dutch, light and dark, screenshots of the dialog

## 3. Verification
- [ ] 3.1 Run the HL7 validator with the Nictiz package over sample bundles; record the result and the package version in `docs/dev/testing.md`
- [ ] 3.2 Fix or document every error and warning

## 4. Documentation and release
- [ ] 4.1 `docs/dev/zib-mapping.md` status per exported metric; export page in `docs/dev/`; README; CHANGELOG
- [ ] 4.2 Archive the change into `openspec/specs/`; release
