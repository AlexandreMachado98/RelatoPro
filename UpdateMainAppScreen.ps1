$main = Get-Content app/src/main/java/com/relatopro/app/ui/screens/MainAppScreen.kt -Raw

# 1. Inject DashboardViewModel and collect templates
$replaceHeader = @"
fun MainAppScreen(
    navController: NavHostController,
    dashboardViewModel: com.relatopro.app.ui.screens.dashboard.DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isMainRoute = currentRoute?.startsWith("dashboard") == true || currentRoute?.startsWith("my_reports") == true || currentRoute?.startsWith("template_builder") == true || currentRoute?.startsWith("evidence_gallery") == true || currentRoute?.startsWith("settings") == true || currentRoute?.startsWith("profile") == true || currentRoute?.startsWith("help") == true

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
                navController.navigate("field_mode/`$templateId")
            }
        )
    }
"@
$main = $main -replace 'fun MainAppScreen\(\s*navController: NavHostController,\s*content: @Composable \(\) -> Unit\s*\)\s*\{[^}]+val isDesktop = configuration\.screenWidthDp >= 800[^\n]*\n', $replaceHeader

# 2. Add showNewReportDialog to the new report buttons
# Floating Button in MobileBottomBar
$replaceFab = @"
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .clip(CircleShape)
                .background(PrimaryBlue)
                .clickable { onNewReportClick() },
            contentAlignment = Alignment.Center
        ) {
"@
$main = $main -replace 'Box\(\s*modifier = Modifier\s*\.align\(Alignment\.TopCenter\)\s*\.size\(56\.dp\)\s*\.clip\(CircleShape\)\s*\.background\(PrimaryBlue\)\s*\.clickable \{ /\* Abre modal de novo relatorio \*/ \},\s*contentAlignment = Alignment\.Center\s*\)\s*\{', $replaceFab

# Update MobileBottomBar signature
$main = $main -replace 'fun MobileBottomBar\(currentRoute: String\?, navController: NavHostController, onMenuClick: \(\) -> Unit = \{\}\)', 'fun MobileBottomBar(currentRoute: String?, navController: NavHostController, onMenuClick: () -> Unit = {}, onNewReportClick: () -> Unit = {})'

# Pass onNewReportClick from MainAppScreen
$main = $main -replace 'MobileBottomBar\(currentRoute = currentRoute, navController = navController\)', 'MobileBottomBar(currentRoute = currentRoute, navController = navController, onNewReportClick = { showNewReportDialog = true })'
$main = $main -replace 'MobileBottomBar\(currentRoute = currentRoute, navController = navController, onMenuClick = \{ scope\.launch \{ drawerState\.open\(\) \} \}\)', 'MobileBottomBar(currentRoute = currentRoute, navController = navController, onMenuClick = { scope.launch { drawerState.open() } }, onNewReportClick = { showNewReportDialog = true })'

# Sidebar "Novo Relatório"
$main = $main -replace 'item \{ SidebarItem\(Icons\.Default\.Add, "Novo Relatório", false\) \{ onItemClick\(\) \} \}', 'item { SidebarItem(Icons.Default.Add, "Novo Relatório", false) { onNewReportClick(); onItemClick() } }'
$main = $main -replace 'item \{ SidebarItem\(Icons\.Default\.Add, "Novo Relat.rio", false\) \{ onItemClick\(\) \} \}', 'item { SidebarItem(Icons.Default.Add, "Novo Relatório", false) { onNewReportClick(); onItemClick() } }'

# Update Sidebar signatures
$main = $main -replace 'fun PermanentSidebar\(currentRoute: String\?, navController: NavHostController, onItemClick: \(\) -> Unit = \{\}\)', 'fun PermanentSidebar(currentRoute: String?, navController: NavHostController, onNewReportClick: () -> Unit = {}, onItemClick: () -> Unit = {})'
$main = $main -replace 'fun DrawerSidebar\(currentRoute: String\?, navController: NavHostController, onItemClick: \(\) -> Unit\)', 'fun DrawerSidebar(currentRoute: String?, navController: NavHostController, onNewReportClick: () -> Unit = {}, onItemClick: () -> Unit)'

# Pass it from MainAppScreen to Sidebars
$main = $main -replace 'PermanentSidebar\(\s*currentRoute = currentRoute,\s*navController = navController\s*\)', 'PermanentSidebar(currentRoute = currentRoute, navController = navController, onNewReportClick = { showNewReportDialog = true })'
$main = $main -replace 'DrawerSidebar\(\s*currentRoute = currentRoute,\s*navController = navController\s*\) \{\s*scope.launch \{\s*drawerState.close\(\)\s*\}\s*\}', 'DrawerSidebar(currentRoute = currentRoute, navController = navController, onNewReportClick = { showNewReportDialog = true }) { scope.launch { drawerState.close() } }'

Set-Content app/src/main/java/com/relatopro/app/ui/screens/MainAppScreen.kt $main
Write-Host "Updated MainAppScreen to inject NewReportDialog"
