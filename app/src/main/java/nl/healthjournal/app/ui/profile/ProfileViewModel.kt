package nl.healthjournal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.MetricDisplayOrderPreference
import nl.healthjournal.app.settings.MetricSlot
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.Dispatcher
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.model.Sex
import nl.healthjournal.domain.profile.command.GetActiveProfileQuery
import nl.healthjournal.domain.profile.command.SaveProfileCommand
import java.time.LocalDate
import javax.inject.Inject

data class ProfileUiState(
    val activeProfile: Profile? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val successMessage: UiText? = null
)

@HiltViewModel
class ProfileViewModel(
    private val dispatcher: Dispatcher,
    /** The order of the metrics on the Log tabs and History chips. */
    val metricOrder: StateFlow<List<MetricSlot>> = MutableStateFlow(MetricSlot.DEFAULT_ORDER),
    private val saveMetricOrder: (List<MetricSlot>) -> Unit = {}
) : ViewModel() {

    /** The constructor Hilt uses; tests pass the order and the save function directly. */
    @Inject
    constructor(
        dispatcher: Dispatcher,
        metricDisplayOrder: MetricDisplayOrderPreference
    ) : this(dispatcher, metricDisplayOrder.order, metricDisplayOrder::save)

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /** Moves the metric at [from] to [to] and saves the new order at once. */
    fun moveMetric(from: Int, to: Int) {
        val order = metricOrder.value.toMutableList()
        if (from !in order.indices || to !in order.indices || from == to) return
        order.add(to, order.removeAt(from))
        saveMetricOrder(order)
    }

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            _uiState.value = when (val result = dispatcher.query(GetActiveProfileQuery)) {
                is DomainResult.Success -> _uiState.value.copy(activeProfile = result.value, isLoading = false)
                is DomainResult.Failure -> _uiState.value.copy(
                    isLoading = false, errorMessage = UiText.Res(R.string.profile_err_load_failed)
                )
            }
        }
    }

    fun saveProfile(name: String, birthDate: LocalDate, heightCm: Int?, sex: Sex? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            _uiState.value = when (val result = dispatcher.dispatch(SaveProfileCommand(name, birthDate, heightCm, sex))) {
                is DomainResult.Success -> _uiState.value.copy(
                    activeProfile = result.value,
                    isLoading = false,
                    successMessage = UiText.Res(R.string.profile_msg_saved)
                )
                is DomainResult.Failure -> _uiState.value.copy(
                    isLoading = false,
                    errorMessage = when (result.error) {
                        is DomainError.Storage -> UiText.Res(R.string.profile_err_save_failed)
                        else -> result.error.toUiText()
                    }
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
