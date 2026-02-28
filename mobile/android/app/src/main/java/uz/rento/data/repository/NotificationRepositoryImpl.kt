package uz.rento.data.repository

import uz.rento.data.remote.api.NotificationApi
import uz.rento.data.remote.dto.NotificationItemDto
import uz.rento.data.remote.dto.RegisterFcmTokenRequest
import uz.rento.domain.model.NotificationItem
import uz.rento.domain.model.NotificationType
import uz.rento.domain.repository.NotificationRepository
import uz.rento.domain.repository.NotificationsPage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationApi: NotificationApi
) : NotificationRepository {

    override suspend fun getNotifications(page: Int, perPage: Int): Result<NotificationsPage> {
        return try {
            val response = notificationApi.getNotifications(page, perPage)
            if (response.success && response.data != null) {
                val data = response.data
                Result.success(
                    NotificationsPage(
                        items = data.items.map { it.toDomain() },
                        page = data.meta.page,
                        perPage = data.meta.perPage,
                        total = data.meta.total,
                        totalPages = data.meta.totalPages
                    )
                )
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Bildirishnomalarni olishda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(): Result<Int> {
        return try {
            val response = notificationApi.getUnreadCount()
            if (response.success && response.data != null) {
                Result.success(response.data.unreadCount)
            } else {
                Result.failure(Exception(response.error?.message ?: "Sonni olishda xatolik"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val response = notificationApi.markAsRead(notificationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception("O'qilgan deb belgilashda xatolik: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(): Result<Int> {
        return try {
            val response = notificationApi.markAllAsRead()
            if (response.success && response.data != null) {
                Result.success(response.data.markedCount)
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Belgilashda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerFcmToken(token: String): Result<Unit> {
        return try {
            val response = notificationApi.registerFcmToken(
                RegisterFcmTokenRequest(token = token, deviceType = "android")
            )
            if (response.success && response.data != null) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Token saqlashda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unregisterFcmToken(token: String): Result<Unit> {
        return try {
            val response = notificationApi.unregisterFcmToken(
                mapOf("token" to token, "device_type" to "android")
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception("Token o'chirishda xatolik: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== DTO → Domain mapper =====
    private fun NotificationItemDto.toDomain(): NotificationItem {
        return NotificationItem(
            id = id,
            type = NotificationType.fromString(type),
            title = title,
            body = body,
            refType = refType,
            refId = refId,
            isRead = isRead,
            createdAt = createdAt
        )
    }
}
