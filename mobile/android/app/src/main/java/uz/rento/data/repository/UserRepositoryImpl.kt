package uz.rento.data.repository

import android.util.Log
import uz.rento.data.local.dao.UserDao
import uz.rento.data.local.mapper.toDomain
import uz.rento.data.local.mapper.toEntity
import uz.rento.data.remote.api.UserApi
import uz.rento.data.remote.dto.UpdateProfileRequest
import uz.rento.domain.model.User
import uz.rento.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao
) : UserRepository {

    companion object {
        private const val TAG = "UserRepo"
    }

    override suspend fun getMyProfile(): Result<User> {
        return try {
            val response = userApi.getMyProfile()
            if (response.success && response.data != null) {
                val user = response.data.toDomain()
                // Cache user profile
                cacheUser(user)
                Result.success(user)
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Profil olishda xatolik")
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network xato, keshdan o'qilmoqda (profile)", e)
            loadUserFromCache()
        }
    }

    override suspend fun updateProfile(
        fullName: String?,
        email: String?,
        language: String?
    ): Result<User> {
        return try {
            val response = userApi.updateProfile(
                UpdateProfileRequest(
                    fullName = fullName,
                    email = email,
                    language = language
                )
            )
            if (response.success && response.data != null) {
                val user = response.data.toDomain()
                cacheUser(user)
                Result.success(user)
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Profil yangilashda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPublicProfile(userId: String): Result<User> {
        return try {
            val response = userApi.getPublicProfile(userId)
            if (response.success && response.data != null) {
                val dto = response.data
                val user = User(
                    id = dto.id,
                    phone = "",
                    phoneVerified = false,
                    fullName = dto.fullName,
                    email = null,
                    avatarUrl = dto.avatarUrl,
                    role = dto.role,
                    idVerified = dto.idVerified,
                    ratingAvg = dto.ratingAvg,
                    ratingCount = dto.ratingCount,
                    subscription = "",
                    language = "",
                    lastSeenAt = dto.lastSeenAt,
                    createdAt = dto.createdAt,
                    isActive = true
                )
                cacheUser(user)
                Result.success(user)
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Profil olishda xatolik")
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network xato, keshdan o'qilmoqda (public profile: $userId)", e)
            loadUserFromCacheById(userId)
        }
    }

    // ===== Offline Cache Helpers =====

    private suspend fun cacheUser(user: User) {
        try {
            userDao.insert(user.toEntity())
        } catch (e: Exception) {
            Log.e(TAG, "Foydalanuvchini keshga saqlashda xato", e)
        }
    }

    private suspend fun loadUserFromCache(): Result<User> {
        return try {
            val entities = userDao.getAll()
            val entity = entities.firstOrNull()
                ?: return Result.failure(Exception("Keshda profil yo'q"))
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Keshdan profilni o'qishda xato", e)
            Result.failure(Exception("Offline profilni olishda xatolik"))
        }
    }

    private suspend fun loadUserFromCacheById(userId: String): Result<User> {
        return try {
            val entity = userDao.getUser(userId)
                ?: return Result.failure(Exception("Keshda foydalanuvchi topilmadi"))
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Keshdan foydalanuvchini o'qishda xato: $userId", e)
            Result.failure(Exception("Offline foydalanuvchini olishda xatolik"))
        }
    }
}

// Extension function: UserDto → Domain
private fun uz.rento.data.remote.dto.UserDto.toDomain(): User {
    return User(
        id = id,
        phone = phone,
        phoneVerified = phoneVerified,
        fullName = fullName,
        email = email,
        avatarUrl = avatarUrl,
        role = role,
        idVerified = idVerified,
        ratingAvg = ratingAvg,
        ratingCount = ratingCount,
        subscription = subscription ?: "free",
        language = language ?: "uz",
        lastSeenAt = lastSeenAt,
        createdAt = createdAt ?: "",
        isActive = isActive
    )
}
