## ADDED Requirements

### Requirement: Context package layout
The domain layer SHALL be organised by context: every source file under the domain module SHALL live in the package `nl.healthjournal.domain.<context>.<role>`, where the context is one of shared, profile, metrics, medication, migraine, mindfulness, lock or backup, and the role is model, usecase, port or command. The directory of a file SHALL match its package. Tests SHALL mirror the layout. An automated test SHALL fail when a domain file lies outside this layout or in a directory that does not match its package, and SHALL name the file.

#### Scenario: A file outside the layout
- **WHEN** a domain source file is placed in a folder that is not a context and role folder
- **THEN** the layout test fails and names the file

#### Scenario: Package and directory disagree
- **WHEN** a domain file declares a package that does not match its directory
- **THEN** the layout test fails and names the file

#### Scenario: One context in one place
- **WHEN** a developer looks for everything that belongs to medication
- **THEN** its model, use cases, ports and commands are all under the medication context folder
