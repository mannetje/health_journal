package nl.healthjournal.domain.shared.command

/** Range check shared by the value object factories. Null when [value] is inside `[min, max]`. */
fun <T : Comparable<T>> rangeError(field: ValidationField, value: T, min: T, max: T): DomainError.Invalid? =
    if (value in min..max) null else DomainError.Invalid(field, ValidationRule.OUT_OF_RANGE, listOf(min, max))

/** Runs the checks in order and returns the first error, or the value built by [build]. */
inline fun <T> validated(vararg checks: DomainError.Invalid?, build: () -> T): DomainResult<T> {
    for (check in checks) if (check != null) return DomainResult.Failure(check)
    return DomainResult.Success(build())
}
