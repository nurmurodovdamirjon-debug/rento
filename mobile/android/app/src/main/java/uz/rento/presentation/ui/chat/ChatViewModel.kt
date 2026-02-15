package uz.rento.presentation.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.data.local.preferences.UserPreferences
import uz.rento.domain.model.Message
import uz.rento.domain.repository.ChatRepository
import uz.rento.domain.usecase.chat.GetMessagesUseCase
import uz.rento.domain.usecase.chat.SendMessageUseCase
import javax.inject.Inject

/**
 * ChatUiState — chat suhbat ekrani holati
 */
data class ChatUiState(
    val roomId: String = "",
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val messageInput: String = "",
    val isSending: Boolean = false,
    val currentUserId: String = "",
    val otherUserTyping: Boolean = false,
    val isConnected: Boolean = false
)

/**
 * ChatViewModel — chat suhbat ekrani ViewModel
 *
 * Xabarlar tarixi, WebSocket orqali real-time xabar almashish,
 * typing indikator, o'qildi belgilash.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatRepository: ChatRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val roomId: String = savedStateHandle["roomId"] ?: ""

    private val _uiState = MutableStateFlow(ChatUiState(roomId = roomId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        loadMessages()
        joinRoom()
        observeWebSocketEvents()
    }

    /**
     * Joriy foydalanuvchi ID ni olish
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = userPreferences.userId.first() ?: ""
            _uiState.update { it.copy(currentUserId = userId) }
        }
    }

    /**
     * Xabarlar tarixini yuklash
     */
    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getMessagesUseCase(roomId, page = 1).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            messages = result.items,
                            isLoading = false,
                            currentPage = result.page,
                            hasMore = result.page < result.totalPages
                        )
                    }
                    // O'qildi belgilash
                    chatRepository.markAsRead(roomId)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Xabarlarni yuklashda xatolik"
                        )
                    }
                }
            )
        }
    }

    /**
     * Eski xabarlarni yuklash (scroll up)
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            getMessagesUseCase(roomId, page = state.currentPage + 1).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + result.items,
                            isLoadingMore = false,
                            currentPage = result.page,
                            hasMore = result.page < result.totalPages
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    /**
     * Xabar inputini yangilash
     */
    fun onMessageInputChange(text: String) {
        _uiState.update { it.copy(messageInput = text) }

        // Typing indikator
        if (text.isNotBlank()) {
            chatRepository.sendTypingStart(roomId)
        } else {
            chatRepository.sendTypingStop(roomId)
        }
    }

    /**
     * Xabar yuborish
     */
    fun sendMessage() {
        val content = _uiState.value.messageInput.trim()
        if (content.isBlank()) return

        _uiState.update { it.copy(messageInput = "", isSending = true) }
        chatRepository.sendTypingStop(roomId)

        viewModelScope.launch {
            sendMessageUseCase(
                roomId = roomId,
                content = content,
                messageType = "text",
                useWebSocket = true
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSending = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    /**
     * Xatolikni tozalash
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Xonaga qo'shilish
     */
    private fun joinRoom() {
        chatRepository.connectWebSocket()
        chatRepository.joinRoom(roomId)
    }

    /**
     * WebSocket eventlarni kuzatish
     */
    private fun observeWebSocketEvents() {
        // Yangi xabarlar
        viewModelScope.launch {
            chatRepository.incomingMessages.collect { message ->
                if (message.roomId == roomId) {
                    _uiState.update { state ->
                        // Dublikatni tekshirish
                        if (state.messages.any { it.id == message.id }) {
                            state
                        } else {
                            state.copy(
                                messages = listOf(message) + state.messages
                            )
                        }
                    }
                    // Avtomatik o'qildi belgilash
                    chatRepository.sendMarkRead(roomId, message.id)
                }
            }
        }

        // Typing indicator
        viewModelScope.launch {
            chatRepository.typingEvents.collect { event ->
                if (event.roomId == roomId && event.userId != _uiState.value.currentUserId) {
                    _uiState.update { it.copy(otherUserTyping = event.isTyping) }
                }
            }
        }

        // Read events
        viewModelScope.launch {
            chatRepository.readEvents.collect { event ->
                if (event.roomId == roomId) {
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { msg ->
                            if (msg.senderId == state.currentUserId && !msg.isRead) {
                                msg.copy(isRead = true)
                            } else msg
                        }
                        state.copy(messages = updatedMessages)
                    }
                }
            }
        }

        // Connection state
        viewModelScope.launch {
            chatRepository.connectionState.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.leaveRoom(roomId)
    }
}
