## 0. Decisions
- [x] 0.1 Database encryption (SQLCipher with Keystore-wrapped key) and an opt-in app lock (device credential) are one separate change, each recorded in ADR 0023 as an exception to ADR 0003 (approved; split approved 2026-10-06)
- [ ] 0.2 Backup: keep `allowBackup="true"` and exclude the database and wrapped key through extraction rules (needs owner confirmation before implementation)

## 1. Domain
- [ ] 1.1 `AppLockPort` and a lock state (locked, unlocked, timeout) with tests; setting stored in the existing preferences

## 2. Data
- [ ] 2.1 `DatabaseKeyProvider` (Keystore AES-GCM wrapping, no user-authentication binding, no-backup storage) and a `SupportOpenHelperFactory` for Room, with an instrumented test on key creation and reopen
- [ ] 2.2 One-time plain to encrypted migration with row-count verification, keep the plain file until verified, failure keeps data and retries; tests on a seeded database including edge cases (empty, large, interrupted)

## 3. App
- [ ] 3.1 `BiometricPrompt` with device credential, API 26 fallback, no-screen-lock handling, lock overlay that composes no health data, timeout options
- [ ] 3.2 Secure-screen setting (`FLAG_SECURE`)
- [ ] 3.3 Data extraction rules per decision 0.2, and a check that the encrypted files and the wrapped key are excluded
- [ ] 3.4 Privacy notice and README: data cannot be restored from backup or moved between devices, CSV export is the way (English and Dutch)
- [ ] 3.5 English and Dutch strings for the lock, prompt text, migration messages; layout check at 1.3x and 2.0x
- [ ] 3.6 Emulator and real-device check, including biometric enrolment change, reboot, an interrupted migration and a restore on another device

## 4. Docs
- [ ] 4.1 ADR 0023: threat model, why device credential and not an own PIN, why SQLCipher, licence, size and 16 KB page check, fallbacks, honest limits, ADR 0003 exception
- [ ] 4.2 README, CHANGELOG (Unreleased), affected specs, the ADR index and `scripts/check-docs.sh`
- [ ] 4.3 Archive the change and merge the deltas into `openspec/specs`
- [ ] 4.4 Commits carry no attribution trailer
