$dashboard = Get-Content app/src/main/java/com/relatopro/app/ui/screens/dashboard/DashboardScreen.kt -Raw

$dashboard = $dashboard -replace 'import androidx\.compose\.runtime\.\*', "import androidx.compose.runtime.*`r`nimport java.text.SimpleDateFormat`r`nimport java.util.Date`r`nimport java.util.Locale"
$dashboard = $dashboard -replace 'import java\.text\.SimpleDateFormat\r?\nimport java\.util\.Date\r?\nimport java\.util\.Locale\r?\n\r?\n@Composable\r?\nfun RecentActivityCard', "@Composable`r`nfun RecentActivityCard"

Set-Content app/src/main/java/com/relatopro/app/ui/screens/dashboard/DashboardScreen.kt $dashboard
Write-Host "Fixed Dashboard activities imports"
