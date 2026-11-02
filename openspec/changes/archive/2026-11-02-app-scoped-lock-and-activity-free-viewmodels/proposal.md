## Why

A review of the manual wiring found that the Activity owns things that must outlive it:

- **Lock state.** `AppLockManager` was created by `MainActivity` (`by lazy`). Every time the Activity was recreated (rotation, a language or region change) a new manager started `Locked`, so the person had to authenticate again while using the app. The same state also lost its place in the lifecycle: a resume that followed no new backgrounding was measured against an old background timestamp.
- **ViewModel lambdas.** `BackupViewModel` and `LoggingViewModel` are created once and survive Activity recreation, but their factory lambdas captured the Activity that built them. After a recreation the ViewModel still called `recreate()` and `lifecycleScope.launch` on the first, destroyed Activity (so a restore after a rotation did not recreate the screen or re-arm the reminders), and it kept that Activity in memory.

## What Changes

- **App lock**: `AppLockManager` (and its `AppLockPreference`) is owned by `HealthJournalApp`, so its state lives as long as the process. `MainActivity` uses `app.appLock`. `onStop` no longer starts the lock timeout during a configuration change or `recreate()`, and `onForegrounded` consumes the background timestamp, so a resume is only measured against the backgrounding that preceded it.
- **`BackupViewModel`**: the `onRestored` lambda is replaced by an `events` flow (`BackupEvent.Restored(preferencesApplied)`), collected by the Activity while it is started. `applyPreferences` only writes preferences; the Activity decides to recreate. The file IO uses the application's `ContentResolver` and the version comes from `BuildConfig.VERSION_NAME`.
- **`UnitPreference` and `BackupPreferenceBridge`** are owned by `HealthJournalApp`, so the `units` and `preferences` lambdas the ViewModels hold do not reference the Activity.
- Unit tests cover the restore events.
- No schema change, no new dependency, no new strings.
- Behaviour change: reopening the app within the chosen lock timeout after it was closed (process still alive) no longer asks again, which is what the timeout setting says. Rotation and language changes no longer ask at all. A cold start, and a return after the timeout, still lock.

## Capabilities

### Modified Capabilities

- `platform-android`: ViewModels hold no Activity; one-shot results leave a ViewModel as events.
- `privacy`: the app lock is not triggered by an Activity being recreated.

## Impact

- `HealthJournalApp.kt`, `MainActivity.kt`, `lock/AppLockManager.kt`, `ui/backup/BackupViewModel.kt`.
- `BackupViewModelTest.kt`.
- `CHANGELOG.md`, `docs/dev/architecture.md`, `docs/dev/conventions.md`.
- Prepares for `adopt-hilt-dependency-injection`: after this, no ViewModel dependency is tied to the Activity.
