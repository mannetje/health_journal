# Changelog

All notable changes to Health Journal. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/). Each release is tagged `vX.Y.Z`; the section below is published as the GitHub Release notes.

## [Unreleased]

### Added
- **Waist circumference (optional):** record it on its own Waist tab (it is not part of the weight entry). It has a History filter, edit, delete, and CSV export and import, and is shown against sex-specific Voedingscentrum ranges when your profile has a sex set.
- **Theme choice:** Profile has a Theme setting: System (default), Light or Dark. It applies immediately and is remembered.
- **Pulse with blood pressure:** an optional pulse (30 to 250 bpm) is stored with each reading and included in the CSV.
- **Smart input pickers:** ruler pickers for weight and waist, stacked scrolling rows for blood pressure and pulse. The starting value is your latest entry, else a value from your profile, else a standard default.
- **About these ranges** now links to Voedingscentrum, the source of the waist circumference ranges.
- **Developer docs** in `docs/dev` and a contributing guide.

### Upgrade note
- The database moves to version 4 (new waist table, new pulse column). Existing data is kept. Blood pressure CSV files now have a `pulse_bpm` column; older files still import.

## [1.5.0] - 2026-10-03

### Changed
- **Range labels:** BMI, blood pressure and glucose labels now read "name · range", for example "High blood pressure · from 140/90", and no longer name a condition. Glucose shows only the range for the entry's context (fasting or after a meal), in your chosen unit.
- **Blood pressure has three bands:** Normal (below 140/90), High (from 140/90) and Seriously raised (from 180/110). Thresholds are unchanged; the six old bands are merged. Stored entries are mapped on read, so existing data and old CSV files keep working and no data changes.
- **Neutral colours:** a calm blue-to-indigo ramp replaces the green, orange and red range colours.
- **About these ranges:** a new note on the Log, History and trend screens names the sources (NHG first, also Thuisarts, Diabetes Fonds, DVN, Hartstichting) and states that a label is not a diagnosis.

### Upgrade note
- Downgrading to an older build is not supported: older builds cannot read the new blood pressure names.

## [1.4.9] - 2026-09-30

### Fixed
- Blood pressure averages in the trend view are rounded to the nearest whole mmHg instead of truncated, so the shown value and category no longer lean low.
- A non-numeric height in Profile now shows an error instead of silently clearing the height.
- The weight moving-average chip says "N-entry avg" (it counts entries, not days).
- The glucose edit dialog checks the full 0.5 to 55 mmol/L range itself.
- Profile names are trimmed in the domain as well as in the UI.
- CSV import: skipped rows report the physical file line number, BMI and categories are always recomputed instead of trusted from the file, and Libra files with a unit other than kg or lbs are rejected instead of being read as kilograms.
- The birth-date picker no longer offers today, which the profile rules reject.
- Success, info and error banners no longer stay visible after switching to another tab.

## [1.4.8] - 2026-09-30

### Added
- **Units:** choose Metric or Imperial (lb, mi, ft/in) and mmol/L or mg/dL in Profile. The default follows the region. Every input shows its unit, and History, trend charts and statistics follow the choice. Data is always stored in metric and CSV files stay metric ([ADR 0014](docs/adr/0014-units-presentation.md)).
- **Regional formats:** a region setting for dates and numbers that is independent of the language, so English can be combined with Dutch formats.
- **Activity distance** is entered in km or mi instead of meters.
- **Edit and delete** entries from History for weight, blood pressure, glucose and activity ([ADR 0012](docs/adr/0012-edit-and-delete-entries.md)).
- Specs are now a complete, platform-neutral description of all business rules, plus an Android-specific spec (`openspec/specs/`).

### Fixed
- Updating the profile no longer creates a second profile; it edits the active one ([ADR 0016](docs/adr/0016-profile-update-edits-active-profile.md)).
- Success and error banners are translated in the Dutch UI, and re-translate when the language changes ([ADR 0015](docs/adr/0015-localized-viewmodel-messages.md)).
- "Postprandial" glucose label in English.

## [1.4.7] - 2026-09-30

### Fixed
- Trend ranges are anchored to the newest entry instead of today, so old data still shows a chart.
- Calendar axis follows the zoom level.

## [1.4.6] - 2026-09-29

### Added
- New app icon and a branded top bar with the logo.
- Light and dark screenshots in the README.

## [1.4.5] - 2026-09-29

### Changed
- Version bump for the release pipeline.

## [1.4.4] - 2026-09-29

### Fixed
- History crash on real devices when the app language was overridden.
- "All" filter and the Activity chip in History; Dutch tab labels.
- Releases are signed with a fixed debug key, so a new APK installs over the old one and keeps your data ([ADR 0008](docs/adr/0008-fixed-debug-signing-key.md)).

### Changed
- Dutch UI polish and a responsive History layout ([ADR 0010](docs/adr/0010-responsive-dutch-ui-layout.md)).

## [1.4.3] - 2026-09-29

### Added
- System file picker for CSV import.

### Fixed
- Trend chart axes and layout.

## [1.4.2] - 2026-09-29

### Changed
- Version bump; Kotlin/KSP 2.3.0, Room 2.8.5 and Gradle 8.13 toolchain upgrade ([ADR 0007](docs/adr/0007-build-toolchain-upgrade.md)).

## [1.4.0] - 2026-09-28

### Added
- Trend charts in History for weight (with moving average), blood pressure and glucose ([ADR 0006](docs/adr/0006-health-trend-visualizations.md)).

## [1.3.0] - 2026-09-28

### Added
- Dutch and English UI with a language override in Profile.
- Dark theme that follows the system setting.
- Optional sex field in the profile (first database migration).

## [1.1.0] - 2026-09-27

### Added
- Manual activity recording.
- Libra weight CSV import.

## [1.0.0] - 2026-09-27

### Added
- Profiles with weight (BMI), blood pressure and glucose logging, classified against the Dutch NHG guidelines ([ADR 0005](docs/adr/0005-dutch-nhg-guidelines.md)).
- Offline-first storage and CSV export ([ADR 0004](docs/adr/0004-room-for-offline-first-persistence.md)).

[Unreleased]: https://github.com/mannetje/health_journal/compare/v1.4.9...HEAD
[1.4.9]: https://github.com/mannetje/health_journal/compare/v1.4.8...v1.4.9
[1.4.8]: https://github.com/mannetje/health_journal/compare/v1.4.7...v1.4.8
[1.4.7]: https://github.com/mannetje/health_journal/compare/v1.4.6...v1.4.7
