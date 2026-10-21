## Context

Today the only way out of the app is CSV (plaintext, measurements only). The database is SQLCipher-encrypted with a device-bound key (ADR 0023), so Android Auto Backup is excluded on purpose, and a lost key ends in the delete-only recovery screen. The user wants a safe, vendor-free backup, and a format documented well enough that another platform (an iPhone port) or a person without the app can read it. See [ADR 0024](../../../docs/adr/0024-portable-encrypted-backup.md) for the decision record and [docs/backup-format](../../../docs/backup-format/README.md) for the byte-level format.

## Goals / Non-Goals

**Goals:**
- One file holds everything worth keeping; restoring it on a new phone, after a reset, or after a lost key rebuilds the journal.
- Safe at rest: the file is useless without the passphrase, and tampering is detected.
- Platform-neutral: the format is defined by a document, not by Room, SQLCipher or Android.
- Readable without the app: published schema, test vectors and a standalone decoder.
- No vendor, no network, no account.

**Non-Goals:**
- Scheduled backups, cloud storage, sync.
- Backing up security and privacy settings.
- Passphrase recovery.

## Decisions

### 1. The backup is a document, not the database file
Copying the SQLCipher file would tie the format to Room's schema, SQLCipher's version and the device key. A JSON document with its own `schemaVersion` survives all three. Room migrations (next is 6 to 7) never change the backup; a writer maps the current database to schema version 1 and a reader maps schema version 1 into the current database. A breaking document change raises `schemaVersion`, an additive one does not (readers ignore unknown properties).

### 2. Container: Argon2id + AES-256-GCM, fixed header, header as associated data
Only standard primitives that every platform has (CryptoKit, JCA, libsodium, OpenSSL 3.2+, Python `cryptography`). The header carries the algorithm ids and the Argon2 parameters, so the defaults can be raised later without a new container version, and the whole header is authenticated. Defaults are 64 MiB, 3 iterations, 1 lane (RFC 9106 asks for at least 64 MiB with t=3 as the memory-constrained profile; this fits low-end phones). Reader limits (1 GiB, 100 iterations, 16 lanes) stop a hostile file from exhausting memory. Considered and rejected: PBKDF2 (no memory hardness, GPU-friendly), scrypt (fine, but Argon2id is the current recommendation and has the same support), a key derived from the device key (would not survive a lost key, the main reason for the backup), and an ad-hoc format (nothing for a port to verify against).

### 3. Compression before encryption
gzip is in every platform's standard library. A year of daily entries is a few hundred KB of JSON and compresses well. Compress-then-encrypt leaks the compressed length only; the plaintext is not attacker-influenced, so the known compression-oracle attacks do not apply. The compression id is in the header, so it can be turned off.

### 4. Argon2id is the only new dependency
The platform has no Argon2. Writing a cryptographic primitive ourselves is not an option, so a library is wrapped behind a small `:domain` port (`BackupKeyDerivationPort`) and implemented in `:data`. The choice and its criteria (licence, maintenance, 16 KB page-size support for the native library on Android 15+, correctness against the published test vectors) are in ADR 0024. The port keeps the library replaceable without touching the format. The published test vectors are run against the chosen library in unit tests.

### 5. JSON with the platform's `org.json`
No serialisation library: `org.json` ships with Android, and the unit tests use the same API through a test-only artifact. The writer emits fields in a fixed order, so equal data gives equal documents (the file as a whole still differs, because of salt and nonce).

### 6. Merge restore, keyed by identifier
Every entity already has a stable identifier. A restore inserts what is missing and keeps what exists, so restoring twice is harmless and a restore into a phone that has new data does not destroy it. Replace-everything is not offered; the lost-key flow reaches the same result by first discarding the unreadable data. The whole restore runs in one Room transaction. Medication rows are keyed by identifier and by their natural keys (`medicationId + effectiveFrom`, `medicationId + planned`), as in the tables.

### 7. Restore from the lost-key screen
`DatabaseRecovery` already deletes the unreadable data and key material and restarts the app. The new flow does that, creates a fresh key and database, then imports. The restore therefore needs the passphrase and the file first (so a wrong passphrase never destroys anything): decrypt and validate in memory, then delete, then import.

### 8. Verification after writing
After writing the file the app reads it back through the same decoder and only then reports success, so a full disk or a broken storage provider is noticed at once, not at restore time.

### 9. Passphrase handling
Minimum 8 characters, typed twice, never stored, never logged, held in a `CharArray` and cleared after use. While typing, a neutral hint (not a verdict, no colours that judge) suggests that a phrase of three or four random words is both stronger and easier to remember than eight random characters, and that length matters more than symbols. There is no stored passphrase hint, no escrow and no recovery. The dialog says so in plain words (English and Dutch). A passphrase manager works; the field does not block pasting.

### 10. Scheduled backup, later
A later change can add a periodic backup to a folder the user picks (a persisted document tree URI and `WorkManager`). It needs the passphrase at run time, so it has to keep a secret on the device: the options are a key wrapped by the Android Keystore (weaker, because the phone can then decrypt its own backups) or asking the user each time (not automatic). That trade-off needs its own decision, so it is out of scope here. The container does not change for it.

### 11. Display preferences in, security settings out
The file carries language, region, unit system, glucose unit, theme and snooze length, so a new phone feels the same. They are applied only when the user ticks "Also restore settings" (default on), because they are overwriting values, not merged ones. App lock, its timeout, the secure screen and the lock-screen detail switch are excluded on purpose: a backup file from anywhere must never be able to turn protection off, and these belong to the device and its credentials. Per-device state (accepted medication notice, permission prompts) is excluded too. Values are tags with `system` meaning follow the device; unknown values are ignored, so older apps still read newer files.

## Risks / Trade-offs

- **Forgotten passphrase means lost backup.** Accepted and stated at creation; the alternative is an unencrypted health file.
- **A weak passphrase weakens the file.** Argon2id slows guessing but cannot save "password123"; the minimum length and the hint help; a scored strength meter is not added (no judgement text).
- **Native library for Argon2.** One more `.so` to align for 16 KB pages and to keep updated. Mitigated by the port and the test vectors.
- **The backup is a decrypted copy once restored elsewhere.** The user chooses where it goes; the app cannot control the destination.
- **Format drift between app and docs.** Mitigated by the shared test vectors (app unit tests and the reference decoder run the same files) and by the spec rule that a format change updates all of them together.
- **Memory.** 64 MiB during derivation; fine for the minSdk 26 devices that run the app at all, and the derivation runs off the main thread.
