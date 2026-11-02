# Conventions

Short rules that keep the code and the docs consistent. When a rule has a reason, the reason is linked.

## Architecture

- `domain` is pure Kotlin: no Android, no Room, no Compose. Dependencies point inward: `app` and `data` depend on `domain`, never the reverse ([ADR 0002](../adr/0002-hexagonal-architecture.md)).
- One use case per class, with a single `operator fun invoke`.
- Wiring is Hilt, in `app` only ([ADR 0035](../adr/0035-hilt-for-dependency-injection.md), [how it works](dependency-injection.md)): `@Provides` functions in `app/src/main/java/nl/healthjournal/app/di/` build the use cases from their ports, because `domain` and `data` never import Hilt, Dagger or `javax.inject` (a guardrail test checks it). A new use case is one `@Provides` function in `UseCaseModule`.
- Prefer the Android SDK, AndroidX and official Kotlinx libraries. A new third-party dependency needs an ADR.

## Data

- Store metric units only. Units are a display setting ([ADR 0014](../adr/0014-units-presentation.md)).
- Never use destructive database migrations. See [Change the database](how-to/change-the-database.md).
- Invalid values must not be constructible: validate in the domain value object.

## Text and UI

- No hard-coded user-facing text. Strings go in English and Dutch together ([Add a translation](how-to/add-a-translation.md)).
- ViewModels expose `UiText`, not resolved strings ([ADR 0015](../adr/0015-localized-viewmodel-messages.md)).
- Layouts must survive long Dutch text and larger fonts ([ADR 0010](../adr/0010-responsive-dutch-ui-layout.md)).
- Touch targets are at least 48 dp and icons have a content description.
- Ask `isAppDarkTheme()` (in `ui/theme/Theme.kt`) whether dark is active, never `isSystemInDarkTheme()`, so the Light, Dark and System choice in Profile applies everywhere ([ADR 0017](../adr/0017-in-app-theme-choice.md)).
- The app is a logging tool, not a medical device. Text gives information only: no advice, no diagnosis, no condition names ([range labels](how-to/change-a-range-label.md)).

## Kotlin style

- Follow the official Kotlin style and the style of the file you are editing.
- Name things after the domain, not the technology (`BloodPressureReading`, not `BpDto`).
- Comment why, not what. Keep comments short.
- Collect a `StateFlow` in a screen with `collectAsStateWithLifecycle()`, not `collectAsState()`.
- A ViewModel never captures the Activity (no `recreate()`, `lifecycleScope`, `contentResolver` or Activity-context preference). Take what it needs from the Hilt graph (`@ApplicationContext`, a `@Singleton` preference), or send an event the Activity collects.
- A ViewModel is `@HiltViewModel`. Keep the plain primary constructor for the unit tests and add a secondary `@Inject constructor`; when it delegates to the primary one, upcast an argument so it does not call itself. Two screens that need separate instances of one ViewModel get two types (`RecoveryBackupViewModel`), not a string key.
- Code the system creates (a `BroadcastReceiver`) takes its objects through an `@EntryPoint` and `EntryPointAccessors.fromApplication`, never by casting the Application.
- Keep `MainActivity` to what needs the Activity. Screen state goes in a plain holder in `ui/shell` that takes its inputs as constructor parameters or functions and has a JVM unit test; the Activity passes in what only it can do as lambdas. A holder never keeps an Activity reference.
- A property with a `private set` and a `fun setX` clash on the JVM signature; name the function `changeX`.
- Around a suspending call, rethrow `CancellationException` before a broad `catch (e: Exception)`, so a cancelled operation stops instead of being reported as a failure.

## Commits

- Use a short prefix: `feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`.
- First line in the imperative, under about 72 characters. The body explains why.
- One logical change per commit.

## Architecture Decision Records

Write an ADR when you make a choice that is costly to reverse or that a newcomer would question: a new dependency, a storage format, a rule about wording. ADRs live in `docs/adr/`, numbered in order. Copy the structure of the latest one (context, decision, consequences) and link it from the README list. Do not rewrite an accepted ADR: add a new one that supersedes it.

## Specs (OpenSpec)

Specs in `openspec/specs/` say **what** the app does, ADRs say **why**, the code says **how**. A behaviour change goes through an OpenSpec change (proposal, tasks, delta specs) described in [CONTRIBUTING.md](../../CONTRIBUTING.md). Never name specific medicines in a spec.
