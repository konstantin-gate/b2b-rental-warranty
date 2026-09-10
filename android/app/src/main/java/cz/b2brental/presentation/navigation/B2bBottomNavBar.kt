@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import cz.b2brental.R
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.model.UserRole

/**
 * Bod spodní navigační lišty (BottomBar).
 * @param route cílová trasa
 * @param labelRes ID resource s popiskem
 * @param icon ikona
 */
private data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

/**
 * Spodní navigační lišta — zobrazuje položky podle role přihlášeného uživatele.
 * @param navController správce navigace
 * @param profile profil přihlášeného uživatele
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun B2bBottomNavBar(
    navController: NavHostController,
    profile: UserProfile,
) {
    val items: List<BottomNavItem> = remember(profile.role) { buildItems(profile.role) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute: String? = backStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}

/**
 * Sestaví seznam bodů navigace podle role.
 * @param role role uživatele
 * @return seznam bodů navigace
 */
private fun buildItems(role: UserRole): List<BottomNavItem> = when (role) {
    UserRole.CLIENT -> listOf(
        BottomNavItem(Routes.CATALOG, R.string.nav_catalog, Icons.Filled.Store),
        BottomNavItem(Routes.CONTRACTS, R.string.nav_contracts, Icons.AutoMirrored.Filled.List),
        BottomNavItem(Routes.MY_EQUIPMENT, R.string.nav_my_equipment, Icons.Filled.Inventory),
        BottomNavItem(Routes.TICKETS, R.string.nav_tickets, Icons.Filled.Build),
        BottomNavItem(Routes.PAYMENTS, R.string.nav_payments, Icons.Filled.Payments),
    )

    UserRole.MANAGER -> listOf(
        BottomNavItem(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Assessment),
        BottomNavItem(Routes.CONTRACTS, R.string.nav_contracts, Icons.AutoMirrored.Filled.List),
        BottomNavItem(Routes.TICKETS, R.string.nav_tickets, Icons.Filled.Build),
        BottomNavItem(Routes.ASSISTANT, R.string.nav_assistant, Icons.Filled.SmartToy),
    )

    UserRole.ADMIN -> listOf(
        BottomNavItem(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Assessment),
        BottomNavItem(Routes.CONTRACTS, R.string.nav_contracts, Icons.AutoMirrored.Filled.List),
        BottomNavItem(Routes.TICKETS, R.string.nav_tickets, Icons.Filled.Build),
        BottomNavItem(Routes.ADMIN_CATALOG, R.string.nav_admin_catalog, Icons.Filled.Inventory),
        BottomNavItem(Routes.ASSISTANT, R.string.nav_assistant, Icons.Filled.SmartToy),
    )

    UserRole.TECHNICIAN -> listOf(
        BottomNavItem(Routes.TICKETS, R.string.nav_tickets, Icons.Filled.Build),
    )
}
