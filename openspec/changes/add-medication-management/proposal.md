# Add Medication Management (Pillbox)

## Why
The journal tracks measurements (weight, blood pressure, glucose, activity) but not the medication that often explains them. People who log blood pressure or glucose typically also take medication on a schedule, and forgetting a dose, or not knowing whether it was taken, is a common problem. A medication feature makes the app something used every day, and lets trends be read next to adherence. The app's existing promise, that everything stays on the device, must hold for this more sensitive data too, so this change also states that promise as testable requirements.

## What Changes
- **Medication list:** add, edit, archive and delete medications with a name, an optional form (tablet, capsule, liquid, drops, spray, inhaler, injection, patch, cream, other), an optional note, and an optional appearance (colour and shape) to recognise the pill.
- **Any kind of medication, not only tablets:** a medication has a form (tablet, capsule, liquid, drops, spray or inhaler, injection or pen, patch, cream, other) and a dose in the unit that fits it: mg, mcg, g, ml, IU or units, drops, puffs or sprays, tablets, capsules, patches, injections. Examples: 500 mg tablet, 5 ml syrup, 2 puffs inhaler, 20 units pen, 0.5 mg injection once a week.
- **Custom dosages:** each medication has an optional strength and a dose per intake (for example 1 tablet, 2.5 ml or 20 units), both user-defined. For doses that change from one injection to the next, the amount actually taken can be entered when logging. There is no built-in drug database: the app stays offline, makes no medical claims, and ships no drug data it would have to keep correct.
- **Schedules:** a medication has one or more reminder times per day, on selected weekdays or every N days, with a start date and an optional end date. "As needed" medications have no schedule and are logged on demand.
- **Pillbox view:** a day view lists every planned intake grouped by time of day (morning, afternoon, evening, night). A week strip shows which days are complete, partial or missed.
- **Intake log:** every planned intake can be marked Taken, Skipped, or left Pending. Taken stores the actual time. As-needed doses are logged with a timestamp.
- **Notifications:** a local notification at each planned time with **Taken** and **Snooze** actions. Marking taken from the notification does not open the app. Snooze offers 10, 30 and 60 minutes. An intake still pending after a grace period counts as missed.
- **Adherence:** per-medication and overall adherence (taken / due) for the last 7, 30 and 90 days, the current streak, and a list of missed doses. Adherence is shown next to the existing trend charts as a read-only overlay.
- **Appearance identification:** the colour and shape chosen for a medication are drawn as a small icon in the pillbox, notifications and history, so the right medication is recognised at a glance. The icon follows the form: a pill shape for tablets and capsules, a bottle for liquids, a pen for injections, a spray can or inhaler for sprays. No photos and no camera.
- **Naming:** "pillbox" is the concept of the day view. In the app the tab is called **Medication** (Dutch: Medicatie), since it covers more than pills.
- **Privacy first:** data stays on the device, with no network use, no analytics, and no cloud backup of the health database (see the `privacy` spec).
- **App lock and encryption:** an opt-in lock with fingerprint, face or the device PIN (the app stores no secret of its own), and the whole database encrypted at rest with a Keystore-protected key. Existing data is migrated safely.
- **Export and import:** medications, schedules and the intake log are included in the CSV export and import, metric and locale-independent like the other data.
- **Dutch authorities as reference:** clinical content and the law follow NHG (including Thuisarts), apotheek.nl (KNMP), and the MDR with IGJ supervision and the AVG with the Autoriteit Persoonsgegevens. The app stays a logging and reminder tool so it is not a medical device, and sends users to apotheek.nl and Thuisarts for medicine information (see `design.md`).
- **Localization:** all text in English and Dutch, using the existing `UiText` pattern, with correctly translated forms and dose units (plurals, decimal comma, notifications in the app language). See `design.md`, "Localization".

## Capabilities
- **Added Capability:** `medication`
- **Added Capability:** `privacy` (including the optional app lock and encrypted database)
- **Added Capability:** `compliance` (the app is a personal logging and reminder tool and never a medical device; see `specs/compliance/spec.md`)
- **Modified Capability:** `localization` (medication text, forms and dose units in English and Dutch with correct plurals and number formats)
- **Modified Capability:** `data-export` (medication data in CSV), `platform-android` (notification permission, alarms, boot receiver, backup rules)

## Impact
- Affected code:
  - `domain`: new `model/medication/` (`Medication`, `Dosage`, `DoseUnit`, `PillAppearance`, `Schedule`, `Intake`, `IntakeStatus`), `AdherenceCalculator`, a `MedicationRepositoryPort`, a `ReminderSchedulerPort`, and use cases.
  - `data`: Room entities, DAOs and a database migration from version 2 to 3, repository implementation, CSV adapters.
  - `app`: a fourth tab (Medication), pillbox and edit screens, a `ReminderScheduler` on `AlarmManager`, a notification receiver, a boot receiver, string resources in English and Dutch.
- The database moves to version 3 with a tested migration that keeps existing rows, and the database file is encrypted in a separate verified one-time migration (phase 4c).
- New Android permissions: `POST_NOTIFICATIONS` (Android 13+), `RECEIVE_BOOT_COMPLETED`, and optionally exact alarms (see design).
- `AndroidManifest.xml` currently has `allowBackup="true"`, so the database is included in Android backups today. This change **modifies** that: backup is disabled through `allowBackup="false"` and data extraction rules. Users who relied on automatic backup must use CSV export instead, so the change is noted in the CHANGELOG and README.
- The manifest already has no `INTERNET` permission. This change turns that into a checked rule.
- Two new dependencies, both recorded as exceptions to ADR 0003 in ADR 0020: SQLCipher for database encryption and `androidx.biometric` for the app lock. Everything else uses the platform. Existing users get their database migrated to the encrypted form on update, with verification before the plain file is removed. The structure follows the existing layers, ports and conventions listed in `design.md` under "Fit with the existing architecture".
- Docs: new ADRs (0017 medication model and reminders, 0018 privacy), README feature bullet and diagram, specs README.
- This is the largest change so far. It is split into delivery phases in `tasks.md` so the first phase can ship on its own.

## Portability
The new `medication`, `privacy`, `compliance`, `localization` and `data-export` specs are platform-neutral so the app can be rebuilt on another platform such as iPhone. Android mechanisms (AlarmManager, Keystore, BiometricPrompt, manifest and backup rules, Room, SQLCipher) are in the `platform-android` delta only. `design.md` has a porting guide mapping each neutral requirement to Android and iOS mechanisms.

## Non-goals
- No drug database, interaction checks, dose advice or any medical guidance. The app is a personal log and not a medical device, and says so in the app and the README.
- No cloud sync, accounts, sharing or caregiver features.
- No photo or camera identification.
- No inventory or refill tracking in this change (a follow-up).
- No PDF report in this change (a follow-up).
