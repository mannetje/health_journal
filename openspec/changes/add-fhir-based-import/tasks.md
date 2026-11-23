## 0. Decisions
- [x] 0.1 Separate change from the export; skipped entries are never overwritten (decided 2026-11-16)
- [x] 0.1a Files from other tools are read too, for the same three readings, with time and value matching (decided 2026-11-21, replaces "own file only")
- [x] 0.2 Scope and wording agreed by the user (2026-11-21); the specs get a final review before implementation starts; requires `add-fhir-based-export` built first
- [x] 0.3 Own file recognised by source marker plus entry identifier; any other Bundle is read as a foreign file (agreed 2026-11-21)
- [x] 0.4 Preview shows counts only (agreed 2026-11-21)
- [x] 0.5 Limits 20 MB and 100,000 entries (agreed 2026-11-21)
- [x] 0.6 Unmatched pulse Observation is skipped (agreed 2026-11-21)
- [x] 0.7 English and Dutch strings approved by the owner (2026-11-23, with plural resources and "metingen" throughout)
- [ ] 0.8 Review the approved strings against the compliance spec before the foreign-file part is built (no named source, no interoperability claim)

## 1. Domain (`:domain`) and data (`:data`)
- [ ] 1.1 Import port and use case: preview, then merge in one transaction; JVM tests
- [ ] 1.2 `FhirBasedBundleReader` with shared constants with the writer; JVM tests for round trip, twice, no overwrite, bad units, file from another tool (read, twice adds nothing, same time other value added), file without readings, size limit, Patient ignored
- [ ] 1.3 Malformed and truncated input test: nothing written, no crash

## 2. App (`:app`)
- [ ] 2.1 Import button, document picker, preview dialog with counts and confirmation, result message, notice
- [ ] 2.2 Strings in English and Dutch (key parity test); the banned-phrase scan covers them
- [ ] 2.3 Emulator pass: English and Dutch, light and dark, screenshots

## 3. Documentation and release
- [ ] 3.1 `docs/dev/` import page, `docs/dev/zib-mapping.md`, README, CHANGELOG
- [ ] 3.2 Archive the change into `openspec/specs/`; release
