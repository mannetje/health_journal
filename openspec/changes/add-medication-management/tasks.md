## 0. Decisions to confirm
- [x] 0.1 Exact alarms: use when granted, otherwise inexact with a hint (approved)
- [x] 0.2 Grace period of 2 hours, fixed (approved)
- [x] 0.3 Skipped counts as due and not taken, shown separately (approved)
- [x] 0.4 Database encryption (SQLCipher with Keystore-wrapped key) and an opt-in app lock (device credential) are included, as phase 4c, each recorded in ADR 0020 as an exception to ADR 0003 (approved). Ship 4c on its own, before or after phase 1, because it also changes the existing data
- [x] 0.5 Medication is a fourth bottom tab with the 💊 icon, second in the bar, sub-tabs Today / Medications / Adherence (approved)

## 1. Phase 1: Medications and pillbox (no reminders)
- [ ] 1.1 Domain: `Medication`, `Dosage`, `DoseUnit`, `PillAppearance`, `Schedule`, `Intake`, `IntakeStatus` with validation
- [ ] 1.1b Forms (tablet, capsule, liquid, drops, spray, inhaler, injection, patch, cream, other) and dose units (mg, mcg, g, ml, IU, units, drops, puffs, tablets, capsules, patches, applications, other), no unit conversion; icon per form; optional actual amount and free-text note on an intake
- [ ] 1.1c Use only placeholder medication names ("Medication A") in specs, tests, strings and screenshots; name field is free text without suggestions
- [ ] 1.2 `Schedule.plannedFor(date)` with tests (weekdays, every N days, start and end, daylight saving)
- [ ] 1.3 `MedicationRepositoryPort` and use cases (save, archive, delete, record intake, list day)
- [ ] 1.4 Room v3: `MedicationEntity`, `MedicationTimeEntity`, `IntakeEntity`, DAOs, mapper, `RoomMedicationRepository`, hand-written `MIGRATION_2_3` and a migration test (shares the in-memory test setup with the missing edit/delete DAO tests)
- [ ] 1.4b Wire the use cases in `HealthJournalApp` and a `MedicationViewModel.Factory` in `MainActivity`, add the fourth `NavigationBarItem`, clear messages on tab change
- [ ] 1.5 Medication tab (💊, second position) with sub-tabs Today (pillbox day view and week strip), Medications (list, add and edit) and Adherence placeholder; pill icon; Profile section for medication settings and notice
- [ ] 1.6 English and Dutch strings, `UiText` messages, disclaimer
- [ ] 1.6b Localization: `values` and `values-nl` strings for every form, unit (with `<plurals>` for counted units), status, time-of-day group and action; labels come from string resources, never raw enum names; Dutch terms per the design table, checked against apotheek.nl wording
- [ ] 1.6c Tests: matching English and Dutch key sets for medication strings; plural formatting (1 tablet, 2 tabletten, 1 puff, 2 pufjes); IU shown as IE in Dutch; decimal comma versus point; language change keeps stored units and amounts
- [ ] 1.6d Layout check with long Dutch strings and large font (ADR 0010) on the medication screens
- [ ] 1.7 ViewModel tests; emulator check in English and Dutch, including a language switch while the Medication tab is open

## 2. Phase 2: Reminders
- [ ] 2.1 `ReminderSchedulerPort` and `AlarmManager` implementation (next alarm only, re-arm)
- [ ] 2.2 Notification channel, Taken and Snooze actions through a `BroadcastReceiver`
- [ ] 2.2b Notification text and action labels built from an app-locale context (not the device language), tested in Dutch on an English device
- [ ] 2.3 Boot, app-update, time-zone and clock-change receivers
- [ ] 2.4 Permission flow (`POST_NOTIFICATIONS`, exact alarm hint) and the denied banner
- [ ] 2.5 Lock-screen detail setting
- [ ] 2.6 Emulator and real-device check, including reboot and Doze

## 3. Phase 3: Adherence
- [ ] 3.1 `AdherenceCalculator` with tests (rounding, skipped, as-needed, start date)
- [ ] 3.2 Adherence screen (7, 30 and 90 days, streak, missed list)
- [ ] 3.3 Read-only overlay next to the trend charts

