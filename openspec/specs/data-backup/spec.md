# data-backup Specification

## Purpose
Lets the user keep and move their data: one passphrase-encrypted `.hjbackup` file with a published format, created and restored without a server or an account, merged on restore and usable without the app.

## Requirements

### Requirement: Backup file container
A backup SHALL be a single file with the extension `.hjbackup`. It SHALL start with a fixed 28-byte big-endian header: the ASCII magic `HJBACKUP` (8 bytes), the container version as an unsigned 16-bit integer (1), the key-derivation id (1 = Argon2id version 1.3), the cipher id (1 = AES-256-GCM), the compression id (0 = none, 1 = gzip), the Argon2 parallelism (1 byte), the salt length (16), the nonce length (12), the Argon2 memory in KiB (unsigned 32-bit), the Argon2 iterations (unsigned 32-bit) and 4 reserved bytes that SHALL be zero. The salt (16 random bytes) and the nonce (12 random bytes) follow the header, then the ciphertext with its 16-byte GCM tag. The whole header including salt and nonce SHALL be passed as associated data to AES-GCM. The plaintext is the UTF-8 JSON document, gzip-compressed when the compression id is 1. A new salt and a new nonce SHALL be generated for every file.

#### Scenario: Tampered header
- **WHEN** one byte of the header, salt or nonce is changed
- **THEN** decryption fails the authentication check and nothing is imported

#### Scenario: Two backups of the same data
- **WHEN** the same journal is backed up twice with the same passphrase
- **THEN** the two files differ because salt and nonce are random

#### Scenario: Reader limits
- **WHEN** a header asks for more than 1 GiB of Argon2 memory, more than 100 iterations, more than 16 lanes, unknown ids or non-zero reserved bytes
- **THEN** the reader refuses the file before deriving a key and says that the file is not supported

### Requirement: Key derivation from the passphrase
The encryption key SHALL be 32 bytes derived with Argon2id (RFC 9106, version 0x13, no secret and no associated data) from the passphrase and the file's salt, using the parameters in the header. The passphrase SHALL be normalised to Unicode NFKC and encoded as UTF-8 before use. New backups SHALL use 64 MiB of memory (65536 KiB), 3 iterations and parallelism 1. Readers SHALL use the parameters from the header, so the defaults can be raised later without a new container version.

#### Scenario: Same passphrase, different Unicode form
- **WHEN** the passphrase "é" was typed as one composed character at backup and as "e" plus a combining accent at restore
- **THEN** the same key is derived and the restore succeeds

#### Scenario: Wrong passphrase
- **WHEN** a wrong passphrase is entered at restore
- **THEN** the restore stops with a message that the passphrase is wrong or the file is damaged, nothing is imported, and the user may try again

### Requirement: Backup document
The decrypted document SHALL be a JSON object that validates against `docs/backup-format/healthjournal-backup.schema.json`, with `format` equal to `healthjournal-backup`, an integer `schemaVersion` (1), `createdAt`, and `profiles`. Each profile SHALL carry its identifier, name, date of birth, height, sex and active flag, and the lists of weights, blood pressures, glucoses, waist circumferences, activities and medications. Each medication SHALL carry its schedule versions (with the daily times of each version) and its intake log. All values SHALL use the canonical conventions of the CSV contract: metric units, ISO-8601 UTC instants with a trailing Z, a decimal point, enumerations by their unlocalised names. Decimal numbers SHALL be written as plain decimal strings so no precision is lost. Planned dose times are local date-times without a zone, as in the medication data. Comments are included. The document SHALL also carry an optional `preferences` object with the display preferences language, region, unit system, glucose unit, theme and reminder snooze length, each by its unlocalised tag (`system` meaning follow the device). Security and privacy settings (app lock and its timeout, secure screen, lock-screen details of reminders) and per-device state (accepted notices, permission prompts) SHALL NOT be included, so that restoring a backup can never weaken the protection of a device. Readers SHALL ignore properties they do not know, and a change that removes or reinterprets a property SHALL raise `schemaVersion`.

#### Scenario: Everything a profile holds is in the file
- **WHEN** a profile with weights, blood pressures, glucose, waist, activity, a medication with two schedule versions and an intake log is backed up
- **THEN** the document contains all of them, including comments, and a restore into an empty app shows identical entries and the same medication schedule history

#### Scenario: Decimals keep their precision
- **WHEN** a weight of 82.4 kg and a dose amount of 2.5 are backed up
- **THEN** the document holds the strings "82.4" and "2.5"

#### Scenario: Display preferences are in the file
- **WHEN** a backup is created while the language is Dutch, the theme is dark and the units are metric
- **THEN** the document's `preferences` hold those values and nothing about the lock

#### Scenario: Security settings are never in the file
- **WHEN** a backup is created with the app lock on and the secure screen enabled
- **THEN** the document contains neither setting, and restoring it never changes the lock or the secure screen

#### Scenario: Newer file, same container
- **WHEN** a reader supporting schemaVersion 1 opens a file with extra properties
- **THEN** it ignores them and imports what it knows

#### Scenario: Unsupported schema
- **WHEN** a file has a `schemaVersion` the reader cannot interpret (greater than it supports and flagged incompatible)
- **THEN** it refuses with a message that a newer app version is needed and imports nothing

