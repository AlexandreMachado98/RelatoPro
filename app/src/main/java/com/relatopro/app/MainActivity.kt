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
import com.relatopro.app.ui.screens.myreports.MyReportsViewModel

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
                    
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                onNavigateToTemplateBuilder = { navController.navigate("template_builder") },
                                onNavigateToFieldMode = { navController.navigate("field_mode") },
                                onNavigateToMyReports = { navController.navigate("my_reports") }
                            )
                        }
                        composable("template_builder") {
                            TemplateBuilderScreen(
                                viewModel = hiltViewModel(),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("field_mode") {
                            FieldModeScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("my_reports") {
                            MyReportsScreen(
                                viewModel = hiltViewModel(),
                                onNavigateBack = { navController.popBackStack() },
                                onOpenPdf = { pdfPath ->
                                    // Logic to open PDF intent can be added here
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
