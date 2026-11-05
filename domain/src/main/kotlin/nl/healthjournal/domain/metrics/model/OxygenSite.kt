package nl.healthjournal.domain.metrics.model

/** Where the sensor sat. A note by the person: nothing is derived from it. */
enum class OxygenSite {
    FINGER,
    TOE,
    EARLOBE;

    companion object {
        /** Null for an unknown or absent code. */
        fun fromCode(code: String?): OxygenSite? = entries.firstOrNull { it.name == code }
    }
}
