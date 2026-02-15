package uz.rento.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.rento.domain.model.ChatRoom
import uz.rento.domain.model.Message

/**
 * ChatRepository — chat operatsiyalari interfeysi
 */
interface ChatRepository {

    /** Yangi chat boshlash yoki mavjud chatga o'tish */
    suspend fun createChat(listingId: String, initialMessage: String): Result<String>

    /** Chat xonalari ro'yxati */
    suspend fun getChats(page: Int = 1, perPage: Int = 20): Result<ChatsPage>

    /** Xabarlar tarixi */
    suspend fun getMessages(roomId: String, page: Int = 1, perPage: Int = 50): Result<MessagesPage>

    /** Xabar yuborish (REST fallback) */
    suspend fun sendMessage(
        roomId: String,
        content: String?,
        messageType: String = "text",
        mediaUrl: String? = null,
        metadata: Map<String, Any>? = null
    ): Result<Message>

    /** O'qildi belgilash */
    suspend fun markAsRead(roomId: String): Result<Unit>

    // ===== WebSocket =====

    /** WebSocket ulanish */
    fun connectWebSocket()

    /** WebSocket uzish */
    fun disconnectWebSocket()

    /** Xonaga qo'shilish */
    fun joinRoom(roomId: String)

    /** Xonadan chiqish */
    fun leaveRoom(roomId: String)

    /** WS orqali xabar yuborish */
    fun sendMessageViaWs(
        roomId: String,
        content: String?,
        type: String = "text",
        mediaUrl: String? = null,
        metadata: Map<String, Any>? = null
    )

    /** Yozmoqda boshlash */
    fun sendTypingStart(roomId: String)

    /** Yozmoqda to'xtatish */
    fun sendTypingStop(roomId: String)

    /** WS orqali o'qildi belgilash */
    fun sendMarkRead(roomId: String, messageId: String)

    // ===== Flows — real-time eventlar =====

    /** Yangi xabar oqimi */
    val incomingMessages: Flow<Message>

    /** Typing holati oqimi */
    val typingEvents: Flow<TypingEvent>

    /** O'qildi eventlari */
    val readEvents: Flow<ReadEvent>

    /** Online/offline eventlari */
    val onlineEvents: Flow<OnlineEvent>

    /** WebSocket ulanish holati */
    val connectionState: Flow<Boolean>
}

/**
 * ChatsPage — sahifalangan chat xonalari
 */
data class ChatsPage(
    val items: List<ChatRoom>,
    val page: Int,
    val perPage: Int,
    val total: Int,
    val totalPages: Int
)

/**
 * MessagesPage — sahifalangan xabarlar
 */
data class MessagesPage(
    val items: List<Message>,
    val page: Int,
    val perPage: Int,
    val total: Int,
    val totalPages: Int
)

/**
 * TypingEvent — foydalanuvchi yozmoqda/to'xtadi
 */
data class TypingEvent(
    val roomId: String,
    val userId: String,
    val isTyping: Boolean
)

/**
 * ReadEvent — xabar o'qildi
 */
data class ReadEvent(
    val roomId: String,
    val readerId: String,
    val lastReadId: String
)

/**
 * OnlineEvent — foydalanuvchi online/offline
 */
data class OnlineEvent(
    val userId: String,
    val isOnline: Boolean
)
