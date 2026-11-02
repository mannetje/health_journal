package nl.healthjournal.app.ui.backup

import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.domain.model.backup.BackupDocument
import nl.healthjournal.domain.model.backup.BackupProfile
import nl.healthjournal.domain.model.backup.MergeResult
import nl.healthjournal.domain.port.secondary.BackupCodecPort
import nl.healthjournal.domain.port.secondary.BackupSnapshotPort
import nl.healthjournal.domain.usecase.CreateBackupUseCase
import nl.healthjournal.domain.usecase.RestoreBackupUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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

    private fun viewModel(snapshotFailure: Throwable): BackupViewModel {
        val snapshot = FailingSnapshot(snapshotFailure)
        return BackupViewModel(
            create = CreateBackupUseCase(snapshot, UnusedCodec),
            restore = RestoreBackupUseCase(snapshot, UnusedCodec),
            io = noIo,
            preferences = { null },
            applyPreferences = {},
            appVersion = "test",
            onRestored = {}
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
}
