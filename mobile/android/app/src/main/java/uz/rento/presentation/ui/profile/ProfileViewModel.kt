package uz.rento.presentation.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.User
import uz.rento.domain.repository.AuthRepository
import uz.rento.domain.repository.UserRepository
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false,
    val editFullName: String = "",
    val editEmail: String = "",
    val editLanguage: String = "uz",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            userRepository.getMyProfile()
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            editFullName = user.fullName ?: "",
                            editEmail = user.email ?: "",
                            editLanguage = user.language
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Profil olishda xatolik"
                        )
                    }
                }
        }
    }

    fun startEditing() {
        _uiState.update { state ->
            state.copy(
                isEditing = true,
                editFullName = state.user?.fullName ?: "",
                editEmail = state.user?.email ?: "",
                editLanguage = state.user?.language ?: "uz"
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun updateFullName(name: String) {
        _uiState.update { it.copy(editFullName = name) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(editEmail = email) }
    }

    fun updateLanguage(language: String) {
        _uiState.update { it.copy(editLanguage = language) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val state = _uiState.value
            userRepository.updateProfile(
                fullName = state.editFullName.ifBlank { null },
                email = state.editEmail.ifBlank { null },
                language = state.editLanguage
            )
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            user = user,
                            saveSuccess = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Saqlashda xatolik"
                        )
                    }
                }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
