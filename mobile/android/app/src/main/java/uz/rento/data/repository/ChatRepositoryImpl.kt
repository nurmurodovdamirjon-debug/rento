package uz.rento.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    private val chatDao: ChatDao,
    private val gson: Gson
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepo"
    }

    private var hubConnection: com.microsoft.signalr.HubConnection? = null
    private var userRequestedDisconnect = false

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

    // ===== WebSocket (SignalR) =====

    override fun connectWebSocket() {
        userRequestedDisconnect = false
        if (hubConnection?.connectionState == com.microsoft.signalr.HubConnectionState.CONNECTED) return

        val token = runBlocking { userPreferences.accessToken.first() }
        if (token.isNullOrBlank()) {
            Log.w(TAG, "Token yo'q — SignalR ulanmaydi")
            return
        }

        val url = "${BuildConfig.CHAT_WS_URL}/hubs/chat?access_token=$token"
        try {
            hubConnection = com.microsoft.signalr.HubConnectionBuilder.create(url)
                .shouldSkipNegotiate(false)
                .build()

            hubConnection!!.on("NewMessage", { message: Any? ->
                parseNewMessage(message)?.let { msg ->
                    _incomingMessages.tryEmit(msg)
                    runBlocking { try { cacheMessages(listOf(msg)) } catch (_: Exception) {} }
                }
            }, Any::class.java)

            hubConnection!!.on("UserTyping", { payload: Any? ->
                parseJsonPayload(payload)?.let { json ->
                    _typingEvents.tryEmit(
                        TypingEvent(
                            roomId = json.get("room_id")?.asString ?: "",
                            userId = json.get("user_id")?.asString ?: "",
                            isTyping = true
                        )
                    )
                }
            }, Any::class.java)

            hubConnection!!.on("UserStopTyping", { payload: Any? ->
                parseJsonPayload(payload)?.let { json ->
                    _typingEvents.tryEmit(
                        TypingEvent(
                            roomId = json.get("room_id")?.asString ?: "",
                            userId = json.get("user_id")?.asString ?: "",
                            isTyping = false
                        )
                    )
                }
            }, Any::class.java)

            hubConnection!!.on("MessageRead", { payload: Any? ->
                parseJsonPayload(payload)?.let { json ->
                    _readEvents.tryEmit(
                        ReadEvent(
                            roomId = json.get("room_id")?.asString ?: "",
                            readerId = json.get("reader_id")?.asString ?: "",
                            lastReadId = json.get("last_read_id")?.asString ?: ""
                        )
                    )
                }
            }, Any::class.java)

            hubConnection!!.on("UserOnline", { payload: Any? ->
                parseJsonPayload(payload)?.let { json ->
                    _onlineEvents.tryEmit(
                        OnlineEvent(userId = json.get("user_id")?.asString ?: "", isOnline = true)
                    )
                }
            }, Any::class.java)

            hubConnection!!.on("UserOffline", { payload: Any? ->
                parseJsonPayload(payload)?.let { json ->
                    _onlineEvents.tryEmit(
                        OnlineEvent(userId = json.get("user_id")?.asString ?: "", isOnline = false)
                    )
                }
            }, Any::class.java)

            hubConnection!!.onClosed { error ->
                Log.i(TAG, "SignalR uzildi: $error")
                _connectionState.tryEmit(false)
                if (!userRequestedDisconnect) {
                    Handler(Looper.getMainLooper()).postDelayed({ connectWebSocket() }, 2000)
                }
            }

            hubConnection!!.start().blockingAwait()
            Log.i(TAG, "SignalR ulandi")
            _connectionState.tryEmit(true)
        } catch (e: Exception) {
            Log.e(TAG, "SignalR ulanishda xato", e)
            _connectionState.tryEmit(false)
        }
    }

    override fun disconnectWebSocket() {
        userRequestedDisconnect = true
        try {
            hubConnection?.stop()
        } catch (_: Exception) {}
        hubConnection = null
        _connectionState.tryEmit(false)
    }

    override fun joinRoom(roomId: String) {
        try {
            hubConnection?.send("JoinRoom", roomId)
        } catch (e: Exception) {
            Log.e(TAG, "JoinRoom xato", e)
        }
    }

    override fun leaveRoom(roomId: String) {
        try {
            hubConnection?.send("LeaveRoom", roomId)
        } catch (e: Exception) {
            Log.e(TAG, "LeaveRoom xato", e)
        }
    }

    override fun sendMessageViaWs(
        roomId: String,
        content: String?,
        type: String,
        mediaUrl: String?,
        metadata: Map<String, Any>?
    ) {
        try {
            hubConnection?.send(
                "SendMessage",
                roomId,
                content,
                type,
                mediaUrl,
                if (metadata != null) com.google.gson.JsonObject().apply {
                    metadata.forEach { (k, v) -> add(k, gson.toJsonTree(v)) }
                } else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "SendMessage via WS xato", e)
        }
    }

    override fun sendTypingStart(roomId: String) {
        try {
            hubConnection?.send("TypingStart", roomId)
        } catch (e: Exception) {
            Log.e(TAG, "TypingStart xato", e)
        }
    }

    override fun sendTypingStop(roomId: String) {
        try {
            hubConnection?.send("TypingStop", roomId)
        } catch (e: Exception) {
            Log.e(TAG, "TypingStop xato", e)
        }
    }

    override fun sendMarkRead(roomId: String, messageId: String) {
        try {
            hubConnection?.send("MarkRead", roomId, messageId)
        } catch (e: Exception) {
            Log.e(TAG, "MarkRead xato", e)
        }
    }

    // ===== Helpers =====

    private fun parseJsonPayload(payload: Any?): JsonObject? {
        if (payload == null) return null
        return try {
            when (payload) {
                is JsonObject -> payload
                is String -> gson.fromJson(payload, JsonObject::class.java)
                else -> gson.toJsonTree(payload).asJsonObject
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse xatosi", e)
            null
        }
    }

    private fun parseNewMessage(payload: Any?): Message? {
        val json = parseJsonPayload(payload) ?: return null
        return try {
            Message(
                id = json.get("id")?.asString ?: "",
                roomId = json.get("room_id")?.asString ?: "",
                senderId = json.get("sender_id")?.asString ?: "",
                content = json.get("content")?.takeIf { !it.isJsonNull }?.asString,
                messageType = MessageType.fromValue(json.get("type")?.asString ?: "text"),
                mediaUrl = json.get("media_url")?.takeIf { !it.isJsonNull }?.asString,
                metadata = null,
                isRead = false,
                readAt = null,
                createdAt = json.get("created_at")?.asString ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Message parse xatosi", e)
            null
        }
    }

    // ===== Offline Cache =====

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
                return Result.failure(Exception("Keshda chatlar yo'q"))
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
                return Result.failure(Exception("Keshda xabarlar yo'q"))
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
