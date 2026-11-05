# Life of an entry

This page follows one blood pressure reading, 150 over 95, from the moment a user taps **Save** until it is stored, shown in the history and exported to CSV. Every other metric follows the same path, so if you understand this page you understand most of the app.

Read [Architecture](architecture.md) first if the words *port*, *handler* or *adapter* are new.

```mermaid
sequenceDiagram
    participant S as LogMetricScreen
    participant V as LoggingViewModel
    participant P as Dispatcher
    participant U as RecordBloodPressureHandler
    participant D as domain model
    participant R as RoomHealthLogRepository
    participant DB as Room (SQLite)

    S->>V: saveCurrentMetric()
    V->>P: dispatch(RecordBloodPressureCommand(profileId, 150, 95))
    P->>U: handle(command)
    U->>D: BloodPressureReading.of(150, 95) validates
    U->>D: NhgBloodPressureCategory.classify -> HIGH
    U->>R: saveBloodPressure(entry) via the port
    R->>DB: bloodPressureDao.insert(entity)
    DB-->>R: Flow emits the new list
    R-->>V: observeBloodPressureHistory
    U-->>V: DomainResult.Success(entry)
```

## 1. The screen collects input

File: `app/src/main/java/nl/healthjournal/app/ui/logging/LogMetricScreen.kt`

The screen is a Compose function. It renders a `LoggingUiState` and calls ViewModel functions as the user types: `onSystolicChanged`, `onDiastolicChanged`, and finally `saveCurrentMetric`. It holds no logic of its own.

While the user types, the ViewModel shows a live preview. `updateBpPreview` in `LoggingViewModel.kt` builds a `BloodPressureReading`, asks the domain to classify it, and stores the result in `previewBpCategory`. The screen turns that category into text with `label()` and a colour with `getBpColor()`, both in `ui/nhg/`.

## 2. The ViewModel sends a command

File: `app/src/main/java/nl/healthjournal/app/ui/logging/LoggingViewModel.kt`

`saveCurrentMetric` launches a coroutine in `viewModelScope`, reads the typed numbers and sends a command to the dispatcher:

```kotlin
dispatcher.dispatch(RecordBloodPressureCommand(profileId, systolic, diastolic, chosenPulse(), comment = state.commentInput))
```

If the text is not a number, it throws a `UiTextException` carrying a `UiText.Res(R.string.log_err_systolic)`. That is a UI-only error. A value that is a number but out of range comes back from the domain as `DomainResult.Failure(DomainError.Invalid(...))`, and the ViewModel maps it with `toUiText()` (`ui/common/DomainErrorUiText.kt`). The ViewModel never builds a user-facing string itself; the screen resolves the resource id in the current language ([ADR 0015](../adr/0015-localized-viewmodel-messages.md), [ADR 0037](../adr/0037-typed-domain-errors-and-single-source-for-rules.md)).

Where does the dispatcher come from? Hilt passes it into the ViewModel's `@Inject` constructor. `DefaultDispatcher` finds the handler for the command's class in a map. The domain has no annotations, so each handler is registered by one `@Provides @IntoMap` function in `app/src/main/java/nl/healthjournal/app/command/CommandModule.kt`:

```kotlin
@Provides @IntoMap @ClassKey(RecordBloodPressureCommand::class)
fun recordBloodPressure(repository: BloodPressureRepositoryPort, clock: Clock) = RecordBloodPressureHandler(repository, clock)
```

The port is supplied by `DataPortsModule` from the one `DataModule` (the Room adapter). `MainActivity` is an `@AndroidEntryPoint` and gets the ViewModel with `by viewModels()`, so no factory is written ([ADR 0035](../adr/0035-hilt-for-dependency-injection.md), [ADR 0036](../adr/0036-application-boundary-commands-and-queries.md)).

## 3. The handler applies the rules

File: `domain/src/main/kotlin/nl/healthjournal/domain/metrics/command/BloodPressureCommands.kt`

The handler does four things, in this order:

