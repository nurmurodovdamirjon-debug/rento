package uz.rento.presentation.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.rento.presentation.components.RentoButton
import uz.rento.presentation.components.RentoTextField

/**
 * LoginScreen — telefon raqam kiritish sahifasi
 * +998 XX XXX XX XX formatida telefon kiritiladi
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToOtp: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Xato bo'lsa snackbar ko'rsatish
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // OTP yuborilganda nav
    LaunchedEffect(uiState.otpSent) {
        if (uiState.otpSent) {
            onNavigateToOtp(uiState.phone)
            viewModel.resetOtpSent()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Text(
                text = "Telefon raqamingiz",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SMS orqali tasdiqlash kodini yuboramiz",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            RentoTextField(
                value = uiState.phone,
                onValueChange = viewModel::updatePhone,
                label = "Telefon raqam",
                placeholder = "+998 XX XXX XX XX",
                keyboardType = KeyboardType.Phone,
                maxLength = 13,
                isError = uiState.phoneError != null,
                errorText = uiState.phoneError,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            RentoButton(
                text = "Kod yuborish",
                onClick = { viewModel.sendOtp() },
                isLoading = uiState.isLoading,
                enabled = uiState.phone.length >= 12
            )
        }
    }
}
