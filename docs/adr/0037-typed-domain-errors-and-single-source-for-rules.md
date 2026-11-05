# 37. Typed domain errors and a single source for rules

- **Date:** 2026-11-05
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Validation limits lived in two places: the `require` checks in the value objects and a copy in the ViewModel (`takeIf { it in MIN..MAX }`). The waist bounds were written twice in `LoggingViewModel` alone. Failures reached the UI as exceptions, with a message in English, or as `UiTextException`. Eight classes in `:domain` defaulted a timestamp to `Instant.now()`, which tests could not control.

Text shown to the user must stay localized ([ADR 0015](0015-localized-viewmodel-messages.md)), so the domain cannot hand over a sentence.

## Decision

1. **`DomainResult<T>`** is `Success(value)` or `Failure(error)`. Every handler returns it ([ADR 0036](0036-application-boundary-commands-and-queries.md)).
2. **`DomainError` carries a code and parameters, never display text:**
   - `Invalid(field, rule, params)` for a rule the input breaks. `ValidationRule` is an enum (`OUT_OF_RANGE`, `REQUIRED`, `TOO_SHORT`, `MISMATCH`, `END_NOT_AFTER_START`, ...).
   - `NotFound(kind, id)` for an entry, medication or tag that does not exist. Existence is checked before the call, not caught afterwards.
   - `Backup(problem)` with a `BackupProblem` for the reasons a backup file cannot be opened.
   - `Storage(cause)` for anything else a port throws. `domainCatching` turns the exception into this and rethrows `CancellationException`.
3. **The UI maps errors to strings.** `DomainError.toUiText(units)` in `ui/common/DomainErrorUiText.kt` picks a string resource per rule, in English and Dutch. A test iterates the enum and resolves a string for every rule in both languages, so a new rule without a string fails the build.
4. **Outcomes that are not failures stay success values.** `AddTagResult.LimitReached` and `RenameTagResult.LabelTaken` are answers the picker shows, not errors.
5. **Safe factories beside validating constructors.** Value objects get `of(...)` returning `DomainResult`. The primary constructor keeps its `require` checks, so Room mapping, tests and any missed caller still cannot build an invalid object. Private constructors were rejected: they would break the mapper layer and 71 `require` sites for little gain.
6. **Bounds exist once**, as constants on the value object. The ViewModel passes typed input to the command and renders the returned error. Live previews (waist and glucose range chips) keep calling the pure classifiers.
7. **Time comes from a `Clock`.** Handlers take a `java.time.Clock`. A command carries the timestamp the user chose, or `null` for "now", and the handler resolves it. Handlers that default a medication date to "today" use `localClock(clock)`, the same instant in the device zone. A guardrail forbids `Instant.now()` in the domain and in any class that takes a `Clock`.

## Consequences

### Positive
- One place per rule. Changing a limit changes the value object, the error text and the preview together.
- The domain is testable without the UI and without wall-clock time. Handler tests pass a fixed `Clock`.
- Dutch users never see an English domain message.
- A cancelled screen no longer produces a storage error.

### Negative
- Every rule needs an enum entry and a string. The generic string is used where no specific one exists, and no new copy is added unless a change asks for it.
- A call site must handle `Failure` instead of relying on an exception. The compiler's `when` over `DomainResult` forces it.
- `UiTextException` remains for UI-only parse errors (a field that is not a number), so there are two error paths at the edge of a form.
