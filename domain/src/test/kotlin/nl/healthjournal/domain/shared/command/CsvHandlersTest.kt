package nl.healthjournal.domain.shared.command

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.port.DataExportPort
import nl.healthjournal.domain.metrics.port.DataImportPort
import nl.healthjournal.domain.shared.model.ImportResult
import nl.healthjournal.domain.migraine.port.MigraineExportPort
import nl.healthjournal.domain.migraine.port.MigraineImportPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvHandlersTest {
    private val pid = ProfileId.generate()

    private val export = object : DataExportPort {
        override suspend fun exportWeightCsv(profileId: ProfileId) = "weight"
        override suspend fun exportBloodPressureCsv(profileId: ProfileId) = "bp"
        override suspend fun exportGlucoseCsv(profileId: ProfileId) = "glucose"
        override suspend fun exportActivityCsv(profileId: ProfileId) = "activity"
        override suspend fun exportWaistCircumferenceCsv(profileId: ProfileId) = "waist"
        override suspend fun exportOxygenSaturationCsv(profileId: ProfileId) = "oxygen"
    }
    private val migraineExport = object : MigraineExportPort {
        override suspend fun exportMigraineCsv(profileId: ProfileId) = "migraine"
    }
    private val importedKeys = mutableListOf<String>()
    private val import = object : DataImportPort {
        override suspend fun importCsv(profileId: ProfileId, metricType: String, csvContent: String): ImportResult {
            importedKeys += metricType
            return ImportResult(1, emptyList())
        }
    }
    private val migraineImport = object : MigraineImportPort {
        override suspend fun importMigraineCsv(profileId: ProfileId, csvContent: String) = ImportResult(7, emptyList())
    }

    @Test fun `export picks the port by kind`() = runBlocking {
        val handler = ExportCsvHandler(export, migraineExport)
        val expected = mapOf(
            EntryKind.WEIGHT to "weight", EntryKind.BLOOD_PRESSURE to "bp", EntryKind.GLUCOSE to "glucose",
            EntryKind.ACTIVITY to "activity", EntryKind.WAIST_CIRCUMFERENCE to "waist",
            EntryKind.OXYGEN_SATURATION to "oxygen", EntryKind.MIGRAINE to "migraine"
        )
        for ((kind, text) in expected) assertEquals(text, handler.handle(ExportCsvQuery(pid, kind)).getOrNull())
    }

    @Test fun `import routes migraine to its own port and others by key`() = runBlocking {
        val handler = ImportCsvHandler(import, migraineImport)
        assertEquals(7, handler.handle(ImportCsvCommand(pid, EntryKind.MIGRAINE, "x")).getOrNull()!!.importedCount)
        handler.handle(ImportCsvCommand(pid, EntryKind.WEIGHT, "x"))
        handler.handle(ImportCsvCommand(pid, EntryKind.WEIGHT, "x", libra = true))
        handler.handle(ImportCsvCommand(pid, EntryKind.OXYGEN_SATURATION, "x"))
        assertEquals(listOf("weight", "libra", "oxygen_saturation"), importedKeys)
    }

    @Test fun `a failing port is a storage error`() = runBlocking {
        val broken = object : DataImportPort {
            override suspend fun importCsv(profileId: ProfileId, metricType: String, csvContent: String): ImportResult =
                throw IllegalStateException("disk")
        }
        val result = ImportCsvHandler(broken, migraineImport).handle(ImportCsvCommand(pid, EntryKind.GLUCOSE, "x"))
        assertTrue(result.errorOrNull() is DomainError.Storage)
    }

    @Test fun `ui keys map to a kind once at the edge`() {
        assertEquals(EntryKind.BLOOD_PRESSURE, entryKindFromKey("bp"))
        assertEquals(EntryKind.OXYGEN_SATURATION, entryKindFromKey("SpO2"))
        assertEquals(EntryKind.WEIGHT, entryKindFromKey("libra"))
        assertTrue(isLibraKey("libra"))
        assertTrue(!isLibraKey("weight"))
        assertNull(entryKindFromKey("nope"))
    }
}
