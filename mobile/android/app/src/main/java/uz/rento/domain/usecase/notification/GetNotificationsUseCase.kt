package uz.rento.domain.usecase.notification

import uz.rento.domain.repository.NotificationRepository
import uz.rento.domain.repository.NotificationsPage
import javax.inject.Inject

/**
 * GetNotificationsUseCase — bildirishnomalar ro'yxatini olish
 */
class GetNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(page: Int = 1, perPage: Int = 20): Result<NotificationsPage> {
        return notificationRepository.getNotifications(page, perPage)
    }
}
