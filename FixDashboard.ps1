$content = Get-Content app/src/main/java/com/relatopro/app/ui/screens/dashboard/DashboardScreen.kt -Raw

# 1. Update variables to map from state
$content = $content -replace 'val totalReports = 32', 'val reportsList by viewModel.reports.collectAsState()`n    val totalReports = reportsList.size'
$content = $content -replace 'val completedReports = 24', 'val completedReports = reportsList.count { it.status == "FINALIZED" }'
$content = $content -replace 'val drafts = 5', 'val drafts = reportsList.count { it.status == "DRAFT" }'
$content = $content -replace 'val pendingReports = 3', 'val pendingReports = reportsList.count { it.syncStatus == "PENDING" && it.status == "FINALIZED" }'

# Note: viewModel.reports needs to be added, it seems it already exists as reports in DashboardViewModel
# Wait, let's see if viewModel.reports exists. Yes, in DashboardViewModel: val reports: StateFlow<List<ReportEntity>>

Set-Content app/src/main/java/com/relatopro/app/ui/screens/dashboard/DashboardScreen.kt $content
Write-Host "Updated Dashboard variables"
