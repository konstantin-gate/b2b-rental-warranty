@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.presentation.feature.catalog.CatalogScreen
import cz.b2brental.presentation.feature.catalog.EquipmentDetailScreen
import cz.b2brental.presentation.feature.contract.ContractDetailScreen
import cz.b2brental.presentation.feature.contract.ContractsScreen
import cz.b2brental.presentation.feature.contract.CreateContractScreen
import cz.b2brental.presentation.feature.document.PdfViewerScreen
import cz.b2brental.presentation.feature.equipment.MyEquipmentScreen
import org.koin.androidx.compose.koinViewModel

/**
 * Navigační host pro přihlášené uživatele.
 * Obrazovky se přidávají postupně v částech 3-B, 3-C, 3-D.
 * @param navController správce navigace
 * @param profile profil přihlášeného uživatele
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun MainNavHost(
    navController: NavHostController,
    profile: UserProfile,
) {
    NavHost(navController = navController, startDestination = Routes.CATALOG) {
        composable(Routes.CATALOG) {
            val viewModel = koinViewModel<cz.b2brental.presentation.feature.catalog.CatalogViewModel>()
            CatalogScreen(
                profile = profile,
                viewModel = viewModel,
                onEquipmentClick = { equipmentId ->
                    navController.navigate(Routes.equipmentDetail(equipmentId))
                },
                onCreateContract = { selectedIds ->
                    navController.navigate(Routes.contractNew(selectedIds))
                },
            )
        }
        composable(
            route = Routes.EQUIPMENT_DETAIL,
            arguments = listOf(navArgument("equipmentId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val equipmentId = backStackEntry.arguments?.getLong("equipmentId") ?: return@composable
            val viewModel = koinViewModel<cz.b2brental.presentation.feature.catalog.EquipmentDetailViewModel>()
            EquipmentDetailScreen(
                equipmentId = equipmentId,
                viewModel = viewModel,
            )
        }
        composable(
            route = Routes.CONTRACT_NEW,
            arguments = listOf(navArgument("ids") { type = NavType.StringType }),
        ) { backStackEntry ->
            val idsString = backStackEntry.arguments?.getString("ids") ?: return@composable
            val ids = idsString.split(",").mapNotNull { it.toLongOrNull() }
            val viewModel = koinViewModel<cz.b2brental.presentation.feature.contract.CreateContractViewModel>()
            CreateContractScreen(
                ids = ids,
                viewModel = viewModel,
                onContractCreated = { contractId ->
                    navController.navigate(Routes.contractDetail(contractId)) {
                        popUpTo(Routes.CATALOG) { inclusive = false }
                    }
                },
            )
        }
        composable(Routes.CONTRACTS) {
            val viewModel = koinViewModel<cz.b2brental.presentation.feature.contract.ContractsViewModel>()
            ContractsScreen(
                viewModel = viewModel,
                onContractClick = { contractId ->
                    navController.navigate(Routes.contractDetail(contractId))
                },
            )
        }
        composable(
            route = Routes.CONTRACT_DETAIL,
            arguments = listOf(navArgument("contractId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val contractId = backStackEntry.arguments?.getLong("contractId") ?: return@composable
            val viewModel = koinViewModel<cz.b2brental.presentation.feature.contract.ContractDetailViewModel>()
            ContractDetailScreen(
                contractId = contractId,
                profile = profile,
                viewModel = viewModel,
                onPdfRequested = { docId ->
                    navController.navigate(Routes.document(docId))
                },
            )
        }
        composable(Routes.MY_EQUIPMENT) {
            val viewModel = koinViewModel<cz.b2brental.presentation.feature.equipment.MyEquipmentViewModel>()
            MyEquipmentScreen(viewModel = viewModel)
        }
        composable(Routes.REPORT_ISSUE) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Hlášení poruchy (bude v C.2)")
            }
        }
        composable(Routes.TICKETS) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Moje hlášení (bude v C.3)")
            }
        }
        composable(Routes.TICKET_DETAIL) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Detail hlášení (bude v C.3)")
            }
        }
        composable(Routes.TICKET_RESOLVE) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Řešení hlášení (bude v D.3)")
            }
        }
        composable(Routes.PAYMENTS) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Platby (bude v C.4)")
            }
        }
        composable(Routes.DASHBOARD) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Přehled (bude v D.1)")
            }
        }
        composable(Routes.ASSISTANT) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("AI Asistent (bude v D.4)")
            }
        }
        composable(Routes.ADMIN_CATALOG) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Správa katalogu (bude v D.5)")
            }
        }
        composable(
            route = Routes.DOCUMENT,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: return@composable
            val viewModel = koinViewModel<cz.b2brental.presentation.feature.document.PdfViewerViewModel>()
            PdfViewerScreen(
                documentId = documentId,
                viewModel = viewModel,
            )
        }
    }
}
