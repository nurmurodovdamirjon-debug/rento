package uz.rento.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ListingEntity — Room entity for offline listing cache
 */
@Entity(
    tableName = "listings",
    indices = [
        Index("user_id"),
        Index("status"),
        Index("city"),
        Index("deal_type"),
        Index("created_at"),
        Index("is_premium")
    ]
)
data class ListingEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    val type: String,

    @ColumnInfo(name = "deal_type")
    val dealType: String,

    val city: String,
    val district: String?,
    val address: String?,
    val landmark: String?,
    val latitude: Double?,
    val longitude: Double?,
    val rooms: Int?,
    val floor: Int?,

    @ColumnInfo(name = "total_floors")
    val totalFloors: Int?,

    @ColumnInfo(name = "area_sqm")
    val areaSqm: Double?,

    val price: Double,
    val currency: String,

    @ColumnInfo(name = "price_negotiable")
    val priceNegotiable: Boolean,

    @ColumnInfo(name = "has_furniture")
    val hasFurniture: Boolean,

    @ColumnInfo(name = "has_appliances")
    val hasAppliances: Boolean,

    @ColumnInfo(name = "has_internet")
    val hasInternet: Boolean,

    @ColumnInfo(name = "has_parking")
    val hasParking: Boolean,

    @ColumnInfo(name = "has_conditioner")
    val hasConditioner: Boolean,

    @ColumnInfo(name = "allows_pets")
    val allowsPets: Boolean,

    @ColumnInfo(name = "allows_children")
    val allowsChildren: Boolean,

    @ColumnInfo(name = "utilities_included")
    val utilitiesIncluded: Boolean,

    @ColumnInfo(name = "deposit_amount")
    val depositAmount: Double?,

    val status: String,

    @ColumnInfo(name = "rejection_reason")
    val rejectionReason: String?,

    @ColumnInfo(name = "is_premium")
    val isPremium: Boolean,

    @ColumnInfo(name = "views_count")
    val viewsCount: Int,

    @ColumnInfo(name = "favorites_count")
    val favoritesCount: Int,

    @ColumnInfo(name = "contacts_count")
    val contactsCount: Int,

    val title: String,
    val description: String?,

    @ColumnInfo(name = "published_at")
    val publishedAt: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String,

    /** Qachon cache qilingan */
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * ListingImageEntity — rasm cache
 */
@Entity(
    tableName = "listing_images",
    indices = [Index("listing_id")]
)
data class ListingImageEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "listing_id")
    val listingId: String,

    val url: String,

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String?,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,

    @ColumnInfo(name = "is_main")
    val isMain: Boolean
)
