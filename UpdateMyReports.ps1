$reportsScreen = Get-Content app/src/main/java/com/relatopro/app/ui/screens/myreports/MyReportsScreen.kt -Raw

$reportsScreen = $reportsScreen -replace 'fun MyReportsScreen\(viewModel: MyReportsViewModel, onNavigateBack: \(\) -> Unit\)', 'fun MyReportsScreen(viewModel: MyReportsViewModel, initialFilter: String = "Todos", onNavigateBack: () -> Unit)'
$reportsScreen = $reportsScreen -replace 'var selectedStatus by remember \{ mutableStateOf\("Todos"\) \}', 'var selectedStatus by remember { mutableStateOf(initialFilter) }'

# Add SENT status mapping (Enviados) to filteredReports
$replaceFilter = @"
        val matchesStatus = when (selectedStatus) {
            "Todos" -> true
            "Concluídos" -> it.status == "FINALIZED"
            "Rascunhos" -> it.status == "DRAFT"
            "Enviados" -> it.status == "SENT"
            else -> true
        }
"@
$reportsScreen = $reportsScreen -replace 'val matchesStatus = when \(selectedStatus\) \{[^}]+\}', $replaceFilter

# Update FilterChipItem row to include Enviados
$replaceChips = @"
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem("Todos", reports.size, selectedStatus == "Todos") { selectedStatus = "Todos" }
                FilterChipItem("Rascunhos", draftsCount, selectedStatus == "Rascunhos") { selectedStatus = "Rascunhos" }
                FilterChipItem("Concluídos", completedCount, selectedStatus == "Concluídos") { selectedStatus = "Concluídos" }
                FilterChipItem("Enviados", reports.count { it.status == "SENT" }, selectedStatus == "Enviados") { selectedStatus = "Enviados" }
            }
"@
$reportsScreen = $reportsScreen -replace 'Row\(\s*modifier = Modifier.fillMaxWidth\(\).horizontalScroll\(rememberScrollState\(\)\),\s*horizontalArrangement = Arrangement.spacedBy\(8.dp\)\s*\)\s*\{[^}]+\}', $replaceChips

# Mark as sent on share
$replaceShare = @"
    val sharePdf = { localPath: String, reportId: Long ->
        val file = File(localPath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "`${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório"))
            viewModel.markAsSent(reportId)
        }
    }
"@
$reportsScreen = $reportsScreen -replace 'val sharePdf = \{ localPath: String ->[^}]+\}', $replaceShare

# Pass reportId to sharePdf in DesktopReportsTable and MobileReportsList
$reportsScreen = $reportsScreen -replace 'onClick = \{ expanded = false; report.pdfLocalPath\?\.let \{ onSharePdf\(it\) \} \}', 'onClick = { expanded = false; report.pdfLocalPath?.let { onSharePdf(it, report.id) } }'
$reportsScreen = $reportsScreen -replace 'fun DesktopReportsTable\([^)]+onSharePdf: \(String\) -> Unit', 'fun DesktopReportsTable(reports: List<ReportEntity>, onOpenPdf: (String) -> Unit, onSharePdf: (String, Long) -> Unit'
$reportsScreen = $reportsScreen -replace 'fun MobileReportsList\([^)]+onSharePdf: \(String\) -> Unit', 'fun MobileReportsList(reports: List<ReportEntity>, onOpenPdf: (String) -> Unit, onSharePdf: (String, Long) -> Unit'

Set-Content app/src/main/java/com/relatopro/app/ui/screens/myreports/MyReportsScreen.kt $reportsScreen
Write-Host "Updated MyReportsScreen"
