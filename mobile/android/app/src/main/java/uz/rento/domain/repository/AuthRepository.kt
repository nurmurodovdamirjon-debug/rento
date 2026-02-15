package uz.rento.domain.repository

import uz.rento.domain.model.User

/**
 * AuthRepository — auth operatsiyalari interfeysi
 */
interface AuthRepository {
    suspend fun sendOtp(phone: String): Result<Unit>
    suspend fun verifyOtp(phone: String, otp: String): Result<User>
    suspend fun refreshToken(): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun isLoggedIn(): Boolean
}
