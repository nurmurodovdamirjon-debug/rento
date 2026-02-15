package uz.rento.data.remote.dto

import com.google.gson.annotations.SerializedName

// ===== NOTIFICATION DTOs =====

/**
 * Bildirishnomalar ro'yxati (paginated) — API: data = { items, meta }
 */
data class NotificationListDto(
    val items: List<NotificationItemDto>,
    val meta: PaginationMeta
)

/**
 * Bitta bildirishnoma
 */
data class NotificationItemDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    @SerializedName("ref_type") val refType: String? = null,
    @SerializedName("ref_id") val refId: String? = null,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String
)

/**
 * O'qilmagan bildirishnomalar soni
 */
data class UnreadCountResponse(
    @SerializedName("unread_count") val unreadCount: Int
)

/**
 * Barchasini o'qilgan deb belgilash javobi
 */
data class MarkAllReadResponse(
    @SerializedName("marked_count") val markedCount: Int
)

/**
 * FCM token ro'yxatdan o'tkazish so'rovi
 */
data class RegisterFcmTokenRequest(
    val token: String,
    @SerializedName("device_type") val deviceType: String = "android"
)
