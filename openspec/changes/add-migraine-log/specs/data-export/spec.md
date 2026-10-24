## ADDED Requirements

### Requirement: Export and import migraines
Export SHALL write a migraine file with the header `start_timestamp,woke_up_with_it,end_timestamp,intensity,areas,pain_character,type,symptoms,triggers,took_something,medication_text,second_dose,intensity_after_2h,relief_methods,custom_triggers,custom_relief_methods,medication_helped_me,relief_helped_me,activities,comment`, one row per migraine of the profile sorted ascending by start. Timestamps follow the existing CSV conventions; `end_timestamp`, `intensity` and `intensity_after_2h` are empty when absent; `areas`, `pain_character`, `symptoms`, `triggers` and `relief_methods` hold the stable English codes joined by `|` and are never translated; `type` and `activities` hold the stable code or are empty; the three flags are `true` or `false`; `custom_triggers` and `custom_relief_methods` hold the personal tag labels joined by `|`; text cells use the existing quoting. Import SHALL accept the same header, report accepted and rejected rows like the other imports, reject a row with an end before the start, an intensity or intensity after 2 hours outside 0 to 10, an intensity after 2 hours on a row whose `took_something` is `false`, or an unknown code, and import nothing for a rejected row. Import SHALL create a personal tag, in the profile's list, for a label that does not exist yet (matched ignoring case) and SHALL reject a row with a label that breaks the tag rules. The UI SHALL offer export and import and SHALL repeat the existing warning that a CSV file is not encrypted.

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
