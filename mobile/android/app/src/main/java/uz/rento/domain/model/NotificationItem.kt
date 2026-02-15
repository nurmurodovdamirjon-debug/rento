package uz.rento.domain.model

/**
 * NotificationItem — bildirishnoma domain modeli
 */
data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val refType: String?,
    val refId: String?,
    val isRead: Boolean,
    val createdAt: String
)

/**
 * Bildirishnoma turlari
 */
enum class NotificationType(val value: String) {
    NEW_MESSAGE("new_message"),
    LISTING_APPROVED("listing_approved"),
    LISTING_REJECTED("listing_rejected"),
    NEW_FAVORITE("new_favorite"),
    PRICE_DROP("price_drop"),
    SYSTEM("system"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(value: String): NotificationType {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}
