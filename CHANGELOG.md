# Changelog

All notable changes to Health Journal. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/). Each release is tagged `vX.Y.Z`; the section below is published as the GitHub Release notes.

## [Unreleased]

### Added
- **Unencrypted-storage banner:** when encryption could not be set up, a banner on every tab says so (dismissible for the session, with a shortcut to the Profile tab). Before, the notice was only in Profile. Light, dark and Dutch screenshots are in `docs/screenshots.md`.

- **Backup restore tells what it leaves out:** a backup made by a newer app can hold tags or codes this version does not know. The restore dialog now says how many items will be left out, before anything is imported. Before, they were dropped without a word.

- **Release policy and checksum:** releases now carry a SHA-256 file next to the APK, and the README and ADR 0008 state that they are personal, sideloaded, debug-signed builds, and name when a private release keystore becomes necessary.

- **Room schemas are exported and every migration is tested:** the schema of database versions 1 to 7 is committed in `data/schemas`, and `MigrationChainTest` migrates a database from each version to the current one and validates the result against the exported schema. Developer docs only; the app itself is unchanged.
- **SQLCipher instrumented tests:** `SqlCipherDatabaseTest` runs on an emulator or device and checks the encrypted database (same key reads, another key fails, the file is not plain) and the real plain-to-encrypted migration. The existing key test expected database version 6 and now expects 7. `docs/dev/testing.md` describes the suite; CI does not run it.
- **Project governance pages:** `SECURITY.md` (how to report a vulnerability, what is in scope), `CODE_OF_CONDUCT.md` and `GOVERNANCE.md` (who decides, how a change is decided, the principles that do not change). `CONTRIBUTING.md` links to them, and the pull request template now lists the lint check like the contributing checklist does. No change to the app.
- **README states what the app is and is not:** the intro now uses the purpose statement the compliance spec requires (a personal tool for logging and reminders, for private use, not a medical device) and has a short table of what it is and is not. Wording that said the app "evaluates" values against clinical standards is gone: the metric bullets say a value is shown with its range label. A guardrail test keeps the statement in and that wording out. No change to the app.
- **Repository guardrails:** `RepositoryGuardrailsTest` fails the build when the schema version, the migration chain, the README ADR index, the release artifact or the picker starting values disagree between code, tests and docs. Developer docs only; the app itself is unchanged.

### Changed
- **Log pickers no longer start at an "ideal":** without an earlier entry, weight no longer starts at the BMI 22.5 weight for your height and waist no longer starts at a value chosen by sex. A picker you have not moved is not a value: no category is shown for it, Save stays off with a hint until you set weight, waist or both blood pressure numbers, and an untouched pulse is saved as no pulse. Glucose starts empty. Your own latest entry is still the starting point. See ADR 0033.
- **SQLCipher 4.9.0 → 4.19.1** (dependabot): the `:data` module now compiles against SDK 36, which the new release requires. Build, unit tests and lint pass.

### Fixed
- **Encryption migration is crash-safe:** the plain database is set aside and only deleted after the encrypted one is in place; an interrupted swap is recovered at the next start.
- Docs: the database is at version 7 everywhere, and the README ADR list now includes ADRs 0026 to 0032.

## [1.6.2] - 2026-10-24

### Added
- **Migraine log:** a Migraine tab on the Log screen with a step form for the start and end time (or still ongoing, or woke up with it), intensity 0 to 10, where it hurts and how, type, symptoms, triggers, medication taken (free text, second dose, a 0 to 10 score after 2 hours) and relief, a comment, and your own trigger and relief tags. Every step is optional and Save is on every step. English and Dutch. The text never gives advice or compares; the "this helped me" ticks and the scores are your own notes and are shown back as entered ([ADR 0025](docs/adr/0025-migraine-log-data-model.md)).
- **Migraine in History:** a Migraine filter with one compact card per migraine showing everything that was filled in, an "It ended now" button for an ongoing entry, edit (the form opens in place of the list) and delete.
- **Migraine graph view:** the graph icon under the Migraine filter shows counts for 7, 30 or 90 days or All: migraines, days with and without one, an intensity timeline, duration and intensity groups, weekdays, a weekday by time-of-day heat map and symptom, trigger, pain and relief counts. Counts only.
- **Migraine calendar:** a Graph and Calendar toggle in the Migraine graph view. The calendar marks days with a migraine (with the count from two up) and days with something taken; tapping a marked day opens History on that day, with a chip to show all days again.
- **Migraine CSV:** export and import of migraines from History's Export and Import dialogs, with the same note that CSV files are not encrypted. The README CSV table lists the columns.
- The encrypted backup and the CSV adapters cover migraines and tags. The database moves from version 6 to 7 (two new tables; existing data is untouched).
- Developer docs: a new [Migraine page](docs/dev/migraine.md), the database page (version 7), the C4 diagrams, architecture, design, graphs and testing pages.

