# 39. Use case or handler

- **Date:** 2026-11-05
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

[ADR 0036](0036-application-boundary-commands-and-queries.md) made the handler the one entry point for a command or query and kept "use cases with real logic". It did not say where the line is. After the move into context packages, 14 use case files remained in `:domain`, and a review found two kinds:

- Seven classes had no production caller at all (the oxygen saturation record, update and delete, and the migraine record, update, delete and end). Their handlers had grown the same logic, with tests of their own, so the use cases survived only through tests that exercised them.
- The rest are called from one handler or one platform adapter, some with real orchestration (the pillbox day, adherence, history, backup) and some that only forward to a port.

Two places holding the same rule drift apart, and a reader cannot tell which one the app runs.

## Decision

1. **The handler holds the logic by default.** A handler validates, builds the model, calls the ports and returns a `DomainResult`. A rule used by one request lives in that handler.
2. **A use case exists only when its logic is shared or is real orchestration.** That means it is called from a platform adapter (alarm receiver, reminder coordinator), from another use case, or from more than one handler, or it combines several ports or derives a result over time (the pillbox day, adherence, history, re-arming reminders, the backup use cases). A handler then wraps it, as ADR 0036 point 6 says.
3. **A use case that only forwards to one port is not worth a class.** The handler calls the port. Existing forwarders are folded into their handler when that code is next touched, not in a sweep.
4. **No dead use cases.** `ArchitectureGuardrailsTest.everyUseCaseIsUsedByProductionCode` fails when a `*UseCase` class in `:domain` is referenced by no other production file. Tests do not count as a caller.

## Consequences

- The seven dead use cases and their tests are removed. Coverage moved to the handler tests (`MetricCommandHandlersTest`, `MigraineHandlersTest`); the oxygen notes and comment checks became a handler test.
- A new metric adds record and update handlers and nothing under `usecase/` ([how to add a metric](../dev/how-to/add-a-metric.md)).
- A use case that loses its last caller fails the build, so it is deleted rather than left behind.
- Single-caller wrappers that remain (the medication archive, schedule change and delete, the migraine tag rename and archive) are a known, accepted leftover; point 3 says how they go.
