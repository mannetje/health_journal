package nl.healthjournal.app.command

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import nl.healthjournal.domain.shared.command.Command
import nl.healthjournal.domain.shared.command.CommandHandler
import nl.healthjournal.domain.shared.command.Dispatcher
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.ObserveQuery
import nl.healthjournal.domain.shared.command.ObserveQueryHandler
import nl.healthjournal.domain.shared.command.Query
import nl.healthjournal.domain.shared.command.QueryHandler
import nl.healthjournal.domain.shared.command.domainCatching
import javax.inject.Inject

/**
 * Routes a command or query to its handler by class. The handlers come from Hilt multibindings (see
 * [CommandModule]); a missing one is a programming error, caught by `HandlerRegistryTest`.
 * A thrown exception becomes [DomainError.Storage]; cancellation is rethrown.
 */
class DefaultDispatcher @Inject constructor(
    private val commands: Map<Class<*>, @JvmSuppressWildcards CommandHandler<*, *>>,
    private val queries: Map<Class<*>, @JvmSuppressWildcards QueryHandler<*, *>>,
    private val observers: Map<Class<*>, @JvmSuppressWildcards ObserveQueryHandler<*, *>>
) : Dispatcher {

    override suspend fun <R> dispatch(command: Command<R>): DomainResult<R> {
        val handler = commands[command.javaClass] ?: noHandler(command)
        @Suppress("UNCHECKED_CAST")
        val typed = handler as CommandHandler<Command<R>, R>
        return domainCatching { typed.handle(command) }.flatMap { it }
    }

    override suspend fun <R> query(query: Query<R>): DomainResult<R> {
        val handler = queries[query.javaClass] ?: noHandler(query)
        @Suppress("UNCHECKED_CAST")
        val typed = handler as QueryHandler<Query<R>, R>
        return domainCatching { typed.handle(query) }.flatMap { it }
    }

    override fun <R> observe(query: ObserveQuery<R>): Flow<DomainResult<R>> {
        val handler = observers[query.javaClass] ?: noHandler(query)
        @Suppress("UNCHECKED_CAST")
        val typed = handler as ObserveQueryHandler<ObserveQuery<R>, R>
        return typed.observe(query).catch { emit(DomainResult.failure(DomainError.Storage(it))) }
    }

    private fun noHandler(request: Any): Nothing = error("No handler registered for ${request.javaClass.name}")
}
