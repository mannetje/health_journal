package nl.healthjournal.domain.model.nhg

import nl.healthjournal.domain.model.metrics.BloodPressureReading

enum class NhgBloodPressureCategory {
    NORMAL,
    HIGH,
    SERIOUSLY_RAISED;

    companion object {
        fun classify(reading: BloodPressureReading): NhgBloodPressureCategory {
            val sys = reading.systolic
            val dia = reading.diastolic

            return when {
                sys >= 180 || dia >= 110 -> SERIOUSLY_RAISED
                sys >= 140 || dia >= 90 -> HIGH
                else -> NORMAL
            }
        }

        /**
         * Reads a stored or imported category name, including the six names written by builds
         * before the three-band change. Unknown names still throw, like [valueOf].
         */
        fun fromStoredName(name: String): NhgBloodPressureCategory = when (name) {
            "OPTIMAL", "NORMAL", "HIGH_NORMAL" -> NORMAL
            "HYPERTENSION_GRADE_1", "HYPERTENSION_GRADE_2", "HIGH" -> HIGH
            "HYPERTENSION_GRADE_3", "SERIOUSLY_RAISED" -> SERIOUSLY_RAISED
            else -> throw IllegalArgumentException("Unknown blood pressure category: $name")
        }
    }
}
