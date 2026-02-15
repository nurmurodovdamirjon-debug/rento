package uz.rento.domain.usecase.notification

import uz.rento.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * RegisterFcmTokenUseCase — FCM token ro'yxatdan o'tkazish / o'chirish
 */
class RegisterFcmTokenUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    /** Token ro'yxatdan o'tkazish */
    suspend fun register(token: String): Result<Unit> {
        return notificationRepository.registerFcmToken(token)
    }

    /** Token o'chirish (logout paytida) */
    suspend fun unregister(token: String): Result<Unit> {
        return notificationRepository.unregisterFcmToken(token)
    }
}
