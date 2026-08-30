$vm = Get-Content app/src/main/java/com/relatopro/app/ui/screens/myreports/MyReportsViewModel.kt -Raw

$markAsSent = @"
    fun markAsSent(reportId: Long) {
        viewModelScope.launch {
            val report = _reports.value.find { it.id == reportId }
            if (report != null && report.status != "SENT") {
                val updated = report.copy(status = "SENT")
                reportRepository.updateReport(updated)
                // The flow from repository will automatically update _reports
            }
        }
    }
}
"@
$vm = $vm -replace '}\s*$', $markAsSent

Set-Content app/src/main/java/com/relatopro/app/ui/screens/myreports/MyReportsViewModel.kt $vm
Write-Host "Updated MyReportsViewModel"
