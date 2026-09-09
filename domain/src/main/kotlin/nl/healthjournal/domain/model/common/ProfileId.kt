package nl.healthjournal.domain.model.common

import java.util.UUID

@JvmInline
value class ProfileId(val value: UUID) {
    init {
        require(value.version() == 4 || value.version() == 7) {
            "ProfileId must be a valid UUID (version 4 or 7), got: $value"
        }
    }

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ProfileId = ProfileId(UUID.randomUUID())
        fun fromString(id: String): ProfileId = ProfileId(UUID.fromString(id))
    }
}
