package uz.rento.domain.usecase.favorite

import uz.rento.domain.repository.FavoriteRepository
import javax.inject.Inject

/**
 * GetFavoriteIdsUseCase — sevimli e'lonlar ID larini olish (batch check)
 */
class GetFavoriteIdsUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(): Result<List<String>> {
        return favoriteRepository.getFavoriteIds()
    }
}
