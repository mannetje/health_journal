package nl.healthjournal.domain.model.medication

import java.util.UUID

@JvmInline
value class MedicationId(val value: UUID) {
    init {
        require(value.version() == 4 || value.version() == 7) {
            "MedicationId must be a valid UUID (version 4 or 7), got: $value"
        }
    }

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): MedicationId = MedicationId(UUID.randomUUID())
        fun fromString(id: String): MedicationId = MedicationId(UUID.fromString(id))
    }
}

/** Own identity of a recorded intake, so as-needed doses (no planned time) need no composite key. */
@JvmInline
value class IntakeId(val value: UUID) {
    init {
        require(value.version() == 4 || value.version() == 7) {
            "IntakeId must be a valid UUID (version 4 or 7), got: $value"
        }
    }

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): IntakeId = IntakeId(UUID.randomUUID())
        fun fromString(id: String): IntakeId = IntakeId(UUID.fromString(id))
    }
}
