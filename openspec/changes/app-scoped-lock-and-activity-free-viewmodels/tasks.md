## 1. App lock

- [x] 1.1 `HealthJournalApp` owns `appLock` (with its `AppLockPreference`); `MainActivity` uses it
- [x] 1.2 `onStop` skips `onBackgrounded()` while `isChangingConfigurations`
- [x] 1.3 `AppLockManager.onForegrounded()` consumes the background timestamp

## 2. ViewModels without the Activity

- [x] 2.1 `HealthJournalApp` owns `unitPreference` and `backupPreferences`
- [x] 2.2 `BackupViewModel`: `onRestored` becomes `events` (`BackupEvent.Restored`); `applyPreferences` no longer recreates
- [x] 2.3 `MainActivity` collects `events` while started, then reloads, re-arms reminders and recreates when settings changed
- [x] 2.4 `ContentResolverBackupFileIo` gets the application's resolver; `BuildConfig.VERSION_NAME` replaces `packageManager`
- [x] 2.5 `BackupViewModelTest`: restore events, with and without settings

## 3. Verify and document

- [x] 3.1 Unit tests and lint pass
- [x] 3.2 Emulator: language change with the lock on does not lock; backup restore after a rotation refreshes; a cold start locks
- [x] 3.3 CHANGELOG entry and notes in `docs/dev/architecture.md` and `docs/dev/conventions.md`; `scripts/check-docs.sh` passes
