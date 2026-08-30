$mainApp = Get-Content app/src/main/java/com/relatopro/app/ui/screens/MainAppScreen.kt -Raw

$mainApp = $mainApp -replace 'navController\.navigate\("my_reports"\) \{ popUpTo\("dashboard"\) \}', 'navController.navigate("my_reports?filter=Todos") { popUpTo("dashboard") }'
$mainApp = $mainApp -replace 'item \{ SidebarItem\(Icons\.Default\.Edit, "Rascunhos", false\) \{ onItemClick\(\) \} \}', 'item { SidebarItem(Icons.Default.Edit, "Rascunhos", false) { navController.navigate("my_reports?filter=Rascunhos") { popUpTo("dashboard") }; onItemClick() } }'
$mainApp = $mainApp -replace 'item \{ SidebarItem\(Icons\.AutoMirrored\.Filled\.Send, "Enviados", false\) \{ onItemClick\(\) \} \}', 'item { SidebarItem(Icons.AutoMirrored.Filled.Send, "Enviados", false) { navController.navigate("my_reports?filter=Enviados") { popUpTo("dashboard") }; onItemClick() } }'
$mainApp = $mainApp -replace 'item \{ SidebarItem\(Icons\.Default\.CheckCircle, "Conclu.dos", false\) \{ onItemClick\(\) \} \}', 'item { SidebarItem(Icons.Default.CheckCircle, "Concluídos", false) { navController.navigate("my_reports?filter=Concluídos") { popUpTo("dashboard") }; onItemClick() } }'
$mainApp = $mainApp -replace 'item \{ SidebarItem\(Icons\.AutoMirrored\.Filled\.Assignment, "Modelos de Checklist", false\) \{ onItemClick\(\) \} \}', 'item { SidebarItem(Icons.AutoMirrored.Filled.Assignment, "Modelos de Checklist", currentRoute == "template_builder") { navController.navigate("template_builder") { popUpTo("dashboard") }; onItemClick() } }'
$mainApp = $mainApp -replace 'item \{ SidebarItem\(Icons\.Default\.Checklist, "Modelos", currentRoute == "template_builder"\) \{ \r?\n\s*navController\.navigate\("template_builder"\) \{ popUpTo\("dashboard"\) \} \r?\n\s*onItemClick\(\)\r?\n\s*\} \}', ''

Set-Content app/src/main/java/com/relatopro/app/ui/screens/MainAppScreen.kt $mainApp
Write-Host "Updated MainAppScreen"
