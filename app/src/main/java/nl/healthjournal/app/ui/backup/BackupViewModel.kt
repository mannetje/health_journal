package nl.healthjournal.app.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.domain.model.backup.BackupDocument
import nl.healthjournal.domain.model.backup.BackupException
import nl.healthjournal.domain.model.backup.BackupPassphrase
import nl.healthjournal.domain.model.backup.BackupPreferences
import nl.healthjournal.domain.usecase.CreateBackupUseCase
import nl.healthjournal.domain.usecase.RestoreBackupUseCase
import java.time.LocalDate

/** Reads and writes the files the user picked. Kept out of the ViewModel so it holds no Android types. */
interface BackupFileIo {
    /** @throws java.io.IOException, or [BackupFileTooLarge] when the file is not plausibly a backup. */
    fun read(uri: Uri): ByteArray
    fun write(uri: Uri, bytes: ByteArray)
}

class BackupFileTooLarge : java.io.IOException()

enum class BackupStep { Idle, CreatePassphrase, RestorePassphrase, RestoreConfirm }

/** What the confirmation dialog shows about a file that has been opened (counts only, no health values). */
data class RestorePreview(
    val profiles: Int,
    val entries: Int,
    val medications: Int,
    val intakes: Int,
    val hasPreferences: Boolean,
    val unreadableItems: Int = 0
)

data class BackupUiState(
    val step: BackupStep = BackupStep.Idle,
    val busy: Boolean = false,
    val passphraseError: UiText? = null,
    val preview: RestorePreview? = null,
    /** Set while an encrypted file is ready and the screen should ask where to save it. */
    val saveSuggestion: String? = null,
    val message: UiText? = null,
    val error: UiText? = null
)

/**
 * Drives creating and restoring a `.hjbackup`. Passphrases arrive as char arrays and are cleared as soon as the
 * key has been derived; nothing here stores them. The key derivation is heavy, so it runs off the main thread.
 */
