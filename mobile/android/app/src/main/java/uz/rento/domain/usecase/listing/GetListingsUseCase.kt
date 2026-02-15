package uz.rento.domain.usecase.listing

import uz.rento.domain.model.Listing
import uz.rento.domain.model.ListingFilter
import uz.rento.domain.repository.ListingRepository
import uz.rento.domain.repository.ListingsPage
import javax.inject.Inject

/**
 * GetListingsUseCase — e'lonlar ro'yxatini olish (filtrlash bilan)
 */
class GetListingsUseCase @Inject constructor(
    private val listingRepository: ListingRepository
) {
    suspend operator fun invoke(filter: ListingFilter = ListingFilter()): Result<ListingsPage> {
        return listingRepository.getListings(filter)
    }
}
