package uz.rento.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * FavoriteEntity — sevimli e'lon offline cache
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "favorite_id")
    val favoriteId: String,

    @ColumnInfo(name = "listing_id")
    val listingId: String,

    val title: String,
    val city: String,
    val price: Double,
    val currency: String,
    val rooms: Int?,

    @ColumnInfo(name = "area_sqm")
    val areaSqm: Double?,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?,

    val status: String,

    @ColumnInfo(name = "is_premium")
    val isPremium: Boolean,

    @ColumnInfo(name = "favorited_at")
    val favoritedAt: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * UserEntity — foydalanuvchi profil offline cache
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,

    val phone: String,

    @ColumnInfo(name = "phone_verified")
    val phoneVerified: Boolean,

    @ColumnInfo(name = "full_name")
    val fullName: String?,

    val email: String?,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    val role: String,

    @ColumnInfo(name = "id_verified")
    val idVerified: Boolean,

    @ColumnInfo(name = "rating_avg")
    val ratingAvg: Double,

    @ColumnInfo(name = "rating_count")
    val ratingCount: Int,

    val subscription: String,
    val language: String,

    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
