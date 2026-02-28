package uz.rento.data.remote.dto

import com.google.gson.annotations.SerializedName

// ===== API Wrapper =====

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorInfo? = null
)

data class ErrorInfo(
    val code: String,
    val message: String,
    val details: Any? = null
)

// ===== AUTH DTOs =====

data class SendOtpRequest(
    val phone: String
)

data class SendOtpResponse(
    val phone: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("retry_after") val retryAfter: Int,
    @SerializedName("attempts_remaining") val attemptsRemaining: Int
)

data class VerifyOtpRequest(
    val phone: String,
    val otp: String
)

data class VerifyOtpResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Int,
    val user: UserDto,
    @SerializedName("is_new_user") val isNewUser: Boolean
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class RefreshTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Int
)

// ===== USER DTOs =====

data class UserDto(
    val id: String,
    val phone: String,
    @SerializedName("phone_verified") val phoneVerified: Boolean = false,
    @SerializedName("full_name") val fullName: String? = null,
    val email: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val role: String = "tenant",
    @SerializedName("id_verified") val idVerified: Boolean = false,
    @SerializedName("rating_avg") val ratingAvg: Double = 0.0,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    val subscription: String? = "free",
    val language: String? = "uz",
    @SerializedName("last_seen_at") val lastSeenAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("is_new_user") val isNewUser: Boolean = false
)

data class PublicUserDto(
    val id: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val role: String,
    @SerializedName("id_verified") val idVerified: Boolean,
    @SerializedName("rating_avg") val ratingAvg: Double,
    @SerializedName("rating_count") val ratingCount: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("last_seen_at") val lastSeenAt: String?
)

data class UpdateProfileRequest(
    @SerializedName("full_name") val fullName: String? = null,
    val email: String? = null,
    val language: String? = null
)
