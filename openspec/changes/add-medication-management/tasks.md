## 0. Decisions
- [x] 0.1 Grace period of 2 hours, fixed (approved)
- [x] 0.2 Medication is a separate pillbox screen opened from a 💊 button in the top bar, not a fourth tab, with room for more views (approved 2026-10-06)
- [x] 0.3 Schedules are versions with an effective-from date; editing a schedule is a documented exception to "history is never rewritten" (approved 2026-10-06)
- [x] 0.4 Medications at the same time form one slot with Taken all (approved 2026-10-06)
- [x] 0.5 Comments reuse `EntryComment` (ADR 0019) (approved 2026-10-06)
- [x] 0.6 `allowBackup` stays unchanged in this change (approved 2026-10-06)
- [x] 0.7 Delivery is split in four changes: this one, reminders, adherence, encryption and lock (approved 2026-10-06)

## 1. Domain
- [x] 1.1 `Medication`, `Dosage`, `Strength`, `StrengthUnit`, `DoseUnit`, `MedicationForm`, `PillAppearance`, `ScheduleVersion`, `Schedule`, `Intake` (with `IntakeId`), `IntakeStatus`, `Slot` with validation; `EntryComment` for comments
- [x] 1.2 Forms (tablet, capsule, liquid, drops, spray, inhaler, injection, patch, cream, other) and units, no unit conversion (the icon per form is UI work, section 3); optional actual amount and comment on an intake
- [x] 1.3 `Schedule.plannedFor(date)` and version selection (latest `effectiveFrom <= date`), slot grouping, missed derivation; tests for weekdays, every N days, start and end, daylight saving, version in force, an edit applied from today and from an earlier date, orphan outcomes, two as-needed doses on one day
- [x] 1.4 `MedicationRepositoryPort` and use cases (save, change schedule, archive, delete, record intake, record several intakes for a slot, list day)
- [x] 1.5 Use only placeholder medication names ("Medication A") in specs, tests, strings and screenshots; the name field is free text without suggestions

## 2. Data
- [x] 2.1 Room v6 (the database is at v5 today): `MedicationEntity`, `MedicationScheduleEntity`, `MedicationTimeEntity`, `IntakeEntity` (unique index on medication and planned time), DAOs, mapper, `RoomMedicationRepository`
- [ ] 2.2 Hand-written `MIGRATION_5_6` (tables only) and a migration test that keeps every existing row (shares the in-memory test setup with the missing edit/delete DAO tests) Progress: `MIGRATION_5_6` is written; the migration test is deferred, because the data module has no instrumented or Robolectric setup yet
- [x] 2.3 CSV export and import of medications, schedule versions and the intake log (`CsvQuoting` for comments), with round-trip, import-twice (planned and as-needed), orphan, unknown-enum and over-long-comment tests

## 3. App
- [x] 3.1 Wire the use cases in `HealthJournalApp` and a `MedicationViewModel.Factory`; clear messages when the pillbox closes
- [x] 3.2 Pill button in the top bar on every tab, opening the pillbox screen with back handling (`BackHandler`) that returns to the previous tab, content description in both languages
- [x] 3.3 Pillbox screen: segmented switch with Today and Medications; Today with slot cards, Taken all, per-row Taken and Skip, correcting past intakes, as-needed logging, week strip
- [x] 3.4 Medication edit screen: name, form, strength and unit, dose and unit, schedule with the "apply from" date when editing, appearance, comment; archive and delete with confirmation; there is no profile deletion dialog in the app, so nothing needed to change there
- [x] 3.5 First-open disclaimer, Profile entry for the notice and the Sources list
- [x] 3.6 Accessibility: descriptions for icons and chips, 48 dp targets, contrast in both themes

## 4. Localization
- [ ] 4.1 `values` and `values-nl` strings for every form, unit (with `<plurals>` for counted units), status, time-of-day group, frequency phrase and action; labels come from string resources, never raw enum names
- [x] 4.2 Tests: matching English and Dutch key sets for medication strings; plural formatting (1 tablet, 2 tabletten, 1 puff, 2 pufjes); IU shown as IE and mcg as microgram in Dutch; decimal comma versus point; language change keeps stored units and amounts. Progress 2026-10-06: `MedicationStringsTest` covers key sets, plurals, placeholders and IE and microgram; `MedicationFormatTest` covers the decimal comma and that the display language does not change a stored amount
- [ ] 4.3 Check the Dutch terms in the design table against apotheek.nl and Thuisarts in a browser (release gate; ADR 0020 written 2026-10-06 with the progress so far), correct the table, and record the result and date in ADR 0020. Progress 2026-10-06: IE, microgram, tabletten, capsules, druppels, "keer aanbrengen" and "eenheden" confirmed; apotheek.nl and Thuisarts use "dosis / doses" and "inhalaties" for inhalers ("pufjes" is informal spoken Dutch)
- [x] 4.4 Layout check with long Dutch strings and large font (ADR 0010) on the pillbox screens and the top bar with the pill button, at 1.3x and 2.0x. Checked 2026-10-06 on the emulator in Dutch at 1.3x and 2.0x: top bar with the pill button, Today, Medications and the form; nothing clipped
- [x] 4.5 ViewModel tests; emulator check in English and Dutch, including a language switch while the pillbox screen is open. Done 2026-10-06: ViewModel tests pass; a language switch with the pillbox open keeps the screen and re-translates it, but drops an unsaved edit form (known limitation)

## 5. Privacy and compliance
- [ ] 5.1 Keep `INTERNET` out of the manifest, with a build check that fails if present
- [ ] 5.2 Release-build logging check (no names or doses)
- [ ] 5.3 Privacy notice states that platform backup is outside the app's control and CSV export is the user-controlled copy (English and Dutch)
- [ ] 5.4 Intended-purpose statement (personal logging tool, not a medical device) in the app (first open and Profile), README and ADR 0021
- [ ] 5.5 Missed shows the status only (no instruction); a neutral Sources list with apotheek.nl and Thuisarts links in English and Dutch
- [ ] 5.6 No-advice audit of every existing and new user-visible string in English and Dutch (errors, empty states, banners, dialogs, README), and a unit or lint check that lists medication strings for review when they change
- [ ] 5.7 Wording check of README, CHANGELOG, store listing and screenshots against the claims rules
- [ ] 5.8 Confirm there is no alert, urgency or reminder driven by a health value
- [x] 5.9 Range labels (BMI, blood pressure, glucose): done by the separate change `reword-range-labels` (shipped in 1.5.0), NHG leading

## 6. Docs
- [ ] 6.1 ADR 0020 (model, schedule versions, slots, pillbox screen, Dutch term check) and ADR 0021 (compliance and privacy record, with deferred items: licence file, legal review, trend overlay)
- [ ] 6.2 Add the ADRs to the README ADR list and the ADR index; update the domain diagram and glossary in `docs/dev`
- [ ] 6.3 README feature bullet, architecture diagram and disclaimer; CHANGELOG (Unreleased); `scripts/check-docs.sh` green
- [ ] 6.4 Archive the change: add `medication`, `privacy`, `compliance` to `openspec/specs`, merge the `localization`, `data-export` and `platform-android` deltas, add them to `openspec/specs/README.md`
- [ ] 6.5 Keep the neutral specs platform-free: a review check that `privacy`, `medication`, `compliance`, `localization` and `data-export` contain no platform API names (manifest, Keystore, AlarmManager, Room, Compose)
- [ ] 6.6 Commits carry no attribution trailer
