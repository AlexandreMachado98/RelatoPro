@file:OptIn(ExperimentalMaterial3Api::class)
package com.relatopro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.relatopro.app.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define main routes that should show the drawer (others will be full screen, like field_mode)
    val isMainRoute = currentRoute in listOf("dashboard", "my_reports", "template_builder")

    if (isMainRoute) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(280.dp),
                    drawerContainerColor = Color.White
                ) {
                    DrawerHeader()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DrawerItem(
                        icon = Icons.Default.Home,
                        label = "Dashboard",
                        selected = currentRoute == "dashboard",
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                    
                    DrawerItem(
                        icon = Icons.Default.ListAlt,
                        label = "Meus Relatórios",
                        selected = currentRoute == "my_reports",
                        onClick = {
                            navController.navigate("my_reports") {
                                popUpTo("dashboard")
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                    
                    DrawerItem(
                        icon = Icons.Default.FactCheck,
                        label = "Modelos (Checklists)",
                        selected = currentRoute == "template_builder",
                        onClick = {
                            navController.navigate("template_builder") {
                                popUpTo("dashboard")
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            // Screen Content wrapped in a scaffold to provide TopAppBar with Hamburger Menu
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(getScreenTitle(currentRoute), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = com.relatopro.app.ui.theme.TextPrimary,
                            navigationIconContentColor = com.relatopro.app.ui.theme.TextPrimary
                        )
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    content()
                }
            }
        }
    } else {
        // Full screen (e.g. Field Mode)
        content()
    }
}

private fun getScreenTitle(route: String?): String {
    return when (route) {
        "dashboard" -> "RelatoPro"
        "my_reports" -> "Meus Relatórios"
        "template_builder" -> "Meus Modelos"
        else -> ""
    }
}

@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryBlue)
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "Logo",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "RelatoPro",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Inspetor logado",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
            selectedIconColor = PrimaryBlue,
            selectedTextColor = PrimaryBlue,
            unselectedIconColor = com.relatopro.app.ui.theme.TextSecondary,
            unselectedTextColor = com.relatopro.app.ui.theme.TextPrimary
        )
    )
}
