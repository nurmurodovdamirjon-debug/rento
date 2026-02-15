package uz.rento.domain.repository

import uz.rento.domain.model.User

/**
 * UserRepository — foydalanuvchi operatsiyalari interfeysi
 */
interface UserRepository {
    suspend fun getMyProfile(): Result<User>
    suspend fun updateProfile(fullName: String?, email: String?, language: String?): Result<User>
    suspend fun getPublicProfile(userId: String): Result<User>
}
