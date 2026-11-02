## Context

See the proposal. [ADR 0002](../../../../docs/adr/0002-hexagonal-architecture.md) keeps `:domain` and `:data` free of Android; this change only reorganises `:app`. The earlier change `app-scoped-lock-and-activity-free-viewmodels` already moved the lock and the restore result out of the Activity; this one moves the screen state.

## Goals / Non-Goals

**Goals:** a state rule can be tested without an Activity; `onCreate` reads as wiring; adding a screen or a setting touches one holder, not the Activity.
**Non-Goals:** a ViewModel per concern, Compose Navigation, Hilt (separate change), changing any behaviour or layout, splitting `ProfileScreen` itself.

## Decisions

### 1. Plain state holders, not ViewModels
Navigation and settings state is UI state rebuilt from preferences and `rememberSaveable`; it does not need to outlive the Activity the way data does. `AppNavigationState` has a `Saver` and is created with `rememberSaveable`; `AppSettingsState` is `remember`ed over the preferences. This avoids moving every variable into a ViewModel just to shorten a file, and avoids a ViewModel that needs Activity-bound callbacks again (the problem the previous change removed).

### 2. What is Activity-bound stays there
Anything that needs `this` as a `Context`, a result launcher, the window or `recreate()` stays in `MainActivity` and reaches the holders as lambdas (`onLanguageChange = { settings.setLanguage(it); recreate() }`). The holders never hold the Activity.

### 3. Where the lines go
| Today in `onCreate` | Moves to |
|---|---|
| `currentTab`, `showPillbox`, `showMindfulness`, `openPillboxOnToday`, `logMigraineTab`, banner flag, three `BackHandler`s | `AppNavigationState` |
| language, region, units, theme, notice, snooze, details, lock and secure-screen values | `AppSettingsState` |
| the tab-change `LaunchedEffect`s and `onBackupRestored` | `ViewModelRefresher` |
| `hasMedication` effect, `showPermissionDialog`, the two flags | `ReminderPermissionState` |
| Scaffold, top bar, tab `when`, dialogs | `AppContent.kt` |
| the 20-parameter `ProfileScreen(...)` call | `ProfileRoute` (own file, `ui/shell/ProfileRoute.kt`) |

### 4. Refresh rules stay equivalent
`ViewModelRefresher` keeps the exact current behaviour: a tab change clears the messages of all six ViewModels except on the first composition (ADR 0015: a banner survives the recreation caused by a language change); opening the Log tab reloads the migraine profile; a restore reloads the five loaders and `reminders.changed()`. Existing screenshots must not change.

### 5. Testing
The holders use `mutableStateOf`, which works in a plain JVM test. The refresher takes small function-typed ports or the real ViewModels with their existing fakes. No instrumented test is added; the emulator pass covers rotation, a language change, a notification tap and a restore.

## Risks / Trade-offs

- **Saved state:** moving five `rememberSaveable` values into one `Saver` must keep surviving rotation and process death. Verify both on the emulator.
- **Recomposition scope:** passing a whole holder to a screen can widen what recomposes. Pass the values and lambdas a screen needs, not the holder, except `ProfileRoute`.
- **A mechanical refactor of a central file:** move one piece at a time, run the tests after each move, and keep one behaviour check per step (see tasks).
