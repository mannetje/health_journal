package nl.healthjournal.domain.model.nhg

import nl.healthjournal.domain.model.metrics.WaistCircumferenceCm
import nl.healthjournal.domain.model.profile.Sex

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
