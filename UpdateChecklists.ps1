$mainApp = Get-Content app/src/main/java/com/relatopro/app/ui/screens/MainAppScreen.kt -Raw

$mainApp = $mainApp -replace 'item \{ SidebarItem\(Icons\.AutoMirrored\.Filled\.FactCheck, "Checklists", false\) \{ onItemClick\(\) \} \}', 'item { SidebarItem(Icons.AutoMirrored.Filled.FactCheck, "Checklists", false) { navController.navigate("my_reports?filter=Todos") { popUpTo("dashboard") }; onItemClick() } }'

Set-Content app/src/main/java/com/relatopro/app/ui/screens/MainAppScreen.kt $mainApp
Write-Host "Updated MainAppScreen checklists"
