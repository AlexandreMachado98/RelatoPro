$dashboard = Get-Content app/src/main/java/com/relatopro/app/ui/screens/dashboard/DashboardScreen.kt -Raw

$replaceCall = @"
                if (isDesktop) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = paddingH), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        DonutChartCard(Modifier.weight(1f))
                        RecentActivityCard(Modifier.weight(1.2f), reportsList)
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        DonutChartCard(Modifier.fillMaxWidth())
                        RecentActivityCard(Modifier.fillMaxWidth(), reportsList)
                    }
                }
"@
$dashboard = $dashboard -replace '(?s)if \(isDesktop\) \{.*?RecentActivityCard\(Modifier\.fillMaxWidth\(\)\)\s*\}\s*\}', $replaceCall

$replaceDef = @"
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentActivityCard(modifier: Modifier, reports: List<com.relatopro.app.data.local.entity.ReportEntity>) {
    Card(
        modifier = modifier.height(280.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Atividade Recente", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma atividade registrada.", color = TextSecondary)
                }
            } else {
                LazyColumn {
                    items(reports.sortedByDescending { it.date }.take(4)) { report ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val dateStr = sdf.format(Date(report.date))
                        
                        val statusText = when(report.status) {
                            "DRAFT" -> "Rascunho"
                            "FINALIZED" -> "Concluído"
                            "SENT" -> "Enviado"
                            else -> report.status
                        }
                        
                        val statusColor = when(report.status) {
                            "DRAFT" -> PrimaryBlue
                            "FINALIZED" -> StatusConforme
                            "SENT" -> StatusConforme
                            else -> StatusWarning
                        }
                        
                        RecentRow(report.title, report.location, statusText, statusColor, dateStr)
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
"@
$dashboard = $dashboard -replace '(?s)@Composable\s*fun RecentActivityCard.*?fun RecentRow', "$replaceDef`n`n@Composable`nfun RecentRow"

Set-Content app/src/main/java/com/relatopro/app/ui/screens/dashboard/DashboardScreen.kt $dashboard
Write-Host "Updated Dashboard activities"
