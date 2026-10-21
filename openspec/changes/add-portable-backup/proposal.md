# Add Portable Backup

## Why
The database is encrypted with a key that never leaves the phone (ADR 0023), so Android's own backup cannot restore it, and a lost key, a new phone or a factory reset means the data is gone. CSV export is the only way out today: it is plaintext, it leaves out the profile and all medication, and it cannot rebuild the app. The user wants a safe backup that does not depend on any vendor or cloud, and a documented format so the data stays reachable on another platform (for example an iPhone port) or when this app is no longer maintained.

## What Changes
- **Backup file (`.hjbackup`):** one file with the whole journal (every profile, all measurements with comments, all medication with schedule versions and intake log), encrypted with a passphrase the user chooses. Format: a small binary container (Argon2id key derivation, AES-256-GCM) around a gzip-compressed JSON document with a published JSON Schema.
- **Create backup (manual):** Profile screen, "Data storage & backups" card, "Create backup". The user chooses a passphrase (typed twice, with a warning that it cannot be recovered) and a destination through the system file picker.
- **Restore backup:** Profile screen "Restore backup" (merge into the current data, never silently replace) and, on the lost-key screen, "Restore from a backup" next to the delete action.
- **CSV export stays:** it remains the plaintext, spreadsheet-friendly export, now with an explicit warning that the file is not encrypted.
- **Published format and recovery guide:** `docs/backup-format/` holds the byte-level specification, the JSON Schema, test vectors, a standalone reference decoder (`hjbackup.py`) and a step-by-step guide to read the data without the app. The developer docs gain a backup page with diagrams.
- **No vendor dependency:** no network, no Google Play Services, no Android Auto Backup of the data. Location is the user's choice through the Storage Access Framework.

## Capabilities
- **New Capability:** `data-backup` (container format, payload, create, restore, merge, failure handling)
- **Modified Capability:** `privacy` (a user-controlled encrypted backup now exists; the notice wording changes)
- **Modified Capability:** `data-export` (CSV export carries a plaintext warning; it is no longer called the only way to move data)

## Impact
- **Database:** none. The Room schema stays at version 6. The backup format has its own `schemaVersion` and is independent of Room migrations, so old backups stay readable.
- **Stored data shape:** unchanged. Preferences (language, theme, units, lock) are not part of the backup.
- **Affected code:** a pure backup model, JSON writer and reader, container encoder and decoder and a `BackupPort` in `:domain`; the Argon2id and AES-GCM adapter and the repository snapshot and merge in `:data`; the Profile card, passphrase dialogs, restore flow and the lost-key screen in `:app`; strings in English and Dutch.
- **Dependencies:** one, for Argon2id (the platform has no implementation). Candidates and the choice are in the design and ADR 0024; AES-GCM, SecureRandom, gzip and JSON come from the platform.
- **Docs:** `docs/backup-format/` (new), `docs/dev/backup.md` (new), `docs/dev/README.md`, `docs/dev/architecture.md`, `docs/dev/c4.md`, `docs/dev/privacy-and-security.md`, README, CHANGELOG, in-app privacy notice, ADR 0024, `openspec/specs/README.md`.

## Non-goals
- No scheduled or automatic backup in this change (possible later, see the design).
- No cloud, no sync, no sharing target other than what the system file picker offers.
- No backup of app preferences or the lock setting.
- No recovery of a forgotten passphrase: there is no escrow and no backdoor.
- No importer for other apps' formats beyond the existing CSV import.
