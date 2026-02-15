package uz.rento.data.remote.dto

import com.google.gson.annotations.SerializedName

// ===== CHAT DTOs =====

/**
 * ChatRoomListDto — GET /chats javobidagi ro'yxat
 */
data class ChatRoomListDto(
    val items: List<ChatRoomListItemDto>,
    val meta: PaginationMeta
)

/**
 * ChatRoomListItemDto — ro'yxatdagi har bir chat xonasi
 */
data class ChatRoomListItemDto(
    @SerializedName("room_id") val roomId: String,
    val listing: ChatListingDto,
    @SerializedName("other_user") val otherUser: ChatUserDto,
    @SerializedName("last_message") val lastMessage: LastMessageDto?,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("created_at") val createdAt: String
)

/**
 * ChatRoomDetailDto — POST /chats javobi
 */
data class ChatRoomDetailDto(
    @SerializedName("room_id") val roomId: String,
    val listing: ChatListingDto,
    @SerializedName("other_user") val otherUser: ChatUserDto?,
    @SerializedName("created_at") val createdAt: String
)

/**
 * ChatListingDto — chat ichidagi e'lon ma'lumoti (qisqa)
 */
data class ChatListingDto(
    val id: String,
    val title: String,
    @SerializedName("image_url") val imageUrl: String?
)

/**
 * ChatUserDto — chatdagi boshqa foydalanuvchi
 */
data class ChatUserDto(
    val id: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("is_online") val isOnline: Boolean,
    @SerializedName("last_seen_at") val lastSeenAt: String?
)

/**
 * LastMessageDto — oxirgi xabar qisqacha
 */
data class LastMessageDto(
    val content: String?,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("created_at") val createdAt: String
)

// ===== MESSAGE DTOs =====

/**
 * MessageListDto — GET /chats/:room_id/messages javobi
 */
data class MessageListDto(
    val items: List<MessageDto>,
    val meta: PaginationMeta
)

/**
 * MessageDto — xabar javobi
 */
data class MessageDto(
    val id: String,
    @SerializedName("room_id") val roomId: String,
    @SerializedName("sender_id") val senderId: String,
    val content: String?,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("media_url") val mediaUrl: String?,
    val metadata: Map<String, Any>?,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("read_at") val readAt: String?,
    @SerializedName("created_at") val createdAt: String
)

// ===== CHAT REQUEST DTOs =====

/**
 * CreateChatRequest — yangi chat boshlash
 */
data class CreateChatRequest(
    @SerializedName("listing_id") val listingId: String,
    @SerializedName("initial_message") val initialMessage: String
)

/**
 * SendMessageRequest — xabar yuborish
 */
data class SendMessageRequest(
    val content: String? = null,
    @SerializedName("message_type") val messageType: String = "text",
    @SerializedName("media_url") val mediaUrl: String? = null,
    val metadata: Map<String, Any>? = null
)
