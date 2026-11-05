package nl.healthjournal.domain.command

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.model.common.MeasurementId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteEntryHandlerTest {
    private class Gateway(var result: Boolean = true, var error: Throwable? = null) : EntryGateway {
        var last: Pair<EntryKind, MeasurementId>? = null
        override suspend fun delete(kind: EntryKind, id: MeasurementId): Boolean {
            error?.let { throw it }
            last = kind to id
            return result
        }
    }

    private val id = MeasurementId.generate()

    @Test fun `deleting a known entry succeeds`() = runBlocking {
        val gateway = Gateway()
        assertTrue(DeleteEntryHandler(gateway).handle(DeleteEntryCommand(EntryKind.GLUCOSE, id)).isSuccess)
        assertEquals(EntryKind.GLUCOSE to id, gateway.last)
    }

    @Test fun `an unknown id is not found`() = runBlocking {
        val result = DeleteEntryHandler(Gateway(result = false)).handle(DeleteEntryCommand(EntryKind.MIGRAINE, id))
        assertEquals(DomainError.NotFound("MIGRAINE", id.toString()), result.errorOrNull())
    }

    @Test fun `a storage failure is typed`() = runBlocking {
        val boom = IllegalStateException("disk")
        val result = DeleteEntryHandler(Gateway(error = boom)).handle(DeleteEntryCommand(EntryKind.WEIGHT, id))
        assertEquals(DomainError.Storage(boom), result.errorOrNull())
    }
}
