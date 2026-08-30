@file:OptIn(ExperimentalMaterial3Api::class)
package com.relatopro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import com.relatopro.app.ui.theme.TextPrimary
import com.relatopro.app.ui.theme.TextSecondary
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
                        icon = Icons.AutoMirrored.Filled.ListAlt,
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
                        icon = Icons.AutoMirrored.Filled.FactCheck,
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
                            titleContentColor = TextPrimary,
                            navigationIconContentColor = TextPrimary
                        ),
                        actions = {
                            IconButton(onClick = { /* TODO */ }) {
                                Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = TextSecondary)
                            }
                            IconButton(onClick = { /* TODO */ }) {
                                Icon(Icons.Default.Person, contentDescription = "Perfil", tint = TextSecondary)
                            }
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
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
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextPrimary
        )
    )
}
