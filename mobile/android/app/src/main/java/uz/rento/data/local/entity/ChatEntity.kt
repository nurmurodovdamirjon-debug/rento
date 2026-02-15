package uz.rento.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ChatRoomEntity — chat xonasi offline cache
 */
@Entity(
    tableName = "chat_rooms",
    indices = [Index("updated_at")]
)
data class ChatRoomEntity(
    @PrimaryKey
    @ColumnInfo(name = "room_id")
    val roomId: String,

    // Listing ma'lumotlari (embedded sifatida)
    @ColumnInfo(name = "listing_id")
    val listingId: String,

    @ColumnInfo(name = "listing_title")
    val listingTitle: String,

    @ColumnInfo(name = "listing_image_url")
    val listingImageUrl: String?,

    // Other user ma'lumotlari
    @ColumnInfo(name = "other_user_id")
    val otherUserId: String,

    @ColumnInfo(name = "other_user_name")
    val otherUserName: String?,

    @ColumnInfo(name = "other_user_avatar")
    val otherUserAvatar: String?,

    @ColumnInfo(name = "other_user_online")
    val otherUserOnline: Boolean,

    @ColumnInfo(name = "other_user_last_seen")
    val otherUserLastSeen: String?,

    // Last message
    @ColumnInfo(name = "last_message_content")
    val lastMessageContent: String?,

    @ColumnInfo(name = "last_message_sender_id")
    val lastMessageSenderId: String?,

    @ColumnInfo(name = "last_message_created_at")
    val lastMessageCreatedAt: String?,

    @ColumnInfo(name = "unread_count")
    val unreadCount: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * MessageEntity — xabar offline cache
 */
@Entity(
    tableName = "messages",
    indices = [
        Index("room_id"),
        Index("created_at")
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "room_id")
    val roomId: String,

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    val content: String?,

    @ColumnInfo(name = "message_type")
    val messageType: String,

    @ColumnInfo(name = "media_url")
    val mediaUrl: String?,

    @ColumnInfo(name = "is_read")
    val isRead: Boolean,

    @ColumnInfo(name = "read_at")
    val readAt: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
