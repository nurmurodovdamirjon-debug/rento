package uz.rento.data.repository

import uz.rento.data.remote.api.ListingApi
import uz.rento.data.remote.dto.CreateListingRequest
import uz.rento.data.remote.dto.ListingDto
import uz.rento.data.remote.dto.ListingListDto
import uz.rento.data.remote.dto.ListingImageDto
import uz.rento.data.remote.dto.NearbyListingListDto
import uz.rento.data.remote.dto.UpdateListingRequest
import uz.rento.data.remote.dto.UpdateStatusRequest
import uz.rento.domain.model.DealType
import uz.rento.domain.model.Listing
import uz.rento.domain.model.ListingFilter
import uz.rento.domain.model.ListingImage
import uz.rento.domain.model.ListingStats
import uz.rento.domain.model.ListingStatus
import uz.rento.domain.model.ListingType
import uz.rento.domain.model.NearbyFilter
import uz.rento.domain.model.NearbyListing
import uz.rento.domain.model.SearchFilter
import uz.rento.domain.repository.ListingRepository
import uz.rento.domain.repository.ListingsPage
import uz.rento.domain.repository.NearbyListingsPage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListingRepositoryImpl @Inject constructor(
    private val listingApi: ListingApi
) : ListingRepository {

    override suspend fun getListings(filter: ListingFilter): Result<ListingsPage> {
        return try {
            val response = listingApi.getListings(filter.toQueryMap())
            if (response.success && response.data != null) {
                val data = response.data
                Result.success(
                    ListingsPage(
                        items = data.items.map { it.toDomain() },
                        page = data.meta.page,
                        perPage = data.meta.perPage,
                        total = data.meta.total,
                        totalPages = data.meta.totalPages
                    )
                )
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "E'lonlarni olishda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getListing(id: String): Result<Listing> {
        return try {
            val response = listingApi.getListing(id)
            if (response.success && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "E'lonni olishda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createListing(
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
        return try {
            val response = listingApi.createListing(
                CreateListingRequest(
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
            )
            if (response.success && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "E'lon yaratishda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateListing(id: String, updates: Map<String, Any?>): Result<Listing> {
        return try {
            val request = UpdateListingRequest(
                type = updates["type"] as? String,
                dealType = updates["deal_type"] as? String,
                city = updates["city"] as? String,
                district = updates["district"] as? String,
                address = updates["address"] as? String,
                landmark = updates["landmark"] as? String,
                latitude = updates["latitude"] as? Double,
                longitude = updates["longitude"] as? Double,
                rooms = updates["rooms"] as? Int,
                floor = updates["floor"] as? Int,
                totalFloors = updates["total_floors"] as? Int,
                areaSqm = updates["area_sqm"] as? Double,
                price = updates["price"] as? Double,
                currency = updates["currency"] as? String,
                priceNegotiable = updates["price_negotiable"] as? Boolean,
                hasFurniture = updates["has_furniture"] as? Boolean,
                hasAppliances = updates["has_appliances"] as? Boolean,
                hasInternet = updates["has_internet"] as? Boolean,
                hasParking = updates["has_parking"] as? Boolean,
                hasConditioner = updates["has_conditioner"] as? Boolean,
                allowsPets = updates["allows_pets"] as? Boolean,
                allowsChildren = updates["allows_children"] as? Boolean,
                utilitiesIncluded = updates["utilities_included"] as? Boolean,
                depositAmount = updates["deposit_amount"] as? Double,
                title = updates["title"] as? String,
                description = updates["description"] as? String
            )
            val response = listingApi.updateListing(id, request)
            if (response.success && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "E'lon yangilashda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteListing(id: String): Result<Unit> {
        return try {
            val response = listingApi.deleteListing(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("E'lon o'chirishda xatolik"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStatus(id: String, status: String): Result<Unit> {
        return try {
            val response = listingApi.updateStatus(id, UpdateStatusRequest(status))
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Status yangilashda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStats(id: String): Result<ListingStats> {
        return try {
            val response = listingApi.getStats(id)
            if (response.success && response.data != null) {
                Result.success(
                    ListingStats(
                        views = response.data.views,
                        favorites = response.data.favorites,
                        contacts = response.data.contacts
                    )
                )
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Statistika olishda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyListings(filter: ListingFilter): Result<ListingsPage> {
        return try {
            val response = listingApi.getMyListings(filter.toQueryMap())
            if (response.success && response.data != null) {
                val data = response.data
                Result.success(
                    ListingsPage(
                        items = data.items.map { it.toDomain() },
                        page = data.meta.page,
                        perPage = data.meta.perPage,
                        total = data.meta.total,
                        totalPages = data.meta.totalPages
                    )
                )
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "E'lonlarni olishda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchListings(filter: SearchFilter): Result<ListingsPage> {
        return try {
            val response = listingApi.searchListings(filter.toQueryMap())
            if (response.success && response.data != null) {
                val data = response.data
                Result.success(
                    ListingsPage(
                        items = data.items.map { it.toDomain() },
                        page = data.meta.page,
                        perPage = data.meta.perPage,
                        total = data.meta.total,
                        totalPages = data.meta.totalPages
                    )
                )
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Qidiruv xatoligi")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNearbyListings(filter: NearbyFilter): Result<NearbyListingsPage> {
        return try {
            val response = listingApi.getNearbyListings(filter.toQueryMap())
            if (response.success && response.data != null) {
                val data = response.data
                Result.success(
                    NearbyListingsPage(
                        items = data.items.map { it.toDomain() },
                        page = data.meta.page,
                        perPage = data.meta.perPage,
                        total = data.meta.total,
                        totalPages = data.meta.totalPages
                    )
                )
            } else {
                Result.failure(
                    Exception(response.error?.message ?: "Yaqin e'lonlarni olishda xatolik")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ===== Extension Functions: DTO → Domain =====

private fun ListingDto.toDomain(): Listing {
    return Listing(
        id = id,
        userId = userId,
        type = ListingType.fromValue(type),
        dealType = DealType.fromValue(dealType),
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
        status = ListingStatus.fromValue(status),
        rejectionReason = rejectionReason,
        isPremium = isPremium,
        viewsCount = viewsCount,
        favoritesCount = favoritesCount,
        contactsCount = contactsCount,
        title = title,
        description = description,
        images = images.map { it.toDomain() },
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun ListingListDto.toDomain(): Listing {
    return Listing(
        id = id,
        userId = "",
        type = ListingType.fromValue(type),
        dealType = DealType.fromValue(dealType),
        city = city,
        district = district,
        address = address,
        landmark = null,
        latitude = null,
        longitude = null,
        rooms = rooms,
        floor = floor,
        totalFloors = totalFloors,
        areaSqm = areaSqm,
        price = price,
        currency = currency,
        priceNegotiable = priceNegotiable,
        hasFurniture = hasFurniture,
        hasAppliances = false,
        hasInternet = hasInternet,
        hasParking = false,
        hasConditioner = false,
        allowsPets = false,
        allowsChildren = false,
        utilitiesIncluded = false,
        depositAmount = null,
        status = ListingStatus.fromValue(status ?: "active"),
        rejectionReason = null,
        isPremium = isPremium,
        viewsCount = viewsCount,
        favoritesCount = favoritesCount,
        contactsCount = 0,
        title = title,
        description = null,
        images = images.map { it.toDomain() },
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = ""
    )
}

private fun ListingImageDto.toDomain(): ListingImage {
    return ListingImage(
        id = id,
        url = url,
        thumbnailUrl = thumbnailUrl,
        sortOrder = sortOrder,
        isMain = isMain
    )
}

private fun NearbyListingListDto.toDomain(): NearbyListing {
    val listing = Listing(
        id = id,
        userId = "",
        type = ListingType.fromValue(type),
        dealType = DealType.fromValue(dealType),
        city = city,
        district = district,
        address = address,
        landmark = null,
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
        hasAppliances = false,
        hasInternet = hasInternet,
        hasParking = false,
        hasConditioner = false,
        allowsPets = false,
        allowsChildren = false,
        utilitiesIncluded = false,
        depositAmount = null,
        status = ListingStatus.ACTIVE,
        rejectionReason = null,
        isPremium = isPremium,
        viewsCount = viewsCount,
        favoritesCount = favoritesCount,
        contactsCount = 0,
        title = title,
        description = null,
        images = images.map { it.toDomain() },
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = ""
    )
    return NearbyListing(
        listing = listing,
        latitude = latitude,
        longitude = longitude,
        distanceMeters = distanceMeters
    )
}
