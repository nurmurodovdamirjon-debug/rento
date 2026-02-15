package uz.rento.domain.model

/**
 * User — domain model
 * DTO dan mustaqil bo'lib, presentation qatlamida ishlatiladi
 */
data class User(
    val id: String,
    val phone: String,
    val phoneVerified: Boolean,
    val fullName: String?,
    val email: String?,
    val avatarUrl: String?,
    val role: String,
    val idVerified: Boolean,
    val ratingAvg: Double,
    val ratingCount: Int,
    val subscription: String,
    val language: String,
    val lastSeenAt: String?,
    val createdAt: String,
    val isActive: Boolean
) {
    val displayName: String
        get() = fullName ?: phone

    val isVerified: Boolean
        get() = idVerified

    val isPro: Boolean
        get() = subscription == "pro" || subscription == "business"
}
