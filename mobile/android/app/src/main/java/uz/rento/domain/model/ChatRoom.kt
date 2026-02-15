package uz.rento.domain.model

/**
 * ChatRoom — chat xonasi domain modeli
 */
data class ChatRoom(
    val roomId: String,
    val listing: ChatListing,
    val otherUser: ChatUser,
    val lastMessage: LastMessage?,
    val unreadCount: Int,
    val createdAt: String
) {
    /** Oxirgi xabar matni (preview uchun) */
    val lastMessagePreview: String
        get() = lastMessage?.content ?: "Yangi chat"

    /** O'qilmagan xabar bormi */
    val hasUnread: Boolean
        get() = unreadCount > 0
}

/**
 * ChatListing — chatdagi e'lon qisqa ma'lumoti
 */
data class ChatListing(
    val id: String,
    val title: String,
    val imageUrl: String?
)

/**
 * ChatUser — chatdagi boshqa foydalanuvchi
 */
data class ChatUser(
    val id: String,
    val fullName: String?,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val lastSeenAt: String?
) {
    val displayName: String
        get() = fullName ?: "Foydalanuvchi"
}

/**
 * LastMessage — oxirgi xabar qisqacha
 */
data class LastMessage(
    val content: String?,
    val senderId: String,
    val createdAt: String
)
