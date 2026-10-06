# Playbook: add a metric

Detail: [How to add a metric](../../docs/dev/how-to/add-a-metric.md).

## Required decisions
- Unit (stored metric) and validation limits.
- Does it have ranges? If so, source (NHG first) and label wording ([change-a-range-label](change-a-range-label.md)).
- Own entry form: never an optional second metric inside another metric's form ([ADR 0018](../../docs/adr/0018-smart-input-pickers.md)).

## Steps
1. Propose an OpenSpec change naming the capability, limits, unit and labels.
2. Domain: value object, entry, classifier (if any), port methods, use cases, tests.
3. Data: entity, DAO, migration ([change-the-database](change-the-database.md)), mapper, repository, CSV export and import, tests.
4. App: wire use cases, logging form, history, optional trend chart, strings in EN and NL ([add-a-translation](add-a-translation.md)), tests.
5. Docs: README, CHANGELOG (Unreleased), spec, ADR if a decision was made.

## Done when
- `./gradlew test` passes and the entry can be added, edited, deleted, exported and imported.
- Old data is still readable after the migration.
- `scripts/check-docs.sh` and `npx --no-install openspec validate --all --strict` pass.