## [1.6.1] - 2026-10-21

### Added
- **Mindfulness area (🪷):** a new lotus button next to the pillbox opens a calm area with Breathe, Meditate, Guided and Start here. Breathe offers box breathing, 4-7-8, the physiological sigh and belly breathing for 1, 3 or 5 minutes, drawn as a soft lotus, a circle or a rounded square; the screen stays on, the phase and seconds are read out to TalkBack, with system animations off the picture steps instead of moving, and the Stop button fades after a few seconds and returns on touch. Meditate is a quiet timer of 5, 10, 15, 20 or 30 minutes with optional interval bells. Guided lists three recorded exercises from the Free Mindfulness Project (used unchanged, credited on the player and on a Credits screen) and plays them on the device. Start here is a three-step ladder for a first time. Tones, vibration and spoken cues (the phone's offline voice, with a voice choice) can be switched on or off. A short safety note is shown on first open, with the full text under About these exercises. Nothing about your practice is stored, only your display choices. English and Dutch. Adds the `VIBRATE` permission.

### Changed
- The data storage notice on the Profile tab now says the Mindfulness area keeps only display choices.
- Tapping the Health Journal logo in the top bar now goes to the Log screen, also from the pillbox and from Mindfulness.

## [1.6.0] - 2026-10-21

### Added
- **Encrypted backup:** Profile → Encrypted backup creates one `.hjbackup` file protected by a passphrase of your own (at least 8 characters, typed twice, no recovery) with Argon2id and AES-256-GCM. The app reads the saved file back to check it. Restore merges: entries that already exist are never overwritten, restoring twice adds nothing. "Also restore settings" brings back language, region, units, theme and snooze length; lock and privacy settings are never in the file. The lost-key screen offers "Restore from a backup" next to deleting the data. The file format is published with a JSON Schema, test vectors, a reference decoder and a [recovery guide](docs/backup-format/recover-your-data.md), so the data stays reachable without the app ([ADR 0024](docs/adr/0024-portable-encrypted-backup.md)). English and Dutch.
- **Database encryption at rest:** The Room SQLite database is encrypted with SQLCipher (AES-256) using a 256-bit passphrase wrapped by a hardware-backed Android Keystore key (TEE/StrongBox) without user-authentication binding ([ADR 0023](docs/adr/0023-database-encryption-and-app-lock.md)). A one-time migration safely converts existing plain databases to encrypted files with row-count verification.
- **Opt-in App Lock:** Protect app data behind the device's own credential (strong biometrics or screen lock PIN/pattern/password) via BiometricPrompt, with configurable background timeout (immediately, 1 minute, 5 minutes). While locked, no health data is rendered.
- **Secure Screen:** An opt-in setting in Profile that enables `FLAG_SECURE` to block screenshots and hide app previews in the recent apps switcher.
- **Data extraction rules:** Excludes the encrypted database and key material from cloud backups and device transfer, preventing silent data loss upon restore on new devices.

### Changed
- The CSV export dialog now says that CSV files are not encrypted and points to the encrypted backup. The privacy texts and the medication privacy notice describe the backup.

### Fixed
- **Encryption on devices without StrongBox:** creating the database key failed there, so the migration silently kept the data unencrypted. The key now falls back to the Trusted Execution Environment.
- **Database key zeroed too early:** on devices where the migration succeeded the database was opened with an all-zero key, which could crash the app at launch.
- **App lock timeout and the clock:** the timeout now uses the monotonic clock, so setting the phone's clock back no longer keeps the app unlocked.
- **Recent apps preview:** the app lock now always keeps the window secure, so the recent-apps thumbnail never shows health data.
- **Lost encryption key:** if the key is gone, the app shows an explanation with a confirmed "delete data and start fresh" option instead of crashing at every launch.
- **Turning the lock off needs authentication:** switching the app lock off or choosing a longer timeout now asks for the device credential first.
- **Interrupted key write:** the wrapped database key is written atomically, so an interrupted write can no longer look like a lost key.
- **Visible encryption status:** a database that could not be encrypted is logged and the Profile "Data storage & backups" card says so.

## [1.5.9] - 2026-10-19

### Added
- **Inspect entries on the trend charts:** tap an entry on the weight, blood pressure, glucose, waist or waist-to-height ratio chart and a vertical line marks it, with its date, time and value and unit below the chart. Only real entries can be selected, never limit lines or moving averages. Tap a second entry to see both and the difference (value, percent and time between them), tap a third to start over, or tap the selected entry again to clear. Figures only; in English and Dutch. Panning and zooming work as before.

### Changed
- The README shows three sample screenshots; all others moved to `docs/screenshots.md`.

## [1.5.8] - 2026-10-19

### Changed
- **Dots on long histories:** the dots on the weight, waist and glucose charts now shrink as the history grows (9 dp up to 30 points, 3 dp from 300 points) instead of disappearing beyond 60 points, so the "All" range shows dots too without merging into a thick band.

### Fixed
- Dutch: "Tijd in Bereik" is now "Tijd in bereik", like the other texts.

## [1.5.7] - 2026-10-19

### Added
- **Glucose graphs** in the Glucose graph view: fasting and after-meal readings as two lines with points coloured by NHG category and lines at the NHG limits (low, fasting, after a meal), time in range per NHG category, average and range by time of day, lowest and highest reading, change since a month, three months, six months and a year, spread (average, standard deviation, range), the last 30 days against the 30 before, and the largest rise between two readings less than 3 hours apart. Figures only, no good or bad colour; in English and Dutch.
- **Dots on the weight chart:** the raw weight line now shows a dot per entry, like the waist chart. Series with more than 60 points are drawn as a plain line, so dense histories do not turn into a thick band.
- **Waist graphs** in a new Waist graph view (the Waist filter now has the graph icon too): a trend chart with the NHG limits as lines and band-coloured points (needs a sex in the profile), the lowest and highest waist, the change over a month, three months, six months and year in cm and percent, the distance to the NHG limits, the waist-to-height ratio over time with a 50 % rule-of-thumb line (needs a height in the profile), and a scatter chart of waist against weight with a trend line and one neutral sentence (needs 5 pairs within 3 days). Figures only, no good or bad colour; in English and Dutch.

### Documentation
- New developer page [Graphs](docs/dev/graphs.md): how the waist and glucose graphs were built, the building blocks and a checklist for a new graph.
- All README screenshots retaken (light and dark) with sample data that includes comments, plus new glucose statistics shots.

## [1.5.6] - 2026-10-19

### Added
- **Graph view in History:** under the Weight, Blood pressure and Glucose filters an icon next to the title switches between the entries and the graph. The list no longer starts with the chart; the 7/30/90-day range selector stays above both views. The choice is remembered per filter while the app is open.
- **Blood pressure graphs** in the Blood Pressure graph view: points on the trend chart coloured by NHG band, a scatter chart (diastolic across, systolic up) on the three bands with newer readings more prominent, how steady your readings are (usual change and biggest jump between two readings), and a comparison of systolic, diastolic, pulse, mean arterial pressure and pulse pressure with the previous period (newest value, range, change, "stable" under 2). Figures only, no good or bad colour; in English and Dutch.
- **Weight statistics** in the Weight graph view, over all weight entries: the lowest and highest weight with their dates, the change and change per week over the last week, 15 days, month, three months, six months and year (a dash when the history is shorter), a switch between kg (or lb) and percent, a bar chart of the change per month with year chips, and a bar chart of the change per year. Figures only, one neutral colour for gain and loss.

### Fixed
- Lint error `NonObservableLocale` in the History timeline rows (the locale is now read from the configuration).

## [1.5.5] - 2026-10-19

### Added
- **History timeline navigation:** a year row and a month row above the History list, for every filter and entry type. Tap a year or month to jump to its newest entry; scrolling updates the chips, so they always show the period at the top of the list. Months without entries are dimmed, and the rows are hidden when everything is from one month. Each month starts with a header with its name, year and entry count.

### Changed
- History: the "About these ranges" button is now an (i) icon next to the title, and Import and Export moved into a three-dot menu on the same row, which leaves more room for the entries.

### Fixed
- A pillbox that is already open now updates when "Taken all" is used from a reminder notification, instead of showing the old state until it is opened again ([ADR 0022](docs/adr/0022-medication-reminders.md)).

### Documentation
- Adherence screenshots (light and dark, sample data) in the README and the pillbox page.
- New developer pages: [C4 diagrams](docs/dev/c4.md) (system context, containers, components of each module, reminder, adherence and entry sequences, deployment) and [Design](docs/dev/design.md) (principles, quality goals, screen map, state flow, layout rules, medication state diagram).

## [1.5.4] - 2026-10-07

### Added
- **Medication reminders:** one notification per time slot at the planned time, with Taken all and Snooze actions that do not open the app. Tapping it opens the pillbox. The lock screen shows only "Medication reminder" unless you turn on details in Profile, where you also set the snooze length (10, 30 or 60 minutes). Reminders are set again after a restart, an app update and a clock or time-zone change. Exact alarms are used when allowed, otherwise reminders can arrive a few minutes late. Text follows the app language ([ADR 0022](docs/adr/0022-medication-reminders.md)).
- **Medication adherence:** a third view in the pillbox (next to Today and Medications) shows taken against planned intakes for the last 7, 30 or 90 days, per medication and overall, with skipped and missed counts, the days in a row with everything taken and the list of missed intakes. Skipped counts as not taken but is shown separately; as-needed medications show only the number of doses. Wording is neutral status only, with no targets or advice.

### Documentation
- The README has a roadmap table and diagram, a medication flow diagram and an updated architecture diagram. The developer docs describe the reminders and the adherence view, and the architecture page shows how the pillbox, the reminders and adherence share one plan.

### Upgrade note
- No database change: the schema stays at version 6.

## [1.5.3] - 2026-10-07

### Added
- **Pillbox:** a pill button in the top bar opens a pillbox with a Today view (slots with "Taken all", Taken and Skip per medication, a week strip, as-needed doses) and a Medications list (add, edit, archive, delete). A schedule edit applies from a chosen date and past outcomes stay as recorded. A notice on first open (also in Profile) says the pillbox is a personal log, not a medical device, with links to apotheek.nl and Thuisarts ([ADR 0020](docs/adr/0020-medication-model-and-pillbox.md), [ADR 0021](docs/adr/0021-medication-compliance-and-privacy.md)).
- **Medication in CSV:** medications, schedule versions and intakes are exported and imported.

### Upgrade note
- The database moves to version 6 (four new medication tables). Existing data is kept. Older app versions cannot read it.

## [1.5.2] - 2026-10-06

### Changed
- **Low glucose limit** is now 3.9 mmol/L (was 3.5), following NHG and Thuisarts. Normal fasting reads 3,9 to 6,0 and normal after a meal 3,9 to under 7,8. Entries saved earlier keep their stored label until edited.

## [1.5.1] - 2026-10-06

### Added
- **Optional comments:** add a short single-line note (up to 200 characters) to any entry: weight, blood pressure, glucose, waist and activity. Enter it while logging or in the edit dialog; History shows it under the entry ([ADR 0019](docs/adr/0019-entry-comments.md)).
- **Comments in CSV:** every export has a trailing `comment` column, quoted when it holds a comma or quote. Older files without it still import. The Libra import reads its `comments` column and cuts a comment longer than 200 characters to its first 200 instead of skipping the row.
- **Waist circumference (optional):** record it on its own Waist tab (it is not part of the weight entry). It has a History filter, edit, delete, and CSV export and import, and is shown against sex-specific Voedingscentrum ranges when your profile has a sex set.
- **Theme choice:** Profile has a Theme setting: System (default), Light or Dark. It applies immediately and is remembered.
- **Pulse with blood pressure:** an optional pulse (30 to 250 bpm) is stored with each reading and included in the CSV.
- **Smart input pickers:** ruler pickers for weight and waist, stacked scrolling rows for blood pressure and pulse. The starting value is your latest entry, else a value from your profile, else a standard default.
- **About these ranges** now links to Voedingscentrum, the source of the waist circumference ranges.
- **Developer docs** in `docs/dev` and a contributing guide.

### Upgrade note
- The database moves to version 5 (new waist table, new pulse column, new comment column on the five entry tables). Existing data is kept. Older app versions cannot read it. Blood pressure CSV files now have a `pulse_bpm` column; older files still import.

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
