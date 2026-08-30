package com.relatopro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.relatopro.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define main routes that should show the shell (others will be full screen, like field_mode)
    val isMainRoute = currentRoute in listOf("dashboard", "my_reports", "template_builder")

    if (isMainRoute) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isDesktop = maxWidth >= 600.dp

            if (isDesktop) {
                // DESKTOP / TABLET LAYOUT: Permanent Sidebar + Content
                Row(modifier = Modifier.fillMaxSize()) {
                    PermanentSidebar(
                        currentRoute = currentRoute,
                        navController = navController,
                        modifier = Modifier.width(260.dp)
                    )
                    
                    // Main Content Area with TopBar
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(getScreenTitle(currentRoute), fontWeight = FontWeight.Bold, color = TextPrimary) },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                                actions = {
                                    TopBarActions()
                                }
                            )
                        },
                        containerColor = BackgroundLight,
                        modifier = Modifier.weight(1f)
                    ) { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                            content()
                        }
                    }
                }
            } else {
                // MOBILE LAYOUT: Scaffold with Bottom Navigation
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(getScreenTitle(currentRoute), fontWeight = FontWeight.Bold, color = TextPrimary) },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                            actions = {
                                TopBarActions()
                            }
                        )
                    },
                    bottomBar = {
                        MobileBottomBar(currentRoute, navController)
                    },
                    containerColor = BackgroundLight
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    } else {
        // Full screen (e.g. Field Mode)
        content()
    }
}

@Composable
private fun TopBarActions() {
    IconButton(onClick = { /* TODO */ }) {
        Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = TextSecondary)
    }
    IconButton(onClick = { /* TODO */ }) {
        Icon(Icons.Default.Person, contentDescription = "Perfil", tint = TextSecondary)
    }
}

@Composable
private fun PermanentSidebar(
    currentRoute: String?,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(SidebarDark)
    ) {
        // Logo Header
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Relato Pro", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        // Navigation Button equivalent
        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            Button(
                onClick = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Dashboard")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Menu Items
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item { SidebarCategory("RELATÓRIOS") }
            item { SidebarItem(Icons.Default.Add, "Novo Relatório", false) { /* Handled in Dashboard currently */ } }
            item { SidebarItem(Icons.AutoMirrored.Filled.ListAlt, "Meus Relatórios", currentRoute == "my_reports") {
                navController.navigate("my_reports") { popUpTo("dashboard") }
            } }
            item { SidebarItem(Icons.Default.Edit, "Rascunhos", false) {} }
            item { SidebarItem(Icons.Default.Send, "Enviados", false) {} }
            item { SidebarItem(Icons.Default.Check, "Concluídos", false) {} }
            item { SidebarItem(Icons.Default.Build, "Modelos", currentRoute == "template_builder") {
                navController.navigate("template_builder") { popUpTo("dashboard") }
            } }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { SidebarCategory("CHECKLISTS") }
            item { SidebarItem(Icons.Default.Check, "Checklists", false) {} }
            item { SidebarItem(Icons.Default.Build, "Modelos de Checklist", false) {} }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SidebarCategory("EVIDÊNCIAS") }
            item { SidebarItem(Icons.Default.Search, "Fotos e Anexos", false) {} }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { SidebarCategory("CONFIGURAÇÕES") }
            item { SidebarItem(Icons.Default.Settings, "Configurações", false) {} }
            item { SidebarItem(Icons.Default.Person, "Perfil", false) {} }
            item { SidebarItem(Icons.Default.Info, "Ajuda e Suporte", false) {} }
        }
        
        // Footer User Profile
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("João da Silva", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Administrador", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SidebarCategory(title: String) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SidebarItem(icon: ImageVector, title: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) Color.White.copy(alpha = 0.1f) else Color.Transparent
    val contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.7f)
    val fw = if (selected) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, color = contentColor, fontSize = 14.sp, fontWeight = fw)
    }
}

@Composable
private fun MobileBottomBar(currentRoute: String?, navController: NavHostController) {
    NavigationBar(
        containerColor = SurfaceWhite,
        contentColor = TextSecondary,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            selected = currentRoute == "dashboard",
            onClick = { navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = false } } },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                indicatorColor = PrimaryBlue.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Relatórios") },
            label = { Text("Relatórios", fontSize = 10.sp) },
            selected = currentRoute == "my_reports",
            onClick = { navController.navigate("my_reports") { popUpTo("dashboard") } },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                indicatorColor = PrimaryBlue.copy(alpha = 0.1f)
            )
        )
        // Central FAB equivalent
        NavigationBarItem(
            icon = {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Novo", tint = Color.White)
                }
            },
            label = { },
            selected = false,
            onClick = { /* Handled in Dashboard for now */ }
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = "Checklists") },
            label = { Text("Checklists", fontSize = 10.sp) },
            selected = currentRoute == "template_builder",
            onClick = { navController.navigate("template_builder") { popUpTo("dashboard") } },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                indicatorColor = PrimaryBlue.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Mais") },
            label = { Text("Mais", fontSize = 10.sp) },
            selected = false,
            onClick = { }
        )
    }
}

private fun getScreenTitle(route: String?): String {
    return when (route) {
        "dashboard" -> "Dashboard"
        "my_reports" -> "Meus Relatórios"
        "template_builder" -> "Meus Modelos"
        else -> "Relato Pro"
    }
}
