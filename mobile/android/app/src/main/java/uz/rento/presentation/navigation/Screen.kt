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
        fun createRoute(phone: String): String = "otp/$phone"
    }

    /** Home — asosiy ekran (hozircha placeholder) */
    data object Home : Screen("home")

    /** Profile — foydalanuvchi profili */
    data object Profile : Screen("profile")

    /** EditProfile — profil tahrirlash */
    data object EditProfile : Screen("edit_profile")
}
