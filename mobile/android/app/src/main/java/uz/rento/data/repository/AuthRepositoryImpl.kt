package uz.rento.data.repository

import kotlinx.coroutines.flow.first
import uz.rento.data.local.preferences.UserPreferences
import uz.rento.data.remote.api.AuthApi
import uz.rento.data.remote.dto.RefreshTokenRequest
import uz.rento.data.remote.dto.SendOtpRequest
import uz.rento.data.remote.dto.VerifyOtpRequest
import uz.rento.domain.model.User
import uz.rento.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userPreferences: UserPreferences
) : AuthRepository {

    override suspend fun sendOtp(phone: String): Result<Unit> {
        return try {
            val response = authApi.sendOtp(SendOtpRequest(phone))
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "SMS yuborishda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyOtp(phone: String, otp: String): Result<User> {
        return try {
            val response = authApi.verifyOtp(VerifyOtpRequest(phone, otp))
            if (response.success && response.data != null) {
                // Tokenlarni saqlash
                userPreferences.saveTokens(
                    accessToken = response.data.accessToken,
                    refreshToken = response.data.refreshToken
                )
                // User ma'lumotlarini saqlash
                userPreferences.saveUserInfo(
                    userId = response.data.user.id,
                    phone = response.data.user.phone
                )
                // DTO → Domain model
                val user = response.data.user.toDomain()
                Result.success(user)
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "OTP tasdiqlashda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<Unit> {
        return try {
            val currentRefreshToken = userPreferences.refreshToken.first()
                ?: return Result.failure(Exception("Refresh token topilmadi"))

            val response = authApi.refreshToken(RefreshTokenRequest(currentRefreshToken))
            if (response.success && response.data != null) {
                userPreferences.saveTokens(
                    accessToken = response.data.accessToken,
                    refreshToken = response.data.refreshToken
                )
                Result.success(Unit)
            } else {
                userPreferences.clearTokens()
                Result.failure(
                    Exception(response.error?.message ?: "Token yangilashda xatolik")
                )
            }
        } catch (e: Exception) {
            userPreferences.clearTokens()
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            authApi.logout()
            userPreferences.clearTokens()
            Result.success(Unit)
        } catch (e: Exception) {
            // Xato bo'lsa ham, local tokenlarni tozalash
            userPreferences.clearTokens()
            Result.success(Unit)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return !userPreferences.accessToken.first().isNullOrBlank()
    }
}

// Extension function: DTO → Domain
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
