## ADDED Requirements

### Requirement: Reminders on AlarmManager
The Android app SHALL schedule only the next reminder with `AlarmManager`, using an exact alarm when the exact-alarm permission is granted and an inexact alarm with a hint otherwise, and SHALL re-arm after boot, app update, time change, time-zone change and any schedule edit through broadcast receivers. Notification actions Taken and Snooze SHALL be handled by a `BroadcastReceiver` without opening the Activity.

#### Scenario: Re-armed after reboot
- **WHEN** the device restarts
- **THEN** the boot receiver re-arms the next reminder for every active medication

#### Scenario: Exact alarm not granted
- **WHEN** the exact-alarm permission is not granted
- **THEN** reminders are inexact and the app shows a hint on how to allow exact alarms

### Requirement: Permissions
The manifest SHALL declare `POST_NOTIFICATIONS` (requested at first reminder setup on Android 13 and higher, with no request on older versions), `RECEIVE_BOOT_COMPLETED`, and the exact-alarm permission as an optional grant, and SHALL NOT declare `INTERNET` or any other network permission. A build check SHALL fail if `INTERNET` appears in the merged manifest.

#### Scenario: Merged manifest check
- **WHEN** a dependency adds the INTERNET permission to the merged manifest
- **THEN** the build fails

#### Scenario: Notification permission on older Android
- **WHEN** the device runs Android 12 or lower
- **THEN** no notification permission prompt is shown

### Requirement: Backup and data extraction
The manifest SHALL set `android:allowBackup="false"` and provide `dataExtractionRules` and `fullBackupContent` that exclude the database and the key material from cloud backup and device transfer.

#### Scenario: Backup excluded
- **WHEN** Android backup or device transfer runs
- **THEN** neither the database nor the wrapped key is included

### Requirement: App lock with BiometricPrompt
The app lock SHALL use `androidx.biometric` `BiometricPrompt` with strong biometrics and device credential allowed, falling back to the confirm-device-credential screen on API 26 to 29 where needed, and SHALL hide content in the recent-apps list and block screenshots when the secure-screen setting is on (`FLAG_SECURE`).

#### Scenario: Older Android
- **WHEN** the device runs API 26
- **THEN** the lock still works through the device credential screen

### Requirement: Encrypted database with SQLCipher and Android Keystore
The Room database SHALL be opened through a SQLCipher `SupportOpenHelperFactory`. The 256-bit passphrase SHALL be generated randomly, wrapped with AES-GCM by a non-exportable Android Keystore key without user-authentication binding (StrongBox or TEE when available), and stored in no-backup storage. The one-time migration from the plain database SHALL use `sqlcipher_export` and verify row counts before removing the plain files.

#### Scenario: Passphrase never stored in the clear
- **WHEN** the app data directory is inspected
- **THEN** the passphrase appears only in wrapped form

### Requirement: Dependency exceptions
`net.zetetic:sqlcipher-android` and `androidx.biometric` SHALL be the only new third-party or AndroidX dependencies added by this change, each recorded in ADR 0020 as an exception to ADR 0003 with licence, size, 16 KB page size support and absence of network code checked.

#### Scenario: Dependency review
- **WHEN** a dependency is added without an ADR entry
- **THEN** the change SHALL NOT be merged

### Requirement: Notification text uses the app language
Notification text and action labels SHALL be built from a context wrapped with the app locale (`withAppLocale`), not the device language, because receivers run without the Activity.

#### Scenario: English device, Dutch app
- **WHEN** the device language is English and the app language is Dutch
- **THEN** a reminder notification and its actions are in Dutch

## MODIFIED Requirements

### Requirement: Local storage with Room
The app SHALL persist data offline in an encrypted SQLite database through Room, with the database at schema version 5. Version 1 to 2 SHALL be a real migration that adds the optional sex column to the profile table without losing rows. Version 2 to 3 SHALL add the waist circumference table, and version 3 to 4 SHALL add the optional pulse column to blood pressure readings, both without changing existing rows. Version 4 to 5 SHALL be a hand-written migration that adds the medication, medication time and intake tables without changing existing rows. Storage SHALL always be metric.

#### Scenario: Upgrade keeps data
- **WHEN** a device with a version 1, 2, 3 or 4 database installs a build that has version 5
- **THEN** all profiles and entries SHALL still be present, the sex of each profile from a version 1 database SHALL be unset, and the medication tables SHALL be empty (ADR 0004)
