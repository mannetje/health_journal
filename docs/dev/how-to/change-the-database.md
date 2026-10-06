# How to change the database

User data lives in a local Room (SQLite) database on the device. A mistake here can destroy someone's health history, so changes follow a strict routine. The decision to use Room is in [ADR 0004](../../adr/0004-room-for-offline-first-persistence.md).

The database is `data/src/main/java/nl/healthjournal/data/local/HealthJournalDatabase.kt`. It has a `version` number and a list of `Migration` objects registered in `create`.

## Decide first: does it need a migration?

| Change | Needs a SQL migration? |
|--------|------------------------|
| New table or column | Yes |
| Changing the meaning of a stored value (for example merging enum names) | **No, if you can read the old value.** Map it on read |
| Changing the display of a value | No |

Prefer a **read mapping** when only the meaning of a stored string changes. The blood pressure bands went from six names to three this way: `NhgBloodPressureCategory.fromStoredName` maps the old names, `HealthLogMapper` uses it, and no rows were rewritten. That keeps old data safe and avoids a risky migration.

## Add a table or column

1. Change the entity in `local/entity/` (or add one) and register new entities in the `@Database` annotation.
2. Increase `version` by one.
3. Write a `Migration(old, new)` with plain SQL. Look at `MIGRATION_1_2` (adds the optional `sex` column), `MIGRATION_2_3` (adds the `waist_circumferences` table with `CREATE TABLE IF NOT EXISTS` and two indexes) and `MIGRATION_3_4` (adds the optional `pulse` column). The simplest one:

   ```kotlin
   val MIGRATION_3_4 = object : Migration(3, 4) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE blood_pressures ADD COLUMN pulse INTEGER")
       }
   }
   ```

4. Register it: `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)` in `create`, and append yours. The list always contains every migration, never only the newest. The database is at version 5 today, so the next change is `Migration(5, 6)` (the medication tables, in the proposed `add-medication-management`). Database encryption (proposed `add-database-encryption-and-lock`) is a separate file-level migration and does not change the schema version.
5. **Never use destructive migration** (`fallbackToDestructiveMigration`). Existing users must keep their data.
6. New columns must be nullable or have a default, so existing rows stay valid.

## Data is metric and stable

Store metric units and plain types. Do not store anything that depends on the user's language, region or display units ([ADR 0014](../../adr/0014-units-presentation.md)).

## Tests

- Test the mapper both ways, including old stored values (`data/src/test/java/nl/healthjournal/data/local/mapper/MapperTest.kt`).
- Test CSV export and import if the shape changed (`data/src/test/java/nl/healthjournal/data/csv/CsvAdaptersTest.kt`).
- Repository logic is tested against fake DAOs in `RoomRepositoriesTest.kt`.

Migrations run only on a device. Test a migration by installing the previous release on an emulator, adding data, installing your build over it and checking the data is still there. Debug builds use a fixed signing key, so this works ([ADR 0008](../../adr/0008-fixed-debug-signing-key.md)).

## Tell the users

If an older app version cannot read data written by the new one, say so in the CHANGELOG under an "Upgrade note" (as release 1.5.0 does for the blood pressure bands). Downgrades are not supported unless stated.
