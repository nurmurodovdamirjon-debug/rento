package uz.rento.data.repository

import android.util.Log
import uz.rento.data.local.dao.FavoriteDao
import uz.rento.data.local.mapper.toDomain
import uz.rento.data.local.mapper.toEntity
import uz.rento.data.remote.api.FavoriteApi
import uz.rento.data.remote.dto.FavoriteListItemDto
import uz.rento.domain.model.FavoriteItem
import uz.rento.domain.model.FavoriteToggleResult
import uz.rento.domain.repository.FavoriteRepository
import uz.rento.domain.repository.FavoritesPage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteApi: FavoriteApi,
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    companion object {
        private const val TAG = "FavoriteRepo"
    }

    override suspend fun toggleFavorite(listingId: String): Result<FavoriteToggleResult> {
        return try {
            val response = favoriteApi.toggle(listingId)
            if (response.success && response.data != null) {
                val result = FavoriteToggleResult(
                    isFavorite = response.data.isFavorite,
                    favoritesCount = response.data.favoritesCount
                )
                // Update local cache
                if (!result.isFavorite) {
                    favoriteDao.deleteByListingId(listingId)
                }
                Result.success(result)
            } else {
                Result.failure(Exception(response.error?.message ?: "Sevimliga qo'shishda xatolik"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavorites(page: Int, perPage: Int): Result<FavoritesPage> {
        return try {
            val response = favoriteApi.getFavorites(page, perPage)
            if (response.success && response.data != null) {
                val data = response.data
                val domainItems = data.items.map { it.toDomain() }
                // Cache favorites
                cacheFavorites(domainItems)
                Result.success(
                    FavoritesPage(
                        items = domainItems,
                        page = data.meta.page,
                        perPage = data.meta.perPage,
                        total = data.meta.total,
                        totalPages = data.meta.totalPages
                    )
                )
            } else {
                Result.failure(Exception(response.error?.message ?: "Sevimlilarni olishda xatolik"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network xato, keshdan o'qilmoqda (favorites)", e)
            loadFavoritesFromCache()
        }
    }

    override suspend fun isFavorite(listingId: String): Result<Boolean> {
        return try {
            val response = favoriteApi.check(listingId)
            if (response.success && response.data != null) {
                Result.success(response.data.isFavorite)
            } else {
                Result.failure(Exception(response.error?.message ?: "Tekshirishda xatolik"))
            }
        } catch (e: Exception) {
            // Offline fallback: check local cache
            try {
                Result.success(favoriteDao.isFavorite(listingId))
            } catch (cacheEx: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getFavoriteIds(): Result<List<String>> {
        return try {
            val response = favoriteApi.getFavoriteIds()
            if (response.success && response.data != null) {
                Result.success(response.data.listingIds)
            } else {
                Result.failure(Exception(response.error?.message ?: "IDlarni olishda xatolik"))
            }
        } catch (e: Exception) {
            // Offline fallback
            try {
                Result.success(favoriteDao.getFavoriteIds())
            } catch (cacheEx: Exception) {
                Result.failure(e)
            }
        }
    }

    // ===== Offline Cache Helpers =====

    private suspend fun cacheFavorites(favorites: List<FavoriteItem>) {
        try {
            favoriteDao.clearAll()
            favoriteDao.insertAll(favorites.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e(TAG, "Sevimlilarni keshga saqlashda xato", e)
        }
    }

    private suspend fun loadFavoritesFromCache(): Result<FavoritesPage> {
        return try {
            val entities = favoriteDao.getAll()
            if (entities.isEmpty()) {
                return Result.failure(Exception("Keshda sevimlilar yo'q va internet mavjud emas"))
            }
            Result.success(
                FavoritesPage(
                    items = entities.map { it.toDomain() },
                    page = 1,
                    perPage = entities.size,
                    total = entities.size,
                    totalPages = 1
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Keshdan sevimlilarni o'qishda xato", e)
            Result.failure(Exception("Offline sevimlilarni olishda xatolik"))
        }
    }

    // ===== DTO → Domain mapper =====
    private fun FavoriteListItemDto.toDomain(): FavoriteItem {
        return FavoriteItem(
            favoriteId = favoriteId,
            listingId = listingId,
            title = title,
            city = city,
            price = price,
            currency = currency,
            rooms = rooms,
            areaSqm = areaSqm,
            imageUrl = imageUrl,
            status = status,
            isPremium = isPremium,
            favoritedAt = favoritedAt
        )
    }
}
