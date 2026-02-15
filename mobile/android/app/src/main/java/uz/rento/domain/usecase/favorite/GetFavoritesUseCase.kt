package uz.rento.domain.usecase.favorite

import uz.rento.domain.repository.FavoriteRepository
import uz.rento.domain.repository.FavoritesPage
import javax.inject.Inject

/**
 * GetFavoritesUseCase — sevimli e'lonlar ro'yxatini olish
 */
class GetFavoritesUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(page: Int = 1, perPage: Int = 20): Result<FavoritesPage> {
        return favoriteRepository.getFavorites(page, perPage)
    }
}
