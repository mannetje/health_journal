## ADDED Requirements

### Requirement: Installed version and changelog link
The Profile screen SHALL show the installed app version as "Version X.Y.Z" and a "What's new" link. The link SHALL open the project `CHANGELOG.md` on GitHub in the browser, scrolled to the section of the installed version. When the changelog has no section for the installed version, the link SHALL open the top of the file. The app SHALL NOT make any network request itself for this.

#### Scenario: Version shown
- **WHEN** the user opens the Profile tab of version 1.6.3
- **THEN** the text "Version 1.6.3" SHALL be visible

#### Scenario: Link jumps to the installed version
- **WHEN** the user taps "What's new" and the changelog has a section for the installed version
- **THEN** the browser SHALL open the changelog URL with the anchor of that section

#### Scenario: No section for this version
- **WHEN** the user taps "What's new" and the changelog has no section for the installed version
- **THEN** the browser SHALL open the changelog URL without an anchor
