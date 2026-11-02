## 1. Prepare

- [x] 1.1 Record the `MainActivity.kt` line count and the longest function, for comparison: 573 lines; `onCreate` is 307 lines (266-572, the `setContent` lambda alone 280); the next longest function is `restartApp`-sized (under 20 lines), so `onCreate` is the only one over the limit
- [x] 1.2 Emulator baseline (current code, Pixel_7_Pro, Dutch): the pillbox, Mindfulness and the History tab survive rotation (portrait, landscape, portrait); the Profile "saved" banner survives a language change to English; a notification-tap intent while Mindfulness is open shows the pillbox on Today, and Back then returns to the tab underneath (History)

## 2. Move one piece at a time (tests after each)

- [x] 2.1 `AppNavigationState` with its `Saver`, `BackHandler` rules and unit tests
- [x] 2.2 `ViewModelRefresher` (tab-change clears, Log-tab migraine reload, restore reloads) and unit tests
- [x] 2.3 `ReminderPermissionState` (flags and the ask-once rule) and unit tests
- [ ] 2.4 `AppSettingsState` (write-through to the preferences); `ProfileRoute` replaces the inline `ProfileScreen(...)` call
- [ ] 2.5 `AppContent.kt`: Scaffold, top bar, tab switch and dialogs; `onCreate` reduced to wiring

## 3. Verify

- [ ] 3.1 Unit tests and lint pass
- [ ] 3.2 Emulator: repeat 1.2, plus restore after rotation, a lock timeout, the lost-key screen, light and dark
- [ ] 3.3 `MainActivity.kt` under about 250 lines, no function over about 80
- [ ] 3.4 Update `docs/dev/architecture.md` and `docs/dev/conventions.md`; add the `CHANGELOG.md` line
