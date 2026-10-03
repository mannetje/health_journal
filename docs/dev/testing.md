# Testing

Tests are plain JUnit 4 with `kotlinx-coroutines-test`. There is no Robolectric and no instrumented test suite, which keeps `./gradlew test` fast and runnable in CI.

```bash
./gradlew test
```

## What to test where

| Module | Folder | What |
|--------|--------|------|
| `domain` | `domain/src/test/kotlin/…` | Value object limits, unit conversion, NHG classifiers (both sides of every boundary), use cases with fake ports (`usecase/UseCasesTest.kt`) |
| `data` | `data/src/test/java/…` | Mappers (`local/mapper/`), CSV export and import (`csv/CsvAdaptersTest.kt`), repositories against fake DAOs (`repository/RoomRepositoriesTest.kt`) |
| `app` | `app/src/test/java/…` | ViewModels (`ui/ViewModelsTest.kt`), history and chart maths (`ui/history/`), glucose ranges and label strings (`ui/nhg/`), unit preferences (`settings/`) |

## Rules of thumb

- Put logic where it is easy to test: in the domain, not in a composable.
- Test boundaries: the value just below, on and above each limit.
- Use fakes of the ports, not mocking frameworks.
- A bug fix starts with a failing test.
- A string test can read `res/values*/strings.xml` directly, as `RangeLabelStringsTest` does, to keep English and Dutch aligned.

## Not covered by unit tests

Compose screens, Room migrations and the Android back stack are checked by hand. Before a release, run the app on a device in English and Dutch, in light and dark theme, and install over the previous version to check that data survives ([Change the database](how-to/change-the-database.md)).

## Known gaps

There are no tests for the DAOs against a real database, and none for the CSV file picker flow. Contributions here are welcome.
