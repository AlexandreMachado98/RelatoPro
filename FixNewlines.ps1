$dash = Get-Content app/src/main/java/com/relatopro/app/ui/screens/dashboard/DashboardScreen.kt -Raw
$dash = $dash.Replace("``n", "`r`n")
Set-Content app/src/main/java/com/relatopro/app/ui/screens/dashboard/DashboardScreen.kt $dash

$rep = Get-Content app/src/main/java/com/relatopro/app/ui/screens/myreports/MyReportsScreen.kt -Raw
$rep = $rep.Replace("``n", "`r`n")
Set-Content app/src/main/java/com/relatopro/app/ui/screens/myreports/MyReportsScreen.kt $rep

Write-Host "Fixed newlines"
