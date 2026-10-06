# Add Database Encryption and App Lock

## Why
The journal holds health data, and medication makes it more personal. Two protections stop different threats: an opt-in **app lock** stops a person who holds the unlocked phone, and an **encrypted database** stops anyone who obtains the database file without the phone's keys. This was part of the original medication proposal. It is now its own change because it touches all existing data (not only medication), adds the first dependencies since ADR 0003, and has its own risks (a one-way migration of every existing user's database), so it must be reviewed and released on its own.

## What Changes
- **Encrypted database:** the Room database is opened through SQLCipher with a random 256-bit passphrase that is stored only wrapped by a non-exportable Android Keystore key (hardware-backed when available) and needs no user authentication.
- **One-time migration** of existing users from the plain to the encrypted file, with row-count verification, the plain file kept until verified, and the plain database kept on any failure.
- **App lock (opt-in, off by default):** authentication with the device's own credential (biometric with PIN, pattern or password as fallback) through `BiometricPrompt`, no secret stored by the app, no health data composed while locked, timeout options (immediately, 1 minute, 5 minutes).
- **Secure screen (opt-in):** hide content in the app switcher and block screenshots.
- **Backup rules:** the encrypted database and the wrapped key are excluded from backup and device transfer through data extraction rules. A restored encrypted file would be unreadable because the Keystore key does not move, so including it would turn a restore into silent data loss. `allowBackup` itself stays `true` so the rest of the app data (preferences) keeps its current behaviour. This is the point where the backup decision of `add-medication-management` is revisited, and it needs the owner's confirmation before implementation.
- **Notices:** the privacy notice says that data cannot be restored from backup or moved between devices, and that CSV export is the way.

## Capabilities
- **Modified Capability:** `privacy` (app lock, database encrypted at rest, secure screen, no automatic restore of the encrypted file)
- **Modified Capability:** `platform-android` (BiometricPrompt, SQLCipher with Keystore, extraction rules, dependency exceptions)

## Impact
- **Database:** the stored data shape and the Room schema version do **not** change. The file format changes (plain to encrypted) through a separate one-time file migration that is independent of the schema version, so all existing data stays readable after verification (ADR 0004). The alternative of read-mapping is not applicable: the same rows move into a new file.
- **Affected code:** `domain` gets `AppLockPort` and a lock state; `data` gets `DatabaseKeyProvider`, the `SupportOpenHelperFactory` wiring and the migration; `app` gets the lock overlay, `BiometricPrompt` integration, settings in Profile, strings in English and Dutch.
- **Dependencies (exception to ADR 0003, recorded in ADR 0023):** `net.zetetic:sqlcipher-android` (BSD-style licence, no network code, adds a few MB per ABI) and `androidx.biometric`. Both checked for licence, size, 16 KB page size support and absence of network code before merge.
- **Manifest:** `USE_BIOMETRIC`, data extraction rules; still no `INTERNET`.
- **Docs:** ADR 0023 (threat model, why the device credential and not an own PIN, why SQLCipher, licence and size, fallbacks, honest limits), README, CHANGELOG, specs.

## Depends on
Nothing. It can ship before or after the medication changes. If it ships first the medication tables simply join the encrypted file when they are created.

## Non-goals
- No own PIN or password, no recovery flow.
- No claim of protection while the app is unlocked and running, or against malware with the user's access.
- No cloud key escrow, no key export.
