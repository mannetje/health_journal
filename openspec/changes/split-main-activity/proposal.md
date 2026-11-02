## Why

`MainActivity.kt` is 573 lines and `onCreate` alone is about 300 of them. Besides the Activity's own job it holds:

- **Navigation state:** the current tab, whether the pillbox or mindfulness is open, the migraine tab of the Log screen, the encryption banner, three `BackHandler`s, and the rule that opening one screen closes the other.
- **Settings state:** 15 `remember { mutableStateOf(...) }` values (language, region, units, theme, notice, lock, secure screen, snooze, lock-screen details), each read from a preference and written back by a lambda inline in the `ProfileScreen(...)` call.
- **Cross-ViewModel coordination:** which ViewModels reload after a restore, which clear their messages on a tab change, when the migraine tab reloads.
- **Reminder permission flow:** the "ask once" rule, the dialog and the two permission flags.
- **Six ViewModel factories** and the recovery ViewModel (removed later by `adopt-hilt-dependency-injection`).

None of this is wrong, but it cannot be unit tested (it needs an Activity), and every new screen or setting adds to the same function. Most of it does not need the Activity at all: it needs preferences and ViewModels.

## What Changes

Keep in `MainActivity` what needs the Android framework, and move the rest into plain classes and composables:

- **Stays in the Activity:** `attachBaseContext` (locale), `onCreate` wiring, `onNewIntent`, `onResume`/`onStop` (lock and reminders), the Activity Result launchers (notification permission, device credential), the settings intents, `recreate()`, `restartApp`, the secure-screen window flag, and the lost-key screen.
- **`AppNavigationState`** (new, plain Kotlin with `mutableStateOf`): current tab, pillbox, mindfulness, Log migraine tab, encryption banner dismissal; methods such as `openPillbox()`, `openMindfulness()`, `openTab(tab)`, `onPillboxRequested()`. It has a `Saver`, so it survives rotation as the `rememberSaveable` values do now. The back-press rules become a function on it.
- **`AppSettingsState`** (new): the settings values and their write-through to the preferences. A `ProfileRoute` composable adapts it to `ProfileScreen`, replacing the 20-parameter inline call.
- **`ViewModelRefresher`** (new, small): `onRestored()`, `onTabChanged()` and `onProfileCreated()`: the "which ViewModels reload or clear" rules in one place, tested with fakes.
- **`ReminderPermissionState`** (new): the permission flags and the ask-once rule. The Activity only owns the launcher and the intents.
- **`AppContent`** (new composable file): the Scaffold, top bar, tab switch and dialogs, taking the state holders and the screens' ViewModels. `onCreate` shrinks to wiring.
- Unit tests cover the navigation rules (opening the pillbox closes mindfulness, Back order, notification tap) and the refresher.
- No schema change, no new dependency, no new strings, no visible change.

Target: `MainActivity.kt` under about 250 lines, no function over about 80, and every screen-state rule reachable from a test.

## Order

Do this **before** `adopt-hilt-dependency-injection`. The new classes take plain constructor parameters and hold no Activity, so Hilt can later provide or inject them unchanged, and the Hilt change then touches a thin Activity instead of a 573-line one. Doing it after would mean converting the factories in a file that is about to be rewritten. This change needs no Hilt.

## Capabilities

### Modified Capabilities

- `platform-android`: the Activity holds Android framework interactions only; screen and navigation state live in testable holders.

## Impact

- `MainActivity.kt`, new `ui/shell/` package in `:app` (`AppNavigationState`, `AppSettingsState`, `ViewModelRefresher`, `ReminderPermissionState`, `AppContent`).
- New unit tests under `app/src/test/.../ui/shell/`.
- `docs/dev/architecture.md` and `docs/dev/conventions.md` (where state lives, what the Activity may hold).
- `CHANGELOG.md`: a developer-facing line.
