## Context

See the proposal. Hexagonal rule from [ADR 0002](../../../docs/adr/0002-hexagonal-architecture.md): `:domain` depends on nothing, `:data` on `:domain`, `:app` wires both. Dependency minimisation from [ADR 0003](../../../docs/adr/0003-dependency-minimization.md) applies: Hilt is a deliberate exception that the new ADR justifies.

## Goals / Non-Goals

**Goals:** no hand-written factories, a missing dependency fails the build, singletons have one owner, tests can replace a binding.
**Non-Goals:** moving code between modules, changing any ViewModel's behaviour, instrumented tests, multi-module Hilt, Compose navigation.

## Decisions

### 1. Hilt lives in `:app` only
`:domain` and `:data` keep plain constructors. A `di/` package in `:app` has the modules; classes from the other modules are bound with `@Provides`, never annotated. A guardrail test (like `RepositoryGuardrailsTest`) fails if `:domain` or `:data` import `dagger` or `javax.inject`.

### 2. Why Hilt and not the alternatives
- **Plain Dagger:** the same graph with more setup; Hilt's Android components and ViewModel support are the point.
- **Koin:** no annotation processing, but the graph is checked at runtime, which is the weakness being removed.
- **A hand-written `AppContainer` with one generic ViewModel factory:** no new dependency and removes most factory boilerplate, but the graph is still unchecked. This is the fallback if Hilt's build cost is not accepted.

### 3. Scopes
- `@Singleton` (process): database holder, repositories, use cases, preferences, `AppLockManager`, `ReminderCoordinator`.
- `@HiltViewModel` (ViewModel): the six ViewModels. No Activity scope is needed; that is what the prerequisite change removed.

### 4. The database under backup and restore
`RestoreBackupUseCase` and the encryption migration act on the database while the graph is live. The `DataModule` is bound as a singleton and its inner pieces stay behind it, so Hilt never holds a Room instance that a restore replaces. Verified by a restore on the emulator before and after.

### 5. Startup order
`DatabaseEncryptionStatus` and the SQLCipher key are set up before the first query. The `DataModule` is created lazily by Hilt on first injection, which must not move that work before the key is available, or to the main thread later than today. A task checks cold start (encrypted, plain-then-migrating and key-lost) on the emulator.

### 6. Receivers
Receivers are not Hilt-managed here (`@AndroidEntryPoint` on a `BroadcastReceiver` works but needs care with `goAsync`). They use `EntryPointAccessors.fromApplication` for the coordinator and use cases.

## Risks / Trade-offs

- KSP and generated code add build time; measure before and after and stop if the cost is out of proportion to the six factories removed.
- Annotation errors are cryptic; the first ViewModel is converted alone to learn them.
- Another thing to upgrade (Hilt, KSP and Kotlin versions move together).
