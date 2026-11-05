package nl.healthjournal.app.ui.backup

import android.net.TestUri
import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import nl.healthjournal.app.R
import nl.healthjournal.app.command.DefaultDispatcher
import nl.healthjournal.domain.backup.command.CreateBackupCommand
import nl.healthjournal.domain.backup.command.CreateBackupHandler
import nl.healthjournal.domain.backup.command.OpenBackupHandler
import nl.healthjournal.domain.backup.command.OpenBackupQuery
import nl.healthjournal.domain.backup.command.RestoreBackupCommand
import nl.healthjournal.domain.backup.command.RestoreBackupHandler
import nl.healthjournal.domain.shared.command.CommandHandler
import nl.healthjournal.domain.shared.command.Dispatcher
import nl.healthjournal.domain.shared.command.QueryHandler
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.domain.backup.model.BackupDocument
import nl.healthjournal.domain.backup.model.BackupPreferences
import nl.healthjournal.domain.backup.model.BackupProfile
import nl.healthjournal.domain.backup.model.MergeResult
import nl.healthjournal.domain.backup.port.BackupCodecPort
import nl.healthjournal.domain.backup.port.BackupSnapshotPort
import nl.healthjournal.domain.backup.usecase.CreateBackupUseCase
import nl.healthjournal.domain.backup.usecase.RestoreBackupUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private class FailingSnapshot(private val failure: Throwable) : BackupSnapshotPort {
        override suspend fun snapshot(): List<BackupProfile> = throw failure
        override suspend fun merge(profiles: List<BackupProfile>): MergeResult = throw failure
    }

    private object UnusedCodec : BackupCodecPort {
        override fun encode(document: BackupDocument, passphrase: CharArray): ByteArray = ByteArray(0)
        override fun decode(bytes: ByteArray, passphrase: CharArray): BackupDocument = error("not used")
    }

    private val noIo = object : BackupFileIo {
        override fun read(uri: Uri): ByteArray = error("not used")
        override fun write(uri: Uri, bytes: ByteArray) = error("not used")
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun dispatcher(snapshot: BackupSnapshotPort, codec: BackupCodecPort): Dispatcher = DefaultDispatcher(
        commands = mapOf<Class<*>, CommandHandler<*, *>>(
            CreateBackupCommand::class.java to CreateBackupHandler(CreateBackupUseCase(snapshot, codec)),
            RestoreBackupCommand::class.java to RestoreBackupHandler(RestoreBackupUseCase(snapshot, codec))
        ),
        queries = mapOf<Class<*>, QueryHandler<*, *>>(
            OpenBackupQuery::class.java to OpenBackupHandler(RestoreBackupUseCase(snapshot, codec))
        ),
        observers = emptyMap()
    )

    private fun viewModel(snapshotFailure: Throwable): BackupViewModel {
        val snapshot = FailingSnapshot(snapshotFailure)
        return BackupViewModel(
            dispatcher = dispatcher(snapshot, UnusedCodec),
            canCreate = true,
            io = noIo,
            preferences = { null },
            applyPreferences = {},
            appVersion = "test"
        )
    }

    private fun BackupViewModel.createBackup() {
        val passphrase = "correct horse battery staple".toCharArray()
        submitCreate(passphrase, passphrase.copyOf())
        runBlocking { viewModelScope.coroutineContext.job.children.toList().joinAll() }
    }

    @Test
    fun `an ordinary failure while creating is reported`() {
        val vm = viewModel(IllegalStateException("disk"))

        vm.createBackup()

        assertEquals(UiText.Res(R.string.backup_err_create), vm.state.value.error)
    }

    @Test
    fun `a cancelled create is not reported as a failure`() {
        val vm = viewModel(CancellationException("screen closed"))

        vm.createBackup()

        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.busy)
    }

    private val backupPreferences = BackupPreferences("nl", "NL", "metric", "mmol", "dark", 10)

    private fun restoreViewModel(applied: MutableList<BackupPreferences>): BackupViewModel {
        val snapshot = object : BackupSnapshotPort {
            override suspend fun snapshot(): List<BackupProfile> = emptyList()
            override suspend fun merge(profiles: List<BackupProfile>): MergeResult = MergeResult.NOTHING
        }
        val codec = object : BackupCodecPort {
            override fun encode(document: BackupDocument, passphrase: CharArray): ByteArray = ByteArray(0)
            override fun decode(bytes: ByteArray, passphrase: CharArray): BackupDocument =
                BackupDocument(Instant.EPOCH, "test", backupPreferences, emptyList())
        }
        val io = object : BackupFileIo {
            override fun read(uri: Uri): ByteArray = "HJBACKUP".toByteArray(Charsets.US_ASCII)
            override fun write(uri: Uri, bytes: ByteArray) = error("not used")
        }
        return BackupViewModel(
            dispatcher = dispatcher(snapshot, codec),
            canCreate = false,
            io = io,
            preferences = { null },
            applyPreferences = { applied += it },
            appVersion = "test"
        )
    }

    private fun BackupViewModel.settle() =
        runBlocking { viewModelScope.coroutineContext.job.children.toList().joinAll() }

    /** Fails after a second instead of waiting for an event that a failed restore never sends. */
    private fun BackupViewModel.nextEvent(): BackupEvent {
        assertNull("restore failed: ${state.value.error}", state.value.error)
        return runBlocking { withTimeout(1000) { events.first() } }
    }

    private val someUri: Uri = TestUri

    private fun BackupViewModel.restore(applySettings: Boolean) {
        onRestoreFile(someUri)
        settle()
        submitRestorePassphrase("correct horse battery staple".toCharArray())
        settle()
        confirmRestore(applySettings)
        settle()
    }

    @Test
    fun `a restore tells the screen to refresh and that the settings changed`() {
        val applied = mutableListOf<BackupPreferences>()
        val vm = restoreViewModel(applied)

        vm.restore(applySettings = true)

        assertEquals(BackupEvent.Restored(preferencesApplied = true), vm.nextEvent())
        assertEquals(listOf(backupPreferences), applied)
    }

    @Test
    fun `a restore without the settings leaves them alone`() {
        val applied = mutableListOf<BackupPreferences>()
        val vm = restoreViewModel(applied)

        vm.restore(applySettings = false)

        assertEquals(BackupEvent.Restored(preferencesApplied = false), vm.nextEvent())
        assertTrue(applied.isEmpty())
    }
}
