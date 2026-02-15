package uz.rento.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.rento.data.local.preferences.UserPreferences
import uz.rento.data.remote.api.AuthApi
import uz.rento.data.remote.api.ChatApi
import uz.rento.data.remote.api.ListingApi
import uz.rento.data.remote.api.UserApi
import uz.rento.data.repository.AuthRepositoryImpl
import uz.rento.data.repository.ChatRepositoryImpl
import uz.rento.data.repository.ListingRepositoryImpl
import uz.rento.data.repository.UserRepositoryImpl
import uz.rento.domain.repository.AuthRepository
import uz.rento.domain.repository.ChatRepository
import uz.rento.domain.repository.ListingRepository
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
    fun provideAuthRepository(
        authApi: AuthApi,
        userPreferences: UserPreferences
    ): AuthRepository {
        return AuthRepositoryImpl(authApi, userPreferences)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userApi: UserApi
    ): UserRepository {
        return UserRepositoryImpl(userApi)
    }

    @Provides
    @Singleton
    fun provideListingRepository(
        listingApi: ListingApi
    ): ListingRepository {
        return ListingRepositoryImpl(listingApi)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        chatApi: ChatApi,
        userPreferences: UserPreferences
    ): ChatRepository {
        return ChatRepositoryImpl(chatApi, userPreferences)
    }
}
