## ADDED Requirements

### Requirement: Fluid intake is an orderable metric
Fluid intake SHALL be one more metric in the user-configurable order, with its own Log tab and History chip. In the default order it comes after Oxygen saturation, and a stored order that lacks it gets it appended in that position, as for every metric added later. Changing the order SHALL NOT change a stored entry, a minimum or a maximum.

#### Scenario: Default order
- **WHEN** the user has never chosen an order
- **THEN** Fluid comes after Oxygen saturation on the Log tabs and the History chips

#### Scenario: Older stored order
- **WHEN** an order saved before Fluid existed is read
- **THEN** Fluid is appended at the end and the other metrics keep their places
