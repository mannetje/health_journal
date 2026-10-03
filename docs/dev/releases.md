# Releases

Maintainers cut releases. This page explains how, so you know what happens after your change is merged.

## Versioning

Semantic Versioning, tags `vX.Y.Z`. The version lives in `app/build.gradle.kts` (`versionName` and an increasing `versionCode`).

## Steps

1. Move the entries under `## [Unreleased]` in `CHANGELOG.md` to a new `## [X.Y.Z] - YYYY-MM-DD` section. Use the headings `Added`, `Changed`, `Fixed`, and an `Upgrade note` when users need to know something.
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Update the download links in the README.
4. Run `./gradlew test` and try the app on a device.
5. Commit (`chore: release X.Y.Z`) and push to `master`.
6. Tag and push the tag:

   ```bash
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

7. The workflow in `.github/workflows/android.yml` builds the debug APK, runs the tests, takes the release notes from the matching CHANGELOG section and publishes a GitHub Release with `health-journal-vX.Y.Z-debug.apk`.

## Signing

Every build is signed with the committed debug key `app/debug.keystore`, so updates install over the previous version ([ADR 0008](../adr/0008-fixed-debug-signing-key.md)). Do not replace it.

## After the release

Archive the finished OpenSpec change so the specs describe the shipped behaviour:

```bash
npx --no-install openspec archive <change-name> -y
```

Then check the new capability's Purpose line, which the archive leaves as TBD.
