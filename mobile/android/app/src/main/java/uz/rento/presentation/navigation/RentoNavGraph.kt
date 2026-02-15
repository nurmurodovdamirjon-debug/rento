package uz.rento.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import uz.rento.presentation.components.BottomNavItem
import uz.rento.presentation.components.RentoBottomNavBar
import uz.rento.presentation.ui.auth.LoginScreen
import uz.rento.presentation.ui.auth.OtpScreen
import uz.rento.presentation.ui.create.CreateListingScreen
import uz.rento.presentation.ui.detail.ListingDetailScreen
import uz.rento.presentation.ui.home.HomeScreen
import uz.rento.presentation.ui.mylistings.MyListingsScreen
import uz.rento.presentation.ui.onboarding.OnboardingScreen
import uz.rento.presentation.ui.map.MapScreen
import uz.rento.presentation.ui.chat.ChatListScreen
import uz.rento.presentation.ui.chat.ChatScreen
import uz.rento.presentation.ui.profile.EditProfileScreen
import uz.rento.presentation.ui.profile.ProfileScreen
import uz.rento.presentation.ui.search.SearchScreen
import uz.rento.presentation.ui.splash.SplashScreen
import uz.rento.presentation.ui.favorites.FavoritesScreen
import uz.rento.presentation.ui.notifications.NotificationsScreen

/**
 * RentoNavGraph — asosiy navigatsiya grafi.
 *
 * Barcha ekranlar orasidagi o'tishlarni boshqaradi.
 * Home, Search, Chat, Profile ekranlarida bottom nav ko'rsatiladi.
 */
@Composable
fun RentoNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Bottom nav ko'rsatiladigan ekranlar
    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.CreateListing.route,
        Screen.Chat.route,
        Screen.Profile.route
    )
    val showBottomNav = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                RentoBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { item ->
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Splash
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Onboarding
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Login
            composable(Screen.Login.route) {
                LoginScreen(
                    onOtpSent = { phone ->
                        navController.navigate(Screen.Otp.createRoute(phone))
                    }
                )
            }

            // OTP
            composable(
                route = Screen.Otp.route,
                arguments = listOf(
                    navArgument("phone") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val phone = backStackEntry.arguments?.getString("phone") ?: ""
                OtpScreen(
                    phone = phone,
                    onVerified = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Home — e'lonlar ro'yxati
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.ListingDetail.createRoute(id))
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToCreate = {
                        navController.navigate(Screen.CreateListing.route)
                    }
                )
            }

            // Listing Detail
            composable(
                route = Screen.ListingDetail.route,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType }
                )
            ) {
                ListingDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Create Listing
            composable(Screen.CreateListing.route) {
                CreateListingScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { id ->
                        navController.navigate(Screen.ListingDetail.createRoute(id)) {
                            popUpTo(Screen.CreateListing.route) { inclusive = true }
                        }
                    }
                )
            }

            // My Listings
            composable(Screen.MyListings.route) {
                MyListingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.ListingDetail.createRoute(id))
                    }
                )
            }

            // Search
            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.ListingDetail.createRoute(id))
                    },
                    onNavigateToMap = {
                        navController.navigate(Screen.Map.route)
                    }
                )
            }

            // Map — yaqin atrofdagi e'lonlar
            composable(Screen.Map.route) {
                MapScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.ListingDetail.createRoute(id))
                    }
                )
            }

            // Chat — xabarlar ro'yxati
            composable(Screen.Chat.route) {
                ChatListScreen(
                    onNavigateToChat = { roomId ->
                        navController.navigate(Screen.ChatRoom.createRoute(roomId))
                    }
                )
            }

            // ChatRoom — suhbat ekrani
            composable(
                route = Screen.ChatRoom.route,
                arguments = listOf(
                    navArgument("roomId") { type = NavType.StringType }
                )
            ) {
                ChatScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Profile
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToEditProfile = {
                        navController.navigate(Screen.EditProfile.route)
                    },
                    onNavigateToFavorites = {
                        navController.navigate(Screen.Favorites.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Edit Profile
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Favorites — sevimlilar
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.ListingDetail.createRoute(id))
                    }
                )
            }

            // Notifications — bildirishnomalar
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * PlaceholderScreen — hali tayyor bo'lmagan ekranlar uchun
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.run { androidx.compose.foundation.layout.height(8.dp) }
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
