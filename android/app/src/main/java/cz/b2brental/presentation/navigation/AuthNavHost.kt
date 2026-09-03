package cz.b2brental.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cz.b2brental.presentation.feature.auth.LoginScreen
import cz.b2brental.presentation.feature.auth.RegisterScreen

/**
 * Navigační host pro autentizační obrazovky (před přihlášením).
 * @param navController správce navigace
 */
@Composable
public fun AuthNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
    }
}
