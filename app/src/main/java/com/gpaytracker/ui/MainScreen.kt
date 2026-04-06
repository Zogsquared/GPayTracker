package com.gpaytracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gpaytracker.ui.screens.*
import com.gpaytracker.viewmodel.ExpenseViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard    : Screen("dashboard",    "Home",     Icons.Filled.Home)
    object Transactions : Screen("transactions", "Txns",     Icons.Filled.List)
    object Analytics    : Screen("analytics",    "Stats",    Icons.Filled.BarChart)
    object Summaries    : Screen("summaries",    "History",  Icons.Filled.History)
    object Settings     : Screen("settings",     "Settings", Icons.Filled.Settings)
}

val screens = listOf(
    Screen.Dashboard, Screen.Transactions,
    Screen.Analytics, Screen.Summaries, Screen.Settings
)

@Composable
fun MainScreen(viewModel: ExpenseViewModel, startTab: String = "dashboard") {
    // Wrap everything in the permission gate — main UI only shows once access granted
    PermissionGateScreen {
        var currentScreen by remember {
            mutableStateOf<Screen>(
                when (startTab) {
                    "summaries" -> Screen.Summaries
                    else -> Screen.Dashboard
                }
            )
        }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0D0D1A),
                    tonalElevation = 0.dp
                ) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Color(0xFFFF6B35),
                                selectedTextColor   = Color(0xFFFF6B35),
                                unselectedIconColor = Color(0x66FFFFFF),
                                unselectedTextColor = Color(0x66FFFFFF),
                                indicatorColor      = Color(0x22FF6B35)
                            )
                        )
                    }
                }
            },
            containerColor = Color(0xFF0D0D1A)
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    Screen.Dashboard    -> DashboardScreen(viewModel)
                    Screen.Transactions -> TransactionsScreen(viewModel)
                    Screen.Analytics    -> AnalyticsScreen(viewModel)
                    Screen.Summaries    -> SummariesScreen(viewModel)
                    Screen.Settings     -> SettingsScreen(viewModel)
                }
            }
        }
    }
}
