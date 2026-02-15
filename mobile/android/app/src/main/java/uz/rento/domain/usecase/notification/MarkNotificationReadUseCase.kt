package uz.rento.domain.usecase.notification

import uz.rento.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * MarkNotificationReadUseCase — bildirishnomani o'qilgan deb belgilash
 */
class MarkNotificationReadUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    /** Bitta bildirishnomani o'qilgan deb belgilash */
    suspend fun markOne(notificationId: String): Result<Unit> {
        return notificationRepository.markAsRead(notificationId)
    }

    /** Barcha bildirishnomalarni o'qilgan deb belgilash */
    suspend fun markAll(): Result<Int> {
        return notificationRepository.markAllAsRead()
    }
}
