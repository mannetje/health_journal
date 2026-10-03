# How to add or change a translation

All user-facing text lives in Android string resources. The app ships English (the default) and Dutch, and is meant to grow to more European languages.

## Rules

- **Never hard-code text** in Kotlin. Use `stringResource(R.string.…)` in a screen. A ViewModel holds a `UiText` that the screen resolves ([ADR 0015](../../adr/0015-localized-viewmodel-messages.md)).
- **Every key exists in every language.** Add a new key to `app/src/main/res/values/strings.xml` and to each `values-<language>/strings.xml` in the same change.
- **Use positional placeholders** (`%1$s`, `%2$d`) so a translation can reorder words, and keep the same placeholders in every language.
- **Plurals and numbers:** use Android plural resources where the text depends on a count, and format numbers with the user's region (decimal comma in the Netherlands). See `ui/common/UnitFormat.kt`.
- **Wording:** the app is a personal logging tool, not a medical device. Translations must not add advice, warnings or condition names. The label rule is in [Change a range label](change-a-range-label.md).
- **Long languages:** Dutch is often 30 percent longer than English. Test your screen in Dutch and at a larger font size ([ADR 0010](../../adr/0010-responsive-dutch-ui-layout.md)).

## Change an existing string

1. Edit the key in `res/values/strings.xml` and in each language file.
2. Run the app in each language and look at the screen.
3. Run `./gradlew test`. `RangeLabelStringsTest` checks the range labels of both languages (same keys, same placeholders, no condition names).

## Add a new language

1. Create `app/src/main/res/values-<code>/strings.xml` (for example `values-de` for German) and translate every key.
2. Add the language to `AppLanguage` in `app/src/main/java/nl/healthjournal/app/settings/LanguagePreference.kt` and to the language picker in `ui/profile/ProfileScreen.kt` (with a new `profile_language_…` string that names the language in its own language, like "Nederlands").
3. Check the range labels with someone who knows the local guidelines. Range limits come from guidelines, and the source is named per country in the "About these ranges" sheet (`ui/nhg/RangeSourceNote.kt`). A new country may need its own limits and sources, which is a design question: open an OpenSpec proposal first.
4. Add a CHANGELOG entry.

## Translating without Android Studio

If you only want to help with a language, you can edit `strings.xml` in any text editor and open a pull request. Mark strings you are unsure about in the pull request description.
