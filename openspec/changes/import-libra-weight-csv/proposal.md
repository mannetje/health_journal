## Why

Users migrating from or tracking historical body weight with the popular Libra Android app (`net.cachapa.libra`) need a seamless way to import their weight history without manually reformatting their files. Adding native support for the Libra CSV export format ensures immediate data portability and effortless onboarding into Health Journal.

## What Changes

- Add Libra CSV parsing support to `DataImportPort` and `CsvDataImportAdapter`.
- Support Libra CSV format conventions:
  - Semicolon delimiters (`;`) and optional metadata headers (`#Version:`, `#Units:`, column headers starting with `#date;weight;...`).
  - Unit recognition: parse `#Units: kg` or `#Units: lbs` and convert pounds to kilograms ($1\text{ lb} = 0.45359237\text{ kg}$).
  - Timestamp parsing: ISO-8601 timestamps (`YYYY-MM-DDTHH:mm:ss.SSSZ` or `YYYY-MM-DD`).
  - Gracefully ignore non-essential columns (weight trend, body fat, muscle mass, log comments) while strictly validating and importing date and weight values.
- Expose "Libra (CSV)" as an import option / auto-detected format in the History Import dialog.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `data-export`: Add requirements for importing weight measurements from Libra (`net.cachapa.libra`) CSV export files with auto-detection of semicolon delimiter, metadata header handling, and unit conversion.

## Impact

- **Domain:** `DataImportPort` interface documentation and metric/format handling.
- **Data Layer:** `CsvDataImportAdapter` in `:data` updated with Libra format detection and parsing logic.
- **Presentation Layer:** `HistoryScreen` import dialog in `:app` updated with a "Libra" import format option / auto-detection.
- **Dependencies:** None. Retains zero third-party dependencies using native Kotlin string/regex parsing.
