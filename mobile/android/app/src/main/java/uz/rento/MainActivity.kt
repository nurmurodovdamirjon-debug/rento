package uz.rento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import uz.rento.presentation.navigation.RentoNavGraph
import uz.rento.presentation.theme.RentoTheme

/**
 * MainActivity — ilovaning asosiy Activity'si
 *
 * @AndroidEntryPoint — Hilt DI yoqiladi
 * Edge-to-edge rejim + Compose + Navigation
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RentoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    RentoNavGraph(navController = navController)
                }
            }
        }
    }
}
