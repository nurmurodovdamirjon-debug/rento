package uz.rento.data.repository

import uz.rento.data.remote.api.UserApi
import uz.rento.data.remote.dto.UpdateProfileRequest
import uz.rento.domain.model.User
import uz.rento.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi
) : UserRepository {

    override suspend fun getMyProfile(): Result<User> {
        return try {
            val response = userApi.getMyProfile()
            if (response.success && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Profil olishda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                Result.success(response.data.toDomain())
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
                Result.success(
                    User(
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
                )
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Profil olishda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
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
        subscription = subscription,
        language = language,
        lastSeenAt = lastSeenAt,
        createdAt = createdAt,
        isActive = isActive
    )
}
