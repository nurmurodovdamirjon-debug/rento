package uz.rento.data.remote.interceptor

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import uz.rento.data.local.preferences.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthInterceptor — har bir API so'rovga Bearer token qo'shadi
 * Token muddati tugagan bo'lsa, refresh token bilen yangilaydi
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val userPreferences: UserPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Token olish (blocking — interceptor thread ichida)
        val accessToken = runBlocking {
            userPreferences.accessToken.first()
        }

        // Token bo'sh bo'lsa, original so'rovni yuborish
        if (accessToken.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        // Bearer token qo'shish
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(authenticatedRequest)

        // 401 — token expired, refresh kerak
        if (response.code == 401) {
            response.close()

            val refreshToken = runBlocking {
                userPreferences.refreshToken.first()
            }

            if (!refreshToken.isNullOrBlank()) {
                // TODO: Sprint 2+ da auto-refresh implementatsiya
                // Hozircha faqat tokenlarni o'chiramiz
                runBlocking {
                    userPreferences.clearTokens()
                }
            }

            return chain.proceed(originalRequest)
        }

        return response
    }
}
