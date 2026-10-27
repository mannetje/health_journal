# Security policy

Health Journal keeps health data on the device, encrypted, and never sends it anywhere. A weakness that exposes that data is the most serious kind of bug in this project, and reports are welcome.

## Reporting a vulnerability

Do not open a public issue. Use GitHub's private vulnerability reporting: open the repository's **Security** tab and choose **Report a vulnerability**. Say what you found, how to reproduce it, which version it affects and what an outsider could read or change.

You will get an acknowledgement, and a fix or a reasoned answer, as fast as one maintainer can manage. This is a personal project, so there is no guaranteed response time. Please give a fix time to ship before you publish details; you will be credited in the changelog if you wish.

## Supported versions

Only the latest release gets fixes. Releases are personal, sideloaded, debug-signed builds ([Releases](docs/dev/releases.md), [ADR 0008](docs/adr/0008-fixed-debug-signing-key.md)); update to the newest one.

## What is in scope

- The encrypted database and its key handling ([ADR 0023](docs/adr/0023-database-encryption-and-app-lock.md), [Privacy and security](docs/dev/privacy-and-security.md)).
- The app lock and the screen protection.
- The encrypted backup file and its restore ([ADR 0024](docs/adr/0024-portable-encrypted-backup.md), [Backup format](docs/backup-format/README.md)).
- Anything that lets health data leave the device: a network permission, logging, a backup route or an exported file that was not asked for.
- Reminder notifications showing more than the generic text when the lock screen is on.

## What is out of scope

- A device that is already rooted or compromised, or one that is unlocked and in someone else's hands.
- A weak backup passphrase chosen by the person. The app states its rules; it cannot choose for you.
- Findings that need a build with `INTERNET` added or other changes to the source.
- Missing hardening in third-party libraries with no way to reach it from this app. Report those upstream; dependabot and CodeQL watch them here ([Continuous integration](docs/dev/ci.md)).

## Guardrails that already exist

The unit tests fail the build if the manifest asks for `INTERNET`, if production code logs, or if the backup rules stop excluding the database and key material (`PrivacyChecksTest`).
