@file:Suppress("KDocMissingDocumentation")

package cz.b2brental

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import cz.b2brental.data.remote.SessionEvents
import cz.b2brental.presentation.SessionViewModel
import cz.b2brental.presentation.navigation.AuthNavHost
import cz.b2brental.presentation.navigation.MainNavHost
import cz.b2brental.presentation.theme.B2bTheme
import cz.b2brental.work.SyncScheduler
import org.koin.androidx.compose.koinViewModel

/**
 * Hlavní activity aplikace B2B Rental.
 * Inicializuje Koin DI kontext a nastaví Compose obsah.
 */
public class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    /**
     * Vytvoří obsah activity s Compose tématem a kořenovým Composable.
     * @param savedInstanceState uložený stav instance
     */
    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            val permission: String = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }
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

    // Reset vlajky po novém přihlášení — jinak by opakovaný login po odhlášení zůstal na přihlašovací obrazovce.
    LaunchedEffect(session) {
        if (session != null) forceLogout = false
    }

    if (session != null && !forceLogout) {
        val navController = rememberNavController()
        val context = LocalContext.current
        LaunchedEffect(session) {
            SyncScheduler.scheduleSync(context)
        }
        MainNavHost(
            navController = navController,
            profile = session ?: return,
            onLogout = {
                forceLogout = true
                sessionViewModel.logout()
            },
        )
    } else {
        val navController = rememberNavController()
        AuthNavHost(navController = navController)
    }
}
