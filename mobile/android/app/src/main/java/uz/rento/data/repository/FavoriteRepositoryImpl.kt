package uz.rento.data.repository

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
    private val favoriteApi: FavoriteApi
) : FavoriteRepository {

    override suspend fun toggleFavorite(listingId: String): Result<FavoriteToggleResult> {
        return try {
            val response = favoriteApi.toggle(listingId)
            if (response.success && response.data != null) {
                Result.success(
                    FavoriteToggleResult(
                        isFavorite = response.data.isFavorite,
                        favoritesCount = response.data.favoritesCount
                    )
                )
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
                Result.success(
                    FavoritesPage(
                        items = data.items.map { it.toDomain() },
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
            Result.failure(e)
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
            Result.failure(e)
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
            Result.failure(e)
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
