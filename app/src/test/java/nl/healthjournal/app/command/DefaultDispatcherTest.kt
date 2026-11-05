package nl.healthjournal.app.command

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.shared.command.Command
import nl.healthjournal.domain.shared.command.CommandHandler
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.ObserveQuery
import nl.healthjournal.domain.shared.command.ObserveQueryHandler
import nl.healthjournal.domain.shared.command.Query
import nl.healthjournal.domain.shared.command.QueryHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultDispatcherTest {
    private data class Twice(val n: Int) : Command<Int>
    private data class Thrice(val n: Int) : Command<Int>
    private data class Boom(val error: Throwable) : Command<Unit>
    private data class Echo(val s: String) : Query<String>
    private data class Ticks(val n: Int) : ObserveQuery<Int>
    private object Unknown : Command<Unit>

    private val dispatcher = DefaultDispatcher(
        commands = mapOf(
            Twice::class.java to CommandHandler<Twice, Int> { DomainResult.success(it.n * 2) },
            Thrice::class.java to CommandHandler<Thrice, Int> { DomainResult.success(it.n * 3) },
            Boom::class.java to CommandHandler<Boom, Unit> { throw it.error }
        ),
        queries = mapOf(Echo::class.java to QueryHandler<Echo, String> { DomainResult.success(it.s) }),
        observers = mapOf(
            Ticks::class.java to ObserveQueryHandler<Ticks, Int> { q ->
                flow {
                    for (i in 1..q.n) emit(DomainResult.success(i))
                    if (q.n < 0) throw IllegalStateException("stream failed")
                }
            }
        )
    )

    @Test fun `routes by class and returns the typed result`() = runBlocking {
        assertEquals(DomainResult.success(8), dispatcher.dispatch(Twice(4)))
        assertEquals(DomainResult.success(12), dispatcher.dispatch(Thrice(4)))
        assertEquals(DomainResult.success("hi"), dispatcher.query(Echo("hi")))
    }

    @Test fun `observe emits each result`() = runBlocking {
        assertEquals(listOf(1, 2, 3), dispatcher.observe(Ticks(3)).toList().map { it.getOrNull() })
    }

    @Test fun `a failing stream ends with a storage error`() = runBlocking {
        val last = dispatcher.observe(Ticks(-1)).toList().last()
        assertTrue(last.errorOrNull() is DomainError.Storage)
    }

    @Test fun `an exception becomes a storage error`() = runBlocking {
        val boom = IllegalStateException("disk")
        assertEquals(DomainError.Storage(boom), dispatcher.dispatch(Boom(boom)).errorOrNull())
    }

    @Test fun `cancellation propagates`() {
        assertThrows(CancellationException::class.java) {
            runBlocking { dispatcher.dispatch(Boom(CancellationException("cancelled"))) }
        }
    }

    @Test fun `a missing handler is a programming error`() {
        assertThrows(IllegalStateException::class.java) { runBlocking { dispatcher.dispatch(Unknown) } }
    }
}
