$s = Get-Content app/src/main/java/com/relatopro/app/ui/screens/settings/SettingsScreens.kt -Raw
$s = $s -replace 'import androidx\.compose\.material\.icons\.Icons', "import androidx.compose.material.icons.Icons`r`nimport androidx.compose.material.icons.filled.Person"
Set-Content app/src/main/java/com/relatopro/app/ui/screens/settings/SettingsScreens.kt $s
Write-Host "Imported Person"
