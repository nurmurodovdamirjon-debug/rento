package uz.rento.domain.usecase.listing

import uz.rento.domain.model.Listing
import uz.rento.domain.repository.ListingRepository
import javax.inject.Inject

/**
 * CreateListingUseCase — yangi e'lon yaratish (validatsiya bilan)
 */
class CreateListingUseCase @Inject constructor(
    private val listingRepository: ListingRepository
) {
    suspend operator fun invoke(
        type: String,
        dealType: String?,
        city: String,
        district: String?,
        address: String?,
        landmark: String?,
        latitude: Double?,
        longitude: Double?,
        rooms: Int?,
        floor: Int?,
        totalFloors: Int?,
        areaSqm: Double?,
        price: Double,
        currency: String,
        priceNegotiable: Boolean?,
        hasFurniture: Boolean?,
        hasAppliances: Boolean?,
        hasInternet: Boolean?,
        hasParking: Boolean?,
        hasConditioner: Boolean?,
        allowsPets: Boolean?,
        allowsChildren: Boolean?,
        utilitiesIncluded: Boolean?,
        depositAmount: Double?,
        title: String,
        description: String?
    ): Result<Listing> {
        // Validatsiya
        if (title.length < 5) {
            return Result.failure(
                IllegalArgumentException("Sarlavha kamida 5 ta belgi bo'lishi kerak")
            )
        }
        if (price <= 0) {
            return Result.failure(
                IllegalArgumentException("Narx 0 dan katta bo'lishi kerak")
            )
        }
        if (city.length < 2) {
            return Result.failure(
                IllegalArgumentException("Shahar nomi kamida 2 ta belgi bo'lishi kerak")
            )
        }
        val validTypes = listOf("apartment", "house", "office", "shop", "warehouse")
        if (type !in validTypes) {
            return Result.failure(
                IllegalArgumentException("Noto'g'ri mulk turi")
            )
        }

        return listingRepository.createListing(
            type = type,
            dealType = dealType,
            city = city,
            district = district,
            address = address,
            landmark = landmark,
            latitude = latitude,
            longitude = longitude,
            rooms = rooms,
            floor = floor,
            totalFloors = totalFloors,
            areaSqm = areaSqm,
            price = price,
            currency = currency,
            priceNegotiable = priceNegotiable,
            hasFurniture = hasFurniture,
            hasAppliances = hasAppliances,
            hasInternet = hasInternet,
            hasParking = hasParking,
            hasConditioner = hasConditioner,
            allowsPets = allowsPets,
            allowsChildren = allowsChildren,
            utilitiesIncluded = utilitiesIncluded,
            depositAmount = depositAmount,
            title = title,
            description = description
        )
    }
}
