package uz.rento.domain.usecase.listing

import uz.rento.domain.model.SearchFilter
import uz.rento.domain.repository.ListingRepository
import uz.rento.domain.repository.ListingsPage
import javax.inject.Inject

/**
 * SearchListingsUseCase — Elasticsearch orqali qidiruv
 * Text + filter + geo + scoring
 */
class SearchListingsUseCase @Inject constructor(
    private val listingRepository: ListingRepository
) {
    suspend operator fun invoke(filter: SearchFilter): Result<ListingsPage> {
        return listingRepository.searchListings(filter)
    }
}
