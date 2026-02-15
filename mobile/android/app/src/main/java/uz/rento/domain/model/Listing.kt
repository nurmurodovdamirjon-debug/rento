package uz.rento.domain.model

/**
 * Listing — e'lon domain modeli
 * DTO dan mustaqil, presentation qatlamida ishlatiladi
 */
data class Listing(
    val id: String,
    val userId: String,
    val type: ListingType,
    val dealType: DealType,
    val city: String,
    val district: String?,
    val address: String?,
    val landmark: String?,
    val latitude: Double?,
    val longitude: Double?,
    val rooms: Int?,
    val floor: Int?,
    val totalFloors: Int?,
    val areaSqm: Double?,
    val price: Double,
    val currency: String,
    val priceNegotiable: Boolean,
    val hasFurniture: Boolean,
    val hasAppliances: Boolean,
    val hasInternet: Boolean,
    val hasParking: Boolean,
    val hasConditioner: Boolean,
    val allowsPets: Boolean,
    val allowsChildren: Boolean,
    val utilitiesIncluded: Boolean,
    val depositAmount: Double?,
    val status: ListingStatus,
    val rejectionReason: String?,
    val isPremium: Boolean,
    val viewsCount: Int,
    val favoritesCount: Int,
    val contactsCount: Int,
    val title: String,
    val description: String?,
    val images: List<ListingImage>,
    val publishedAt: String?,
    val createdAt: String,
    val updatedAt: String
) {
    /** Birinchi (asosiy) rasm URL */
    val mainImageUrl: String?
        get() = images.firstOrNull { it.isMain }?.url
            ?: images.firstOrNull()?.url

    /** Thumbnail URL */
    val thumbnailUrl: String?
        get() = images.firstOrNull { it.isMain }?.thumbnailUrl
            ?: images.firstOrNull()?.thumbnailUrl

    /** Formatlangan narx (masalan: "5 000 000 UZS") */
    val formattedPrice: String
        get() {
            val formatted = String.format("%,.0f", price).replace(',', ' ')
            return "$formatted $currency"
        }

    /** Qavat ma'lumoti ("3/9" yoki null) */
    val floorInfo: String?
        get() = if (floor != null && totalFloors != null) "$floor/$totalFloors" else floor?.toString()

    /** Xonalar soni matni */
    val roomsText: String?
        get() = rooms?.let { "$it xona" }

    /** Maydon matni */
    val areaText: String?
        get() = areaSqm?.let { "${it.toInt()} m²" }

    /** Manzil qisqacha */
    val shortAddress: String
        get() = buildList {
            district?.let { add(it) }
            add(city)
        }.joinToString(", ")

    /** Qulayliklar ro'yxati */
    val amenities: List<String>
        get() = buildList {
            if (hasFurniture) add("Mebel")
            if (hasAppliances) add("Maishiy texnika")
            if (hasInternet) add("Internet")
            if (hasParking) add("Parking")
            if (hasConditioner) add("Konditsioner")
            if (allowsPets) add("Hayvonlar ruxsat")
            if (allowsChildren) add("Bolalar ruxsat")
        }
}

/**
 * ListingImage — rasm modeli
 */
data class ListingImage(
    val id: String,
    val url: String,
    val thumbnailUrl: String?,
    val sortOrder: Int,
    val isMain: Boolean
)

/**
 * ListingType — mulk turi
 */
enum class ListingType(val value: String, val displayName: String) {
    APARTMENT("apartment", "Kvartira"),
    HOUSE("house", "Uy"),
    OFFICE("office", "Ofis"),
    SHOP("shop", "Do'kon"),
    WAREHOUSE("warehouse", "Ombor");

    companion object {
        fun fromValue(value: String): ListingType =
            entries.firstOrNull { it.value == value } ?: APARTMENT
    }
}

/**
 * DealType — shartnoma turi
 */
enum class DealType(val value: String, val displayName: String) {
    RENT("rent", "Ijara"),
    DAILY("daily", "Kunlik");

    companion object {
        fun fromValue(value: String): DealType =
            entries.firstOrNull { it.value == value } ?: RENT
    }
}

/**
 * ListingStatus — e'lon holati
 */
enum class ListingStatus(val value: String, val displayName: String) {
    PENDING("pending", "Kutilmoqda"),
    ACTIVE("active", "Faol"),
    RENTED("rented", "Ijaraga berilgan"),
    REJECTED("rejected", "Rad etilgan"),
    ARCHIVED("archived", "Arxivlangan"),
    EXPIRED("expired", "Muddati tugagan");

    companion object {
        fun fromValue(value: String): ListingStatus =
            entries.firstOrNull { it.value == value } ?: PENDING
    }
}

/**
 * ListingFilter — qidiruv filtrlari
 */
data class ListingFilter(
    val city: String? = null,
    val district: String? = null,
    val type: ListingType? = null,
    val dealType: DealType? = null,
    val roomsMin: Int? = null,
    val roomsMax: Int? = null,
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val currency: String? = null,
    val hasFurniture: Boolean? = null,
    val hasParking: Boolean? = null,
    val allowsPets: Boolean? = null,
    val sort: String? = null,
    val page: Int = 1,
    val perPage: Int = 20
) {
    /** Filter → Map (query params uchun) */
    fun toQueryMap(): Map<String, String> = buildMap {
        city?.let { put("city", it) }
        district?.let { put("district", it) }
        type?.let { put("type", it.value) }
        dealType?.let { put("deal_type", it.value) }
        roomsMin?.let { put("rooms_min", it.toString()) }
        roomsMax?.let { put("rooms_max", it.toString()) }
        priceMin?.let { put("price_min", it.toString()) }
        priceMax?.let { put("price_max", it.toString()) }
        currency?.let { put("currency", it) }
        hasFurniture?.let { put("has_furniture", it.toString()) }
        hasParking?.let { put("has_parking", it.toString()) }
        allowsPets?.let { put("allows_pets", it.toString()) }
        sort?.let { put("sort", it) }
        put("page", page.toString())
        put("per_page", perPage.toString())
    }
}

/**
 * ListingStats — e'lon statistikasi
 */
data class ListingStats(
    val views: Int,
    val favorites: Int,
    val contacts: Int
)
