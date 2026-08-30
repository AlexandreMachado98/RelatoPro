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
import com.relatopro.app.ui.screens.auth.RegisterScreen
import com.relatopro.app.ui.screens.auth.ForgotPasswordScreen
import com.relatopro.app.ui.screens.dashboard.IndicatorsScreen

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
                    
                    com.relatopro.app.ui.screens.MainAppScreen(navController = navController) {
                        NavHost(navController = navController, startDestination = "splash") {
                            composable("splash") {
                                SplashScreen(onNavigateToLogin = { navController.navigate("login") { popUpTo("splash") { inclusive = true } } })
                            }
                            composable("login") {
                                LoginScreen(
                                    onNavigateToDashboard = { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } },
                                    onNavigateToRegister = { navController.navigate("register") },
                                    onNavigateToForgotPassword = { navController.navigate("forgot_password") }
                                )
                            }
                            composable("register") {
                                RegisterScreen(
                                    onNavigateToDashboard = { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } },
                                    onNavigateToLogin = { navController.navigate("login") { popUpTo("register") { inclusive = true } } }
                                )
                            }
                            composable("forgot_password") {
                                ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("dashboard") {
                                val viewModel = hiltViewModel<DashboardViewModel>()
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToTemplateBuilder = { navController.navigate("template_builder") },
                                    onNavigateToFieldMode = { templateId -> navController.navigate("field_mode/$templateId") },
                                    onNavigateToMyReports = { navController.navigate("my_reports") }
                                )
                            }
                            composable("template_builder") {
                                val viewModel = hiltViewModel<TemplateBuilderViewModel>()
                                TemplateBuilderScreen(
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
                                IndicatorsScreen(
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
                            composable("profile") { com.relatopro.app.ui.screens.settings.ProfileScreen { navController.popBackStack() } }
                            composable("settings") { com.relatopro.app.ui.screens.settings.SettingsScreen { navController.popBackStack() } }
                            composable("help") { com.relatopro.app.ui.screens.settings.HelpScreen { navController.popBackStack() } }
                            composable("evidence_gallery") { com.relatopro.app.ui.screens.settings.EvidenceGalleryScreen { navController.popBackStack() } }
                        }
                    }
                }
            }
        }
    }
}

