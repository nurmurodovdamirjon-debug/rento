package uz.rento.domain.repository

import uz.rento.domain.model.FavoriteItem
import uz.rento.domain.model.FavoriteToggleResult

/**
 * FavoriteRepository — sevimlilar operatsiyalari interfeysi
 */
interface FavoriteRepository {

    /** Sevimliga qo'shish / olib tashlash (toggle) */
    suspend fun toggleFavorite(listingId: String): Result<FavoriteToggleResult>

    /** Sevimlilar ro'yxati */
    suspend fun getFavorites(page: Int = 1, perPage: Int = 20): Result<FavoritesPage>

    /** Bitta e'lon sevimliga qo'shilganmi? */
    suspend fun isFavorite(listingId: String): Result<Boolean>

    /** Sevimli e'lonlar ID lari (batch check uchun) */
    suspend fun getFavoriteIds(): Result<List<String>>
}

/**
 * FavoritesPage — sahifalangan sevimlilar
 */
data class FavoritesPage(
    val items: List<FavoriteItem>,
    val page: Int,
    val perPage: Int,
    val total: Int,
    val totalPages: Int
)