### Requirement: Create a backup
The Profile screen SHALL offer "Create backup". The user SHALL choose a passphrase of at least 8 characters and type it twice, SHALL see a hint, in plain words, that a phrase of three or four random words is stronger and easier to remember than eight random characters, SHALL be told that the passphrase cannot be recovered and that without it the backup cannot be opened, and SHALL choose the destination through the system document picker. The file name SHALL be proposed as `health-journal-<yyyy-mm-dd>.hjbackup` and SHALL NOT contain a profile name. The backup SHALL be written only after the user's explicit action and only to the chosen destination. The app SHALL NOT store the passphrase. The app SHALL show the number of entries written and SHALL read the file back and verify it decrypts before reporting success.

#### Scenario: Passphrase too short
- **WHEN** the user enters 7 characters
- **THEN** the backup is not created and the dialog says the minimum

#### Scenario: Hint while typing
- **WHEN** the passphrase dialog is shown
- **THEN** it suggests a phrase of three or four random words and does not score or colour the entry as weak or strong

#### Scenario: Passphrases differ
- **WHEN** the two entries differ
- **THEN** the backup is not created and the dialog says so

#### Scenario: Verified after writing
- **WHEN** the backup has been written
- **THEN** the app reads it back and decrypts it, and reports success only if that works

#### Scenario: Cancelled picker
- **WHEN** the user cancels the document picker
- **THEN** nothing is written

#### Scenario: Both languages
- **WHEN** the language is English or Dutch
- **THEN** every label, warning and message of the backup flow is shown in that language

### Requirement: Restore a backup
The Profile screen SHALL offer "Restore backup", and the lost-key screen SHALL offer "Restore from a backup" next to the delete action. The user picks the file through the system document picker and enters the passphrase. A restore SHALL merge: profiles, entries, medications, schedule versions and intakes whose identifier already exists are kept as they are and are not overwritten, everything else is added. A restore SHALL run in one database transaction, so a failure leaves the data unchanged. After a restore the app SHALL report how many entries were added and how many were skipped because they existed. When restoring from the lost-key screen the app SHALL first create the new empty encrypted database, then import into it. The restore dialog SHALL offer "Also restore settings" (language, region, units, theme, snooze), on by default when the file contains preferences; when ticked the stored values replace the current ones, when not ticked the preferences of the device are unchanged. Unknown or invalid preference values SHALL be ignored. A restore SHALL NOT change which profile is active when the app already has an active profile, and SHALL activate the active profile of the file when the app has none.

#### Scenario: Restore into an empty app
- **WHEN** a backup is restored into a freshly installed app
- **THEN** all profiles, entries and medication from the file are present and the active profile is the one that was active in the file

#### Scenario: Settings restored on request
- **WHEN** a backup with Dutch language and dark theme is restored with "Also restore settings" ticked
- **THEN** the app switches to Dutch and the dark theme, and the lock settings are unchanged

#### Scenario: Settings left alone
- **WHEN** the same backup is restored with the option unticked
- **THEN** the language and theme of the device are unchanged

#### Scenario: Restore twice
- **WHEN** the same backup is restored twice
- **THEN** the second restore adds nothing and reports every entry as already present

#### Scenario: Existing entry not overwritten
- **WHEN** an entry with the same identifier exists with a different value
- **THEN** the existing value is kept and the entry is counted as skipped

#### Scenario: Restore fails halfway
- **WHEN** the file is truncated or a value is invalid
- **THEN** nothing from the file is imported and the data is unchanged

#### Scenario: Restore after the key was lost
- **WHEN** the lost-key screen is showing and the user restores a backup
- **THEN** the unreadable data is removed, a new key and database are created, the backup is imported, and the app starts with the restored data

#### Scenario: Not a backup file
- **WHEN** the user picks a file that does not start with the magic bytes
- **THEN** the app says it is not a Health Journal backup and imports nothing

### Requirement: Backup stays under the user's control
The backup flow SHALL need no network, no account and no vendor service, SHALL use only the system document picker for reading and writing, and SHALL NOT upload, schedule, sync or share on its own. The app SHALL NOT keep a copy of a backup or of its passphrase.

#### Scenario: No network permission
- **WHEN** the manifest is inspected after the backup feature is added
- **THEN** it still has no INTERNET permission

#### Scenario: Destination choice
- **WHEN** the user creates a backup
- **THEN** the destination is whatever the user picks in the system picker, such as local storage, a removable drive or a storage app of their own choice

### Requirement: Published format and recovery without the app
The container, the document schema, test vectors and a reference decoder SHALL be published in the repository under `docs/backup-format/`, together with a guide that lets a person with the file and the passphrase read the data without Health Journal. Test vectors SHALL include at least one file with the default parameters and one with fast parameters and no compression, each with the passphrase and the expected decrypted document. Any change of the container or of a document property SHALL update the specification, the schema, the vectors and the reference decoder in the same change.

#### Scenario: Reference decoder reads the vectors
- **WHEN** the reference decoder runs on each test vector with its passphrase
- **THEN** the output equals the expected document

#### Scenario: App reads the vectors
- **WHEN** the app's unit tests decode the same test vectors
- **THEN** they produce the expected document, so the app and the published format cannot drift apart

#### Scenario: Abandoned project
- **WHEN** the app is no longer maintained or installable
- **THEN** the guide, the schema and the decoder are enough to turn a backup file and its passphrase into JSON and CSV
