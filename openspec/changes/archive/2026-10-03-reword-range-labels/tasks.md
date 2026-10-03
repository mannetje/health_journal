## 0. Decisions (approved)
- [x] 0.1 NHG leads; Diabetes Fonds, DVN and Hartstichting are also used for wording
- [x] 0.2 Blood pressure: three bands, Normal below 140/90, High from 140/90, Seriously raised from 180/110, no low band
- [x] 0.3 BMI keeps its four names, with the range
- [x] 0.4 Glucose: Low, Normal, Slightly raised, High blood glucose, thresholds unchanged (low limit 3.5)

## 1. Domain
- [x] 1.1 `NhgBloodPressureCategory`: NORMAL, HIGH, SERIOUSLY_RAISED; classify with the three rules; `fromStoredName` accepting the six legacy names and the three new ones
- [x] 1.2 Tests: boundaries 139/89, 140/80, 120/90, 179/109, 180/80, 120/110; legacy mapping table; a grid test showing old rule plus mapping equals the new rule for every reading in range
- [x] 1.3 Update existing tests that name the old values (`NhgClassificationTest`, `UseCasesTest`, `ViewModelsTest`, `MapperTest`, `RoomRepositoriesTest`, `CsvAdaptersTest`)

## 2. Data
- [x] 2.1 `HealthLogMapper` reads blood pressure categories through `fromStoredName`
- [x] 2.2 CSV export writes the new names; test that an old file with the old names still imports and is recomputed
- [x] 2.3 Mapper test with a stored legacy row

## 3. App
- [x] 3.1 String resources in `values` and `values-nl`: name strings and range strings per band, for fasting and after a meal; remove the old `nhg_bp_*` and glucose condition strings; "About these ranges" text; no advice wording (task 4b.2b audit applies)
- [x] 3.2 `NhgLabels.kt`: `label()` returns name and range; glucose takes the context and the display unit (`LocalDisplayUnits`); decimals follow the region
- [x] 3.3 Replace `log_nhg_badge` ("NHG: %s") with the new label; update the log preview, history cards, BP trend average line, distribution bar (grouped by band) and the weight trend BMI line
- [x] 3.4 Source and note sheet reachable from each screen that shows a label (info icon, accessible description), with the links
- [x] 3.5 `NhgColors.kt` and `Color.kt`: neutral ramp, removal of the unused traffic-light constants; contrast check at least 4.5:1 for label text in both themes
- [x] 3.6 Wrapping: two-line labels at font scale 1.3 in English and Dutch, no truncation (ADR 0010)
- [x] 3.7 Label text tests in both languages (`RangeLabelStringsTest`, from the resource files, no condition names, matching placeholders and limits) and glucose limits in mmol/L (`GlucoseRangesTest`); the mg/dL display was checked on the emulator only (no automated test, as the label is a Composable)

## 4. Specs and docs
- [ ] 4.1 Apply the delta specs on archive (`range-labels` added; `blood-pressure`, `glucose`, `weight`, `health-trends`, `data-export` modified)
- [x] 4.2 Update the `add-medication-management` compliance delta so "Reference ranges are informational and neutral" points to `range-labels` (done in this change), and mark its task 4b.3 as covered by this change
- [x] 4.3 README: blood pressure bands and screenshots of the log, history and trend screens (English; Dutch labels are covered by the emulator check)
- [x] 4.4 ADR 0005: source order (NHG first) and the three-band decision; ADR 0006: neutral colours
- [x] 4.5 `openspec/specs/README.md`: index entry for `range-labels`, glossary entries for the three bands and the source order
- [x] 4.6 Release notes: old to new blood pressure names, downgrade note, no data change

## 5. Verification and release
- [ ] 5.1 Check every band, limit and name against the NHG text (CVRM, diabetes type 2, BMI) and the three other sources, record the result in ADR 0005, correct the spec if NHG differs (open points: diastolic 110 limit, the low glucose limit 3.5, the BMI class wording) — partly done 2026-10-03: see ADR 0005; the NHG text is still not reachable, three points stay open
- [x] 5.2 Emulator check in English and Dutch, light and dark, fasting and after a meal, mg/dL and mmol/L
- [ ] 5.3 `openspec validate reword-range-labels --strict`
- [x] 5.4 Version bump to 1.5.0 (versionCode 15); tag and release only when asked
