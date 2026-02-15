package uz.rento.data.repository

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import uz.rento.BuildConfig
import uz.rento.data.local.dao.ChatDao
import uz.rento.data.local.db.RentoDatabase
import uz.rento.data.local.mapper.toDomain
import uz.rento.data.local.mapper.toEntity
import uz.rento.data.local.preferences.UserPreferences
import uz.rento.data.remote.api.ChatApi
import uz.rento.data.remote.dto.ChatListingDto
import uz.rento.data.remote.dto.ChatRoomListItemDto
import uz.rento.data.remote.dto.ChatUserDto
import uz.rento.data.remote.dto.CreateChatRequest
import uz.rento.data.remote.dto.LastMessageDto
import uz.rento.data.remote.dto.MessageDto
import uz.rento.data.remote.dto.SendMessageRequest
import uz.rento.domain.model.ChatListing
import uz.rento.domain.model.ChatRoom
import uz.rento.domain.model.ChatUser
import uz.rento.domain.model.LastMessage
import uz.rento.domain.model.Message
import uz.rento.domain.model.MessageType
import uz.rento.domain.repository.ChatRepository
import uz.rento.domain.repository.ChatsPage
import uz.rento.domain.repository.MessagesPage
import uz.rento.domain.repository.OnlineEvent
import uz.rento.domain.repository.ReadEvent
import uz.rento.domain.repository.TypingEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val userPreferences: UserPreferences,
    private val chatDao: ChatDao
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepo"
    }

    private var socket: Socket? = null

    // ===== Flows =====
    private val _incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    override val incomingMessages: Flow<Message> = _incomingMessages.asSharedFlow()

    private val _typingEvents = MutableSharedFlow<TypingEvent>(extraBufferCapacity = 32)
    override val typingEvents: Flow<TypingEvent> = _typingEvents.asSharedFlow()

    private val _readEvents = MutableSharedFlow<ReadEvent>(extraBufferCapacity = 32)
    override val readEvents: Flow<ReadEvent> = _readEvents.asSharedFlow()

    private val _onlineEvents = MutableSharedFlow<OnlineEvent>(extraBufferCapacity = 32)
    override val onlineEvents: Flow<OnlineEvent> = _onlineEvents.asSharedFlow()

    private val _connectionState = MutableStateFlow(false)
    override val connectionState: Flow<Boolean> = _connectionState.asStateFlow()

    // ===== REST API =====

    override suspend fun createChat(listingId: String, initialMessage: String): Result<String> {
        return try {
            val response = chatApi.createChat(
                CreateChatRequest(listingId = listingId, initialMessage = initialMessage)
            )
            if (response.success && response.data != null) {
                Result.success(response.data.roomId)
            } else {
                Result.failure(Exception(response.error?.message ?: "Chat yaratishda xatolik"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChats(page: Int, perPage: Int): Result<ChatsPage> {
        return try {
            val response = chatApi.getChats(page, perPage)
            if (response.success && response.data != null) {
                val data = response.data
                val domainItems = data.items.map { it.toDomain() }
                // Cache chat rooms
                cacheChatRooms(domainItems)
                Result.success(
                    ChatsPage(
                        items = domainItems,
                        page = data.meta.page,
                        perPage = data.meta.perPage,
                        total = data.meta.total,
                        totalPages = data.meta.totalPages
                    )
                )
            } else {
                Result.failure(Exception(response.error?.message ?: "Chatlarni olishda xatolik"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network xato, keshdan o'qilmoqda (chats)", e)
            loadChatsFromCache(page, perPage)
        }
    }

    override suspend fun getMessages(roomId: String, page: Int, perPage: Int): Result<MessagesPage> {
        return try {
            val response = chatApi.getMessages(roomId, page, perPage)
            if (response.success && response.data != null) {
                val data = response.data
                val domainItems = data.items.map { it.toDomain() }
                // Cache messages
                cacheMessages(domainItems)
                Result.success(
                    MessagesPage(
                        items = domainItems,
                        page = data.meta.page,
                        perPage = data.meta.perPage,
                        total = data.meta.total,
                        totalPages = data.meta.totalPages
                    )
                )
            } else {
                Result.failure(Exception(response.error?.message ?: "Xabarlarni olishda xatolik"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network xato, keshdan o'qilmoqda (messages)", e)
            loadMessagesFromCache(roomId, page, perPage)
        }
    }

    override suspend fun sendMessage(
        roomId: String,
        content: String?,
        messageType: String,
        mediaUrl: String?,
        metadata: Map<String, Any>?
    ): Result<Message> {
        return try {
            val response = chatApi.sendMessage(
                roomId,
                SendMessageRequest(
                    content = content,
                    messageType = messageType,
                    mediaUrl = mediaUrl,
                    metadata = metadata
                )
            )
            if (response.success && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(Exception(response.error?.message ?: "Xabar yuborishda xatolik"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(roomId: String): Result<Unit> {
        return try {
            val response = chatApi.markAsRead(roomId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("O'qildi belgilashda xatolik"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== WebSocket =====

    override fun connectWebSocket() {
        if (socket?.connected() == true) return

        try {
            val token = kotlinx.coroutines.runBlocking {
                userPreferences.accessToken.first()
            }
            if (token.isNullOrBlank()) {
                Log.w(TAG, "Token yo'q — WebSocket ulanmaydi")
                return
            }

            val options = IO.Options().apply {
                path = "/ws/chat"
                query = "token=$token"
                forceNew = true
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 2000
                timeout = 10000
            }

            socket = IO.socket(BuildConfig.CHAT_WS_URL, options).apply {
                on(Socket.EVENT_CONNECT) {
                    Log.i(TAG, "WebSocket ulandi")
                    _connectionState.tryEmit(true)
                }

                on(Socket.EVENT_DISCONNECT) { args ->
                    val reason = args.firstOrNull()?.toString() ?: "unknown"
                    Log.i(TAG, "WebSocket uzildi: $reason")
                    _connectionState.tryEmit(false)
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val error = args.firstOrNull()?.toString() ?: "unknown"
                    Log.e(TAG, "WebSocket ulanish xatosi: $error")
                    _connectionState.tryEmit(false)
                }

                // ——— Server → Client eventlar ———

                on("new_message") { args ->
                    parseMessage(args)?.let { msg ->
                        _incomingMessages.tryEmit(msg)
                        // Cache incoming message
                        kotlinx.coroutines.runBlocking {
                            try { cacheMessages(listOf(msg)) } catch (_: Exception) {}
                        }
                    }
                }

                on("user_typing") { args ->
                    parseJson(args)?.let { json ->
                        _typingEvents.tryEmit(
                            TypingEvent(
                                roomId = json.optString("room_id"),
                                userId = json.optString("user_id"),
                                isTyping = true
                            )
                        )
                    }
                }

                on("user_stop_typing") { args ->
                    parseJson(args)?.let { json ->
                        _typingEvents.tryEmit(
                            TypingEvent(
                                roomId = json.optString("room_id"),
                                userId = json.optString("user_id"),
                                isTyping = false
                            )
                        )
                    }
                }

                on("message_read") { args ->
                    parseJson(args)?.let { json ->
                        _readEvents.tryEmit(
                            ReadEvent(
                                roomId = json.optString("room_id"),
                                readerId = json.optString("reader_id"),
                                lastReadId = json.optString("last_read_id")
                            )
                        )
                    }
                }

                on("user_online") { args ->
                    parseJson(args)?.let { json ->
                        _onlineEvents.tryEmit(
                            OnlineEvent(userId = json.optString("user_id"), isOnline = true)
                        )
                    }
                }

                on("user_offline") { args ->
                    parseJson(args)?.let { json ->
                        _onlineEvents.tryEmit(
                            OnlineEvent(userId = json.optString("user_id"), isOnline = false)
                        )
                    }
                }

                on("error") { args ->
                    val error = args.firstOrNull()?.toString() ?: "unknown"
                    Log.e(TAG, "WebSocket server xatosi: $error")
                }

                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket ulanishda xato", e)
        }
    }

    override fun disconnectWebSocket() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.tryEmit(false)
    }

    override fun joinRoom(roomId: String) {
        socket?.emit("join_room", JSONObject().put("room_id", roomId))
    }

    override fun leaveRoom(roomId: String) {
        socket?.emit("leave_room", JSONObject().put("room_id", roomId))
    }

    override fun sendMessageViaWs(
        roomId: String,
        content: String?,
        type: String,
        mediaUrl: String?,
        metadata: Map<String, Any>?
    ) {
        val data = JSONObject().apply {
            put("room_id", roomId)
            content?.let { put("content", it) }
            put("type", type)
            mediaUrl?.let { put("media_url", it) }
            metadata?.let { put("metadata", JSONObject(it)) }
        }
        socket?.emit("send_message", data)
    }

    override fun sendTypingStart(roomId: String) {
        socket?.emit("typing_start", JSONObject().put("room_id", roomId))
    }

    override fun sendTypingStop(roomId: String) {
        socket?.emit("typing_stop", JSONObject().put("room_id", roomId))
    }

    override fun sendMarkRead(roomId: String, messageId: String) {
        socket?.emit("mark_read", JSONObject().apply {
            put("room_id", roomId)
            put("message_id", messageId)
        })
    }

    // ===== Offline Cache Helpers =====

    private suspend fun cacheChatRooms(rooms: List<ChatRoom>) {
        try {
            chatDao.insertChatRooms(rooms.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e(TAG, "Chatlarni keshga saqlashda xato", e)
        }
    }

    private suspend fun cacheMessages(messages: List<Message>) {
        try {
            chatDao.deleteOldMessages(System.currentTimeMillis() - RentoDatabase.MESSAGE_CACHE_TTL_MS)
            chatDao.insertMessages(messages.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e(TAG, "Xabarlarni keshga saqlashda xato", e)
        }
    }

    private suspend fun loadChatsFromCache(page: Int, perPage: Int): Result<ChatsPage> {
        return try {
            val offset = (page - 1) * perPage
            val entities = chatDao.getChatRooms(perPage, offset)
            if (entities.isEmpty()) {
                return Result.failure(Exception("Keshda chatlar yo'q va internet mavjud emas"))
            }
            Result.success(
                ChatsPage(
                    items = entities.map { it.toDomain() },
                    page = page,
                    perPage = perPage,
                    total = entities.size,
                    totalPages = 1
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Keshdan chatlarni o'qishda xato", e)
            Result.failure(Exception("Offline chatlarni olishda xatolik"))
        }
    }

    private suspend fun loadMessagesFromCache(roomId: String, page: Int, perPage: Int): Result<MessagesPage> {
        return try {
            val offset = (page - 1) * perPage
            val entities = chatDao.getMessages(roomId, perPage, offset)
            if (entities.isEmpty()) {
                return Result.failure(Exception("Keshda xabarlar yo'q va internet mavjud emas"))
            }
            Result.success(
                MessagesPage(
                    items = entities.map { it.toDomain() },
                    page = page,
                    perPage = perPage,
                    total = entities.size,
                    totalPages = 1
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Keshdan xabarlarni o'qishda xato", e)
            Result.failure(Exception("Offline xabarlarni olishda xatolik"))
        }
    }

    // ===== Helpers =====

    private fun parseJson(args: Array<Any>): JSONObject? {
        return try {
            when (val arg = args.firstOrNull()) {
                is JSONObject -> arg
                is String -> JSONObject(arg)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse xatosi", e)
            null
        }
    }

    private fun parseMessage(args: Array<Any>): Message? {
        return try {
            val json = parseJson(args) ?: return null
            Message(
                id = json.optString("id"),
                roomId = json.optString("room_id"),
                senderId = json.optString("sender_id"),
                content = json.optString("content", null),
                messageType = MessageType.fromValue(json.optString("type", "text")),
                mediaUrl = json.optString("media_url", null),
                metadata = null,
                isRead = false,
                readAt = null,
                createdAt = json.optString("created_at")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Message parse xatosi", e)
            null
        }
    }
}

// ===== DTO → Domain mapperlar =====

private fun ChatRoomListItemDto.toDomain(): ChatRoom = ChatRoom(
    roomId = roomId,
    listing = listing.toDomain(),
    otherUser = otherUser.toDomain(),
    lastMessage = lastMessage?.toDomain(),
    unreadCount = unreadCount,
    createdAt = createdAt
)

private fun ChatListingDto.toDomain(): ChatListing = ChatListing(
    id = id,
    title = title,
    imageUrl = imageUrl
)

private fun ChatUserDto.toDomain(): ChatUser = ChatUser(
    id = id,
    fullName = fullName,
    avatarUrl = avatarUrl,
    isOnline = isOnline,
    lastSeenAt = lastSeenAt
)

private fun LastMessageDto.toDomain(): LastMessage = LastMessage(
    content = content,
    senderId = senderId,
    createdAt = createdAt
)

private fun MessageDto.toDomain(): Message = Message(
    id = id,
    roomId = roomId,
    senderId = senderId,
    content = content,
    messageType = MessageType.fromValue(messageType),
    mediaUrl = mediaUrl,
    metadata = metadata,
    isRead = isRead,
    readAt = readAt,
    createdAt = createdAt
)
