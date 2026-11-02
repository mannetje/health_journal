## Why

A code review found two small coroutine and lifecycle issues in the UI layer:

- `BackupViewModel` catches `Exception` in five places. `CancellationException` is an `Exception`, so a cancelled backup or restore (the ViewModel is cleared mid-operation) was handled as an ordinary failure: the state was set to an error and, for a restore, the opened document was dropped as if it had failed.
- Screens collect their `StateFlow` with `collectAsState()`, which keeps collecting while the app is in the background. `collectAsStateWithLifecycle()` stops collecting below the `STARTED` state, so nothing is recomposed for a screen nobody can see.

## What Changes

- **`BackupViewModel`**: every `catch (e: Exception)` is preceded by `catch (e: CancellationException) { throw e }`, so cancellation stops the operation without reporting an error.
- **Screens**: the 11 `collectAsState()` calls in `app/src/main` become `collectAsStateWithLifecycle()`, using the new `androidx.lifecycle:lifecycle-runtime-compose` dependency (same version as the other lifecycle libraries).
- A `BackupViewModelTest` checks that an ordinary failure is reported and that a cancellation is not.
- No visible change, no data change, no schema change.

## Capabilities

### Modified Capabilities

- `platform-android`: UI state is collected with the lifecycle, and a cancelled operation is not reported as a failure.

## Impact

- `BackupViewModel.kt`, the screens that collect state, `gradle/libs.versions.toml`, `app/build.gradle.kts`.
- `app/src/test/java/nl/healthjournal/app/ui/backup/BackupViewModelTest.kt`.
- `CHANGELOG.md`, `docs/dev/architecture.md`, `docs/dev/conventions.md`.
