package nl.healthjournal.domain.shared.command

import kotlin.coroutines.cancellation.CancellationException

/** Outcome of a domain operation: a value or a typed [DomainError]. It never carries display text. */
sealed interface DomainResult<out T> {
    data class Success<out T>(val value: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = (this as? Success)?.value
    fun errorOrNull(): DomainError? = (this as? Failure)?.error

    fun <R> map(transform: (T) -> R): DomainResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun <R> flatMap(transform: (T) -> DomainResult<R>): DomainResult<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    companion object {
        fun <T> success(value: T): DomainResult<T> = Success(value)
        fun failure(error: DomainError): DomainResult<Nothing> = Failure(error)
    }
}

/**
 * Runs [block] and maps a thrown exception to [DomainError.Storage]. Cancellation is rethrown, never mapped:
 * a cancelled coroutine is not a storage failure.
 */
suspend inline fun <T> domainCatching(block: () -> T): DomainResult<T> =
    try {
        DomainResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DomainResult.Failure(DomainError.Storage(e))
    }

/** The value, or leaves the caller through [onFailure] (usually a `return`); lets a handler read as a straight line. */
inline fun <T> DomainResult<T>.getOrElse(onFailure: (DomainError) -> Nothing): T = when (this) {
    is DomainResult.Success -> value
    is DomainResult.Failure -> onFailure(error)
}
