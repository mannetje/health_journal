## ADDED Requirements

### Requirement: Developer documentation
The project SHALL provide developer documentation in English under `docs/dev/` that explains how to build and run the project, how the layers fit together, how one entry travels through every layer, and how to do the common changes (add a metric, add a translation, change a range label, change the database). It SHALL link to the ADRs and OpenSpec specs for decisions and behaviour instead of duplicating them.

#### Scenario: Newcomer finds the starting point
- **WHEN** a newcomer opens `docs/dev/README.md`
- **THEN** it lists the guides in reading order and links to the ADRs and the specs

#### Scenario: Language
- **WHEN** any page under `docs/dev/` or `CONTRIBUTING.md` is reviewed
- **THEN** it is written in English

### Requirement: Contribution guide
The repository SHALL contain a `CONTRIBUTING.md` at its root that describes the contribution flow (propose with OpenSpec, implement, test, update docs and ADRs, add a changelog entry) and a pull request checklist, and SHALL provide a pull request template and issue templates for bugs and feature requests.

#### Scenario: Contributor opens a pull request
- **WHEN** a pull request is opened on GitHub
- **THEN** the template shows the checklist for tests, docs, ADRs and the changelog

### Requirement: Docs stay accurate
A script run in CI SHALL fail when a relative link in `docs/dev`, `CONTRIBUTING.md` or `README.md` points to a file that does not exist, or when a source path written in backticks in `docs/dev` no longer exists.

#### Scenario: A file is renamed
- **WHEN** a source file named in a developer guide is renamed without updating the guide
- **THEN** the CI docs check fails and names the guide and the missing path

### Requirement: No stale documentation folders
The repository SHALL NOT keep placeholder documentation folders. Documentation lives in `docs/` (ADRs, developer docs, screenshots) and `openspec/` (specs and changes).

#### Scenario: Folder layout
- **WHEN** the repository root is listed
- **THEN** there is no `doc/` folder
