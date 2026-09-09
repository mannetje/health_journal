package nl.healthjournal.domain.model.nhg

import nl.healthjournal.domain.model.metrics.BloodPressureReading

enum class NhgBloodPressureCategory {
    OPTIMAL,
    NORMAL,
    HIGH_NORMAL,
    HYPERTENSION_GRADE_1,
    HYPERTENSION_GRADE_2,
    HYPERTENSION_GRADE_3;

    companion object {
        fun classify(reading: BloodPressureReading): NhgBloodPressureCategory {
            val sys = reading.systolic
            val dia = reading.diastolic

            return when {
                // Hypertension grade 3
                sys >= 180 || dia >= 110 -> HYPERTENSION_GRADE_3
                // Hypertension grade 2
                sys >= 160 || dia >= 100 -> HYPERTENSION_GRADE_2
                // Hypertension grade 1
                sys >= 140 || dia >= 90 -> HYPERTENSION_GRADE_1
                // High normal
                (sys in 130..139) || (dia in 85..89) -> HIGH_NORMAL
                // Normal
                (sys in 120..129 && dia < 80) || (sys < 130 && dia in 80..84) -> NORMAL
                // Optimal
                sys < 120 && dia < 80 -> OPTIMAL
                else -> NORMAL
            }
        }
    }
}
