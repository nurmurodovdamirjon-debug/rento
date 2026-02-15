package uz.rento.domain.model

/**
 * FavoriteItem — sevimli e'lon domain modeli
 */
data class FavoriteItem(
    val favoriteId: String,
    val listingId: String,
    val title: String,
    val city: String,
    val price: Double,
    val currency: String,
    val rooms: Int?,
    val areaSqm: Double?,
    val imageUrl: String?,
    val status: String,
    val isPremium: Boolean,
    val favoritedAt: String
) {
    /** Formatlangan narx */
    val formattedPrice: String
        get() {
            val formatted = String.format("%,.0f", price).replace(',', ' ')
            return "$formatted $currency"
        }
}

/**
 * Sevimli toggle natijasi
 */
data class FavoriteToggleResult(
    val isFavorite: Boolean,
    val favoritesCount: Int
)
