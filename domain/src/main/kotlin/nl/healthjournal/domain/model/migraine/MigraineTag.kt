package nl.healthjournal.domain.model.migraine

import nl.healthjournal.domain.model.common.ProfileId
import java.util.UUID

@JvmInline
value class MigraineTagId(val value: UUID) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): MigraineTagId = MigraineTagId(UUID.randomUUID())
        fun fromString(id: String): MigraineTagId = MigraineTagId(UUID.fromString(id))
    }
}

/**
 * A personal trigger or relief label the person typed. Entries refer to it by id, so a rename shows everywhere;
 * removing a tag only sets [archived] (it is no longer offered, old entries keep it).
 * Uniqueness per kind and profile and the limit of [MAX_ACTIVE_PER_KIND] are enforced by the use cases.
 */
data class MigraineTag(
    val id: MigraineTagId,
    val profileId: ProfileId,
    val kind: MigraineTagKind,
    val label: String,
    val archived: Boolean = false
) {
    init {
        require(label == normalizeLabel(label)) { "Tag label must be trimmed and on a single line" }
        require(label.isNotEmpty()) { "Tag label must not be empty" }
        require(label.length <= MAX_LABEL_LENGTH) { "Tag label must be at most $MAX_LABEL_LENGTH characters, got: ${label.length}" }
        require('|' !in label) { "Tag label must not contain a pipe character" }
    }

    companion object {
        const val MAX_LABEL_LENGTH = 30
        const val MAX_ACTIVE_PER_KIND = 30

        private val lineBreaks = Regex("\\s*[\\r\\n]+\\s*")

        /** Trims and turns line breaks into a single space. */
        fun normalizeLabel(raw: String): String = raw.replace(lineBreaks, " ").trim()

        /** True when two labels are the same tag for uniqueness purposes (ignoring case). */
        fun sameLabel(a: String, b: String): Boolean = normalizeLabel(a).equals(normalizeLabel(b), ignoreCase = true)
    }
}
