package uz.rento.domain.repository

import uz.rento.domain.model.NotificationItem

/**
 * NotificationRepository — bildirishnomalar operatsiyalari interfeysi
 */
interface NotificationRepository {

    /** Bildirishnomalar ro'yxati */
    suspend fun getNotifications(page: Int = 1, perPage: Int = 20): Result<NotificationsPage>

    /** O'qilmagan bildirishnomalar soni */
    suspend fun getUnreadCount(): Result<Int>

    /** Bitta bildirishnomani o'qilgan deb belgilash */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /** Barcha bildirishnomalarni o'qilgan deb belgilash */
    suspend fun markAllAsRead(): Result<Int>

    /** FCM token ro'yxatdan o'tkazish */
    suspend fun registerFcmToken(token: String): Result<Unit>

    /** FCM token o'chirish */
    suspend fun unregisterFcmToken(token: String): Result<Unit>
}

/**
 * NotificationsPage — sahifalangan bildirishnomalar
 */
data class NotificationsPage(
    val items: List<NotificationItem>,
    val page: Int,
    val perPage: Int,
    val total: Int,
    val totalPages: Int
)
