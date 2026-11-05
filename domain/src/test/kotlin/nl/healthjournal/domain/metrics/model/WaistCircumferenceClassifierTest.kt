package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.profile.model.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WaistCircumferenceClassifierTest {
    @Test
    fun `no sex means no category`() {
        assertNull(WaistCircumferenceClassifier.classify(null, WaistCircumferenceCm(90.0)))
    }

    @Test
    fun `the sex picks the limits`() {
        val waist = WaistCircumferenceCm(90.0)
        assertEquals(NhgWaistCircumferenceCategory.HIGH_RISK, WaistCircumferenceClassifier.classify(Sex.FEMALE, waist))
        assertEquals(NhgWaistCircumferenceCategory.HEALTHY, WaistCircumferenceClassifier.classify(Sex.MALE, waist))
    }
}
