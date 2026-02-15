package uz.rento.domain.repository

import uz.rento.domain.model.Listing
import uz.rento.domain.model.ListingFilter
import uz.rento.domain.model.ListingStats
import uz.rento.domain.model.NearbyFilter
import uz.rento.domain.model.NearbyListing
import uz.rento.domain.model.SearchFilter

/**
 * ListingRepository — e'lon operatsiyalari interfeysi
 */
interface ListingRepository {

    /** Barcha e'lonlarni olish (filtr bilan) */
    suspend fun getListings(filter: ListingFilter): Result<ListingsPage>

    /** Bitta e'lonni olish */
    suspend fun getListing(id: String): Result<Listing>

    /** Yangi e'lon yaratish */
    suspend fun createListing(
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
    ): Result<Listing>

    /** E'lonni yangilash (partial) */
    suspend fun updateListing(id: String, updates: Map<String, Any?>): Result<Listing>

    /** E'lonni o'chirish (soft delete) */
    suspend fun deleteListing(id: String): Result<Unit>

    /** E'lon statusini o'zgartirish */
    suspend fun updateStatus(id: String, status: String): Result<Unit>

    /** E'lon statistikasini olish */
    suspend fun getStats(id: String): Result<ListingStats>

    /** Mening e'lonlarim */
    suspend fun getMyListings(filter: ListingFilter): Result<ListingsPage>

    /** Elasticsearch qidiruv */
    suspend fun searchListings(filter: SearchFilter): Result<ListingsPage>

    /** Yaqin atrofdagi e'lonlar (PostGIS) */
    suspend fun getNearbyListings(filter: NearbyFilter): Result<NearbyListingsPage>
}

/**
 * ListingsPage — sahifalangan e'lonlar
 */
data class ListingsPage(
    val items: List<Listing>,
    val page: Int,
    val perPage: Int,
    val total: Int,
    val totalPages: Int
)

/**
 * NearbyListingsPage — sahifalangan yaqin e'lonlar
 */
data class NearbyListingsPage(
    val items: List<NearbyListing>,
    val page: Int,
    val perPage: Int,
    val total: Int,
    val totalPages: Int
)
