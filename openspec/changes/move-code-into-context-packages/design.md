## Context
After `refactor-application-boundaries`, the contexts are defined, enforced by an import test, and new code goes into `domain/<context>/...`. The old code is still in layer folders. This change finishes the job without changing behaviour.

## Goals / Non-Goals
**Goals:** one folder per context in `:domain` holding its model, use cases, ports and commands; directory and package always match; the layout is enforced by a test.
**Non-Goals:** any logic or signature change, splitting `:domain` into Gradle modules, moving `:data` or `:app` code, renaming types, turning use cases into commands (that is the other change).

## Decisions

### 1. Target layout
`nl.healthjournal.domain.<context>.{model, usecase, port, command}`; a folder exists only if it has files. `port` holds the secondary ports of that context; commands and handlers are the primary side and live in `command`.

| Context | From |
|---|---|
| `shared` | `model/common` (`ProfileId`, `MeasurementId`, `MeasurementUnit`, `UnitConversion`), plus `EntryComment`, `HeightCm`, `WeightKg` after the refactor moved them |
| `profile` | `model/profile`; `CreateProfileUseCase`; `ProfileRepositoryPort` |
| `metrics` | `model/metrics`, `model/nhg`; Record, Update and Delete use cases of the six metrics (or their handlers if the refactor already replaced them), `OxygenSaturationUseCases`, `GetHealthHistoryUseCase`; the six metric ports; `DataExportPort`, `DataImportPort` (CSV of the metrics) |
| `medication` | `model/medication`; Archive, ChangeSchedule, Delete, Save, RecordIntake, RecordSlotIntakes, TakeAllForSlot, GetAdherence, GetPillboxDay, RearmReminders; `MedicationRepositoryPort`, `MedicationDataPorts`, `ReminderSchedulerPort` |
| `migraine` | `model/migraine`; `MigraineUseCases`; `MigraineRepositoryPort`, `MigraineDataPorts` |
| `mindfulness` | `model/mindfulness` (no ports, no use cases) |
| `lock` | `model/lock`; `AppLockPort` |
| `backup` | `model/backup`; `BackupUseCases`; `BackupPorts` |

### 2. Mechanical, verified, one context per commit
Each phase moves one context: `git mv` of its files (tests included), a script rewrites the `package` line and every `import` and fully qualified name that refers to the old name, then the three test tasks run. The script is generated from an explicit old-to-new mapping so the mapping can be reviewed; the script itself is not kept in the repository. Kotlin allows a package that differs from the directory, so the test in decision 4 checks that they agree.

### 3. Order of phases
Smallest blast radius first, so the process is proven on files few others import: `mindfulness`, `lock`, `migraine`, `medication`, `backup`, `profile`, `metrics`, and `shared` last because every context imports it. After each phase the build and the three test tasks pass, so any phase can be reverted alone.

### 4. Guardrail
The context test from the refactor gains three checks: no Kotlin file in `:domain` sits outside `<context>/{model,usecase,port,command}`; the directory matches the `package` line; the old layer folders do not exist. A failure names the file.

### 5. Things that reference paths
Before the first move, search for paths and package names outside Kotlin imports: `RepositoryGuardrailsTest` (it scans directories by path), `scripts/`, R8 and ProGuard rules, `docs/` and ADRs, `openspec/` specs and playbooks, Gradle files. Each is updated in the phase that moves what it names.

### 6. Cross-context read use cases
`GetHealthHistoryUseCase` is placed in `metrics` because it reads only the metric port today. If a use case turns out to read two contexts, it does not go into either one silently: the extra read becomes an explicit query through the other context's port, or the edge is recorded as an exception in the context test with a reason. Task 0.2 checks every use case before the move.

## Order against other changes
1. `refactor-application-boundaries` first, all of it. It creates the shared kernel, the context test and the new ports and handlers; this change moves what is left.
2. This change second, before the six `add-*` metric changes. Each of those adds a model, a port, a command and use cases. If they land first, each lands in the old layout and moves again here. If they land after, each is created once, directly in `metrics/`.
3. The six `add-*` changes last. Their tasks and designs name old paths and `HealthLogRepositoryPort`; update them once, after this change, to the context layout.

If a feature must ship before this change, it still lands in the new layout (it is new code, so the refactor already requires that) and this change moves only the old files.

## Risks / Trade-offs
- **Large diff, no logic.** Mitigated by one context per commit, the reviewed mapping, and unchanged test assertions as the check.
- **Merge conflicts with in-flight branches.** Any open branch that touches `:domain` conflicts on imports. Mitigated by doing this change when no such branch is open, or by rebasing right after each phase.
- **Search-and-replace mistakes** (two types with the same simple name in different packages). Mitigated by the explicit mapping and by compiling after each phase.
- **History:** `git mv` keeps file history with `--follow`; blame needs `-C`.

## Open Questions
- Do the Room mappers in `:data` stay grouped by layer or follow contexts later? Not part of this change.
- Should `DataExportPort` and `DataImportPort` live in `metrics`, or in `backup` as the one cross-context consumer? Default here is `metrics` because they only handle the six metrics.
