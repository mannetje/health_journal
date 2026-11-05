# Move existing code into context packages

## Why
`refactor-application-boundaries` defines the bounded contexts (shared kernel, profile, metrics, medication, migraine, mindfulness, lock, backup), enforces the dependencies between them with a test, and places all new code in context packages. The existing code stays where it is: `:domain` is still organised by layer (`model/`, `usecase/`, `port/secondary/`), so a reader must look in three places to follow one context, and the context map exists only as a test and a document. 229 Kotlin files import these packages.

Doing the move inside the refactor would bury a behaviour-preserving restructure under a mechanical rename of hundreds of imports. It is a separate change so each can be reviewed and reverted on its own.

## What Changes
- **`:domain` packages become `nl.healthjournal.domain.<context>.{model,usecase,port,command}`** for `shared`, `profile`, `metrics`, `medication`, `migraine`, `mindfulness`, `lock` and `backup`. The `nhg` classifiers live in `metrics` (already decided).
- **Every existing file moves with its tests** (`domain/src/test` mirrors the main layout). No logic changes: `git mv` plus package and import edits only.
- **The layer folders disappear:** `model/`, `usecase/` and `port/secondary/` no longer exist as top-level folders.
- **Context guardrail extended:** the test from the refactor also checks that no file lies outside the context layout and that each file's directory matches its package.
- **Docs:** ADR 0038 gets a short note that the move is done; the context map, the add-a-metric how-to and playbook, the README architecture section and any path in an ADR or spec are updated.
- `:data` and `:app` keep their own layout (Room, CSV and UI are adapters, not domain contexts). Their imports change; their packages do not.

## Capabilities
- **Modified Capability:** `architecture-governance` (new requirement "Context package layout")

No behaviour spec changes. No requirement of any other capability changes.

## Impact
- **Database, backup, CSV, medical thresholds, user-visible behaviour:** none. Room entities live in `:data` and are not renamed; backup and CSV formats do not contain domain class names.
- **Affected code:** `:domain` (all files move), `:data` and `:app` (import lines only), unit tests, Hilt modules (imports), R8 rules if any name a domain package.
- **Dependencies:** none.
- **In-flight changes:** the six `add-*` metric changes name old paths. See design, "Order against other changes".

## Sources
- `refactor-application-boundaries` (design decision 11 and ADR 0038) and the architecture review of November 2026.
