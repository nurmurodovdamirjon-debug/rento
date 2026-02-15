package uz.rento.presentation.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.NotificationItem
import uz.rento.domain.usecase.notification.GetNotificationsUseCase
import uz.rento.domain.usecase.notification.GetUnreadCountUseCase
import uz.rento.domain.usecase.notification.MarkNotificationReadUseCase
import javax.inject.Inject

/**
 * NotificationsViewModel — bildirishnomalar ekrani holati
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getNotificationsUseCase(page = 1, perPage = 50)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifications = page.items,
                            total = page.total,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Xatolik yuz berdi"
                        )
                    }
                }

            // O'qilmagan soni
            getUnreadCountUseCase()
                .onSuccess { count ->
                    _uiState.update { it.copy(unreadCount = count) }
                }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            markNotificationReadUseCase.markOne(notificationId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { notification ->
                                if (notification.id == notificationId) {
                                    notification.copy(isRead = true)
                                } else notification
                            },
                            unreadCount = (state.unreadCount - 1).coerceAtLeast(0)
                        )
                    }
                }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            markNotificationReadUseCase.markAll()
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { it.copy(isRead = true) },
                            unreadCount = 0
                        )
                    }
                }
        }
    }

    fun refresh() {
        loadNotifications()
    }
}

/**
 * NotificationsUiState — bildirishnomalar UI holati
 */
data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val total: Int = 0,
    val unreadCount: Int = 0,
    val error: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && notifications.isEmpty() && error == null
}
