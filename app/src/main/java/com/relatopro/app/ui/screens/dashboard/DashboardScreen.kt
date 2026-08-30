package com.relatopro.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    @Suppress("UNUSED_PARAMETER") onNavigateToTemplateBuilder: () -> Unit,
    onNavigateToFieldMode: (Long) -> Unit,
    onNavigateToMyReports: () -> Unit
) {
    val templates by viewModel.templates.collectAsState()
    var showTemplateSelector by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isDesktop = configuration.screenWidthDp >= 800

    val reportsList by viewModel.reports.collectAsState()
    val totalReports = reportsList.size
    val completedReports = reportsList.count { it.status == "FINALIZED" }
    val drafts = reportsList.count { it.status == "DRAFT" }
    val pendingReports = reportsList.count { it.syncStatus == "PENDING" && it.status == "FINALIZED" }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // METRIC SUMMARY CARDS
            item {
                Spacer(modifier = Modifier.height(24.dp))
                val paddingH = 24.dp
                if (isDesktop) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = paddingH),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryCard(Modifier.weight(1f), "Total de Relatórios", totalReports, "Todos os registros", Icons.AutoMirrored.Filled.ListAlt, PrimaryBlue)
                        SummaryCard(Modifier.weight(1f), "Concluídos", completedReports, "Finalizados", Icons.Default.CheckCircle, StatusConforme)
                        SummaryCard(Modifier.weight(1f), "Rascunhos", drafts, "Em andamento", Icons.Default.Edit, StatusWarning)
                        SummaryCard(Modifier.weight(1f), "Pendentes", pendingReports, "Aguardando envio", Icons.Default.Info, StatusNaoConforme)
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = paddingH),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SummaryCard(Modifier.weight(1f), "Total", totalReports, "Todos os registros", Icons.AutoMirrored.Filled.ListAlt, PrimaryBlue)
                            SummaryCard(Modifier.weight(1f), "Concluídos", completedReports, "Finalizados", Icons.Default.CheckCircle, StatusConforme)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SummaryCard(Modifier.weight(1f), "Rascunhos", drafts, "Em andamento", Icons.Default.Edit, StatusWarning)
                            SummaryCard(Modifier.weight(1f), "Pendentes", pendingReports, "Aguardando envio", Icons.Default.Info, StatusNaoConforme)
                        }
                    }
                }
            }

            // CHARTS AND RECENT ACTIVITY
            item {
                Spacer(modifier = Modifier.height(24.dp))
                val paddingH = 24.dp
                if (isDesktop) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = paddingH), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        DonutChartCard(Modifier.weight(1f), totalReports, completedReports, drafts, pendingReports)
                        RecentActivityCard(Modifier.weight(1.2f), reportsList)
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        DonutChartCard(Modifier.fillMaxWidth(), totalReports, completedReports, drafts, pendingReports)
                        RecentActivityCard(Modifier.fillMaxWidth(), reportsList)
                    }
                }
            }

            // QUICK ACTIONS
            item {
                Spacer(modifier = Modifier.height(24.dp))
                val paddingH = 24.dp
                if (isDesktop) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = paddingH), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        QuickActionsCard(Modifier.weight(1f), { showTemplateSelector = true }, onNavigateToTemplateBuilder, onNavigateToMyReports)
                        Spacer(modifier = Modifier.weight(1.2f))
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        QuickActionsCard(Modifier.fillMaxWidth(), { showTemplateSelector = true }, onNavigateToTemplateBuilder, onNavigateToMyReports)
                    }
                }
            }
        }
    }

    if (showTemplateSelector) {
        AlertDialog(
            onDismissRequest = { showTemplateSelector = false },
            title = { Text("Escolher Modelo") },
            text = {
                if (templates.isEmpty()) {
                    Text("Nenhum Modelo encontrado.")
                } else {
                    LazyColumn {
                        items(templates) { template ->
                            ListItem(
                                headlineContent = { Text(template.name) },
                                modifier = Modifier.clickable {
                                    showTemplateSelector = false
                                    onNavigateToFieldMode(template.id)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplateSelector = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun SummaryCard(modifier: Modifier, title: String, value: Int, subtitle: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Box(
                    modifier = Modifier.size(24.dp).background(iconColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
                }
            }
            Text(value.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
fun DonutChartCard(modifier: Modifier, total: Int, completed: Int, drafts: Int, pending: Int) {
    Card(
        modifier = modifier.height(280.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Text("Relatórios por Status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                    Canvas(modifier = Modifier.size(140.dp)) {
                        val strokeWidth = 36f
                        val t = if (total > 0) total.toFloat() else 1f
                        val cAngle = (completed.toFloat() / t) * 360f
                        val dAngle = (drafts.toFloat() / t) * 360f
                        val pAngle = (pending.toFloat() / t) * 360f
                        
                        var currentAngle = -90f
                        
                        // Green (Concluídos)
                        drawArc(color = StatusConforme, startAngle = currentAngle, sweepAngle = if (total == 0) 360f else cAngle, useCenter = false, style = Stroke(strokeWidth))
                        currentAngle += cAngle
                        
                        // Blue (Rascunhos)
                        if (total > 0) {
                            drawArc(color = PrimaryBlue, startAngle = currentAngle, sweepAngle = dAngle, useCenter = false, style = Stroke(strokeWidth))
                            currentAngle += dAngle
                            
                            // Orange (Pendentes)
                            drawArc(color = StatusWarning, startAngle = currentAngle, sweepAngle = pAngle, useCenter = false, style = Stroke(strokeWidth))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(total.toString(), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = TextPrimary)
                        Text("Total", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.width(32.dp))
                
                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem("Concluídos", "$completed", StatusConforme)
                    LegendItem("Rascunhos", "$drafts", PrimaryBlue)
                    LegendItem("Pendentes", "$pending", StatusWarning)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun LegendItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(70.dp))
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun RecentActivityCard(modifier: Modifier, reports: List<ReportEntity>) {
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
                        
                        RecentRow(report.title.ifEmpty { "Relatório #${report.id}" }, report.location, statusText, statusColor, dateStr)
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun RecentRow(title: String, subtitle: String, statusText: String, statusColor: Color, date: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Description, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, maxLines = 1)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(statusText, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(date, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(90.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}

@Composable
fun QuickActionsCard(
    modifier: Modifier,
    onNewReport: () -> Unit,
    onTemplateBuilder: () -> Unit,
    onMyReports: () -> Unit
) {
    Card(
        modifier = modifier.height(180.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text("Ações Rápidas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickActionItem(Icons.Default.Add, "Novo\nRelatório", onNewReport)
                QuickActionItem(Icons.AutoMirrored.Filled.Assignment, "Modelos\nChecklist", onTemplateBuilder)
                QuickActionItem(Icons.AutoMirrored.Filled.ListAlt, "Meus\nRelatórios", onMyReports)
            }
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = TextPrimary, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
    }
}
