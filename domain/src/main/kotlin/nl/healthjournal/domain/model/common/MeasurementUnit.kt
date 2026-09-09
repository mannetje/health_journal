package nl.healthjournal.domain.model.common

enum class MeasurementUnit(val symbol: String) {
    KILOGRAM("kg"),
    MILLIMETERS_OF_MERCURY("mmHg"),
    MILLIMOLES_PER_LITER("mmol/L"),
    MILLIGRAMS_PER_DECILITER("mg/dL"),
    METERS("m"),
    SECONDS("s")
}
