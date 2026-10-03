# Developer documentation

Welcome. These pages explain how Health Journal is built and how to contribute. They are written in English so contributors from any country can join. The app itself is available in English and Dutch.

Health Journal is an offline-first Android app for logging weight, blood pressure, blood glucose and activity. It is a personal logging tool, not a medical device: it shows ranges and never gives advice or a diagnosis.

## Reading order

| # | Page | Read it to |
|---|------|------------|
| 1 | [Getting started](getting-started.md) | build, test and run the app |
| 2 | [Architecture](architecture.md) | understand the three modules and the dependency rule |
| 3 | [Life of an entry](life-of-an-entry.md) | follow one blood pressure reading through every layer (the best way to learn the code) |
| 4 | [Conventions](conventions.md) | know the rules the code follows |
| 5 | [Testing](testing.md) | see what is tested where and how to run it |
| 6 | [Releases](releases.md) | understand versions, tags and the release build |

How-to guides for common changes:

- [Add a metric](how-to/add-a-metric.md)
- [Add a translation](how-to/add-a-translation.md)
- [Change a range label](how-to/change-a-range-label.md)
- [Change the database](how-to/change-the-database.md)

## Where the other knowledge lives

The developer docs say *how*. Two other places say *why* and *what*, and these pages link to them instead of copying them:

- **[Architecture Decision Records](../adr/0001-record-architecture-decisions.md)**: why a decision was made (hexagonal architecture, Room, NHG ranges, units, and more). All ADRs are in `docs/adr/`.
- **[OpenSpec specs](../../openspec/specs/README.md)**: what the system must do, as requirements with scenarios. Proposed work lives in `openspec/changes/`.

## Contributing

Start with [CONTRIBUTING.md](../../CONTRIBUTING.md). It explains the flow: propose with OpenSpec, implement, test, update the docs, add a changelog entry.
