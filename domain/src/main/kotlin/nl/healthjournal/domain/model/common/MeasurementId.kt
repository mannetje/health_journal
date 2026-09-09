package nl.healthjournal.domain.model.common

import java.util.UUID

@JvmInline
value class MeasurementId(val value: UUID) {
    init {
        require(value.version() == 4 || value.version() == 7) {
            "MeasurementId must be a valid UUID (version 4 or 7), got: $value"
        }
    }

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): MeasurementId = MeasurementId(UUID.randomUUID())
        fun fromString(id: String): MeasurementId = MeasurementId(UUID.fromString(id))
    }
}
