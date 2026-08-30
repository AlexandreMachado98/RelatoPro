package com.relatopro.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
    dashboardViewModel: com.relatopro.app.ui.screens.dashboard.DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isMainRoute = currentRoute?.startsWith("dashboard") == true ||
                      currentRoute?.startsWith("my_reports") == true ||
                      currentRoute?.startsWith("template_builder") == true ||
                      currentRoute?.startsWith("evidence_gallery") == true ||
                      currentRoute?.startsWith("settings") == true ||
                      currentRoute?.startsWith("profile") == true ||
                      currentRoute?.startsWith("help") == true

    val configuration = LocalConfiguration.current
    val isDesktop = configuration.screenWidthDp >= 800

    var showNewReportDialog by remember { mutableStateOf(false) }
    val templates by dashboardViewModel.templates.collectAsState()

    if (showNewReportDialog) {
        NewReportDialog(
            templates = templates,
            onDismiss = { showNewReportDialog = false },
            onTemplateSelected = { templateId ->
                showNewReportDialog = false
                navController.navigate("field_mode/$templateId")
            }
        )
    }

    if (isMainRoute) {
        if (isDesktop) {
            Row(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
                PermanentSidebar(
                    currentRoute = currentRoute,
                    navController = navController,
                    modifier = Modifier.width(260.dp),
                    onNewReportClick = { showNewReportDialog = true }
                )
                
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    DesktopTopBar(currentRoute)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        content()
                    }
                }
            }
        } else {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(280.dp),
                        drawerContainerColor = SidebarDark
                    ) {
                        PermanentSidebar(
                            currentRoute = currentRoute,
                            navController = navController,
                            modifier = Modifier.fillMaxSize(),
                            onNewReportClick = { 
                                scope.launch { drawerState.close() }
                                showNewReportDialog = true 
                            },
                            onItemClick = { scope.launch { drawerState.close() } }
                        )
                    }
                },
                gesturesEnabled = true
            ) {
                Scaffold(
                    containerColor = BackgroundLight,
                    bottomBar = { 
                        MobileBottomBar(
                            currentRoute = currentRoute, 
                            navController = navController,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onNewReportClick = { showNewReportDialog = true }
                        ) 
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    } else {
        content()
    }
}

@Composable
private fun DesktopTopBar(currentRoute: String?) {
    val title = when {
        currentRoute?.startsWith("dashboard") == true -> "Dashboard"
        currentRoute?.startsWith("my_reports") == true -> "Meus Relatórios"
        currentRoute?.startsWith("template_builder") == true -> "Modelos de Checklist"
        currentRoute?.startsWith("evidence_gallery") == true -> "Fotos e Anexos"
        currentRoute?.startsWith("profile") == true -> "Perfil"
        currentRoute?.startsWith("settings") == true -> "Configurações"
        currentRoute?.startsWith("help") == true -> "Ajuda e Suporte"
        else -> "Relato Pro"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(SurfaceWhite)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .background(BackgroundLight, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("28/08/2026 - 30/08/2026", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Box {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notificações", tint = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(16.dp)
                        .background(StatusNaoConforme, CircleShape)
                        .border(2.dp, SurfaceWhite, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("3", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    HorizontalDivider(color = BorderColor, thickness = 1.dp)
}

@Composable
private fun PermanentSidebar(
    currentRoute: String?,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNewReportClick: () -> Unit = {},
    onItemClick: () -> Unit = {}
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
            Image(painterResource(id = com.relatopro.app.R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Relato Pro", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        // Dashboard Button
        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            val isDashboard = currentRoute == "dashboard"
            val bg = if (isDashboard) PrimaryBlue else Color.Transparent
            Button(
                onClick = { 
                    navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = false } } 
                    onItemClick()
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = bg),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Dashboard", color = Color.White, fontSize = 14.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Menu Items
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item { SidebarCategory("RELATÓRIOS") }
            item { SidebarItem(Icons.Default.Add, "Novo Relatório", false) { onNewReportClick(); onItemClick() } }
            item { SidebarItem(Icons.AutoMirrored.Filled.ListAlt, "Meus Relatórios", currentRoute?.startsWith("my_reports") == true) { 
                navController.navigate("my_reports?filter=Todos") { popUpTo("dashboard") } 
                onItemClick()
            } }
            item { SidebarItem(Icons.Default.Edit, "Rascunhos", false) { navController.navigate("my_reports?filter=Rascunhos") { popUpTo("dashboard") }; onItemClick() } }
            item { SidebarItem(Icons.AutoMirrored.Filled.Send, "Enviados", false) { navController.navigate("my_reports?filter=Enviados") { popUpTo("dashboard") }; onItemClick() } }
            item { SidebarItem(Icons.Default.CheckCircle, "Concluídos", false) { navController.navigate("my_reports?filter=Concluídos") { popUpTo("dashboard") }; onItemClick() } }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { SidebarCategory("CHECKLISTS") }
            item { SidebarItem(Icons.AutoMirrored.Filled.FactCheck, "Checklists", false) { navController.navigate("my_reports?filter=Todos") { popUpTo("dashboard") }; onItemClick() } }
            item { SidebarItem(Icons.AutoMirrored.Filled.Assignment, "Modelos de Checklist", currentRoute == "template_builder") { navController.navigate("template_builder") { popUpTo("dashboard") }; onItemClick() } }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SidebarCategory("EVIDÊNCIAS") }
            item { SidebarItem(Icons.Default.CameraAlt, "Fotos e Anexos", currentRoute == "evidence_gallery") { 
                navController.navigate("evidence_gallery") { popUpTo("dashboard") }
                onItemClick() 
            } }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { SidebarCategory("CONFIGURAÇÕES") }
            item { SidebarItem(Icons.Default.Settings, "Configurações", currentRoute == "settings") { 
                navController.navigate("settings") { popUpTo("dashboard") }
                onItemClick() 
            } }
            item { SidebarItem(Icons.Default.Person, "Perfil", currentRoute == "profile") { 
                navController.navigate("profile") { popUpTo("dashboard") }
                onItemClick() 
            } }
            item { SidebarItem(Icons.AutoMirrored.Filled.HelpOutline, "Ajuda e Suporte", currentRoute == "help") { 
                navController.navigate("help") { popUpTo("dashboard") }
                onItemClick() 
            } }
        }
        
        // Footer User Profile
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("João da Silva", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Administrador", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SidebarCategory(title: String) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 11.sp,
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
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, color = contentColor, fontSize = 13.sp, fontWeight = fw)
    }
}

@Composable
private fun MobileBottomBar(
    currentRoute: String?,
    navController: NavHostController,
    onMenuClick: () -> Unit = {},
    onNewReportClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).background(SurfaceWhite),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavIcon(Icons.Default.Home, "Início", currentRoute == "dashboard") {
                navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = false } }
            }
            BottomNavIcon(Icons.AutoMirrored.Filled.ListAlt, "Relatórios", currentRoute?.startsWith("my_reports") == true) {
                navController.navigate("my_reports?filter=Todos") { popUpTo("dashboard") }
            }
            Spacer(modifier = Modifier.width(48.dp))
            
            BottomNavIcon(Icons.Default.Checklist, "Checklists", currentRoute == "template_builder") {
                navController.navigate("template_builder") { popUpTo("dashboard") }
            }
            BottomNavIcon(Icons.Default.Menu, "Mais", false) { onMenuClick() }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .clip(CircleShape)
                .background(PrimaryBlue)
                .clickable { onNewReportClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Novo", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun BottomNavIcon(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) PrimaryBlue else TextSecondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
