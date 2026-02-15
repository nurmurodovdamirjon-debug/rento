package uz.rento.data.remote.dto

import com.google.gson.annotations.SerializedName

// ===== LISTING DTOs =====

/**
 * Paginated response wrapper — ro'yxat javoblari uchun
 */
data class PaginatedResponse<T>(
    val items: List<T>,
    val meta: PaginationMeta
)

data class PaginationMeta(
    val page: Int,
    @SerializedName("per_page") val perPage: Int,
    val total: Int,
    @SerializedName("total_pages") val totalPages: Int
)

// ===== LISTING RESPONSE DTOs =====

/**
 * ListingDto — to'liq e'lon javobi (detail sahifa uchun)
 */
data class ListingDto(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val type: String,
    @SerializedName("deal_type") val dealType: String,
    val city: String,
    val district: String?,
    val address: String?,
    val landmark: String?,
    val latitude: Double?,
    val longitude: Double?,
    val rooms: Int?,
    val floor: Int?,
    @SerializedName("total_floors") val totalFloors: Int?,
    @SerializedName("area_sqm") val areaSqm: Double?,
    val price: Double,
    val currency: String,
    @SerializedName("price_negotiable") val priceNegotiable: Boolean,
    @SerializedName("has_furniture") val hasFurniture: Boolean,
    @SerializedName("has_appliances") val hasAppliances: Boolean,
    @SerializedName("has_internet") val hasInternet: Boolean,
    @SerializedName("has_parking") val hasParking: Boolean,
    @SerializedName("has_conditioner") val hasConditioner: Boolean,
    @SerializedName("allows_pets") val allowsPets: Boolean,
    @SerializedName("allows_children") val allowsChildren: Boolean,
    @SerializedName("utilities_included") val utilitiesIncluded: Boolean,
    @SerializedName("deposit_amount") val depositAmount: Double?,
    val status: String,
    @SerializedName("rejection_reason") val rejectionReason: String?,
    @SerializedName("is_premium") val isPremium: Boolean,
    @SerializedName("premium_until") val premiumUntil: String?,
    @SerializedName("views_count") val viewsCount: Int,
    @SerializedName("favorites_count") val favoritesCount: Int,
    @SerializedName("contacts_count") val contactsCount: Int,
    val title: String,
    val description: String?,
    val images: List<ListingImageDto>,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

/**
 * ListingListDto — ro'yxat uchun qisqa e'lon (karta ko'rsatish)
 */
data class ListingListDto(
    val id: String,
    val type: String,
    @SerializedName("deal_type") val dealType: String,
    val city: String,
    val district: String?,
    val address: String?,
    val rooms: Int?,
    val floor: Int?,
    @SerializedName("total_floors") val totalFloors: Int?,
    @SerializedName("area_sqm") val areaSqm: Double?,
    val price: Double,
    val currency: String,
    @SerializedName("price_negotiable") val priceNegotiable: Boolean,
    @SerializedName("has_furniture") val hasFurniture: Boolean,
    @SerializedName("has_internet") val hasInternet: Boolean,
    @SerializedName("is_premium") val isPremium: Boolean,
    @SerializedName("views_count") val viewsCount: Int,
    @SerializedName("favorites_count") val favoritesCount: Int,
    val title: String,
    val status: String? = null,
    val images: List<ListingImageDto>,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("created_at") val createdAt: String
)

/**
 * NearbyListingListDto — yaqin atrofdagi e'lon (masofa bilan)
 */
data class NearbyListingListDto(
    val id: String,
    val type: String,
    @SerializedName("deal_type") val dealType: String,
    val city: String,
    val district: String?,
    val address: String?,
    val rooms: Int?,
    val floor: Int?,
    @SerializedName("total_floors") val totalFloors: Int?,
    @SerializedName("area_sqm") val areaSqm: Double?,
    val price: Double,
    val currency: String,
    @SerializedName("price_negotiable") val priceNegotiable: Boolean,
    @SerializedName("has_furniture") val hasFurniture: Boolean,
    @SerializedName("has_internet") val hasInternet: Boolean,
    @SerializedName("is_premium") val isPremium: Boolean,
    @SerializedName("views_count") val viewsCount: Int,
    @SerializedName("favorites_count") val favoritesCount: Int,
    val title: String,
    val images: List<ListingImageDto>,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("created_at") val createdAt: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("distance_meters") val distanceMeters: Double
)

/**
 * ListingImageDto — rasm javobi
 */
data class ListingImageDto(
    val id: String,
    val url: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("sort_order") val sortOrder: Int,
    @SerializedName("is_main") val isMain: Boolean
)

/**
 * StatsDto — e'lon statistikasi
 */
data class StatsDto(
    val views: Int,
    val favorites: Int,
    val contacts: Int
)

// ===== LISTING REQUEST DTOs =====

/**
 * CreateListingRequest — yangi e'lon yaratish
 */
data class CreateListingRequest(
    val type: String,
    @SerializedName("deal_type") val dealType: String? = null,
    val city: String,
    val district: String? = null,
    val address: String? = null,
    val landmark: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rooms: Int? = null,
    val floor: Int? = null,
    @SerializedName("total_floors") val totalFloors: Int? = null,
    @SerializedName("area_sqm") val areaSqm: Double? = null,
    val price: Double,
    val currency: String,
    @SerializedName("price_negotiable") val priceNegotiable: Boolean? = null,
    @SerializedName("has_furniture") val hasFurniture: Boolean? = null,
    @SerializedName("has_appliances") val hasAppliances: Boolean? = null,
    @SerializedName("has_internet") val hasInternet: Boolean? = null,
    @SerializedName("has_parking") val hasParking: Boolean? = null,
    @SerializedName("has_conditioner") val hasConditioner: Boolean? = null,
    @SerializedName("allows_pets") val allowsPets: Boolean? = null,
    @SerializedName("allows_children") val allowsChildren: Boolean? = null,
    @SerializedName("utilities_included") val utilitiesIncluded: Boolean? = null,
    @SerializedName("deposit_amount") val depositAmount: Double? = null,
    val title: String,
    val description: String? = null
)

/**
 * UpdateListingRequest — e'lonni tahrirlash (partial)
 */
data class UpdateListingRequest(
    val type: String? = null,
    @SerializedName("deal_type") val dealType: String? = null,
    val city: String? = null,
    val district: String? = null,
    val address: String? = null,
    val landmark: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rooms: Int? = null,
    val floor: Int? = null,
    @SerializedName("total_floors") val totalFloors: Int? = null,
    @SerializedName("area_sqm") val areaSqm: Double? = null,
    val price: Double? = null,
    val currency: String? = null,
    @SerializedName("price_negotiable") val priceNegotiable: Boolean? = null,
    @SerializedName("has_furniture") val hasFurniture: Boolean? = null,
    @SerializedName("has_appliances") val hasAppliances: Boolean? = null,
    @SerializedName("has_internet") val hasInternet: Boolean? = null,
    @SerializedName("has_parking") val hasParking: Boolean? = null,
    @SerializedName("has_conditioner") val hasConditioner: Boolean? = null,
    @SerializedName("allows_pets") val allowsPets: Boolean? = null,
    @SerializedName("allows_children") val allowsChildren: Boolean? = null,
    @SerializedName("utilities_included") val utilitiesIncluded: Boolean? = null,
    @SerializedName("deposit_amount") val depositAmount: Double? = null,
    val title: String? = null,
    val description: String? = null
)

/**
 * UpdateStatusRequest — status o'zgartirish
 */
data class UpdateStatusRequest(
    val status: String
)
