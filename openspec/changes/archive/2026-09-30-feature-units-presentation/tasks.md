## 0. Decisions
- [x] 0.1 Confirm the open decisions in proposal.md

## 1. Domain
- [x] 1.1 `UnitConversion` (kg-lb, cm-ft/in, km-mi) reusing the glucose factor
- [x] 1.2 Unit tests including round-trip stability

## 2. App
- [x] 2.1 `UnitPreference` and `UnitSystem` resolution from the device region
- [x] 2.2 Profile selector (English and Dutch strings)
- [x] 2.3 Log inputs and edit dialog convert display to metric before validation
- [x] 2.4 History cards, charts and stat chips use display units
- [x] 2.5 Region-aware decimal formatting for numbers

## 3. Verification
- [x] 3.1 Emulator: imperial logging, edit round-trip, charts, Dutch and English
- [x] 3.2 CSV export and import still metric (export preview checked on emulator: weight_kg, dot decimals; import covered by CsvAdaptersTest)

## 3b. Found during verification
- [x] 3.3 Profile update created a second profile (height edit lost): fixed, see `fix-profile-update-and-localized-messages`
- [x] 3.4 English banners in the Dutch UI: fixed in the same change

## 4. Docs
- [x] 4.1 ADR 0014, README, diagrams
