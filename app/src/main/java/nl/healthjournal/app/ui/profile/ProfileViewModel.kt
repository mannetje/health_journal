package nl.healthjournal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.CreateProfileUseCase
import java.time.LocalDate

data class ProfileUiState(
    val activeProfile: Profile? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val profileRepository: ProfileRepositoryPort,
    private val createProfileUseCase: CreateProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val profile = profileRepository.getActiveProfile()
                _uiState.value = _uiState.value.copy(
                    activeProfile = profile,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    fun saveProfile(name: String, birthDate: LocalDate, heightCm: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            try {
                createProfileUseCase(name = name, dateOfBirth = birthDate, heightCm = heightCm)
                val updatedProfile = profileRepository.getActiveProfile()
                _uiState.value = _uiState.value.copy(
                    activeProfile = updatedProfile,
                    isLoading = false,
                    successMessage = "Profile saved successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error saving profile"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    class Factory(
        private val profileRepository: ProfileRepositoryPort,
        private val createProfileUseCase: CreateProfileUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(profileRepository, createProfileUseCase) as T
        }
    }
}
