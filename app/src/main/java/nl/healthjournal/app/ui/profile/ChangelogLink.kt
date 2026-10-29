package nl.healthjournal.app.ui.profile

private const val CHANGELOG_URL = "https://github.com/mannetje/health_journal/blob/master/CHANGELOG.md"

/** The changelog on GitHub; [anchor] (the heading of one version) scrolls it to that section. */
fun changelogUrl(anchor: String): String =
    if (anchor.isBlank()) CHANGELOG_URL else "$CHANGELOG_URL#$anchor"
