# Delta Specification: Core Health Features & Architecture Governance

This delta specification defines the complete set of capabilities, requirements, and constraints for `init-core-health-features`.

---

## 1. Architecture Governance & Quality Directives

### Purpose
Enforces architectural and documentation governance standards for the health_journal codebase, ensuring strict dependency minimization, architectural decision tracking through ADRs in `docs/adr/`, and continuous maintenance of a living root `README.md` with Mermaid diagrams.

### ADDED Requirements

#### Requirement: Dependency Minimization
The system SHALL strictly minimize third-party dependencies, prioritizing native Android SDK, official AndroidX/Jetpack libraries (Compose, Room, ViewModel, Lifecycle), and core Kotlin/Kotlinx libraries (Coroutines, Serialization).
- **Scenario: Third-party library inclusion restricted**
  - **WHEN** a new dependency is proposed for addition to `libs.versions.toml`
  - **THEN** it MUST be evaluated against native/Jetpack alternatives, verified to be reputable and established, and documented in an ADR before inclusion

#### Requirement: Architecture Decision Records (ADR)
The repository SHALL maintain an Architecture Decision Record (ADR) log under `docs/adr/` capturing all significant architectural choices.
- **Scenario: Architecture decision recorded**
  - **WHEN** an architectural or major technical choice is made (e.g. Hexagonal Architecture, persistence engine, library adoption)
  - **THEN** an ADR document SHALL be created in `docs/adr/` using sequential numbering and standard sections: Title, Status, Context, Decision, and Consequences

#### Requirement: Living README Maintenance
The repository root SHALL provide a continuously maintained `README.md` reflecting the actual system state and architecture.
- **Scenario: Living README verified**
  - **WHEN** the root `README.md` is inspected
  - **THEN** it SHALL contain a concise overview of supported health metrics (Weight, BP, Glucose with NHG benchmarks), Mermaid diagrams illustrating Hexagonal Architecture boundaries and data flow, and markdown tables detailing core technologies, module structures, and supported import formats

---

## 2. Profile Management

### Purpose
Represents a person using the application. A Profile holds identifying information (name, date-of-birth, optional height) and serves as the Aggregate Root that owns all health log entries.

### ADDED Requirements

#### Requirement: Create profile
The system SHALL allow creating a new Profile with a full name, date-of-birth, and optional height in centimetres.
- **Scenario: Valid profile creation**
  - **WHEN** a client provides a non-empty name, a valid date-of-birth (not in the future, not more than 130 years ago), and an optional height between 50 and 300 cm
  - **THEN** the system SHALL persist the Profile and return its generated identifier
- **Scenario: Missing name rejected**
  - **WHEN** a client provides a blank or empty name
  - **THEN** the system SHALL reject the request with a validation error
- **Scenario: Future date-of-birth rejected**
  - **WHEN** a client provides a date-of-birth that is today or in the future
  - **THEN** the system SHALL reject the request with a validation error

#### Requirement: Read profile
The system SHALL allow retrieving a previously created Profile by its identifier.
- **Scenario: Existing profile returned**
  - **WHEN** a client requests a Profile by a known identifier
  - **THEN** the system SHALL return the Profile including name, date-of-birth, and height (if set)
- **Scenario: Unknown identifier**
  - **WHEN** a client requests a Profile by an identifier that does not exist
  - **THEN** the system SHALL return a not-found result (no error thrown)

---

## 3. Health Metric: Weight & BMI

### Purpose
Records body weight measurements over time for a Profile. Supports BMI calculation when the Profile has a height set. Evaluates weight status against WHO/NHG BMI categories.

### ADDED Requirements

#### Requirement: Record weight measurement
The system SHALL allow recording a body weight measurement in kilograms for a Profile at a given timestamp.
- **Scenario: Valid weight recorded**
  - **WHEN** a client provides a Profile identifier, a weight between 1.0 kg and 700.0 kg, and a measurement timestamp
  - **THEN** the system SHALL persist the measurement and return its identifier
- **Scenario: Weight out of range rejected**
  - **WHEN** a client provides a weight value less than 1.0 kg or greater than 700.0 kg
  - **THEN** the system SHALL reject the request with a validation error

#### Requirement: Calculate and classify BMI against NHG categories
The system SHALL calculate BMI when height is available and classify according to Dutch NHG guidelines:
- Underweight: < 18.5
- Normal weight: 18.5 – 24.9
- Overweight: 25.0 – 29.9
- Obese: >= 30.0

---

## 4. Health Metric: Blood Pressure (NHG Benchmarks)

### Purpose
Records systolic and diastolic blood pressure readings in mmHg for a Profile and classifies them against Dutch NHG (Nederlands Huisartsen Genootschap) guideline thresholds.

### ADDED Requirements

#### Requirement: Record blood pressure measurement
The system SHALL allow recording a blood pressure reading consisting of a systolic and diastolic value in mmHg, associated with a Profile and a timestamp.
- **Scenario: Valid reading recorded**
  - **WHEN** a client provides a Profile identifier, a systolic value between 40 and 300 mmHg, a diastolic value between 20 and 200 mmHg, and a timestamp (with systolic > diastolic)
  - **THEN** the system SHALL persist the reading and return its identifier
- **Scenario: Systolic below diastolic rejected**
  - **WHEN** a client provides a systolic value <= diastolic value
  - **THEN** the system SHALL reject the reading with a validation error

#### Requirement: Classify blood pressure against NHG thresholds
The system SHALL classify blood pressure readings according to Dutch NHG categories:
- Optimal: Systolic < 120 and Diastolic < 80
- Normal: Systolic 120–129 and Diastolic < 80, or Systolic < 130 and Diastolic 80–84
- High Normal: Systolic 130–139 or Diastolic 85–89
- Hypertension Grade 1: Systolic 140–159 or Diastolic 90–99
- Hypertension Grade 2: Systolic 160–179 or Diastolic 100–109
- Hypertension Grade 3: Systolic >= 180 or Diastolic >= 110

---

## 5. Health Metric: Blood Glucose (mmol/L Default & NHG Benchmarks)

### Purpose
Records blood glucose measurements for a Profile. The canonical unit is mmol/L (as required by Dutch NHG guidelines). Supports conversion from mg/dL. Classifies readings as fasting or postprandial against NHG reference ranges.

### ADDED Requirements

#### Requirement: Record glucose measurement
The system SHALL record blood glucose with mmol/L as primary storage unit, supporting Fasting and Postprandial contexts.
- Fasting NHG ranges:
  - Hypoglycaemia: < 3.5 mmol/L
  - Normal: 3.5 – 6.0 mmol/L
  - Impaired Fasting: 6.1 – 6.9 mmol/L
  - Diabetes Range: >= 7.0 mmol/L
- Postprandial NHG ranges:
  - Normal: < 7.8 mmol/L
  - Impaired Glucose Tolerance: 7.8 – 11.0 mmol/L
  - Diabetes Range: > 11.0 mmol/L

#### Requirement: Convert mg/dL input to mmol/L
The system SHALL accept input in mg/dL, converting to mmol/L using factor `value * 0.0555` rounded to 2 decimal places.

---

## 6. Health Metric: Activity Sessions

### Purpose
Records GPS-tracked physical activity sessions for a Profile capturing start time, end time, and total distance in metres.

---

## 7. Data Export & Import (CSV)

### Purpose
Enables export of a Profile's measurement history as a UTF-8 CSV file and import of measurements from CSV into the application.
