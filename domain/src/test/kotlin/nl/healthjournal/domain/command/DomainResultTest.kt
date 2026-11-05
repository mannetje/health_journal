package nl.healthjournal.domain.command

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainResultTest {

    @Test
    fun `map and flatMap follow success and pass failure through`() {
        val ok: DomainResult<Int> = DomainResult.success(2)
        assertEquals(4, ok.map { it * 2 }.getOrNull())
        assertEquals(3, ok.flatMap { DomainResult.success(it + 1) }.getOrNull())
        val failure: DomainResult<Int> = DomainResult.failure(DomainError.NotFound("weight", "x"))
        assertNull(failure.map { it * 2 }.getOrNull())
        assertEquals(DomainError.NotFound("weight", "x"), failure.flatMap { DomainResult.success(it) }.errorOrNull())
    }

    @Test
    fun `an exception becomes a storage error`() = runBlocking {
        val boom = IllegalStateException("disk full")
        val result = domainCatching { throw boom }
        assertEquals(DomainError.Storage(boom), result.errorOrNull())
    }

    @Test
    fun `cancellation is never mapped to storage`() {
        assertThrows(CancellationException::class.java) {
            runBlocking { domainCatching { throw CancellationException("cancelled") } }
        }
    }

    @Test
    fun `a value passes through`() = runBlocking {
        assertTrue(domainCatching { 1 }.isSuccess)
    }
}
