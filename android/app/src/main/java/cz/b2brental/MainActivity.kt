package cz.b2brental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import cz.b2brental.data.remote.SessionEvents
import cz.b2brental.presentation.SessionViewModel
import cz.b2brental.presentation.navigation.AuthNavHost
import cz.b2brental.presentation.navigation.MainNavHost
import cz.b2brental.presentation.theme.B2bTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Hlavní activity aplikace B2B Rental.
 * Inicializuje Koin DI kontext a nastaví Compose obsah.
 */
public class MainActivity : ComponentActivity() {

    /**
     * Vytvoří obsah activity s Compose tématem a kořenovým Composable.
     * @param savedInstanceState uložený stav instance
     */
    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        setContent {
            B2bTheme {
                AppRoot()
            }
        }
    }
}

/**
 * Kořenový Composable — přepíná mezi auth a main obrazovkou podle session.
 * Sleduje [SessionEvents.unauthorized] pro vynucené odhlášení při 401.
 */
@Composable
public fun AppRoot() {
    val sessionViewModel: SessionViewModel = koinViewModel()
    val session by sessionViewModel.session.collectAsState()
    var forceLogout by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SessionEvents.unauthorized.collect {
            forceLogout = true
        }
    }

    if (session != null && !forceLogout) {
        val navController = rememberNavController()
        MainNavHost(navController = navController, profile = session ?: return)
    } else {
        val navController = rememberNavController()
        AuthNavHost(navController = navController)
    }
}
