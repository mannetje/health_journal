## Context

See `proposal.md` for motivation.

Currently, `CsvDataImportAdapter` in `:data` implements `DataImportPort` by parsing canonical comma-separated UTF-8 rows for `weight`, `blood_pressure`, `glucose`, and `activity`.

The Libra Android app (`net.cachapa.libra`) exports weight data in a semicolon-delimited (`#date;weight;weight trend;...`) structure that includes leading metadata comment lines (`#Version: 6`, `#Units: kg|lbs`).

## Goals / Non-Goals

**Goals:**
- Implement a dedicated parser for the Libra CSV export format inside `:data` infrastructure layer.
- Support metadata directive extraction:
  - Unit mode: `#Units: kg` (default) vs `#Units: lbs` (converted via factor $0.45359237$).
  - Header detection: `#date;weight;...` or `date;weight;...`.
- Parse robust date formats (ISO timestamps `2026-09-20T08:00:00.000Z`, `2026-09-20T08:00:00`, or `2026-09-20` at start of day UTC).
- Automatically derive BMI if the active Profile has a configured height.
- Update `HistoryScreen` in `:app` to include "Libra (CSV)" as an import selection option.

**Non-Goals:**
- Direct reading of Libra SQLite binary database files.
- Exporting in Libra-specific semicolon format (standard export remains RFC 4180 canonical CSV).

## Decisions

### 1. Unified Entry Point via `DataImportPort`
**Decision:** Support `metricType = "libra"` or `"libra_weight"` in `DataImportPort.importCsv()`, as well as auto-detecting Libra header comments (`#Version:` or `#Units:` or `#date;weight`) if submitted under generic `weight`.
**Rationale:** Keeps the domain interface concise while providing zero-friction UX whether the user explicitly selects "Libra" or pastes a Libra CSV into the weight importer.

### 2. Flexible Decimal and Date Parsing
**Decision:**
- Weight: Trim quotes and support period or comma decimal notations (e.g. `74.5` or `74,5`).
- Dates: Attempt `Instant.parse()`, then fallback to `LocalDateTime.parse().toInstant(ZoneOffset.UTC)` and `LocalDate.parse().atStartOfDay().toInstant(ZoneOffset.UTC)`.

### 3. Unit Conversion Precision
**Decision:** Use `BigDecimal` with factor `0.45359237` and round to 2 decimal places when converting pounds to kilograms.

## Risks / Trade-offs

- **[Missing `#Units` Header]** → If `#Units:` is absent, assume kilograms (`kg`) as the canonical default.
- **[Comments in Body]** → Lines beginning with `#` that do not contain valid data are treated as comments and skipped cleanly without incrementing the skipped row error counter.
