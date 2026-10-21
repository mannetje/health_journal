## ADDED Requirements

### Requirement: Mindfulness practice is not recorded
The system SHALL NOT store, log or transmit which mindfulness exercises a person did, when, for how long or how often. Spoken cues SHALL use only a text-to-speech voice that works without a network, so no text is sent to a remote service.

#### Scenario: Exercise finished
- **WHEN** a breathing exercise or timer finishes in a release build
- **THEN** nothing about it appears in storage, in a backup or in the device log

#### Scenario: Voice that needs a network
- **WHEN** the only voice for the app language needs a network connection
- **THEN** it is not used and spoken cues are unavailable
