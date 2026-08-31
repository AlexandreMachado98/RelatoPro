package com.relatopro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.relatopro.app.ui.screens.dashboard.DashboardScreen
import com.relatopro.app.ui.screens.fieldmode.FieldModeScreen
import com.relatopro.app.ui.screens.templatebuilder.TemplateBuilderScreen
import com.relatopro.app.ui.theme.RelatoProTheme
import dagger.hilt.android.AndroidEntryPoint

import com.relatopro.app.ui.screens.myreports.MyReportsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.relatopro.app.ui.screens.dashboard.DashboardViewModel
import com.relatopro.app.ui.screens.templatebuilder.TemplateBuilderViewModel

import com.relatopro.app.ui.screens.auth.SplashScreen
import com.relatopro.app.ui.screens.auth.LoginScreen
import com.relatopro.app.ui.screens.dashboard.IndicatorsScreen
import com.relatopro.app.ui.screens.settings.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RelatoProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    val handleLogout: () -> Unit = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }

                    com.relatopro.app.ui.screens.MainAppScreen(
                        navController = navController,
                        onLogout = handleLogout
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "splash",
                            enterTransition = {
                                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) +
                                androidx.compose.animation.slideInHorizontally(animationSpec = androidx.compose.animation.core.tween(240)) { width -> width / 4 }
                            },
                            exitTransition = {
                                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) +
                                androidx.compose.animation.slideOutHorizontally(animationSpec = androidx.compose.animation.core.tween(240)) { width -> -width / 4 }
                            },
                            popEnterTransition = {
                                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) +
                                androidx.compose.animation.slideInHorizontally(animationSpec = androidx.compose.animation.core.tween(240)) { width -> -width / 4 }
                            },
                            popExitTransition = {
                                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) +
                                androidx.compose.animation.slideOutHorizontally(animationSpec = androidx.compose.animation.core.tween(240)) { width -> width / 4 }
                            }
                        ) {
                            composable("splash") {
                                SplashScreen(
                                    onNavigateToDashboard = {
                                        navController.navigate("dashboard") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    },
                                    onNavigateToLogin = {
                                        navController.navigate("login") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("login") {
                                LoginScreen(
                                    onNavigateToDashboard = {
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("dashboard") {
                                val viewModel = hiltViewModel<DashboardViewModel>()
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToTemplateBuilder = { navController.navigate("checklists") },
                                    onNavigateToFieldMode = { templateId -> navController.navigate("field_mode/$templateId") },
                                    onNavigateToMyReports = { navController.navigate("my_reports") },
                                    onNavigateToIndicators = { navController.navigate("indicators") }
                                )
                            }
                            composable("checklists") {
                                val viewModel = hiltViewModel<com.relatopro.app.ui.screens.checklists.ChecklistsViewModel>()
                                com.relatopro.app.ui.screens.checklists.ChecklistsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToCreate = { navController.navigate("template_builder?templateId=0") },
                                    onNavigateToEdit = { templateId -> navController.navigate("template_builder?templateId=$templateId") },
                                    onNavigateToStartReport = { templateId -> navController.navigate("field_mode/$templateId") }
                                )
                            }
                            composable(
                                route = "template_builder?templateId={templateId}",
                                arguments = listOf(navArgument("templateId") { type = NavType.LongType; defaultValue = 0L })
                            ) { backStackEntry ->
                                val templateId = backStackEntry.arguments?.getLong("templateId") ?: 0L
                                val viewModel = hiltViewModel<TemplateBuilderViewModel>()
                                TemplateBuilderScreen(
                                    templateId = templateId,
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "field_mode/{templateId}",
                                arguments = listOf(navArgument("templateId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val templateId = backStackEntry.arguments?.getLong("templateId") ?: 1L
                                FieldModeScreen(
                                    templateId = templateId,
                                    viewModel = hiltViewModel(),
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("indicators") {
                                val viewModel = hiltViewModel<com.relatopro.app.ui.screens.dashboard.IndicatorsViewModel>()
                                IndicatorsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "my_reports?filter={filter}",
                                arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "Todos" })
                            ) { backStackEntry ->
                                val filter = backStackEntry.arguments?.getString("filter") ?: "Todos"
                                MyReportsScreen(
                                    viewModel = hiltViewModel(),
                                    initialFilter = filter,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("history") {
                                com.relatopro.app.ui.screens.history.HistoryScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("profile") {
                                ProfileScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    onLogout = handleLogout
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToProfile = { navController.navigate("profile") },
                                    onLogout = handleLogout
                                )
                            }
                            composable("help") {
                                HelpScreen(onNavigateBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }
}
