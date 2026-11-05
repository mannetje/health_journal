package nl.healthjournal.app.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.healthjournal.app.BuildConfig
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.BackupPreferenceBridge
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.domain.backup.command.CreateBackupCommand
import nl.healthjournal.domain.backup.command.OpenBackupQuery
import nl.healthjournal.domain.backup.command.RestoreBackupCommand
import nl.healthjournal.domain.shared.command.BackupProblem
import nl.healthjournal.domain.shared.command.Dispatcher
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.backup.model.BackupDocument
import nl.healthjournal.domain.backup.model.BackupPreferences
import java.time.LocalDate
import javax.inject.Inject

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

/** One-shot results the screen must act on, which the ViewModel cannot do itself without holding the Activity. */
sealed interface BackupEvent {
    /** A restore finished: the data on screen is stale, and [preferencesApplied] means the display settings changed. */
    data class Restored(val preferencesApplied: Boolean) : BackupEvent
}

/**
 * Drives creating and restoring a `.hjbackup`. Passphrases arrive as char arrays and are cleared as soon as the
 * key has been derived; nothing here stores them. The key derivation is heavy, so it runs off the main thread.
 */
@HiltViewModel
open class BackupViewModel(
    private val dispatcher: Dispatcher,
    /** False on the lost-key screen, where there is no data to back up. */
    private val canCreate: Boolean,
    private val io: BackupFileIo,
    private val preferences: () -> BackupPreferences?,
    private val applyPreferences: (BackupPreferences) -> Unit,
    private val appVersion: String
) : ViewModel() {

    /** The constructor Hilt uses: the application's dispatcher, and file access through its content resolver. */
    @Inject
    constructor(
        dispatcher: Dispatcher,
        @ApplicationContext context: Context,
        backupPreferences: BackupPreferenceBridge
    ) : this(
        dispatcher, true, ContentResolverBackupFileIo(context.contentResolver),
        { backupPreferences.collect() }, { backupPreferences.apply(it) }, BuildConfig.VERSION_NAME
    )

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    // Buffered: an event raised while the app is in the background is delivered when it is visible again.
    private val _events = Channel<BackupEvent>(Channel.BUFFERED)
    val events: Flow<BackupEvent> = _events.receiveAsFlow()

    private var fileToSave: ByteArray? = null
    private var fileToOpen: ByteArray? = null
    private var opened: BackupDocument? = null

    // ---- create -------------------------------------------------------------------------------------------

    fun startCreate() {
        _state.value = BackupUiState(step = BackupStep.CreatePassphrase)
    }

    fun submitCreate(passphrase: CharArray, confirmation: CharArray) {
        if (!canCreate) return
        _state.value = _state.value.copy(busy = true, passphraseError = null)
        viewModelScope.launch {
            try {
                // The key derivation is heavy, so it runs off the main thread. The command checks the passphrase.
                val result = withContext(Dispatchers.Default) {
                    dispatcher.dispatch(CreateBackupCommand(passphrase, confirmation, preferences(), appVersion))
                }
                when (result) {
                    is DomainResult.Success -> {
                        fileToSave = result.value
                        _state.value = BackupUiState(saveSuggestion = "healthjournal-${LocalDate.now()}.hjbackup")
                    }
                    is DomainResult.Failure -> _state.value = when (result.error) {
                        is DomainError.Invalid -> _state.value.copy(busy = false, passphraseError = result.error.toUiText())
                        else -> BackupUiState(error = UiText.Res(R.string.backup_err_create))
                    }
                }
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: RuntimeException) {
                    false // the picked document can fail in many ways (security, provider, storage)
                } catch (e: java.io.IOException) {
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: RuntimeException) {
                    null
                } catch (e: java.io.IOException) {
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
                val result = withContext(Dispatchers.Default) { dispatcher.query(OpenBackupQuery(bytes, passphrase)) }
                when (result) {
                    is DomainResult.Success -> {
                        val document = result.value
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
                    }
                    is DomainResult.Failure -> openFailed(result.error)
                }
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    private fun openFailed(error: DomainError) {
        if (error is DomainError.Backup && error.problem == BackupProblem.WRONG_PASSPHRASE_OR_CORRUPT) {
            // Stay in the dialog so the user can try again.
            _state.value = _state.value.copy(busy = false, passphraseError = error.toUiText())
            return
        }
        clearOpened()
        _state.value = BackupUiState(
            error = if (error is DomainError.Backup) error.toUiText() else UiText.Res(R.string.backup_err_read)
        )
    }

    fun confirmRestore(applySettings: Boolean) {
        val document = opened ?: return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            when (val result = dispatcher.dispatch(RestoreBackupCommand(document))) {
                is DomainResult.Success -> {
                    val merged = result.value
                    val preferences = document.preferences.takeIf { applySettings }
                    preferences?.let(applyPreferences)
                    clearOpened()
                    _state.value = BackupUiState(
                        message = if (merged.profilesAdded + merged.entriesAdded + merged.medicationsAdded + merged.intakesAdded == 0) {
                            UiText.Res(R.string.backup_msg_nothing_new)
                        } else {
                            UiText.Res(
                                R.string.backup_msg_restored,
                                merged.profilesAdded, merged.entriesAdded, merged.medicationsAdded, merged.intakesAdded,
                                merged.alreadyPresent
                            )
                        }
                    )
                    _events.trySend(BackupEvent.Restored(preferencesApplied = preferences != null))
                }
                is DomainResult.Failure -> {
                    clearOpened()
                    _state.value = BackupUiState(error = UiText.Res(R.string.backup_err_restore))
                }
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

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private companion object {
        val MAGIC = "HJBACKUP".toByteArray(Charsets.US_ASCII)
    }
}
