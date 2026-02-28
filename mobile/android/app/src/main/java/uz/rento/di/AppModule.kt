package uz.rento.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.rento.data.local.dao.ChatDao
import uz.rento.data.local.dao.FavoriteDao
import uz.rento.data.local.dao.ListingDao
import uz.rento.data.local.dao.UserDao
import uz.rento.data.local.preferences.UserPreferences
import uz.rento.data.remote.api.AuthApi
import uz.rento.data.remote.api.ChatApi
import uz.rento.data.remote.api.FavoriteApi
import uz.rento.data.remote.api.ListingApi
import uz.rento.data.remote.api.NotificationApi
import uz.rento.data.remote.api.UserApi
import com.google.gson.Gson
import uz.rento.data.repository.AuthRepositoryImpl
import uz.rento.data.repository.ChatRepositoryImpl
import uz.rento.data.repository.FavoriteRepositoryImpl
import uz.rento.data.repository.ListingRepositoryImpl
import uz.rento.data.repository.NotificationRepositoryImpl
import uz.rento.data.repository.UserRepositoryImpl
import uz.rento.domain.repository.AuthRepository
import uz.rento.domain.repository.ChatRepository
import uz.rento.domain.repository.FavoriteRepository
import uz.rento.domain.repository.ListingRepository
import uz.rento.domain.repository.NotificationRepository
import uz.rento.domain.repository.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApi: AuthApi,
        userPreferences: UserPreferences
    ): AuthRepository {
        return AuthRepositoryImpl(authApi, userPreferences)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userApi: UserApi,
        userDao: UserDao
    ): UserRepository {
        return UserRepositoryImpl(userApi, userDao)
    }

    @Provides
    @Singleton
    fun provideListingRepository(
        listingApi: ListingApi,
        listingDao: ListingDao
    ): ListingRepository {
        return ListingRepositoryImpl(listingApi, listingDao)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        chatApi: ChatApi,
        userPreferences: UserPreferences,
        chatDao: ChatDao,
        gson: Gson
    ): ChatRepository {
        return ChatRepositoryImpl(chatApi, userPreferences, chatDao, gson)
    }

    @Provides
    @Singleton
    fun provideFavoriteRepository(
        favoriteApi: FavoriteApi,
        favoriteDao: FavoriteDao
    ): FavoriteRepository {
        return FavoriteRepositoryImpl(favoriteApi, favoriteDao)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationApi: NotificationApi
    ): NotificationRepository {
        return NotificationRepositoryImpl(notificationApi)
    }
}
