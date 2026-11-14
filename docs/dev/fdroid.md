# Publishing on F-Droid

This page explains how Health Journal gets onto [F-Droid](https://f-droid.org): what the repository already provides, what lives in the separate `fdroiddata` repository, the steps in order, and what to do with every release afterwards.

F-Droid does not host our APK. It builds the app from a tagged commit of our source, signs it with its own key and publishes it. Our GitHub release APK stays the way to get the app outside F-Droid. The two are signed with different keys, so a person cannot update from one to the other without uninstalling (see [Signing](#signing)).

## What we already meet

| F-Droid requirement | Where we stand |
|---|---|
| Free licence | Apache-2.0, in `LICENSE` |
| Source is public and builds with Gradle | GitHub `mannetje/health_journal`, `applicationId` `nl.healthjournal` |
| No proprietary libraries or tracking | No Google Play Services, Firebase, ads or analytics in the Gradle files (checked on the version catalog and the build files, not every transitive dependency; `fdroid scanner` is the final check) |
| No network use | No `INTERNET` permission in the manifest ([privacy and security](privacy-and-security.md)) |
| Store text and images | `fastlane/metadata/android/` (below) |

## The metadata in this repository

F-Droid reads the store listing from `fastlane/metadata/android/<locale>/`:

```
fastlane/metadata/android/
  en-US/
    title.txt                  Health Journal
    short_description.txt      80 characters at most
    full_description.txt       4000 characters at most; a little HTML (b, ul, li) is allowed
    changelogs/<versionCode>.txt   500 characters at most, one per release
    images/icon.png            512 x 512
    images/phoneScreenshots/   1_..., 2_... (sorted by file name)
  nl-NL/
    title.txt, short_description.txt, full_description.txt, changelogs/<versionCode>.txt
```

Rules to keep:

- The descriptions follow the README: private, offline, not a medical device, no advice or targets. Do not add health claims.
- The screenshots are light theme and English. F-Droid shows them for Dutch too; add `nl-NL/images/phoneScreenshots/` only if Dutch shots are wanted.
- Every release needs its own `changelogs/<versionCode>.txt` in both locales. The file name is the `versionCode` from `app/build.gradle.kts`, not the version name (1.7.5 is `37`).

## The recipe in `fdroiddata`

F-Droid's own repository, `gitlab.com/fdroid/fdroiddata`, holds one file per app: `metadata/nl.healthjournal.yml`. It says where the source is and how to build it. A first version:

```yaml
Categories:
  - Health Manager
  - Medication
License: Apache-2.0
AuthorName: marvinstorage
SourceCode: https://github.com/mannetje/health_journal
IssueTracker: https://github.com/mannetje/health_journal/issues
Changelog: https://github.com/mannetje/health_journal/blob/master/CHANGELOG.md

AutoName: Health Journal

RepoType: git
Repo: https://github.com/mannetje/health_journal.git

Builds:
  - versionName: 1.7.5
    versionCode: 37
    commit: v1.7.5
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.7.5
CurrentVersionCode: 37
```

Things in it that need checking against F-Droid's current documentation before the merge request:

- **Categories.** Only names from `config/categories.yml` in `fdroiddata` are valid. "Health Manager" (weight, blood pressure and other health metrics) and "Medication" fit the app.
- **JDK and Gradle.** The app builds with JDK 21 and a recent Gradle wrapper. The build server's JDK may need an extra setting in the recipe (for example a `sudo:` step or a different image). `fdroid build` shows this.
- **The commit.** It must be a tag that contains `fastlane/`. v1.7.4 does not; v1.7.5 is the first tag that does.

## Steps, in order

1. **Make the metadata.** Done: `fastlane/metadata/android/`, on `master`.
2. **Tag a release that contains it.** Done: `v1.7.5`.
3. **Install the GitLab CLI** (`winget install --id GLab.GLab`) and sign in with a personal access token that has the `api` scope: `glab auth login`. Done for the account `marvinstorage`.
4. **Fork `fdroid/fdroiddata`** into your own account:
   ```bash
   glab repo fork fdroid/fdroiddata --clone=false --remote=false
   ```
   Done: `gitlab.com/marvinstorage/fdroiddata`. The repository is large, so avoid cloning it unless you want to run `fdroid build` locally.
5. **Add the recipe** `metadata/nl.healthjournal.yml` on a branch of the fork (the web editor works, or clone and push). Done on the branch `add-nl.healthjournal`, committed with the API (`glab api` to create the branch, then a commit with one `create` action). The fork must be public and the branch unprotected; both are true.
6. **Test the build** if you can: install the F-Droid tooling (`fdroidserver`), then run `fdroid lint nl.healthjournal` and `fdroid build -v -l nl.healthjournal` from a clone of `fdroiddata`. The merge request pipeline runs the same checks.
7. **Open a merge request** from the fork to `fdroid/fdroiddata`, titled `New app: Health Journal`, with the template filled in (the abstract, why the app exists and what it adds over other apps on F-Droid, and the checklist). Reviewers answer on the merge request. Expect questions about the dependencies, the health data and the anti-features.
8. **After the merge**, F-Droid builds and publishes within a few days. Check the listing at `f-droid.org/packages/nl.healthjournal`.

## Every release afterwards

Follow [Releases](releases.md) as usual, and also:

1. Add `changelogs/<versionCode>.txt` in `en-US` and `nl-NL`.
2. Tag `vX.Y.Z`.

With `UpdateCheckMode: Tags` and `AutoUpdateMode: Version`, F-Droid's bot sees the new tag and updates the recipe itself. No change to `fdroiddata` is needed unless the build setup changes.

## Signing

F-Droid builds and signs the app with its own key. The GitHub release APK is signed with the shared debug key (`app/debug.keystore`, see [Releases](releases.md)). The two do not match, so a person who has one installed must uninstall before installing the other. Say so in the store description or the README if people mix them.

F-Droid also supports reproducible builds: it builds the app, compares it with the APK we sign ourselves, and publishes ours. That needs a real release keystore kept private and a build that is byte-for-byte reproducible. Neither exists yet and it is not needed for the first listing.

## Status

| Item | State |
|---|---|
| Fastlane metadata in the repository | on `master` |
| Release tag with the metadata | `v1.7.5` |
| GitLab fork `marvinstorage/fdroiddata` | created |
| Recipe `metadata/nl.healthjournal.yml` | on the fork branch `add-nl.healthjournal` |
| `fdroid lint` and `fdroid build` | not run |
| Merge request to `fdroid/fdroiddata` | not opened |

Pipelines that run in the fork on a push can fail with no jobs; a merge request pipeline runs in the target project instead. Judge the build by the merge request pipeline, not by the fork's.
