## 1. Libra CSV Parser Implementation in `:data`

- [x] 1.1 Implement Libra CSV parsing logic in `CsvDataImportAdapter` (detect `#Units:`, `#date;weight;...` header, parse semicolon-delimited rows, convert lbs to kg, derive BMI); verify with JVM unit tests.
- [x] 1.2 Add test cases in `CsvAdaptersTest.kt` covering valid kg Libra exports, imperial lbs Libra exports, metadata comment handling, malformed rows, and out-of-range weights; verify with `./gradlew :data:test`.

## 2. UI Integration in `:app`

- [x] 2.1 Update `HistoryScreen` import dialog and `HistoryViewModel` in `:app` to include "Libra (CSV)" as a selectable import type and handle Libra format errors; verify with `./gradlew :app:test`.

## 3. Documentation & Verification

- [x] 3.1 Update root `README.md` Supported Import Formats table to document Libra CSV format compatibility; verify markdown renders cleanly.
- [x] 3.2 Run full project test suite (`./gradlew test`) and validate OpenSpec change artifacts with `openspec validate --changes`; verify all pass.
