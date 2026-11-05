package nl.healthjournal.domain.shared.model

import nl.healthjournal.domain.shared.command.*

/** Optional free-text note on an entry: single line, trimmed, at most [MAX_LENGTH] characters. */
@JvmInline
value class EntryComment(val text: String) {
    init {
        require(text.isNotBlank()) { "Comment must not be blank" }
        require(text.length <= MAX_LENGTH) { "Comment must be at most $MAX_LENGTH characters, got: ${text.length}" }
        require(text == normalize(text)) { "Comment must be trimmed and on a single line" }
    }

    companion object {
        const val MAX_LENGTH = 200

        private val lineBreaks = Regex("\\s*[\\r\\n]+\\s*")

        /** Trims and turns line breaks into a single space. */
        fun normalize(raw: String): String = raw.replace(lineBreaks, " ").trim()

        /** Checked variant of [ofOrNull]: too long after normalising is a typed error, blank is a null comment. */
        fun of(raw: String?): DomainResult<EntryComment?> {
            val text = raw?.let(::normalize).orEmpty()
            return when {
                text.isEmpty() -> DomainResult.Success(null)
                text.length > MAX_LENGTH ->
                    DomainResult.Failure(DomainError.Invalid(ValidationField.COMMENT, ValidationRule.TOO_LONG, listOf(MAX_LENGTH)))
                else -> DomainResult.Success(EntryComment(text))
            }
        }

        /** Null for null or blank input; throws [IllegalArgumentException] when over [MAX_LENGTH] after normalising. */
        fun ofOrNull(raw: String?): EntryComment? {
            val text = raw?.let(::normalize).orEmpty()
            return if (text.isEmpty()) null else EntryComment(text)
        }
    }
}
