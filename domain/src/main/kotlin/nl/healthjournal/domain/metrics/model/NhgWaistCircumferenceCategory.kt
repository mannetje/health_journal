package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.profile.model.Sex

enum class NhgWaistCircumferenceCategory {
    HEALTHY,
    INCREASED_RISK,
    HIGH_RISK;

    companion object {
        fun classify(waist: WaistCircumferenceCm, sex: Sex): NhgWaistCircumferenceCategory {
            val cm = waist.value
            return when (sex) {
                Sex.FEMALE -> when {
                    cm < 80 -> HEALTHY
                    cm < 88 -> INCREASED_RISK
                    else -> HIGH_RISK
                }
                Sex.MALE -> when {
                    cm < 94 -> HEALTHY
                    cm < 102 -> INCREASED_RISK
                    else -> HIGH_RISK
                }
            }
        }
    }
}
