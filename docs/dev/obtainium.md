# Installing and updating with Obtainium

[Obtainium](https://obtainium.imranr.dev/) is an open source Android app that installs and updates apps directly from their source, here the GitHub releases of this repository. For a user it is the easiest way to get Health Journal and to stay up to date without a store account. This page explains how to set it up, how it finds the APK, and what a release must keep doing so it keeps working.

Health Journal itself has no `INTERNET` permission and never contacts a server. Only Obtainium talks to GitHub, to read the release list and download the APK.

## For users: add the app

1. Install Obtainium on the device (see its website for the current download).
2. Open Obtainium and tap **Add App**.
3. Paste the repository URL: `https://github.com/mannetje/health_journal`. The source is detected as GitHub.
4. Tap **Add**. Obtainium reads the latest release and lists the app as `nl.healthjournal`.
5. Tap **Install**. When Android asks, allow Obtainium to *Install unknown apps*.

One-tap link (opens Obtainium and fills in the URL; it needs Obtainium installed):

```
obtainium://add/https://github.com/mannetje/health_journal
```

After this, Obtainium checks for new releases in the background, on its own schedule (set it in Obtainium's settings), and shows an update notification. Updating installs over the existing app and keeps all data, because every release since v1.4.4 is signed with the same key ([ADR 0008](../adr/0008-fixed-debug-signing-key.md)). If the installed copy is v1.4.3 or older, Android reports a signature conflict once; export a CSV or make an encrypted backup first, as the [README](../../README.md#method-1-download-from-github-releases-easiest) explains.

### Finding the app by search

Instead of pasting the URL, a user can search in Obtainium: on the **Add App** page, type `health journal` in the search box and pick the GitHub source. Search only covers some sources (GitHub is one), and results are ranked by GitHub, so the official repository may not be first: check that the owner is `mannetje` before adding. Setting clear GitHub topics (for example `android`, `health`, `obtainium`) on the repository helps it show up.

### Settings worth knowing

The defaults work. These are the options you may want to look at under the app's settings in Obtainium:

| Setting | Advice |
|---------|--------|
| Include prereleases | Leave off. Health Journal releases are normal releases. |
| Filter APKs by regex | Not needed. The release has one APK; the `.sha256` file next to it is not an APK. |
| Track only | Leave off, so Obtainium installs and updates, instead of only tracking the version. |
| Background update checks | Obtainium's own setting; pick an interval you like. |

## How Obtainium finds the APK

Obtainium asks the GitHub API for the latest release of `mannetje/health_journal`, takes the version from the tag (`v1.7.5` gives `1.7.5`), and picks the `.apk` asset. It compares that version with the installed one and offers the update when it is newer.

For this to keep working, each release must:

- be a published, non-draft, non-prerelease GitHub release with a tag of the form `vX.Y.Z` (the release flow in [Releases](releases.md) does this);
- attach exactly one `.apk` (the workflow attaches `health-journal-vX.Y.Z-debug.apk`; the `.sha256` file is a second asset and is ignored);
- keep the application id `nl.healthjournal` and the same signing key, or Android refuses the update;
- keep `versionName` in `app/build.gradle.kts` equal to the tag without the `v`, so what Obtainium compares matches what the app reports in Profile.

## Verify the download

Obtainium downloads the same file as the release page. To check it by hand, compare the APK with the `.sha256` file as described in the [README](../../README.md#method-1-download-from-github-releases-easiest).

## Troubleshooting

| Symptom | Likely cause and fix |
|---------|----------------------|
| "No APK found" or no release found | The latest release has no `.apk` asset, or is a draft or prerelease. Check the release page. |
| Obtainium says the app is up to date, but a newer release exists | Obtainium caches checks. Pull to refresh in the app list, or check the release is not marked prerelease. |
| GitHub rate limit message | Unauthenticated GitHub API calls are limited per hour. Wait, or add a GitHub token in Obtainium's settings. |
| Install fails with a signature conflict | The installed copy is v1.4.3 or older, or came from a different build. Export your data, uninstall, install again, import. |
| Update fails to install | Allow Obtainium to *Install unknown apps* in Android settings. |

## Relation to F-Droid

Obtainium installs the project's own release APK, signed with the project's key. A listing on F-Droid (see [Publishing on F-Droid](fdroid.md)) would be a different build, signed by F-Droid, so the two cannot update each other. Pick one source per device.