## 4. Phase 4: Privacy and data
- [ ] 4.1 Keep `INTERNET` out of the manifest, with a build check that fails if present
- [ ] 4.2 `allowBackup="false"` and `dataExtractionRules`
- [ ] 4.3 Optional secure-screen setting
- [ ] 4.4 CSV export and import of medications, schedules and the intake log, with tests
- [ ] 4.5 Release-build logging check

## 4b. Regulation and compliance (spec `compliance`)
- [ ] 4b.1 Intended-purpose statement (personal logging and reminder tool, not a medical device) in the app (first run and settings), README and ADR 0019
- [ ] 4b.2 Missed intake shows the status only (no instruction); a neutral "Sources" list with apotheek.nl and Thuisarts links on the information screen, in English and Dutch
- [ ] 4b.2b No-advice audit of every existing and new user-visible string in English and Dutch (notifications, errors, empty states, banners, dialogs, README), and a unit or lint check that lists strings for review when they change
- [x] 4b.3 Range labels (BMI, blood pressure, glucose): done by the separate change `reword-range-labels` (shipped in 1.5.0) (NHG leading), which ships before any medication code
- [ ] 4b.4 Wording check of README, CHANGELOG, store listing and screenshots against the claims rules
- [ ] 4b.5 Confirm there is no alert, urgency or reminder driven by a health value
- [ ] 4b.6 ADR 0019 compliance record (intended purpose, MDR, AVG, Play requirements, review gate, date of last review)
- [ ] 4b.7 Add a licence file to the repository
- [ ] 4b.8 Short legal or privacy review (MDR intended purpose, AVG) before distribution beyond personal use

## 4c. App lock and database encryption (spec `privacy`)
- [ ] 4c.1 ADR 0020: threat model, why device credential and not an own PIN, why SQLCipher, licence, size and 16 KB page check, fallbacks, ADR 0003 exception
- [ ] 4c.2 Domain port `AppLockPort` and a `LockViewModel` state (locked, unlocked, timeout) with tests; setting stored in the existing preferences
- [ ] 4c.3 `BiometricPrompt` integration with device credential, API 26 fallback, no-screen-lock handling, lock overlay that composes no health data, timeout options
- [ ] 4c.4 Notification tap requires unlock; Taken action works without unlock; documented in the setting text
- [ ] 4c.5 `DatabaseKeyProvider` (Keystore AES-GCM wrapping, no user-authentication binding, no-backup storage) and a `SupportOpenHelperFactory` for Room, with an instrumented test on key creation and reopen
- [ ] 4c.6 One-time plain to encrypted migration with row-count verification, keep the plain file until verified, failure keeps data; tests on a seeded database including edge cases (empty, large, interrupted)
- [ ] 4c.7 Privacy notice and README: explain that data cannot be restored from backup or moved between devices, and that CSV export is the way
- [ ] 4c.8 English and Dutch strings for the lock, prompt text, migration messages; emulator and real-device check, including biometric enrolment change and reboot

## 4d. Contracts and portability
- [ ] 4d.1 `data-export` delta: medications, schedule and intakes CSV files, enum names, round trip and duplicate-skipping tests
- [ ] 4d.2 `platform-android` delta (reminders, permissions, backup rules, lock, SQLCipher, notification locale, Room v3) implemented and the manifest check added
- [ ] 4d.3 Keep the neutral specs platform-free: a review check that `privacy`, `medication`, `compliance`, `localization` and `data-export` contain no platform API names (manifest, Keystore, AlarmManager, Room, Compose)

## 5. Phase 5: Docs
- [ ] 5.1 ADR 0017 (model and reminders), ADR 0018 (privacy) and ADR 0020 (lock and encryption)
- [ ] 5.2 Archive the change: add `medication`, `privacy`, `compliance` to `openspec/specs`, merge the `localization`, `data-export` and `platform-android` deltas, add them to the index in `openspec/specs/README.md`, update the domain diagram (Medication, Intake) and the glossary
- [ ] 5.3 README feature bullet, architecture diagram and disclaimer; CHANGELOG
