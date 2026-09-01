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
import com.relatopro.app.ui.components.buttons.PrimaryButton
import com.relatopro.app.ui.components.buttons.TextActionButton
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
    val colors = AppTheme.colors

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
                Text(
                    "Você precisará entrar novamente com sua conta para acessar o aplicativo.\n\nTodos os seus relatórios e fotos salvos permanecerão protegidos.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.statusNaoConforme,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sair da Conta", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextActionButton(
                    text = "Cancelar",
                    onClick = { showLogoutDialog = false }
                )
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
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
                modifier = Modifier.width(285.dp)
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
                                    contentDescription = "Logo Relato Pro",
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Relato Pro",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = colors.textPrimary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu Principal", tint = colors.textPrimary)
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
        currentRoute?.startsWith("dashboard") == true -> "Painel de Controle"
        currentRoute?.startsWith("my_reports") == true -> "Meus Relatórios & Vistorias"
        currentRoute?.startsWith("history") == true -> "Histórico Mensal de Relatórios"
        currentRoute?.startsWith("indicators") == true -> "Indicadores & Conformidade"
        currentRoute?.startsWith("checklists") == true -> "Modelos de Checklist"
        currentRoute?.startsWith("template_builder") == true -> "Criador de Checklist"
        currentRoute?.startsWith("companies") == true -> "Empresas & Unidades"
        currentRoute?.startsWith("profile") == true -> "Meu Perfil"
        currentRoute?.startsWith("settings") == true -> "Configurações do Sistema"
        currentRoute?.startsWith("help") == true -> "Ajuda & Suporte"
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
                modifier = Modifier.height(38.dp)
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
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = com.relatopro.app.R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Relato Pro", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Vistorias & Laudos", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }

        // Dashboard Main Button
        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            val isDashboard = currentRoute == "dashboard"
            val bg = if (isDashboard) PrimaryBlue else Color.White.copy(alpha = 0.06f)
            Button(
                onClick = {
                    navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = false } }
                    onItemClick()
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = bg,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Painel de Controle", color = Color.White, fontSize = 13.sp, fontWeight = if (isDashboard) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Menu Items Grouped Logically
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { SidebarCategory("INSPEÇÕES & CAMPO") }
            item {
                SidebarItem(Icons.Default.AddCircle, "Iniciar Nova Inspeção", false, badge = "Novo") {
                    onNewReportClick()
                    onItemClick()
                }
            }
            item {
                SidebarItem(Icons.AutoMirrored.Filled.ListAlt, "Meus Relatórios", currentRoute?.startsWith("my_reports") == true) {
                    navController.navigate("my_reports?filter=Todos") { popUpTo("dashboard") }
                    onItemClick()
                }
            }
            item {
                SidebarItem(Icons.Default.EditNote, "Rascunhos em Aberto", false) {
                    navController.navigate("my_reports?filter=Rascunhos") { popUpTo("dashboard") }
                    onItemClick()
                }
            }
            item {
                SidebarItem(Icons.AutoMirrored.Filled.Assignment, "Modelos de Checklist", currentRoute?.startsWith("checklists") == true) {
                    navController.navigate("checklists") { popUpTo("dashboard") }
                    onItemClick()
                }
            }
            item {
                SidebarItem(Icons.Default.PlaylistAdd, "Criar Novo Checklist", currentRoute?.startsWith("template_builder") == true) {
                    navController.navigate("template_builder?templateId=0") { popUpTo("dashboard") }
                    onItemClick()
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item { SidebarCategory("GESTÃO & CLIENTES") }
            item {
                SidebarItem(Icons.Default.Business, "Empresas Inspecionadas", currentRoute?.startsWith("companies") == true) {
                    navController.navigate("companies") { popUpTo("dashboard") }
                    onItemClick()
                }
            }
            item {
                SidebarItem(Icons.Default.Analytics, "Indicadores & Gráficos", currentRoute == "indicators") {
                    navController.navigate("indicators") { popUpTo("dashboard") }
                    onItemClick()
                }
            }
            item {
                SidebarItem(Icons.Default.History, "Histórico Mensal", currentRoute == "history") {
                    navController.navigate("history") { popUpTo("dashboard") }
                    onItemClick()
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item { SidebarCategory("SISTEMA") }
            item {
                val isDark = AppTheme.isDark
                SidebarItem(
                    icon = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    title = if (isDark) "Alternar Modo Claro" else "Alternar Modo Escuro",
                    selected = false
                ) {
                    com.relatopro.app.ui.theme.ThemeManager.toggleTheme(context, isDark)
                }
            }
            item {
                SidebarItem(Icons.Default.Person, "Meu Perfil", currentRoute == "profile") {
                    navController.navigate("profile") { popUpTo("dashboard") }
                    onItemClick()
                }
            }
            item {
                SidebarItem(Icons.Default.Settings, "Configurações", currentRoute == "settings") {
                    navController.navigate("settings") { popUpTo("dashboard") }
                    onItemClick()
                }
            }
            item {
                SidebarItem(Icons.AutoMirrored.Filled.HelpOutline, "Ajuda & Suporte", currentRoute == "help") {
                    navController.navigate("help") { popUpTo("dashboard") }
                    onItemClick()
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                SidebarItem(Icons.AutoMirrored.Filled.ExitToApp, "Sair da Conta", false, tint = Color(0xFFF87171)) {
                    onLogoutClick()
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
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
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
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
                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(userRole, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SidebarCategory(title: String) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    badge: String? = null,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val bgColor = if (selected) PrimaryBlue.copy(alpha = 0.25f) else Color.Transparent
    val contentColor = tint ?: if (selected) Color.White else Color.White.copy(alpha = 0.8f)
    val fw = if (selected) FontWeight.Bold else FontWeight.Medium

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
        Text(text = title, color = contentColor, fontSize = 13.sp, fontWeight = fw, modifier = Modifier.weight(1f))
        if (badge != null) {
            Box(
                modifier = Modifier
                    .background(PrimaryBlue, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Barra de Navegação Inferior Mobile Otimizada (5 abas acessíveis)
 * - Início: Dashboard & Ações Rápidas
 * - Relatórios: Lista de vistorias com filtros
 * - + Novo: Botão Central Proeminente
 * - Checklists: Acesso direto a modelos
 * - Mais: Menu de gestão & preferências
 */
@Composable
private fun MobileBottomBar(
    currentRoute: String?,
    navController: NavHostController,
    onMenuClick: () -> Unit = {},
    onNewReportClick: () -> Unit = {}
) {
    val colors = AppTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = colors.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth().height(66.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavIcon(
                    icon = Icons.Default.Home,
                    label = "Início",
                    selected = currentRoute == "dashboard"
                ) {
                    navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = false } }
                }

                BottomNavIcon(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    label = "Relatórios",
                    selected = currentRoute?.startsWith("my_reports") == true
                ) {
                    navController.navigate("my_reports?filter=Todos") { popUpTo("dashboard") }
                }

                Spacer(modifier = Modifier.width(56.dp)) // Espaço para o FAB Central

                BottomNavIcon(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    label = "Checklists",
                    selected = currentRoute?.startsWith("checklists") == true
                ) {
                    navController.navigate("checklists") { popUpTo("dashboard") }
                }

                BottomNavIcon(
                    icon = Icons.Default.Menu,
                    label = "Mais",
                    selected = false
                ) {
                    onMenuClick()
                }
            }
        }

        // Botão Central de Ação Rápida (FAB "+ Novo")
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.primary)
                .clickable { onNewReportClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Iniciar Inspeção",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun BottomNavIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val color = if (selected) colors.primary else colors.textSecondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
