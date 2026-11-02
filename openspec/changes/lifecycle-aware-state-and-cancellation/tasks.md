## 1. Cancellation

- [x] 1.1 Rethrow `CancellationException` before each broad catch in `BackupViewModel`
- [x] 1.2 `BackupViewModelTest`: an ordinary failure is reported, a cancellation is not

## 2. Lifecycle-aware collection

- [x] 2.1 Add `androidx.lifecycle:lifecycle-runtime-compose` to the version catalog and the app module
- [x] 2.2 Replace the 11 `collectAsState()` calls with `collectAsStateWithLifecycle()`

## 3. Docs and checks

- [x] 3.1 CHANGELOG entry and a note in `docs/dev/architecture.md` and `docs/dev/conventions.md`
- [x] 3.2 Unit tests and lint pass, `scripts/check-docs.sh` passes
