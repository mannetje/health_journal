## ADDED Requirements

### Requirement: Export and import migraines
Export SHALL write a migraine file with the header `start_timestamp,end_timestamp,intensity,areas,type,symptoms,triggers,took_something,medication_text,second_dose,activities,comment`, one row per migraine of the profile sorted ascending by start. Timestamps follow the existing CSV conventions; `end_timestamp` and `intensity` are empty when absent; `areas`, `symptoms` and `triggers` hold the stable English codes joined by `|` and are never translated; `type` and `activities` hold the stable code or are empty; the two flags are `true` or `false`; text cells use the existing quoting. Import SHALL accept the same header, report accepted and rejected rows like the other imports, reject a row with an end before the start, an intensity outside 0 to 10 or an unknown code, and import nothing for a rejected row. The UI SHALL offer export and import and SHALL repeat the existing warning that a CSV file is not encrypted.

#### Scenario: Round trip
- **WHEN** a migraine with symptoms, a medication text containing a comma and a comment is exported and imported into an empty profile
- **THEN** the imported entry equals the original

#### Scenario: Ongoing migraine
- **WHEN** a migraine without an end is exported
- **THEN** `end_timestamp` is empty, and on import the entry is ongoing

#### Scenario: Unknown code
- **WHEN** a row has the symptom code `unknown_thing`
- **THEN** that row is rejected with a reason and the other rows are imported

#### Scenario: Warning shown
- **WHEN** migraine export is offered
- **THEN** the unencrypted-file warning is shown as for other exports
