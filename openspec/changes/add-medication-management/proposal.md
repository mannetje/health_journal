# Add Medication Management (Pillbox)

## Why
The journal tracks measurements (weight, blood pressure, glucose, activity) but not the medication that often explains them. People who log blood pressure or glucose typically also take medication on a schedule, and not knowing whether a dose was taken is a common problem. A pillbox that records what was planned and what was taken makes the app something used every day. The app's existing promise, that nothing leaves the device, must hold for this more sensitive data too, so this change also states that promise as testable requirements.

The original proposal bundled medication, reminders, adherence, database encryption and an app lock into one change. It is now split into four so each can ship and be reviewed on its own (see "Related changes"). This change is the first and has no dependency on the others.

## What Changes
- **Medication list:** add, edit, archive and delete medications with a name, an optional form (tablet, capsule, liquid, drops, spray, inhaler, injection, patch, cream, other), an optional comment (same rules as an entry comment, ADR 0019), and an optional appearance (colour and shape) to recognise the pill.
- **Any kind of medication, not only tablets:** a dose in the unit that fits the form (mg, microgram, g, ml, IU, units, drops, puffs, tablets, capsules, patches, applications) and an optional strength in its own unit. Examples: 500 mg tablet, 5 ml syrup, 2 puffs inhaler, 20 units pen, 0.5 mg injection once a week. There is no built-in drug database.
- **Custom dosages:** each medication has an optional strength and a dose per intake, both user-defined. The amount actually taken can be entered when logging.
- **Schedules with versions:** one to eight times a day, on selected weekdays or every N days, with a start and optional end date, or "as needed". A schedule edit applies from a chosen date (default today), so earlier days keep the schedule they had. See `design.md`, "Schedule edits and history", for the deliberate exception to "history is never rewritten".
- **Pillbox screen:** a separate screen opened with a pill button in the top bar, not a fourth bottom tab. It has **Today** (a day view of slots grouped by morning, afternoon, evening and night, plus a week strip) and **Medications** (the list). Medications planned at the same time form one slot with a **Taken all** action, and each one can still be handled on its own.
- **Intake log:** every planned intake can be marked Taken, Skipped, or left Pending (shown as Missed after a 2-hour grace period). Taken stores the actual time. As-needed doses are logged with a timestamp, several per day allowed.
- **Export and import:** medications, schedule versions and the intake log are included in the CSV export and import, metric and locale-independent, with duplicate-skipping on import.
- **Dutch terms:** forms and dose units follow Dutch pharmacy wording (for example IE, eenheden, microgram), checked against apotheek.nl and Thuisarts before release.
- **Compliance:** the app stays a logging tool and never a medical device (see `compliance`). Missed doses show a status only, nothing is advised.
- **Privacy:** the app declares no network permission (a build check), writes no medication data to logs, and the privacy notice says that backup is outside the app's control and CSV export is the copy the user controls.
- **Localization:** all text in English and Dutch, using the existing `UiText` pattern, with plurals and decimal comma.

## Related changes
| Change | Delivers |
|---|---|
| `add-medication-reminders` | Notifications with Taken and Snooze, one per slot, permission flow, lock-screen details |
| `add-medication-adherence` | Adherence figures, streak, missed list and the Adherence view |
| `add-database-encryption-and-lock` | SQLCipher database, Keystore key, opt-in app lock, secure screen, backup rules (ADR 0003 exception) |

Deferred and not part of any of these: the adherence overlay on the trend charts (was task 3.3), a licence file for the repository (was 4b.7) and a legal review (was 4b.8). They are tracked as open items in ADR 0021.

## Capabilities
- **Added Capability:** `medication`
- **Added Capability:** `privacy` (no network, user-controlled export, no sensitive logging)
- **Added Capability:** `compliance` (the app is a personal logging tool and never a medical device)
- **Modified Capability:** `localization` (medication text, forms and dose units in English and Dutch with correct plurals and number formats)
- **Modified Capability:** `data-export` (medication data in CSV)
- **Modified Capability:** `platform-android` (Room version 6, no network permission check, pill button in the top bar)

## Impact
- **Database:** yes. Room moves from version 5 to 6 with a hand-written `MIGRATION_5_6` that only creates the new tables, so existing rows are untouched and the medication tables start empty (ADR 0004). No stored data shape of existing entries changes, and no range labels or medical thresholds are touched.
- **Affected code:**
  - `domain`: new `model/medication/` (`Medication`, `Dosage`, `Strength`, `StrengthUnit`, `DoseUnit`, `MedicationForm`, `PillAppearance`, `Schedule`, `ScheduleVersion`, `Intake`, `IntakeStatus`, `Slot`), `MedicationRepositoryPort`, and use cases.
  - `data`: Room entities, DAOs, mapper, repository, the migration, CSV adapters.
  - `app`: the pill button in the top bar, the pillbox screen, edit screens, string resources in English and Dutch.
- **Manifest:** no new permissions. `allowBackup` is unchanged (`true`). The manifest already has no `INTERNET` permission, and this change turns that into a checked rule.
- **Dependencies:** none (ADR 0003 unchanged).
- **Docs:** ADR 0020 (medication model and pillbox, including the schedule-version decision and the Dutch term check) and ADR 0021 (compliance and privacy record), README feature bullet and diagram, specs README.

## Portability
The new `medication`, `privacy`, `compliance`, `localization` and `data-export` specs are platform-neutral so the app can be rebuilt on another platform such as iPhone. Android mechanisms are in the `platform-android` delta only. `design.md` has a porting guide mapping each neutral requirement to Android and iOS mechanisms.

## Non-goals
- No drug database, interaction checks, dose advice or any medical guidance. The app is a personal log and not a medical device, and says so in the app and the README.
- No reminders or notifications, no adherence percentages (separate changes).
- No encryption or app lock (separate change).
- No cloud sync, accounts, sharing or caregiver features.
- No photo or camera identification.
- No inventory or refill tracking, no PDF report (follow-ups).
