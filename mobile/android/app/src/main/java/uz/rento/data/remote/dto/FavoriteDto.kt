package uz.rento.data.remote.dto

import com.google.gson.annotations.SerializedName

// ===== FAVORITE DTOs =====

/**
 * Toggle favorite javobi
 */
data class ToggleFavoriteResponse(
    @SerializedName("is_favorite") val isFavorite: Boolean,
    @SerializedName("favorites_count") val favoritesCount: Int
)

/**
 * Check favorite javobi
 */
data class FavoriteCheckResponse(
    @SerializedName("is_favorite") val isFavorite: Boolean
)

/**
 * Sevimli e'lonlar ID ro'yxati (batch check uchun)
 */
data class FavoriteIdsResponse(
    @SerializedName("listing_ids") val listingIds: List<String>
)

/**
 * Sevimlilar ro'yxati (paginated) — API: data = { items, meta }
 */
data class FavoriteListDto(
    val items: List<FavoriteListItemDto>,
    val meta: PaginationMeta
)

/**
 * Sevimlilar ro'yxatidagi bitta element
 */
data class FavoriteListItemDto(
    @SerializedName("favorite_id") val favoriteId: String,
    @SerializedName("listing_id") val listingId: String,
    val title: String,
    val city: String,
    val price: Double,
    val currency: String,
    val rooms: Int? = null,
    @SerializedName("area_sqm") val areaSqm: Double? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    val status: String,
    @SerializedName("is_premium") val isPremium: Boolean,
    @SerializedName("favorited_at") val favoritedAt: String
)
