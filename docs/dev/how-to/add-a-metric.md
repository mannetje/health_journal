# How to add a metric

A *metric* is something the user logs: weight, blood pressure, glucose, activity. This guide lists every place a new one touches, in the order to work. Use blood pressure as the model; [Life of an entry](../life-of-an-entry.md) walks through it.

Before writing code, **propose the change**: create an OpenSpec change describing the metric, its validation limits, its unit and, if it has ranges, its labels (see [CONTRIBUTING.md](../../../CONTRIBUTING.md)). A proposal for a waist circumference metric is an example: `openspec/changes/archive/2026-10-05-add-waist-circumference-tracking/`.

## 1. Domain (`domain/src/main/kotlin/nl/healthjournal/domain/`)

1. **Value object** in `model/metrics/` with an `init` block that rejects impossible values (compare `BloodPressureReading.kt`). Store metric units only.
2. **Entry** in `model/metrics/` holding id, profile id, timestamp and the value (compare `BloodPressureEntry.kt`).
3. **Range classifier**, only if the metric has ranges, in `model/nhg/`. Keep it a pure function with the limits in one place. Follow the label rule in [Change a range label](change-a-range-label.md).
4. **Port methods** in `port/secondary/HealthLogRepositoryPort.kt`: save, get history, observe history, update, delete.
5. **Use cases** in `usecase/`: `Record…`, `Update…`, `Delete…`.
6. **Tests** in `domain/src/test/kotlin/…`: value limits at each boundary, classifier boundaries, and the use case with a fake port (compare `UseCasesTest.kt`).

## 2. Data (`data/src/main/java/nl/healthjournal/data/`)

1. **Entity** in `local/entity/` and **DAO** in `local/dao/` (compare `BloodPressureEntity.kt` and `BloodPressureDao.kt`). Add an index on `profileId` and `timestamp`.
2. Register the entity in `local/HealthJournalDatabase.kt`, increase `version`, add a `Migration` and register it in `create`. Follow [Change the database](change-the-database.md); a wrong migration loses user data.
3. **Mapper** functions in `local/mapper/HealthLogMapper.kt`.
4. Implement the new port methods in `repository/RoomHealthLogRepository.kt`.
5. Add the DAO to the repository constructor call in `DataModule.kt`.
6. **CSV** export in `csv/CsvDataExportAdapter.kt` and import in `csv/CsvDataImportAdapter.kt`, with a header and values in metric units. Import must recompute any category instead of trusting the file. Add the matching methods to `DataExportPort` and `DataImportPort`.
7. **Tests:** mapper round trip (`data/src/test/java/…/mapper`), repository (`RoomRepositoriesTest.kt`) and CSV (`CsvAdaptersTest.kt`).

## 3. App (`app/src/main/java/nl/healthjournal/app/`)

1. In `HealthJournalApp.kt`, create the use cases once and, for update and delete, add them to `EntryUseCases` in `ui/history/HistoryViewModel.kt`.
2. **Logging:** extend `MetricType` and the state in `ui/logging/LoggingViewModel.kt`, and add the input form in `ui/logging/LogMetricScreen.kt`. If the user types a quantity that has display units (weight, glucose, distance), convert it to metric before validating (see `ui/common/UnitFormat.kt` and [ADR 0014](../../adr/0014-units-presentation.md)). Each metric saves as its own entry; do not add an optional second metric to another metric's form (see [ADR 0018](../../adr/0018-smart-input-pickers.md)).
3. **History:** add the entries to the list and the filter in `ui/history/HistoryViewModel.kt` and `HistoryScreen.kt`, plus edit and delete dialogs in `EntryDialogs.kt`.
4. **Trend chart**, optional: a `…TrendSection.kt` in `ui/history/charts/`, built from the shared pieces in `TrendBuildingBlocks.kt` and `ChartPrimitives.kt`.
5. **Text:** every user-facing string goes in **both** `res/values/strings.xml` and `res/values-nl/strings.xml`. See [Add a translation](add-a-translation.md).
6. **Tests:** ViewModel tests in `app/src/test/java/…/ui/ViewModelsTest.kt`.

## 4. Docs

- Update the README feature list and the CSV format table.
- Add a CHANGELOG entry under `Unreleased`.
- Record a decision in a new ADR if you made one (see [Conventions](../conventions.md)).
- Update or add the OpenSpec spec, and archive the change when it ships.

## Check yourself

- `domain` has no Android imports.
- Nothing is stored in a display unit.
- No label names a condition or gives advice.
- English and Dutch strings both exist and long Dutch text wraps ([ADR 0010](../../adr/0010-responsive-dutch-ui-layout.md)).
- `./gradlew test` passes.
