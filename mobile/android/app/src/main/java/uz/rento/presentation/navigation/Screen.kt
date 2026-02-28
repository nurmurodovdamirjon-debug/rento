package uz.rento.presentation.navigation

/**
 * Screen — ilovadagi barcha navigatsiya yo'llari.
 *
 * Sealed class bilan type-safe navigation ta'minlanadi.
 */
sealed class Screen(val route: String) {

    /** Splash ekrani — ilovaga kirganda birinchi ko'rsatiladi */
    data object Splash : Screen("splash")

    /** Onboarding — yangi foydalanuvchilarga uchta sahifali tanishtirish */
    data object Onboarding : Screen("onboarding")

    /** Login — telefon raqamini kiritish */
    data object Login : Screen("login")

    /** OTP — tasdiqlash kodi kiritish */
    data object Otp : Screen("otp/{phone}") {
        fun createRoute(phone: String): String = "otp/${android.net.Uri.encode(phone)}"
    }

    /** Home — asosiy e'lonlar ro'yxati */
    data object Home : Screen("home")

    /** ListingDetail — e'lon batafsil sahifasi */
    data object ListingDetail : Screen("listing/{id}") {
        fun createRoute(id: String): String = "listing/$id"
    }

    /** CreateListing — yangi e'lon yaratish */
    data object CreateListing : Screen("create_listing")

    /** MyListings — mening e'lonlarim */
    data object MyListings : Screen("my_listings")

    /** Search — qidirish */
    data object Search : Screen("search")

    /** Map — xarita (yaqin atrofdagi e'lonlar) */
    data object Map : Screen("map")

    /** Chat — xabarlar ro'yxati */
    data object Chat : Screen("chat")

    /** ChatRoom — suhbat ekrani */
    data object ChatRoom : Screen("chat/{roomId}") {
        fun createRoute(roomId: String): String = "chat/$roomId"
    }

    /** Profile — foydalanuvchi profili */
    data object Profile : Screen("profile")

    /** EditProfile — profil tahrirlash */
    data object EditProfile : Screen("edit_profile")

    /** Favorites — sevimli e'lonlar */
    data object Favorites : Screen("favorites")

    /** Notifications — bildirishnomalar */
    data object Notifications : Screen("notifications")
}
