package uz.rento.presentation.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.User
import uz.rento.domain.usecase.auth.SendOtpUseCase
import uz.rento.domain.usecase.auth.VerifyOtpUseCase
import javax.inject.Inject

data class AuthUiState(
    val phone: String = "+998",
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val phoneError: String? = null,
    val otpSent: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: User? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updatePhone(phone: String) {
        _uiState.update {
            it.copy(
                phone = phone,
                phoneError = null,
                error = null
            )
        }
    }

    fun updateOtp(otp: String) {
        _uiState.update {
            it.copy(otp = otp, error = null)
        }
    }

    fun sendOtp() {
        val phone = _uiState.value.phone
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, phoneError = null) }

            sendOtpUseCase(phone)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, otpSent = true) }
                }
                .onFailure { error ->
                    val message = error.message ?: "SMS yuborishda xatolik"
                    if (message.contains("format", ignoreCase = true)) {
                        _uiState.update {
                            it.copy(isLoading = false, phoneError = message)
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, error = message)
                        }
                    }
                }
        }
    }

    fun verifyOtp() {
        val phone = _uiState.value.phone
        val otp = _uiState.value.otp

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            verifyOtpUseCase(phone, otp)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = user
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "OTP tasdiqlashda xatolik",
                            otp = "" // OTP ni tozalash
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetOtpSent() {
        _uiState.update { it.copy(otpSent = false) }
    }
}
