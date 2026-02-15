package uz.rento.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import uz.rento.data.local.entity.ListingEntity
import uz.rento.data.local.entity.ListingImageEntity

/**
 * ListingDao — e'lonlar offline cache uchun
 */
@Dao
interface ListingDao {

    // ==================== QUERY ====================

    /** Barcha cache qilingan e'lonlar (yangi birinchi) */
    @Query("SELECT * FROM listings ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getListings(limit: Int, offset: Int): List<ListingEntity>

    /** Barcha cache qilingan e'lonlar (faqat yangi kesh) */
    @Query("SELECT * FROM listings WHERE cached_at > :expiry ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getListings(expiry: Long, limit: Int, offset: Int): List<ListingEntity>

    /** Bitta e'lon */
    @Query("SELECT * FROM listings WHERE id = :id")
    suspend fun getListingById(id: String): ListingEntity?

    /** Mening e'lonlarim */
    @Query("SELECT * FROM listings WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getMyListings(userId: String, limit: Int, offset: Int): List<ListingEntity>

    /** Shahar bo'yicha */
    @Query("SELECT * FROM listings WHERE city = :city ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getByCity(city: String, limit: Int, offset: Int): List<ListingEntity>

    /** Shahar bo'yicha (faqat yangi kesh) */
    @Query("SELECT * FROM listings WHERE city = :city AND cached_at > :expiry ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getListingsByCity(city: String, expiry: Long, limit: Int, offset: Int): List<ListingEntity>

    /** Status bo'yicha */
    @Query("SELECT * FROM listings WHERE status = :status ORDER BY created_at DESC")
    suspend fun getByStatus(status: String): List<ListingEntity>

    /** Jami listings soni */
    @Query("SELECT COUNT(*) FROM listings")
    suspend fun getCount(): Int

    /** Jami listings soni (faqat yangi kesh) */
    @Query("SELECT COUNT(*) FROM listings WHERE cached_at > :expiry")
    suspend fun getListingsCount(expiry: Long): Int

    /** Mening e'lonlarim soni */
    @Query("SELECT COUNT(*) FROM listings WHERE user_id = :userId")
    suspend fun getMyCount(userId: String): Int

    // ==================== IMAGES ====================

    /** E'lon rasmlari */
    @Query("SELECT * FROM listing_images WHERE listing_id = :listingId ORDER BY sort_order")
    suspend fun getImages(listingId: String): List<ListingImageEntity>

    /** Bir nechta e'lon rasmlari */
    @Query("SELECT * FROM listing_images WHERE listing_id IN (:listingIds) ORDER BY sort_order")
    suspend fun getImagesByListingIds(listingIds: List<String>): List<ListingImageEntity>

    // ==================== INSERT ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: ListingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListings(listings: List<ListingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ListingImageEntity>)

    /** E'lon + rasmlarini birga saqlash */
    @Transaction
    suspend fun insertListingWithImages(listing: ListingEntity, images: List<ListingImageEntity>) {
        insertListing(listing)
        if (images.isNotEmpty()) {
            insertImages(images)
        }
    }

    /** Bir nechta e'lon + rasmlarini birga saqlash */
    @Transaction
    suspend fun insertListingsWithImages(
        listings: List<ListingEntity>,
        images: List<ListingImageEntity>
    ) {
        insertListings(listings)
        if (images.isNotEmpty()) {
            insertImages(images)
        }
    }

    // ==================== DELETE ====================

    @Query("DELETE FROM listings WHERE id = :id")
    suspend fun deleteListing(id: String)

    @Query("DELETE FROM listing_images WHERE listing_id = :listingId")
    suspend fun deleteImages(listingId: String)

    @Transaction
    suspend fun deleteListingWithImages(id: String) {
        deleteImages(id)
        deleteListing(id)
    }

    /** Eski cache ni tozalash (24 soatdan eski) */
    @Query("DELETE FROM listings WHERE cached_at < :cutoff")
    suspend fun deleteOldCache(cutoff: Long)

    @Query("DELETE FROM listing_images WHERE listing_id NOT IN (SELECT id FROM listings)")
    suspend fun deleteOrphanImages()

    /** Barcha cache ni tozalash */
    @Query("DELETE FROM listings")
    suspend fun clearAll()

    @Query("DELETE FROM listing_images")
    suspend fun clearAllImages()

    @Transaction
    suspend fun clearCache() {
        clearAllImages()
        clearAll()
    }
}
