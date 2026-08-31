package com.relatopro.app.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    navController: NavHostController,
    dashboardViewModel: com.relatopro.app.ui.screens.dashboard.DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onLogout: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isMainRoute = currentRoute?.startsWith("dashboard") == true ||
                      currentRoute?.startsWith("my_reports") == true ||
                      currentRoute?.startsWith("history") == true ||
                      currentRoute?.startsWith("indicators") == true ||
                      currentRoute?.startsWith("checklists") == true ||
                      currentRoute?.startsWith("companies") == true ||
                      currentRoute?.startsWith("template_builder") == true ||
                      currentRoute?.startsWith("settings") == true ||
                      currentRoute?.startsWith("profile") == true ||
                      currentRoute?.startsWith("help") == true

    val configuration = LocalConfiguration.current
    val isDesktop = configuration.screenWidthDp >= 800

    var showNewReportDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val templates by dashboardViewModel.templates.collectAsState()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE) }

    if (showNewReportDialog) {
        NewReportDialog(
            templates = templates,
            onDismiss = { showNewReportDialog = false },
            onTemplateSelected = { templateId: Long ->
                showNewReportDialog = false
                navController.navigate("field_mode/$templateId")
            }
        )
    }

    val colors = AppTheme.colors

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = colors.statusNaoConforme)
                    Spacer(Modifier.width(8.dp))
                    Text("Sair do Relato Pro?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                }
            },
            text = {
                Text("Você precisará entrar novamente com sua conta Google para acessar o aplicativo.\n\nSeus relatórios permanecerão salvos.", fontSize = 13.sp, color = colors.textSecondary, lineHeight = 18.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.statusNaoConforme)
                ) {
                    Text("Sair", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    if (!isMainRoute) {
        content()
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDark = AppTheme.isDark

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isDesktop,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.sidebar,
                modifier = Modifier.width(280.dp)
            ) {
                PermanentSidebar(
                    currentRoute = currentRoute,
                    navController = navController,
                    onNewReportClick = { showNewReportDialog = true },
                    onItemClick = { scope.launch { drawerState.close() } },
                    onLogoutClick = {
                        scope.launch { drawerState.close() }
                        showLogoutDialog = true
                    }
                )
            }
        }
    ) {
        if (isDesktop) {
            Row(modifier = Modifier.fillMaxSize()) {
                PermanentSidebar(
                    currentRoute = currentRoute,
                    navController = navController,
                    modifier = Modifier.width(260.dp),
                    onNewReportClick = { showNewReportDialog = true },
                    onLogoutClick = { showLogoutDialog = true }
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    DesktopTopBar(currentRoute)
                    Box(modifier = Modifier.weight(1f)) {
                        content()
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = colors.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = com.relatopro.app.R.drawable.logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Relato Pro", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.textPrimary)
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    com.relatopro.app.ui.theme.ThemeManager.toggleTheme(context, isDark)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isDark) "Modo Claro" else "Modo Escuro",
                                    tint = if (isDark) Color(0xFFFBBF24) else colors.textPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
                    )
                },
                bottomBar = {
                    MobileBottomBar(
                        currentRoute = currentRoute,
                        navController = navController,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onNewReportClick = { showNewReportDialog = true }
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun DesktopTopBar(currentRoute: String?) {
    val context = LocalContext.current
    val isDark = AppTheme.isDark

    val colors = AppTheme.colors
    val title = when {
        currentRoute?.startsWith("dashboard") == true -> "Dashboard"
        currentRoute?.startsWith("my_reports") == true -> "Meus Relatórios"
        currentRoute?.startsWith("history") == true -> "Histórico de Relatórios"
        currentRoute?.startsWith("indicators") == true -> "Indicadores & Estatísticas"
        currentRoute?.startsWith("checklists") == true -> "Checklists & Modelos"
        currentRoute?.startsWith("template_builder") == true -> "Criar/Editar Checklist"
        currentRoute?.startsWith("profile") == true -> "Meu Perfil"
        currentRoute?.startsWith("settings") == true -> "Configurações"
        currentRoute?.startsWith("help") == true -> "Ajuda e Suporte"
        else -> "Relato Pro"
    }

    val todayFormatted = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(colors.surface)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Theme Toggle Button
            OutlinedButton(
                onClick = {
                    com.relatopro.app.ui.theme.ThemeManager.toggleTheme(context, isDark)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.surfaceVariant,
                    contentColor = colors.textPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFFBBF24) else colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isDark) "Modo Claro" else "Modo Escuro",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .background(colors.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(todayFormatted, color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    HorizontalDivider(color = colors.border, thickness = 1.dp)
}

@Composable
private fun PermanentSidebar(
    currentRoute: String?,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNewReportClick: () -> Unit = {},
    onItemClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("user_name", "")?.ifBlank { "Alexandre Machado" } ?: "Alexandre Machado"
    val userRole = prefs.getString("user_role", "Inspetor Técnico") ?: "Inspetor Técnico"

    val initials = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()

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
            Image(
                painter = painterResource(id = com.relatopro.app.R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
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
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Menu Items
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item { SidebarCategory("RELATÓRIOS") }
            item { SidebarItem(Icons.Default.Add, "Novo Relatório", false) { onNewReportClick(); onItemClick() } }
            item { SidebarItem(Icons.AutoMirrored.Filled.ListAlt, "Meus Relatórios", currentRoute?.startsWith("my_reports") == true) { 
                navController.navigate("my_reports?filter=Todos") { popUpTo("dashboard") } 
                onItemClick()
            } }
            item { SidebarItem(Icons.Default.History, "Histórico Mensal", currentRoute == "history") { 
                navController.navigate("history") { popUpTo("dashboard") } 
                onItemClick() 
            } }
            item { SidebarItem(Icons.Default.Analytics, "Indicadores & Estatísticas", currentRoute == "indicators") { 
                navController.navigate("indicators") { popUpTo("dashboard") } 
                onItemClick() 
            } }
            item { SidebarItem(Icons.Default.Edit, "Rascunhos", false) { navController.navigate("my_reports?filter=Rascunhos") { popUpTo("dashboard") }; onItemClick() } }
            item { SidebarItem(Icons.AutoMirrored.Filled.Send, "Enviados", false) { navController.navigate("my_reports?filter=Enviados") { popUpTo("dashboard") }; onItemClick() } }
            item { SidebarItem(Icons.Default.CheckCircle, "Concluídos", false) { navController.navigate("my_reports?filter=Concluídos") { popUpTo("dashboard") }; onItemClick() } }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { SidebarCategory("EMPRESAS & CLIENTES") }
            item { SidebarItem(Icons.Default.Business, "Empresas Inspecionadas", currentRoute?.startsWith("companies") == true) { 
                navController.navigate("companies") { popUpTo("dashboard") }
                onItemClick() 
            } }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { SidebarCategory("CHECKLISTS") }
            item { SidebarItem(Icons.AutoMirrored.Filled.Assignment, "Meus Checklists & Modelos", currentRoute?.startsWith("checklists") == true) { 
                navController.navigate("checklists") { popUpTo("dashboard") }
                onItemClick() 
            } }
            item { SidebarItem(Icons.Default.Add, "Criar Checklist", currentRoute?.startsWith("template_builder") == true) { 
                navController.navigate("template_builder?templateId=0") { popUpTo("dashboard") }
                onItemClick() 
            } }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { SidebarCategory("CONFIGURAÇÕES") }
            item {
                val isDark = AppTheme.isDark
                SidebarItem(
                    icon = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    title = if (isDark) "Ativar Modo Claro" else "Ativar Modo Escuro",
                    selected = false
                ) {
                    com.relatopro.app.ui.theme.ThemeManager.toggleTheme(context, isDark)
                }
            }
            item { SidebarItem(Icons.Default.Person, "Meu Perfil", currentRoute == "profile") { 
                navController.navigate("profile") { popUpTo("dashboard") }
                onItemClick() 
            } }
            item { SidebarItem(Icons.Default.Settings, "Configurações", currentRoute == "settings") { 
                navController.navigate("settings") { popUpTo("dashboard") }
                onItemClick() 
            } }
            item { SidebarItem(Icons.AutoMirrored.Filled.HelpOutline, "Ajuda e Suporte", currentRoute == "help") { 
                navController.navigate("help") { popUpTo("dashboard") }
                onItemClick() 
            } }

            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SidebarItem(Icons.AutoMirrored.Filled.ExitToApp, "Sair", false) { onLogoutClick() } }
        }
        
        // Footer User Profile
        val customPhotoPath = prefs.getString("user_custom_photo_path", null)
        val googlePhotoUrl = prefs.getString("user_photo_url", null)
        val activeAvatar = customPhotoPath ?: googlePhotoUrl

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Row(
            modifier = Modifier
                .clickable {
                    navController.navigate("profile") { popUpTo("dashboard") }
                    onItemClick()
                }
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                if (!activeAvatar.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = activeAvatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(initials.ifEmpty { "RP" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(userRole, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
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
    val colors = AppTheme.colors

    Box(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).background(colors.surface),
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
            
            BottomNavIcon(Icons.Default.History, "Histórico", currentRoute == "history") {
                navController.navigate("history") { popUpTo("dashboard") }
            }
            BottomNavIcon(Icons.Default.Menu, "Mais", false) { onMenuClick() }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.primary)
                .clickable { onNewReportClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Novo", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun BottomNavIcon(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val color = if (selected) colors.primary else colors.textSecondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