1. Creates the reading with `BloodPressureReading.of(systolic, diastolic, pulse)`. The factory rejects impossible values (systolic must be 40 to 300, diastolic 20 to 200, systolic above diastolic, an optional pulse 30 to 250) and returns a `DomainError.Invalid` that names the rule and its bounds. The constructor keeps its own `require` checks, so an invalid reading can never exist, even when Room or a test builds one.
2. Normalises the comment with `EntryComment.of(comment)`.
3. Calls `NhgBloodPressureCategory.classify(reading)`. For 150/95 the answer is `HIGH` (from 140/90).
4. Builds a `BloodPressureEntry` (id, profile, timestamp, reading, category, comment) and hands it to `repository.saveBloodPressure(entry)`. "Now" comes from the injected `Clock`, never from `Instant.now()`. An exception from the port becomes `DomainError.Storage`.

`repository` has the type `BloodPressureRepositoryPort`, an interface in `domain/src/main/kotlin/nl/healthjournal/domain/port/secondary/BloodPressureRepositoryPort.kt`. Each metric has its own port. The handler does not know that Room exists. That is the whole point of the architecture.

The category is **computed once and stored** with the entry, and recomputed when the entry is updated (`UpdateBloodPressureHandler`).

## 4. The adapter stores it

Files:
- `data/src/main/java/nl/healthjournal/data/repository/RoomHealthLogRepository.kt`
- `data/src/main/java/nl/healthjournal/data/local/mapper/HealthLogMapper.kt`
- `data/src/main/java/nl/healthjournal/data/local/entity/BloodPressureEntity.kt`
- `data/src/main/java/nl/healthjournal/data/local/dao/BloodPressureDao.kt`

`RoomHealthLogRepository` implements the port. For a save it converts the domain entry to a Room entity and inserts it:

```kotlin
override suspend fun saveBloodPressure(entry: BloodPressureEntry) {
    bloodPressureDao.insert(HealthLogMapper.toEntity(entry))
}
```

The entity (`blood_pressures` table) stores plain types: the id as a `String`, the timestamp as a `Long`, the category as its name. Going back, `HealthLogMapper.toDomain` rebuilds the `BloodPressureReading` (validating again) and reads the category with `NhgBloodPressureCategory.fromStoredName`, which also understands the names written by older app versions. This is why a rename of a stored value needs a read mapping, as described in [Change the database](how-to/change-the-database.md).

## 5. The history updates by itself

File: `app/src/main/java/nl/healthjournal/app/ui/history/HistoryViewModel.kt`

The DAO exposes `observeByProfileId`, a Room query that returns a Kotlin `Flow`. The repository maps it to domain entries (`observeBloodPressureHistory`). Whenever a row is inserted, updated or deleted, the flow emits a new list, the ViewModel updates its state, and the History screen recomposes. No one tells the screen to refresh.

The same state feeds the trend charts in `ui/history/charts/`. For blood pressure, `BloodPressureTrendSection.kt` groups entries by band and shows averages (rounded to whole mmHg).

## 6. Editing and deleting

The History screen can edit or delete an entry. That sends an `UpdateBloodPressureCommand` or a `DeleteEntryCommand(kind, id)` to the dispatcher. The port returns `false` when the id is unknown and the handler turns that into `DomainError.NotFound`, so a stale screen cannot silently change nothing ([ADR 0012](../adr/0012-edit-and-delete-entries.md)).

## 7. Export and import

File: `data/src/main/java/nl/healthjournal/data/csv/CsvDataExportAdapter.kt`

Export is an `ExportCsvQuery(profileId, kind)`. Its handler reads the history through the export port and writes one CSV line per entry:

```text
timestamp,systolic_mmhg,diastolic_mmhg,pulse_bpm,classification,comment
2026-09-09T08:30:00Z,150,95,70,HIGH,"After a walk, felt fine"
```

The `comment` cell is quoted per RFC 4180 when it holds a comma, quote or line break (`CsvQuoting.kt`). Import (`CsvDataImportAdapter.kt`) does not trust the `classification` column: it recomputes the category from the values, so a file with an old or wrong name still imports with the right band. A file from before the pulse existed (no `pulse_bpm` column) imports too.

## What to remember

| Layer | Knows about | Does not know about |
|-------|-------------|---------------------|
| Screen | UI state, resources | Room, rules |
| ViewModel | UI state, the dispatcher | Android `Context`, SQL, ports |
| Handler | Model, rules, ports, a clock | Compose, Room |
| Repository | Room, mapper | Compose, UI |

Next: try a change. [Add a metric](how-to/add-a-metric.md) uses exactly the files named on this page.
