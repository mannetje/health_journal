package nl.healthjournal.domain.model.metrics

data class BloodPressureReading(
    val systolic: Int,
    val diastolic: Int
) {
    init {
        require(systolic in MIN_SYSTOLIC..MAX_SYSTOLIC) {
            "Systolic pressure must be between $MIN_SYSTOLIC and $MAX_SYSTOLIC mmHg, got: $systolic"
        }
        require(diastolic in MIN_DIASTOLIC..MAX_DIASTOLIC) {
            "Diastolic pressure must be between $MIN_DIASTOLIC and $MAX_DIASTOLIC mmHg, got: $diastolic"
        }
        require(systolic > diastolic) {
            "Systolic pressure ($systolic mmHg) must be strictly greater than diastolic pressure ($diastolic mmHg)"
        }
    }

    companion object {
        const val MIN_SYSTOLIC = 40
        const val MAX_SYSTOLIC = 300
        const val MIN_DIASTOLIC = 20
        const val MAX_DIASTOLIC = 200
    }
}
