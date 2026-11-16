## 0. Decisions
- [x] 0.1 Separate change from the export; first version reads only the app's own file; skipped entries are never overwritten (decided 2026-11-16)
- [ ] 0.2 Spec review by the user before implementation starts; requires `add-fhir-based-export` agreed first
- [ ] 0.3 Open: both source marker and entry identifier required to recognise the file
- [ ] 0.4 Open: preview shows counts only
- [ ] 0.5 Open: file size and entry limits (proposed 20 MB, 100,000 entries)
- [ ] 0.6 Open: unmatched pulse Observation is skipped
- [ ] 0.7 Open: final English and Dutch wording

## 1. Domain (`:domain`) and data (`:data`)
- [ ] 1.1 Import port and use case: preview, then merge in one transaction; JVM tests
- [ ] 1.2 `FhirBasedBundleReader` with shared constants with the writer; JVM tests for round trip, twice, no overwrite, bad units, foreign file, size limit, Patient ignored
- [ ] 1.3 Malformed and truncated input test: nothing written, no crash

## 2. App (`:app`)
- [ ] 2.1 Import button, document picker, preview dialog with counts and confirmation, result message, notice
- [ ] 2.2 Strings in English and Dutch (key parity test); the banned-phrase scan covers them
- [ ] 2.3 Emulator pass: English and Dutch, light and dark, screenshots

## 3. Documentation and release
- [ ] 3.1 `docs/dev/` import page, `docs/dev/zib-mapping.md`, README, CHANGELOG
- [ ] 3.2 Archive the change into `openspec/specs/`; release
