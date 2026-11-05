package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.profile.model.Sex

/**
 * Domain service: classifies a waist circumference against the Voedingscentrum NHG categories, which
 * depend on sex. Pure, no ports. Returns null when the sex is unknown.
 */
object WaistCircumferenceClassifier {
    fun classify(sex: Sex?, waist: WaistCircumferenceCm): NhgWaistCircumferenceCategory? =
        sex?.let { NhgWaistCircumferenceCategory.classify(waist, it) }
}
