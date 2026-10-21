## 0. Decisions
- [x] 0.1 Backup must be safe, vendor-free and platform-neutral (requested 2026-10-21)
- [x] 0.2 Encrypted with a user passphrase; the unencrypted CSV export stays, with a warning
- [x] 0.3 Manual first; scheduled backup possible later (design decision 10)
- [x] 0.4 Argon2id (suggestion approved)
- [x] 0.5 Specification, JSON Schema, ADR, developer docs and a "recover without the app" guide come before the code
- [x] 0.6 Spec review by the user before implementation starts (approved 2026-10-21)
- [x] 0.7 Argon2 library: Bouncy Castle 1.86 behind `BackupKeyDerivationPort` (ADR 0024 Accepted; `argon2kt` not chosen)

## 1. Specification and published format (done in the proposal step)
- [x] 1.1 `docs/backup-format/README.md`: container, key derivation, payload, versioning
- [x] 1.2 `docs/backup-format/healthjournal-backup.schema.json` (validated against the sample)
- [x] 1.3 `docs/backup-format/hjbackup.py` reference encoder and decoder, test vectors generated with it
- [x] 1.4 `docs/backup-format/recover-your-data.md`: how to read the data without the app
- [x] 1.5 ADR 0024, `docs/dev/backup.md`, architecture and C4 additions, privacy page

## 2. Domain (`:domain`)
- [ ] 2.1 `BackupDocument` model (profiles, entries, medication, schedule versions, intakes, optional display preferences) with no Android types
- [ ] 2.2 Ports: `BackupSnapshotPort` (read everything, merge), `BackupCodecPort` (document to bytes and back), `BackupKeyDerivationPort`
- [ ] 2.3 Use cases `CreateBackup` and `RestoreBackup` (merge counts), passphrase rules (min 8, NFKC)
- [ ] 2.4 JVM tests: passphrase rules, merge counts, restore twice

## 3. Data (`:data`)
- [ ] 3.1 Argon2id adapter (library per ADR 0024) and AES-256-GCM, gzip, `SecureRandom` salt and nonce
- [ ] 3.2 Container encoder and decoder with the reader limits and header authentication
- [ ] 3.3 JSON writer and reader with `org.json`, fixed field order, decimals as strings
- [ ] 3.4 Room snapshot and merge in one transaction (all tables, schema versions of medication)
- [ ] 3.5 Tests with the published vectors (`docs/backup-format/test-vectors`) decoded to the expected document; tamper, truncation, wrong passphrase, hostile header; round trip through an in-memory database

## 4. App (`:app`)
- [ ] 4.1 Profile card: "Create backup" and "Restore backup", passphrase dialogs (twice, min length, warning)
- [ ] 4.1a Passphrase hint ("three or four random words are stronger and easier to remember than eight random characters"), no scoring or colouring
- [ ] 4.2 Document picker contracts (create and open), read-back verification, result banner with counts
- [ ] 4.2a Restore dialog: "Also restore settings" (display preferences only, default on when the file has them; never lock, secure screen, lock-screen details)
- [ ] 4.3 Lost-key screen: "Restore from a backup" (decrypt first, then delete, then import)
- [ ] 4.4 CSV export warning before the destination is chosen
- [ ] 4.5 Strings in English and Dutch for every label, warning and message
- [ ] 4.6 Privacy notice text updated (EN, NL); `PrivacyChecksTest` still green (no INTERNET, no logging)
- [ ] 4.7 Check on the emulator: create, restore into a cleared app, wrong passphrase, cancelled picker, lost-key restore, light and dark, English and Dutch

## 5. Docs
- [ ] 5.1 README (key features, roadmap), CHANGELOG, `docs/dev/privacy-and-security.md`, `docs/dev/testing.md`
- [ ] 5.2 Merge the deltas into `openspec/specs` (data-backup, privacy, data-export) and archive the change
- [ ] 5.3 `bash scripts/check-docs.sh` and `npx --no-install openspec validate --all --strict` pass
