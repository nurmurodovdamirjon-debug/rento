package uz.rento.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uz.rento.data.local.entity.FavoriteEntity

/**
 * FavoriteDao — sevimli e'lonlar offline cache
 */
@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY favorited_at DESC")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT listing_id FROM favorites")
    suspend fun getFavoriteIds(): List<String>

    @Query("SELECT COUNT(*) FROM favorites WHERE listing_id = :listingId")
    suspend fun isFavorite(listingId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE listing_id = :listingId")
    suspend fun deleteByListingId(listingId: String)

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}
