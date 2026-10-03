## ADDED Requirements

### Requirement: Export medication data
Export SHALL write three CSV files for a profile, following the conventions of this spec (UTF-8, comma separator, decimal point, line feed endings, ISO-8601 UTC instants). Doses and strengths are exported exactly as entered, with their unit, and are never converted. Planned times are wall-clock values and SHALL NOT carry a time zone.
- medications: `ref,name,form,strength_amount,strength_unit,dose_amount,dose_unit,as_needed,colour,shape,note,start_date,end_date,archived`
- medication schedule: `ref,local_time,days_of_week,interval_days` (one row per daily time; `days_of_week` is a pipe-separated list of MON to SUN, empty when an interval is used; `interval_days` is a whole number, empty when weekdays are used)
- intakes: `ref,planned_local_datetime,status,actual_timestamp,actual_amount,actual_unit,note` (status TAKEN, SKIPPED or AS_NEEDED; planned is empty for as-needed doses; pending and missed are never stored, because they are derived)

`form`, `unit`, `colour` and `shape` are the language-independent enum names, never translated labels. `ref` is a per-export identifier used only to link rows across the three files.

#### Scenario: Language independence
- **WHEN** the app language is Dutch and a medication with form liquid and dose unit ml is exported
- **THEN** the file contains `LIQUID` and `ML`, not the Dutch labels

#### Scenario: Doses unchanged
- **WHEN** a dose of 2.5 ml is exported with a Dutch region
- **THEN** the file contains `2.5` and `ML`

#### Scenario: Derived status not exported
- **WHEN** a planned intake has no outcome
- **THEN** it does not appear in the intakes file

### Requirement: Import medication data
Import SHALL accept the three files, link rows through `ref` within the batch, assign new identifiers, and attach everything to the active profile. Invalid rows are skipped with their physical line number and a reason, and the rest are imported. A schedule or intake row whose `ref` does not exist in the batch is skipped. Medications SHALL be validated with the same rules as manual entry. An intake that has the same medication and planned time as an existing one SHALL be skipped as a duplicate, so importing the same file twice does not duplicate the log. Import with no active profile SHALL do nothing.

#### Scenario: Round trip
- **WHEN** a profile is exported and the files are imported into an empty profile
- **THEN** the medications, schedules and intake log are identical, and adherence figures are the same

#### Scenario: Import twice
- **WHEN** the same intakes file is imported twice
- **THEN** the second import adds no intakes and reports them as skipped duplicates

#### Scenario: Unknown enum value
- **WHEN** a row has a form or unit that is not in the contract
- **THEN** the row is skipped with a reason and nothing is guessed

#### Scenario: Orphan row
- **WHEN** an intake row refers to a `ref` that is not in the medications file
- **THEN** it is skipped with its line number
