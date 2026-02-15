package uz.rento.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import uz.rento.data.local.dao.ChatDao
import uz.rento.data.local.dao.FavoriteDao
import uz.rento.data.local.dao.ListingDao
import uz.rento.data.local.dao.UserDao
import uz.rento.data.local.entity.ChatRoomEntity
import uz.rento.data.local.entity.FavoriteEntity
import uz.rento.data.local.entity.ListingEntity
import uz.rento.data.local.entity.ListingImageEntity
import uz.rento.data.local.entity.MessageEntity
import uz.rento.data.local.entity.UserEntity

/**
 * RentoDatabase — Room database
 *
 * Offline cache uchun: e'lonlar, chat, sevimlilar, foydalanuvchi profili
 */
@Database(
    entities = [
        ListingEntity::class,
        ListingImageEntity::class,
        ChatRoomEntity::class,
        MessageEntity::class,
        FavoriteEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RentoDatabase : RoomDatabase() {

    abstract fun listingDao(): ListingDao
    abstract fun chatDao(): ChatDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "rento_cache.db"

        /** Cache muddati — 24 soat */
        const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L

        /** Xabar cache muddati — 7 kun */
        const val MESSAGE_CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
