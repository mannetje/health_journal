## ADDED Requirements

### Requirement: Fluid intake entries in History
The History list SHALL include fluid intake entries as a further entry type in the merged list, sorted by timestamp, and the single-metric filter SHALL offer Fluid. Under the Fluid filter the list SHALL show, per local calendar day, a day-total row (date and total ml, plus the position label of `fluid-intake` when the person has set a minimum or maximum) above that day's entries. Each entry can be edited in a dialog with an editable date and time, the drink, the millilitres (10 to 5000) and the comment, validated like recording; deleting uses the same confirmation as other entries. A failed update keeps the dialog open. Without a minimum or maximum the day-total row SHALL carry no label, colour or comparison; with them it carries only the neutral position label.

#### Scenario: Drink shown
- **WHEN** a fluid entry of Coffee is listed
- **THEN** the row shows the drink name with the amount

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and a fluid entry at 11:00 exist
- **THEN** the fluid entry is listed first

#### Scenario: Day totals under the filter
- **WHEN** the Fluid filter is on and a day holds 250 ml and 500 ml
- **THEN** a row for that day reads 750 ml above the two entries

#### Scenario: Day total with a limit
- **WHEN** a maximum of 1500 is set and a day holds 1800 ml
- **THEN** the row reads 1800 ml with the label "Above your maximum · 1500 ml"

#### Scenario: Edit rejects invalid value
- **WHEN** an edit sets the millilitres to 6000
- **THEN** an "invalid value" message is shown and the stored entry is unchanged

#### Scenario: Delete
- **WHEN** an entry is deleted and confirmed
- **THEN** the entry and the day total no longer include it

#### Scenario: Timeline navigation
- **WHEN** entries span two months and the Fluid filter is on
- **THEN** the year and month rows appear as for other filters
