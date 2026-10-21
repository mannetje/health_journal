package nl.healthjournal.domain.model.mindfulness

/** A language the app has text or audio in. Dutch recordings are added later as more catalogue entries. */
enum class ContentLanguage(val tag: String) {
    ENGLISH("en"),
    DUTCH("nl");

    companion object {
        /** The language for a tag such as "nl" or "nl-NL"; English when the tag is not one the app has. */
        fun fromTag(tag: String): ContentLanguage =
            entries.firstOrNull { it.tag.equals(tag.take(2), ignoreCase = true) } ?: ENGLISH
    }
}

/** A text per language. English is required and is the fallback. */
data class LocalizedText(private val byLanguage: Map<ContentLanguage, String>) {
    init {
        require(!byLanguage[ContentLanguage.ENGLISH].isNullOrBlank()) { "English text is required" }
    }

    fun forLanguage(language: ContentLanguage): String =
        byLanguage[language]?.takeIf { it.isNotBlank() } ?: byLanguage.getValue(ContentLanguage.ENGLISH)

    companion object {
        fun of(english: String, dutch: String? = null): LocalizedText =
            LocalizedText(listOfNotNull(ContentLanguage.ENGLISH to english, dutch?.let { ContentLanguage.DUTCH to it }).toMap())
    }
}

/** Where a piece of outside material comes from and under which licence; shown on the exercise and on the credits screen. */
data class ContentCredit(
    val originalTitle: String,
    val author: String,
    val sourceName: String,
    val sourceUrl: String,
    val licenceName: String,
    val licenceUrl: String,
) {
    init {
        require(originalTitle.isNotBlank() && author.isNotBlank() && sourceName.isNotBlank() && licenceName.isNotBlank()) {
            "A credit names the title, author, source and licence"
        }
        require(sourceUrl.startsWith("https://") && licenceUrl.startsWith("https://")) {
            "Credit links must be https"
        }
    }
}

/** A recorded exercise bundled with the app as a raw resource, used unchanged. */
data class GuidedExercise(
    val id: String,
    val audioLanguage: ContentLanguage,
    val durationSeconds: Int,
    val title: LocalizedText,
    val description: LocalizedText,
    val rawResourceName: String,
    val credit: ContentCredit,
) {
    init {
        require(id.isNotBlank()) { "An exercise needs an id" }
        require(durationSeconds > 0) { "Duration must be positive, got: $durationSeconds" }
        require(RESOURCE_NAME.matches(rawResourceName)) { "Not a valid raw resource name: $rawResourceName" }
    }

    fun audioMatches(appLanguage: ContentLanguage): Boolean = audioLanguage == appLanguage

    private companion object {
        val RESOURCE_NAME = Regex("[a-z][a-z0-9_]*")
    }
}

/** The exercises in the app and their order. */
object GuidedExerciseCatalogue {
    private fun freeMindfulnessProject(originalTitle: String) = ContentCredit(
        originalTitle = originalTitle,
        author = "Peter Morgan",
        sourceName = "Free Mindfulness Project",
        sourceUrl = "https://www.freemindfulness.org/download",
        licenceName = "Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported",
        licenceUrl = "https://creativecommons.org/licenses/by-nc-sa/3.0/",
    )

    val exercises: List<GuidedExercise> = listOf(
        GuidedExercise(
            id = "three-step-breathing-space",
            audioLanguage = ContentLanguage.ENGLISH,
            durationSeconds = 3 * 60 + 34,
            title = LocalizedText.of(english = "Three Step Breathing Space", dutch = "Ademruimte in drie stappen"),
            description = LocalizedText.of(
                english = "A short guided pause of about three and a half minutes, built around following the breath.",
                dutch = "Een korte begeleide pauze van ongeveer drie en een halve minuut, rond het volgen van je ademhaling.",
            ),
            rawResourceName = "fmp_three_step_breathing_space",
            credit = freeMindfulnessProject("Three Step Breathing Space"),
        ),
        GuidedExercise(
            id = "mindfulness-of-sounds",
            audioLanguage = ContentLanguage.ENGLISH,
            durationSeconds = 3 * 60 + 2,
            title = LocalizedText.of(english = "Three minute mindfulness of sounds", dutch = "Drie minuten aandacht voor geluiden"),
            description = LocalizedText.of(
                english = "About three minutes of guided listening to the sounds around you.",
                dutch = "Ongeveer drie minuten begeleid luisteren naar de geluiden om je heen.",
            ),
            rawResourceName = "fmp_mindfulness_of_sounds",
            credit = freeMindfulnessProject("Three minute mindfulness of sounds"),
        ),
        GuidedExercise(
            id = "mountain-meditation",
            audioLanguage = ContentLanguage.ENGLISH,
            durationSeconds = 8 * 60 + 12,
            title = LocalizedText.of(english = "Mountain meditation", dutch = "Bergmeditatie"),
            description = LocalizedText.of(
                english = "A longer guided sitting of about eight minutes, with the image of a mountain.",
                dutch = "Een langere begeleide zitoefening van ongeveer acht minuten, met het beeld van een berg.",
            ),
            rawResourceName = "fmp_mountain_meditation",
            credit = freeMindfulnessProject("Mountain meditation"),
        ),
    )

    /**
     * The exercises whose audio is in the app language first, then the others, each group in catalogue order.
     * The screen labels the others with the language of their audio.
     */
    fun orderedFor(appLanguage: ContentLanguage, items: List<GuidedExercise> = exercises): List<GuidedExercise> =
        items.sortedBy { if (it.audioMatches(appLanguage)) 0 else 1 }

    fun byId(id: String): GuidedExercise? = exercises.firstOrNull { it.id == id }
}
