package uz.rento.presentation.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.ChatRoom
import uz.rento.domain.repository.ChatRepository
import uz.rento.domain.repository.OnlineEvent
import uz.rento.domain.usecase.chat.GetChatsUseCase
import javax.inject.Inject

/**
 * ChatListUiState — chat ro'yxati ekrani holati
 */
data class ChatListUiState(
    val chats: List<ChatRoom> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val totalUnread: Int = 0
)

/**
 * ChatListViewModel — xabarlar ro'yxati ViewModel
 *
 * Chat xonalarini yuklash, WebSocket eventlarni tinglash,
 * online statusni kuzatish.
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadChats()
        observeWebSocketEvents()
    }

    /**
     * Chat ro'yxatini yuklash
     */
    fun loadChats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getChatsUseCase(page = 1).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            chats = result.items,
                            isLoading = false,
                            currentPage = result.page,
                            totalPages = result.totalPages,
                            hasMore = result.page < result.totalPages,
                            totalUnread = result.items.sumOf { chat -> chat.unreadCount }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Chatlarni yuklashda xatolik"
                        )
                    }
                }
            )
        }
    }

    /**
     * Yangilash (Pull-to-refresh)
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            getChatsUseCase(page = 1).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            chats = result.items,
                            isRefreshing = false,
                            currentPage = result.page,
                            totalPages = result.totalPages,
                            hasMore = result.page < result.totalPages,
                            totalUnread = result.items.sumOf { chat -> chat.unreadCount }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    /**
     * Keyingi sahifani yuklash
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            getChatsUseCase(page = state.currentPage + 1).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            chats = it.chats + result.items,
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
     * Xatolikni tozalash
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * WebSocket eventlarni kuzatish — yangi xabar, online/offline
     */
    private fun observeWebSocketEvents() {
        // WebSocket ulanish
        chatRepository.connectWebSocket()

        // Yangi xabar kelganda ro'yxatni yangilash
        viewModelScope.launch {
            chatRepository.incomingMessages.collect { message ->
                // Ro'yxatda tegishli chatni yangilash
                _uiState.update { state ->
                    val updatedChats = state.chats.map { chat ->
                        if (chat.roomId == message.roomId) {
                            chat.copy(
                                lastMessage = uz.rento.domain.model.LastMessage(
                                    content = message.content,
                                    senderId = message.senderId,
                                    createdAt = message.createdAt
                                ),
                                unreadCount = chat.unreadCount + 1
                            )
                        } else chat
                    }.sortedByDescending { it.lastMessage?.createdAt ?: it.createdAt }

                    state.copy(
                        chats = updatedChats,
                        totalUnread = updatedChats.sumOf { it.unreadCount }
                    )
                }
            }
        }

        // Online/offline status yangilash
        viewModelScope.launch {
            chatRepository.onlineEvents.collect { event ->
                _uiState.update { state ->
                    val updatedChats = state.chats.map { chat ->
                        if (chat.otherUser.id == event.userId) {
                            chat.copy(
                                otherUser = chat.otherUser.copy(isOnline = event.isOnline)
                            )
                        } else chat
                    }
                    state.copy(chats = updatedChats)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnectWebSocket()
    }
}
