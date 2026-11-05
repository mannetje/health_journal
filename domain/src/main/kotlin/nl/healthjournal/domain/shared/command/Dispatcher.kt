package nl.healthjournal.domain.shared.command

import kotlinx.coroutines.flow.Flow

/** A request to change state. [R] is what a success returns, so a call site cannot state a wrong type. */
interface Command<out R>

/** A request to read once. */
interface Query<out R>

/** A request to read and keep reading while the data changes. */
interface ObserveQuery<out R>

/** Handles exactly one command type. A handler knows ports, value objects and a clock, nothing else. */
fun interface CommandHandler<C : Command<R>, R> {
    suspend fun handle(command: C): DomainResult<R>
}

fun interface QueryHandler<Q : Query<R>, R> {
    suspend fun handle(query: Q): DomainResult<R>
}

fun interface ObserveQueryHandler<Q : ObserveQuery<R>, R> {
    fun observe(query: Q): Flow<DomainResult<R>>
}

/**
 * The primary port of the application: ViewModels send commands and queries here and know nothing else.
 *
 * Handlers are main-safe through their ports (Room dispatches itself), so the dispatcher never switches threads.
 * Cancellation is rethrown, never turned into [DomainError.Storage]. A missing handler is a programming error
 * that a unit test catches at build time.
 */
interface Dispatcher {
    suspend fun <R> dispatch(command: Command<R>): DomainResult<R>
    suspend fun <R> query(query: Query<R>): DomainResult<R>
    fun <R> observe(query: ObserveQuery<R>): Flow<DomainResult<R>>
}
