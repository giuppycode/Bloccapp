package com.example.bloccapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bloccapp.ui.screen.AccountSettingsScreen
import com.example.bloccapp.ui.screen.AddBlockScreen
import com.example.bloccapp.ui.screen.AppSelectionScreen
import com.example.bloccapp.ui.screen.AuthScreen
import com.example.bloccapp.ui.screen.BlocksScreen
import com.example.bloccapp.ui.screen.DailyUsageScreen
import com.example.bloccapp.ui.screen.ReportsScreen
import com.example.bloccapp.ui.screen.WeeklyReportScreen
import com.example.bloccapp.ui.viewmodel.AddBlockViewModel
import com.example.bloccapp.ui.viewmodel.AuthViewModel

/** Schermate principali con bottom navigation. */
private val bottomNavItems = listOf(
    Screen.Blocks,
    Screen.DailyUsage,
    Screen.Reports,
    Screen.AccountSettings
)

@Composable
fun AppNavGraph(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val isLoggedIn    by authViewModel.isLoggedIn.collectAsStateWithLifecycle()

    val start = if (isLoggedIn) Screen.Blocks.route else Screen.Auth.route

    NavHost(navController = navController, startDestination = start) {

        // ── Auth ─────────────────────────────────────────────────────────────
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Screen.Blocks.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        // ── Blocks (tab 1) ───────────────────────────────────────────────────
        composable(Screen.Blocks.route) {
            MainScaffold(navController) {
                BlocksScreen(
                    onAddBlock = {
                        navController.navigate(Screen.AddBlock.createRoute())
                    }
                )
            }
        }

        // ── Daily Usage (tab 2) ──────────────────────────────────────────────
        composable(Screen.DailyUsage.route) {
            MainScaffold(navController) {
                DailyUsageScreen()
            }
        }

        // ── Reports (tab 3) ──────────────────────────────────────────────────
        composable(Screen.Reports.route) {
            MainScaffold(navController) {
                ReportsScreen(
                    onWeekClick = { label ->
                        navController.navigate(Screen.WeeklyReport.createRoute(label))
                    }
                )
            }
        }

        // ── Account & Settings (tab 4) ───────────────────────────────────────
        composable(Screen.AccountSettings.route) {
            MainScaffold(navController) {
                AccountSettingsScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ── Add Block (sub-screen) ────────────────────────────────────────────
        composable(
            route     = Screen.AddBlock.route,
            arguments = listOf(navArgument(Screen.AddBlock.ARG) { type = NavType.LongType })
        ) { backStackEntry ->
            val addVm: AddBlockViewModel = viewModel(backStackEntry)

            // Leggi i package tornati dall'AppSelectionScreen tramite SavedStateHandle
            val returnedPackages = backStackEntry.savedStateHandle
                .get<ArrayList<String>>("selected_packages")
            LaunchedEffect(returnedPackages) {
                returnedPackages?.let { addVm.setSelectedPackages(it.toList()) }
            }

            AddBlockScreen(
                vm           = addVm,
                onBack       = { navController.popBackStack() },
                onSelectApps = { currentPkgs ->
                    // Passa i package correnti alla schermata di selezione
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("initial_packages", ArrayList(currentPkgs))
                    navController.navigate(Screen.AppSelection.route)
                }
            )
        }

        // ── App Selection (sub-screen) ────────────────────────────────────────
        composable(Screen.AppSelection.route) {
            val prevEntry = navController.previousBackStackEntry
            val initial   = prevEntry?.savedStateHandle
                ?.get<ArrayList<String>>("initial_packages") ?: arrayListOf()

            AppSelectionScreen(
                initialSelectedPackages = initial,
                onBack = { navController.popBackStack() },
                onConfirm = { selected ->
                    prevEntry?.savedStateHandle?.set(
                        "selected_packages", ArrayList(selected)
                    )
                    navController.popBackStack()
                }
            )
        }

        // ── Weekly Report (sub-screen) ────────────────────────────────────────
        composable(
            route     = Screen.WeeklyReport.route,
            arguments = listOf(navArgument(Screen.WeeklyReport.ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString(Screen.WeeklyReport.ARG) ?: ""
            val label   = Screen.WeeklyReport.decodeLabel(encoded)
            WeeklyReportScreen(
                weekLabel = label,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Scaffold condiviso con 4-tab bottom navigation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MainScaffold(
    navController: androidx.navigation.NavHostController,
    content: @Composable () -> Unit
) {
    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.Blocks          -> Icons.Default.Lock
                                    Screen.DailyUsage      -> Icons.Default.BarChart
                                    Screen.Reports         -> Icons.Default.DateRange
                                    Screen.AccountSettings -> Icons.Default.Person
                                    else                   -> Icons.Default.Lock
                                },
                                contentDescription = screen.route
                            )
                        },
                        label    = { Text(screen.tabLabel()) },
                        selected = currentDestination?.hierarchy
                            ?.any { it.route == screen.route } == true,
                        onClick  = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(innerPadding)) {
            content()
        }
    }
}

private fun Screen.tabLabel() = when (this) {
    Screen.Blocks          -> "Blocks"
    Screen.DailyUsage      -> "Usage"
    Screen.Reports         -> "Reports"
    Screen.AccountSettings -> "Account"
    else                   -> ""
}
