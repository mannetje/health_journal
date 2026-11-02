## Why

Dependencies are wired by hand: `HealthJournalApp.onCreate` builds the `DataModule`, about 25 use cases and the reminder coordinator, and `MainActivity` builds six ViewModels with six hand-written `ViewModelProvider.Factory` classes. This works and is easy to read at the current size, so this change is **optional**. It is worth doing only if the boilerplate starts to cost more than a DI library does: each new metric adds a use case, a ViewModel parameter, a factory parameter and a line in two places, and a missing wire is found at runtime (`lateinit` not initialised), not at build time.

It depends on `app-scoped-lock-and-activity-free-viewmodels`: Hilt cannot inject a lambda that captures an Activity, so no ViewModel dependency may be tied to the Activity first. It is best done after `split-main-activity`, so the factories are removed from a thin Activity instead of a 570-line one.

## What Changes

- Add Hilt (Gradle plugin, `hilt-android`, the compiler through KSP, `hilt-navigation-compose` only if `hiltViewModel()` is used).
- `HealthJournalApp` becomes `@HiltAndroidApp`, `MainActivity` `@AndroidEntryPoint`.
- A `@Module @InstallIn(SingletonComponent::class)` in `:app` provides the singletons through `@Provides`: the `DataModule` pieces and ports, the use cases, the preferences, `AppLockManager`, `ReminderCoordinator`, the clock. `:domain` and `:data` stay free of Hilt and of annotations: their classes are not annotated.
- The six ViewModels become `@HiltViewModel` with `@Inject constructor`; their factories and the `by viewModels { Factory(...) }` blocks are deleted. The recovery ViewModel for the lost-key screen becomes a second `@HiltViewModel` type or a `@Named` binding.
- Receivers (`RescheduleReceiver`, reminder actions) get their dependencies through an `@EntryPoint`.
- Tests keep constructing ViewModels directly; no instrumented test is added by this change.
- No schema change, no visible change, no new strings.

## Capabilities

### Modified Capabilities

- `platform-android`: dependencies are provided by Hilt; the domain and data modules do not depend on it.

## Impact

- `gradle/libs.versions.toml`, root and `app/build.gradle.kts`, `HealthJournalApp.kt`, `MainActivity.kt`, the six ViewModel files, the receivers, a new `di/` package in `:app`.
- Build time and APK size grow a little (KSP and the generated components); to be measured and recorded.
- `docs/dev/architecture.md`, `docs/dev/conventions.md`, the add-a-metric guide and playbook (where a use case is wired), a new ADR.
