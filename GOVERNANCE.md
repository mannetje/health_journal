# Governance

Health Journal is a personal, open-source project with one maintainer, the repository owner. This page says how decisions are made so that contributors know what to expect.

## Who decides

The maintainer reviews and merges pull requests, accepts or declines changes in scope, and tags releases. Contributors propose; the maintainer decides. Dependabot updates follow the same review as any other change.

## How a change is decided

| Kind of change | How it is decided | Where it is recorded |
|----------------|-------------------|----------------------|
| Typo, small fix, docs | Pull request | the commit and the changelog |
| New or changed behaviour | An issue first, then an OpenSpec change with a proposal, tasks and delta specs | `openspec/changes/`, then `openspec/specs/` when it ships |
| An architecture, storage, privacy or policy choice | An ADR next to the change | `docs/adr/`, listed in the README index |
| A release | The maintainer tags `vX.Y.Z` from `master` after the changelog section is written | [Releases](docs/dev/releases.md) |

An ADR keeps its history: a later decision is a new ADR, or a dated amendment note in the earlier one that points forward (as ADR 0033 does for [ADR 0018](docs/adr/0018-smart-input-pickers.md)). A guardrail test keeps the index and the numbering complete ([Testing](docs/dev/testing.md#guardrails)).

## What does not change by vote or by request

These are the project's fixed principles. A change that breaks one is declined however well it is built.

- **Private and offline.** No network permission, no telemetry, no analytics, no sync.
- **Record, never assess.** The app shows what the person entered. It gives no advice, targets, rankings, comparisons or warnings in its own voice ([Conventions](docs/dev/conventions.md)).
- **Small dependency list.** A new library needs a reason that the platform cannot meet ([ADR 0003](docs/adr/0003-dependency-minimization.md)).
- **No data loss on update.** A database change has a tested migration and no destructive fallback ([Change the database](docs/dev/how-to/change-the-database.md)).

## Becoming a maintainer

There is no formal process yet. A contributor who has made several accepted changes and knows the conventions may be invited to review and merge. If that happens, this page is updated first.

## Changing this page

Governance changes go through a pull request like any other change, and the maintainer decides.

See also: [Contributing](CONTRIBUTING.md), [Code of conduct](CODE_OF_CONDUCT.md), [Security policy](SECURITY.md).
