package uz.rento.domain.model

/**
 * Message — xabar domain modeli
 */
data class Message(
    val id: String,
    val roomId: String,
    val senderId: String,
    val content: String?,
    val messageType: MessageType,
    val mediaUrl: String?,
    val metadata: Map<String, Any>?,
    val isRead: Boolean,
    val readAt: String?,
    val createdAt: String
) {
    /** Foydalanuvchi yuborgan xabarmi */
    fun isMine(currentUserId: String): Boolean = senderId == currentUserId

    /** Rasm xabari */
    val isImage: Boolean get() = messageType == MessageType.IMAGE

    /** Lokatsiya xabari */
    val isLocation: Boolean get() = messageType == MessageType.LOCATION
}

/**
 * MessageType — xabar turi
 */
enum class MessageType(val value: String) {
    TEXT("text"),
    IMAGE("image"),
    LOCATION("location"),
    CONTACT("contact");

    companion object {
        fun fromValue(value: String): MessageType {
            return entries.firstOrNull { it.value == value } ?: TEXT
        }
    }
}
