package uz.rento.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.rento.data.local.dao.ChatDao
import uz.rento.data.local.dao.FavoriteDao
import uz.rento.data.local.dao.ListingDao
import uz.rento.data.local.dao.UserDao
import uz.rento.data.local.db.RentoDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): RentoDatabase {
        return Room.databaseBuilder(
            context,
            RentoDatabase::class.java,
            RentoDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideListingDao(db: RentoDatabase): ListingDao = db.listingDao()

    @Provides
    @Singleton
    fun provideChatDao(db: RentoDatabase): ChatDao = db.chatDao()

    @Provides
    @Singleton
    fun provideFavoriteDao(db: RentoDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    @Singleton
    fun provideUserDao(db: RentoDatabase): UserDao = db.userDao()
}
