## 0. Decisions
- [x] 0.1 Technical conformance to the Nictiz zib2020 profiles where the data allows, labelled "based on FHIR" (decided 2026-11-16)
- [x] 0.2 No claim of interoperability or use in care; "use at your own risk" notice (decided 2026-11-16)
- [x] 0.3 Scope and wording agreed by the user (2026-11-21); the specs get a final review before implementation starts
- [x] 0.4 Identity defaults: generated id only; birth date and sex opt-in; never name or BSN (agreed 2026-11-21)
- [x] 0.5 Glucose is out of the first version (agreed 2026-11-21)
- [x] 0.6 No link from the pulse Observation to the blood pressure Observation (agreed 2026-11-21)
- [x] 0.7 `meta.profile` points to the draft, beta profiles, with the package version stated (agreed 2026-11-21)
- [x] 0.8 Validator run locally only, not in CI (agreed 2026-11-21)
- [x] 0.9 EHDS text verified on EUR-Lex and the HL7 trademark rules read on hl7.org (2026-11-16); see the proposal Sources
- [x] 0.9a Wording "based on FHIR" / "op FHIR gebaseerd" and trademark notice agreed (2026-11-21); final strings are written with the implementation, including the HL7 trademark sentence in the README and the docs
- [x] 0.10 The notice also states that the person is responsible for the data entered and exported and the makers are not (requested 2026-11-16)
- [ ] 0.11 Open: whether the responsibility sentence should also appear once for the whole app in the Profile notice (separate change)

## 1. Domain (`:domain`) and data (`:data`)
- [x] 1.0 Check whether entries store a zone or offset or only an instant, and write the offset rule in the docs accordingly
- [x] 1.1 Read-only export port and use case (range, metrics); JVM tests
- [x] 1.2 `FhirBasedBundleWriter` for weight, blood pressure and pulse; JVM tests for structure, codes, units, ids, offsets
- [x] 1.3 Test that a bundle never contains a name, an interpretation or any identifier other than the entry identifier on each Observation

## 2. App (`:app`)
- [ ] 2.1 Button in the History export dialog, the range choice, the two identity options, the short notice with an accessible "More" expander for the full notice, and the file-name suggestion
- [x] 2.1a Source marker and `timestamp` in the bundle; test asserts both and the absence of conformance wording
- [ ] 2.2 Strings in English and Dutch (the key parity test passes); scan test for banned phrases (care system, EHR, GP, compatible, certified, "FHIR export" without "based on")
- [ ] 2.3 Emulator pass: English and Dutch, light and dark, screenshots of the dialog

## 3. Verification
- [ ] 3.1 Run the HL7 validator with the Nictiz package over sample bundles; record the result and the package version in `docs/dev/testing.md`
- [ ] 3.2 Fix or document every error and warning

## 4. Documentation and release
- [ ] 4.1 `docs/dev/zib-mapping.md` status per exported metric; export page in `docs/dev/`; README; CHANGELOG
- [ ] 4.2 Archive the change into `openspec/specs/`; release
