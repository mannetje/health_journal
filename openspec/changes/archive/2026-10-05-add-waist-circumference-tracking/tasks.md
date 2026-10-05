## 1. Domain Layer

- [x] 1.1 Create value class `WaistCircumferenceCm` and `NhgWaistCircumferenceCategory` with classification logic (Women: 68-80cm healthy, 80-88cm increased risk, >=88cm high risk; Men: 79-94cm healthy, 94-102cm increased risk, >=102cm high risk) and unit tests verifying range validation and sex-based classification.
- [x] 1.2 Create `WaistCircumferenceEntry` model and `RecordWaistCircumferenceUseCase`, `UpdateWaistCircumferenceUseCase`, `DeleteWaistCircumferenceUseCase` with unit tests verifying domain behavior and graceful null-category handling when `sex` is unset.

## 2. Data & Persistence Layer

- [x] 2.1 Add `WaistCircumferenceEntity`, DAO methods, and repository implementations in `:data`, update Room database version and migration, and verify persistence and history retrieval with unit/migration tests.
- [x] 2.2 Add CSV export and import support for waist circumference entries and verify round-trip export/import tests.

## 3. UI & Logging Flow

- [x] 3.1 Update `LoggingViewModel` and `LogMetricScreen` to support a Waist Circumference tab as well as an optional Waist Circumference input field on the Weight logging tab, saving both when provided, and verify UI preview / ViewModel unit tests.
- [x] 3.2 Add waist circumference filter, history card, edit, and delete support in `HistoryScreen` and `HistoryViewModel`, and verify history list rendering.
- [x] 3.3 Add English and Dutch localization strings for waist circumference labels and categories, and verify string resources load correctly.

## 4. Documentation & ADR

- [x] 4.1 Update `docs/adr/0005-dutch-nhg-guidelines.md` and `README.md` with Voedingscentrum waist circumference guidance and feature bullet.
