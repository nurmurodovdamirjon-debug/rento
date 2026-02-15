package uz.rento.domain.usecase.notification

import uz.rento.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * GetUnreadCountUseCase — o'qilmagan bildirishnomalar sonini olish
 */
class GetUnreadCountUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(): Result<Int> {
        return notificationRepository.getUnreadCount()
    }
}
