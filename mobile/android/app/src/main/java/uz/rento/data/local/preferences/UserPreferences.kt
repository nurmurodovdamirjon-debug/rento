package uz.rento.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rento_prefs")

/**
 * UserPreferences — DataStore orqali foydalanuvchi ma'lumotlarini saqlash
 * - JWT tokenlar (access + refresh)
 * - Onboarding ko'rilganmi
 * - Foydalanuvchi ID va telefon
 */
@Singleton
class UserPreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_PHONE = stringPreferencesKey("user_phone")
        private val KEY_ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
        private val KEY_IS_DEMO = booleanPreferencesKey("is_demo_mode")
    }

    // ===== Token operatsiyalari =====

    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACCESS_TOKEN]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_REFRESH_TOKEN]
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_PHONE)
            prefs.remove(KEY_IS_DEMO)
        }
    }

    // ===== User ma'lumotlari =====

    val userId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    val userPhone: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_PHONE]
    }

    suspend fun saveUserInfo(userId: String, phone: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_PHONE] = phone
        }
    }

    // ===== Onboarding =====

    val isOnboardingShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_SHOWN] ?: false
    }

    suspend fun setOnboardingShown() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_SHOWN] = true
        }
    }

    // ===== Tizimdan chiqish =====

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[KEY_ACCESS_TOKEN].isNullOrBlank()
    }

    // ===== Demo rejim =====

    val isDemoMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_DEMO] ?: false
    }

    suspend fun saveDemoMode() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = "demo_token"
            prefs[KEY_REFRESH_TOKEN] = "demo_refresh"
            prefs[KEY_USER_ID] = "demo_user"
            prefs[KEY_USER_PHONE] = "+998901234567"
            prefs[KEY_IS_DEMO] = true
            prefs[KEY_ONBOARDING_SHOWN] = true
        }
    }
}
