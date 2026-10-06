# Playbook: change the database

Detail: [How to change the database](../../docs/dev/how-to/change-the-database.md).

## Required decisions
- Migration or read mapping? If only the meaning of a stored string changes, map it on read.
- New columns are nullable or have a default.

## Steps
1. Change the entity and register new entities in `HealthJournalDatabase.kt`.
2. Increase `version` by one and write a `Migration(old, new)` in plain SQL.
3. Register it in `create`, next to every earlier migration.
4. Test the mapper (including old stored values) and CSV export and import.
5. Add an "Upgrade note" to the CHANGELOG if an older app cannot read the new data.

## Done when
- No destructive migration anywhere.
- The previous release, upgraded over on an emulator, keeps its data.
