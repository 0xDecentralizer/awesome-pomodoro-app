package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.manual.ManualEntryScreen
import com.example.presentation.manual.ManualEntryViewModel
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.stats.StatsScreen
import com.example.presentation.stats.StatsViewModel
import com.example.presentation.timer.TimerScreen
import com.example.presentation.timer.TimerViewModel
import com.example.ui.theme.FocusLogTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import com.example.data.repository.SettingsManager
import javax.inject.Inject
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Timer : Screen("timer", "Timer", Icons.Default.Timer)
    object Logs : Screen("logs", "Log Book", Icons.Default.History)
    object Stats : Screen("stats", "Stats", Icons.Default.TrendingUp)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by settingsManager.appTheme.collectAsState(initial = "SYSTEM")
            val isDarkTheme = when (appTheme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            FocusLogTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val items = listOf(
                    Screen.Timer,
                    Screen.Logs,
                    Screen.Stats,
                    Screen.Settings
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            items.forEach { screen ->
                                val isSelected = currentRoute == screen.route
                                NavigationBarItem(
                                    icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                                    label = { Text(text = screen.title, style = MaterialTheme.typography.labelSmall) },
                                    selected = isSelected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Timer.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Timer.route) {
                            val timerViewModel: TimerViewModel = hiltViewModel()
                            TimerScreen(
                                viewModel = timerViewModel,
                                onNavigateToManualEntry = {
                                    navController.navigate(Screen.Logs.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable(Screen.Logs.route) {
                            val manualViewModel: ManualEntryViewModel = hiltViewModel()
                            ManualEntryScreen(viewModel = manualViewModel)
                        }
                        composable(Screen.Stats.route) {
                            val statsViewModel: StatsViewModel = hiltViewModel()
                            StatsScreen(viewModel = statsViewModel)
                        }
                        composable(Screen.Settings.route) {
                            val settingsViewModel: SettingsViewModel = hiltViewModel()
                            SettingsScreen(viewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }
}
