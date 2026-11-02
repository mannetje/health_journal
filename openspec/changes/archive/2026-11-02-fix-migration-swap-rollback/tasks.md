## 1. Fix

- [x] 1.1 Make `swapIn` restore all set-aside files on any failure, with an injectable rename for tests
- [x] 1.2 Add `PlainToEncryptedMigration.isSetAside` and stop `create()` from making a new database while the original is set aside

## 2. Tests and docs

- [x] 2.1 Fault-injection test: fail each rename in turn, assert files and data are back, and that the retry succeeds
- [x] 2.2 Test `isSetAside`
- [x] 2.3 CHANGELOG entry and `docs/dev/privacy-and-security.md`
- [x] 2.4 Unit tests and lint pass
