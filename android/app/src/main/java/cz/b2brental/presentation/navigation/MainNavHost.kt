@file:Suppress("HardCodedStringLiteral", "KDocMissingDocumentation")

package cz.b2brental.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cz.b2brental.domain.model.UserRole
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.presentation.feature.admin.AdminCatalogScreen
import cz.b2brental.presentation.feature.admin.AdminCatalogViewModel
import cz.b2brental.presentation.feature.assistant.AssistantScreen
import cz.b2brental.presentation.feature.assistant.AssistantViewModel
import cz.b2brental.presentation.feature.catalog.CatalogScreen
import cz.b2brental.presentation.feature.catalog.CatalogViewModel
import cz.b2brental.presentation.feature.catalog.EquipmentDetailScreen
import cz.b2brental.presentation.feature.catalog.EquipmentDetailViewModel
import cz.b2brental.presentation.feature.contract.ContractDetailScreen
import cz.b2brental.presentation.feature.contract.ContractDetailViewModel
import cz.b2brental.presentation.feature.contract.ContractsScreen
import cz.b2brental.presentation.feature.contract.ContractsViewModel
import cz.b2brental.presentation.feature.contract.CreateContractScreen
import cz.b2brental.presentation.feature.contract.CreateContractViewModel
import cz.b2brental.presentation.feature.dashboard.DashboardScreen
import cz.b2brental.presentation.feature.dashboard.DashboardViewModel
import cz.b2brental.presentation.feature.document.PdfViewerScreen
import cz.b2brental.presentation.feature.document.PdfViewerViewModel
import cz.b2brental.presentation.feature.equipment.MyEquipmentScreen
import cz.b2brental.presentation.feature.equipment.MyEquipmentViewModel
import cz.b2brental.presentation.feature.payment.PaymentsScreen
import cz.b2brental.presentation.feature.payment.PaymentsViewModel
import cz.b2brental.presentation.feature.ticket.ReportIssueScreen
import cz.b2brental.presentation.feature.ticket.ReportIssueViewModel
import cz.b2brental.presentation.feature.ticket.ResolveTicketScreen
import cz.b2brental.presentation.feature.ticket.ResolveTicketViewModel
import cz.b2brental.presentation.feature.ticket.TicketDetailScreen
import cz.b2brental.presentation.feature.ticket.TicketDetailViewModel
import cz.b2brental.presentation.feature.ticket.TicketsScreen
import cz.b2brental.presentation.feature.ticket.TicketsViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Navigační host pro přihlášené uživatele.
 * @param navController správce navigace
 * @param profile profil přihlášeného uživatele
 * @param onLogout callback odhlášení (vymaže session)
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun MainNavHost(
    navController: NavHostController,
    profile: UserProfile,
    onLogout: () -> Unit,
) {
    val startDestination: String = startDestinationFor(profile.role)
    Scaffold(
        bottomBar = { B2bBottomNavBar(navController = navController, profile = profile) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.CATALOG) {
                val viewModel: CatalogViewModel = koinViewModel()
                CatalogScreen(
                    profile = profile,
                    viewModel = viewModel,
                    onEquipmentClick = { equipmentId ->
                        navController.navigate(Routes.equipmentDetail(equipmentId))
                    },
                    onCreateContract = { selectedIds ->
                        navController.navigate(Routes.contractNew(selectedIds))
                    },
                    onLogout = onLogout,
                )
            }
            composable(
                route = Routes.EQUIPMENT_DETAIL,
                arguments = listOf(navArgument("equipmentId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val equipmentId: Long = backStackEntry.arguments?.getLong("equipmentId") ?: return@composable
                val viewModel: EquipmentDetailViewModel = koinViewModel()
                EquipmentDetailScreen(equipmentId = equipmentId, viewModel = viewModel)
            }
            composable(
                route = Routes.CONTRACT_NEW,
                arguments = listOf(navArgument("ids") { type = NavType.StringType }),
            ) { backStackEntry ->
                val idsString: String = backStackEntry.arguments?.getString("ids") ?: return@composable
                val ids: List<Long> = idsString.split(",").mapNotNull { it.toLongOrNull() }
                val viewModel: CreateContractViewModel = koinViewModel()
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
                val viewModel: ContractsViewModel = koinViewModel()
                ContractsScreen(
                    viewModel = viewModel,
                    onContractClick = { contractId ->
                        navController.navigate(Routes.contractDetail(contractId))
                    },
                    onLogout = onLogout,
                )
            }
            composable(
                route = Routes.CONTRACT_DETAIL,
                arguments = listOf(navArgument("contractId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val contractId: Long = backStackEntry.arguments?.getLong("contractId") ?: return@composable
                val viewModel: ContractDetailViewModel = koinViewModel()
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
                val viewModel: MyEquipmentViewModel = koinViewModel()
                MyEquipmentScreen(
                    viewModel = viewModel,
                    onReportIssueClick = { equipmentId ->
                        navController.navigate(Routes.reportIssue(equipmentId))
                    },
                    onLogout = onLogout,
                )
            }
            composable(
                route = Routes.REPORT_ISSUE,
                arguments = listOf(navArgument("equipmentId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val equipmentId: Long = backStackEntry.arguments?.getLong("equipmentId") ?: return@composable
                val viewModel: ReportIssueViewModel = koinViewModel()
                ReportIssueScreen(
                    equipmentId = equipmentId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTickets = {
                        navController.navigate(Routes.TICKETS) {
                            popUpTo(Routes.REPORT_ISSUE) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.TICKETS) {
                val viewModel: TicketsViewModel = koinViewModel()
                TicketsScreen(
                    viewModel = viewModel,
                    onTicketClick = { ticketId -> navController.navigate(Routes.ticketDetail(ticketId)) },
                    onLogout = onLogout,
                )
            }
            composable(
                route = Routes.TICKET_DETAIL,
                arguments = listOf(navArgument("ticketId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val ticketId: Long = backStackEntry.arguments?.getLong("ticketId") ?: return@composable
                val viewModel: TicketDetailViewModel = koinViewModel()
                TicketDetailScreen(
                    ticketId = ticketId,
                    profile = profile,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToResolve = { id -> navController.navigate(Routes.ticketResolve(id)) },
                )
            }
            composable(
                route = Routes.TICKET_RESOLVE,
                arguments = listOf(navArgument("ticketId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val ticketId: Long = backStackEntry.arguments?.getLong("ticketId") ?: return@composable
                val viewModel: ResolveTicketViewModel = koinViewModel()
                ResolveTicketScreen(
                    ticketId = ticketId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenReport = { documentId -> navController.navigate(Routes.document(documentId)) },
                )
            }
            composable(Routes.PAYMENTS) {
                val viewModel: PaymentsViewModel = koinViewModel()
                PaymentsScreen(viewModel = viewModel, onLogout = onLogout)
            }
            composable(Routes.DASHBOARD) {
                val viewModel: DashboardViewModel = koinViewModel()
                DashboardScreen(
                    profile = profile,
                    viewModel = viewModel,
                    onOpenAssistant = { navController.navigate(Routes.ASSISTANT) },
                    onOpenAdminCatalog = { navController.navigate(Routes.ADMIN_CATALOG) },
                    onLogout = onLogout,
                )
            }
            composable(Routes.ASSISTANT) {
                val viewModel: AssistantViewModel = koinViewModel()
                AssistantScreen(viewModel = viewModel, onLogout = onLogout)
            }
            composable(Routes.ADMIN_CATALOG) {
                val viewModel: AdminCatalogViewModel = koinViewModel()
                AdminCatalogScreen(viewModel = viewModel, onLogout = onLogout)
            }
            composable(
                route = Routes.DOCUMENT,
                arguments = listOf(navArgument("documentId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val documentId: Long = backStackEntry.arguments?.getLong("documentId") ?: return@composable
                val viewModel: PdfViewerViewModel = koinViewModel()
                PdfViewerScreen(documentId = documentId, viewModel = viewModel)
            }
        }
    }
}

/**
 * Startovní trasa podle role přihlášeného uživatele.
 * @param role role uživatele
 * @return klíč trasy
 */
private fun startDestinationFor(role: UserRole): String = when (role) {
    UserRole.CLIENT -> Routes.CATALOG
    UserRole.MANAGER, UserRole.ADMIN -> Routes.DASHBOARD
    UserRole.TECHNICIAN -> Routes.TICKETS
}