class BackupViewModel(
    private val create: CreateBackupUseCase?,
    private val restore: RestoreBackupUseCase,
    private val io: BackupFileIo,
    private val preferences: () -> BackupPreferences?,
    private val applyPreferences: (BackupPreferences) -> Unit,
    private val appVersion: String,
    private val onRestored: () -> Unit
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private var fileToSave: ByteArray? = null
    private var fileToOpen: ByteArray? = null
    private var opened: BackupDocument? = null

    // ---- create -------------------------------------------------------------------------------------------

    fun startCreate() {
        _state.value = BackupUiState(step = BackupStep.CreatePassphrase)
    }

    fun submitCreate(passphrase: CharArray, confirmation: CharArray) {
        val useCase = create ?: return
        val problem = when (BackupPassphrase.check(passphrase, confirmation)) {
            BackupPassphrase.Check.OK -> null
            BackupPassphrase.Check.TOO_SHORT -> UiText.Res(R.string.backup_err_too_short, BackupPassphrase.MIN_LENGTH)
            BackupPassphrase.Check.MISMATCH -> UiText.Res(R.string.backup_err_mismatch)
        }
        if (problem != null) {
            _state.value = _state.value.copy(passphraseError = problem)
            return
        }
        _state.value = _state.value.copy(busy = true, passphraseError = null)
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) {
                    useCase(passphrase, confirmation, preferences(), appVersion)
                }
                fileToSave = bytes
                _state.value = BackupUiState(saveSuggestion = "healthjournal-${LocalDate.now()}.hjbackup")
            } catch (e: Exception) {
                _state.value = BackupUiState(error = UiText.Res(R.string.backup_err_create))
            } finally {
                passphrase.fill('\u0000')
                confirmation.fill('\u0000')
            }
        }
    }

    /** The picker returned: write the prepared file to [uri], or drop it when the user cancelled. */
    fun onSaveTarget(uri: Uri?) {
        val bytes = fileToSave
        fileToSave = null
        if (bytes == null) return
        if (uri == null) {
            _state.value = BackupUiState()
            return
        }
        viewModelScope.launch {
            _state.value = BackupUiState(busy = true)
            val verified = withContext(Dispatchers.IO) {
                try {
                    io.write(uri, bytes)
                    // Read it back: a file that was cut short must not be reported as a good backup.
                    io.read(uri).contentEquals(bytes)
                } catch (e: Exception) {
                    false
                }
            }
            _state.value = if (verified) {
                BackupUiState(message = UiText.Res(R.string.backup_msg_saved))
            } else {
                BackupUiState(error = UiText.Res(R.string.backup_err_write))
            }
        }
    }

    // ---- restore ------------------------------------------------------------------------------------------

    fun onRestoreFile(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                try {
                    io.read(uri)
                } catch (e: Exception) {
                    null
                }
            }
            when {
                bytes == null -> _state.value = BackupUiState(error = UiText.Res(R.string.backup_err_read))
                !bytes.startsWith(MAGIC) -> _state.value = BackupUiState(error = UiText.Res(R.string.backup_err_not_backup))
                else -> {
                    fileToOpen = bytes
                    _state.value = BackupUiState(step = BackupStep.RestorePassphrase)
                }
            }
        }
    }

    fun submitRestorePassphrase(passphrase: CharArray) {
        val bytes = fileToOpen ?: return
        _state.value = _state.value.copy(busy = true, passphraseError = null)
        viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.Default) { restore.open(bytes, passphrase) }
                opened = document
                _state.value = BackupUiState(
                    step = BackupStep.RestoreConfirm,
                    preview = RestorePreview(
                        profiles = document.profiles.size,
                        entries = document.profiles.sumOf { it.entryCount },
                        medications = document.profiles.sumOf { it.medications.size },
                        intakes = document.profiles.sumOf { it.intakeCount },
                        hasPreferences = document.preferences != null,
                        unreadableItems = document.unreadableItems
                    )
                )
            } catch (e: BackupException.WrongPassphraseOrCorrupt) {
                // Stay in the dialog so the user can try again.
                _state.value = _state.value.copy(busy = false, passphraseError = UiText.Res(R.string.backup_err_wrong_passphrase))
            } catch (e: BackupException) {
                clearOpened()
                _state.value = BackupUiState(error = e.toUiText())
            } catch (e: Exception) {
                clearOpened()
                _state.value = BackupUiState(error = UiText.Res(R.string.backup_err_read))
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun confirmRestore(applySettings: Boolean) {
        val document = opened ?: return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            try {
                val result = restore.merge(document)
                if (applySettings) document.preferences?.let(applyPreferences)
                clearOpened()
                _state.value = BackupUiState(
                    message = if (result.profilesAdded + result.entriesAdded + result.medicationsAdded + result.intakesAdded == 0) {
                        UiText.Res(R.string.backup_msg_nothing_new)
                    } else {
                        UiText.Res(
                            R.string.backup_msg_restored,
                            result.profilesAdded, result.entriesAdded, result.medicationsAdded, result.intakesAdded,
                            result.alreadyPresent
                        )
                    }
                )
                onRestored()
            } catch (e: Exception) {
                clearOpened()
                _state.value = BackupUiState(error = UiText.Res(R.string.backup_err_restore))
            }
        }
    }

    fun cancel() {
        fileToSave?.fill(0)
        fileToSave = null
        clearOpened()
        _state.value = BackupUiState()
    }

    fun clearMessages() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    override fun onCleared() {
        cancel()
    }

    private fun clearOpened() {
        fileToOpen?.fill(0)
        fileToOpen = null
        opened = null
    }

    private fun BackupException.toUiText(): UiText = when (this) {
        is BackupException.NotABackup -> UiText.Res(R.string.backup_err_not_backup)
        is BackupException.Unsupported -> UiText.Res(R.string.backup_err_unsupported)
        is BackupException.Malformed -> UiText.Res(R.string.backup_err_malformed)
        is BackupException.WrongPassphraseOrCorrupt -> UiText.Res(R.string.backup_err_wrong_passphrase)
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    class Factory(
        private val create: CreateBackupUseCase?,
        private val restore: RestoreBackupUseCase,
        private val io: BackupFileIo,
        private val preferences: () -> BackupPreferences?,
        private val applyPreferences: (BackupPreferences) -> Unit,
        private val appVersion: String,
        private val onRestored: () -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BackupViewModel(create, restore, io, preferences, applyPreferences, appVersion, onRestored) as T
    }

    private companion object {
        val MAGIC = "HJBACKUP".toByteArray(Charsets.US_ASCII)
    }
}
