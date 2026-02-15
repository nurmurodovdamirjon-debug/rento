package uz.rento.domain.usecase.favorite

import uz.rento.domain.model.FavoriteToggleResult
import uz.rento.domain.repository.FavoriteRepository
import javax.inject.Inject

/**
 * ToggleFavoriteUseCase — e'lonni sevimliga qo'shish yoki olib tashlash
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(listingId: String): Result<FavoriteToggleResult> {
        return favoriteRepository.toggleFavorite(listingId)
    }
}
