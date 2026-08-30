$content = Get-Content app/src/main/java/com/relatopro/app/ui/screens/myreports/MyReportsScreen.kt -Raw

# 1. Pass onDeleteReport to DesktopReportsTable
$content = $content -replace 'fun DesktopReportsTable\(\s*reports: List<ReportEntity>,\s*onOpenPdf: \(String\) -> Unit,\s*onSharePdf: \(String\) -> Unit\s*\)', "fun DesktopReportsTable(`n    reports: List<ReportEntity>,`n    onOpenPdf: (String) -> Unit,`n    onSharePdf: (String) -> Unit,`n    onDeleteReport: (ReportEntity) -> Unit`n)"

# 2. Add onDeleteReport parameter to MobileReportsList
$content = $content -replace 'fun MobileReportsList\(\s*reports: List<ReportEntity>,\s*onOpenPdf: \(String\) -> Unit,\s*onSharePdf: \(String\) -> Unit\s*\)', "fun MobileReportsList(`n    reports: List<ReportEntity>,`n    onOpenPdf: (String) -> Unit,`n    onSharePdf: (String) -> Unit,`n    onDeleteReport: (ReportEntity) -> Unit`n)"

# 3. Replace the Excluir onClick inside the screen
$content = $content -replace 'text = \{ Text\("Excluir", color = StatusNaoConforme\) \},\s*onClick = \{ expanded = false \}', 'text = { Text("Excluir", color = StatusNaoConforme) },`n                                    onClick = { expanded = false; onDeleteReport(report) }'

# 4. Pass the viewmodel function from the main screen calling these
$content = $content -replace 'DesktopReportsTable\(filteredReports, openPdf, sharePdf\)', 'DesktopReportsTable(filteredReports, openPdf, sharePdf) { viewModel.deleteReport(it) }'
$content = $content -replace 'MobileReportsList\(filteredReports, openPdf, sharePdf\)', 'MobileReportsList(filteredReports, openPdf, sharePdf) { viewModel.deleteReport(it) }'

Set-Content app/src/main/java/com/relatopro/app/ui/screens/myreports/MyReportsScreen.kt $content
Write-Host "Updated MyReportsScreen"
