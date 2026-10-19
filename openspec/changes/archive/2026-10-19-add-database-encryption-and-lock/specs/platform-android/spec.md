## ADDED Requirements

### Requirement: App lock with BiometricPrompt
The app lock SHALL use `androidx.biometric` `BiometricPrompt` with strong biometrics and device credential allowed, falling back to the confirm-device-credential screen on API 26 to 29 where needed, and SHALL hide content in the recent-apps list and block screenshots when the secure-screen setting is on (`FLAG_SECURE`).

#### Scenario: Older Android
- **WHEN** the device runs API 26
- **THEN** the lock still works through the device credential screen

### Requirement: Encrypted database with SQLCipher and Android Keystore
The Room database SHALL be opened through a SQLCipher `SupportOpenHelperFactory`. The 256-bit passphrase SHALL be generated randomly, wrapped with AES-GCM by a non-exportable Android Keystore key without user-authentication binding (StrongBox or TEE when available), and stored in no-backup storage. The one-time migration from the plain database SHALL use `sqlcipher_export` and verify row counts before removing the plain files, and SHALL be independent of the Room schema version.

#### Scenario: Passphrase never stored in the clear
- **WHEN** the app data directory is inspected
- **THEN** the passphrase appears only in wrapped form

#### Scenario: Schema version unchanged by encryption
- **WHEN** the plain database is migrated to the encrypted file
- **THEN** the Room schema version stays the same and no row changes

### Requirement: Backup and data extraction rules
The manifest SHALL provide `dataExtractionRules` and `fullBackupContent` that exclude the database files and the wrapped key material from cloud backup and device transfer, and SHALL leave `android:allowBackup` as it is.

#### Scenario: Database excluded from backup
- **WHEN** Android backup or device transfer runs
- **THEN** neither the encrypted database nor the wrapped key is included

### Requirement: Dependency exceptions
`net.zetetic:sqlcipher-android` and `androidx.biometric` SHALL be the only new third-party or AndroidX dependencies added by this change, each recorded in ADR 0023 as an exception to ADR 0003 with licence, size, 16 KB page size support and absence of network code checked.

#### Scenario: Dependency review
- **WHEN** a dependency is added without an ADR entry
- **THEN** the change SHALL NOT be merged
