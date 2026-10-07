# Testing

Tests are plain JUnit 4 with `kotlinx-coroutines-test`. Robolectric is used only for the database migration test; there is no instrumented test suite. This keeps `./gradlew test` fast and runnable in CI.

```bash
./gradlew test             # unit tests, all modules
./gradlew :app:lintDebug   # Android Lint (errors fail CI)
```

Both run on every push and pull request. [Continuous integration](ci.md) describes the workflows, the run summary and the artifacts.

## What to test where

| Module | Folder | What |
|--------|--------|------|
| `domain` | `domain/src/test/kotlin/…` | Value object limits, unit conversion, NHG classifiers (both sides of every boundary), use cases with fake ports (`usecase/UseCasesTest.kt`, `usecase/EntryCommentUseCasesTest.kt`), the medication model (`model/medication/`) and its use cases (`usecase/MedicationUseCasesTest.kt`, which also holds the in-memory `FakeMedicationRepository`) |
| `data` | `data/src/test/java/…` | Mappers (`local/mapper/`), CSV export and import (`csv/CsvAdaptersTest.kt`, `csv/MedicationCsvTest.kt`), repositories against fake DAOs (`repository/RoomRepositoriesTest.kt`, `repository/RoomMedicationRepositoryTest.kt`) |
| `app` | `app/src/test/java/…` | ViewModels (`ui/ViewModelsTest.kt`, `ui/MedicationViewModelTest.kt`), history and chart maths (`ui/history/`), glucose ranges and label strings (`ui/nhg/`, `ui/MedicationStringsTest.kt`), medication formatting (`ui/MedicationFormatTest.kt`), unit and theme preferences (`settings/`), and the privacy checks (`PrivacyChecksTest.kt`) |

## Rules of thumb

- Put logic where it is easy to test: in the domain, not in a composable.
- Test boundaries: the value just below, on and above each limit.
- Use fakes of the ports, not mocking frameworks.
- A bug fix starts with a failing test.
- A string test can read `res/values*/strings.xml` directly, as `RangeLabelStringsTest` does, to keep English and Dutch aligned.

## Coverage

CI measures line coverage of the unit tests with JaCoCo and prints it, with a code statistics table, on the run summary page. To see it locally, run the tasks below and open the HTML reports under each module's `build/reports`.

```bash
./gradlew :domain:jacocoTestReport :data:createDebugUnitTestCoverageReport :app:createDebugUnitTestCoverageReport
```

The numbers are for information. There is no threshold that fails the build. `scripts/job-summary.sh` builds the summary, and the HTML and XML reports are attached to each CI run as the `coverage-reports` artifact for 14 days ([Continuous integration](ci.md)).

Line coverage when this was written: `domain` about 90%, `data` about 32%, `app` about 20%. The last two look low for a reason, not from neglect: in `data` most lines are the code Room generates for the DAOs, and in `app` most lines are Compose screens and `MainActivity`. Neither can run in a plain JVM unit test. The logic (use cases, mappers, CSV, ViewModels) is covered; the rest needs Robolectric or an instrumented suite.

## Not covered by unit tests

Compose screens and the Android back stack are checked by hand; the migration from version 5 to 6 has a test (`data/src/test/java/nl/healthjournal/data/local/MigrationTest.kt`). Before a release, run the app on a device in English and Dutch, with each Theme choice in Profile (System, Light and Dark), at large font scales (1.3x and 2.0x to verify Dutch labels like "Activiteit" do not clip in tab rows or navigation bars), and install over the previous version to check that data survives ([Change the database](how-to/change-the-database.md)).

## Known gaps

There are no tests for the DAOs against a real database (the repositories are tested against fake DAOs that mirror the SQL), no migration tests before version 5, no tests for the Compose screens, and none for the CSV file picker flow. The Robolectric setup of the migration test can be reused to close most of this. Contributions here are welcome.
