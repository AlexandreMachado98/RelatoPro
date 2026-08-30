package com.relatopro.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTemplateBuilder: () -> Unit,
    onNavigateToFieldMode: (Long) -> Unit,
    onNavigateToMyReports: () -> Unit
) {
    val templates by viewModel.templates.collectAsState()
    val reports by viewModel.reports.collectAsState()
    var showTemplateSelector by remember { mutableStateOf(false) }

    val totalReports = reports.size
    val completedReports = reports.count { it.status == "FINALIZED" }
    val drafts = reports.count { it.status == "DRAFT" }
    val pendingReports = reports.count { it.syncStatus == "PENDING" && it.status != "DRAFT" }

    // Usar Layout responsivo em grade
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        val isDesktop = maxWidth >= 600.dp
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column {
                    Text("Bom dia, João! 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Aqui está um resumo das suas atividades.", fontSize = 14.sp, color = TextSecondary)
                }
            }
            
            // 4 CARDS SUMMARY
            item {
                if (isDesktop) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SummaryCard(Modifier.weight(1f), "Relatórios", totalReports, "Total de relatórios", Icons.Default.Description, PrimaryBlue)
                        SummaryCard(Modifier.weight(1f), "Concluídos", completedReports, "Neste período", Icons.Default.CheckCircle, StatusConforme)
                        SummaryCard(Modifier.weight(1f), "Rascunhos", drafts, "Em andamento", Icons.Default.Edit, StatusWarning)
                        SummaryCard(Modifier.weight(1f), "Pendentes", pendingReports, "Aguardando envio", Icons.Default.Info, StatusNaoConforme)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SummaryCard(Modifier.weight(1f), "Relatórios", totalReports, "Total", Icons.Default.Description, PrimaryBlue)
                            SummaryCard(Modifier.weight(1f), "Concluídos", completedReports, "Neste período", Icons.Default.CheckCircle, StatusConforme)
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
                if (isDesktop) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DonutChartCard(Modifier.weight(1f), totalReports, completedReports, drafts, pendingReports)
                        RecentActivityCard(Modifier.weight(1f), reports)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DonutChartCard(Modifier.fillMaxWidth(), totalReports, completedReports, drafts, pendingReports)
                        RecentActivityCard(Modifier.fillMaxWidth(), reports)
                    }
                }
            }

            // QUICK ACTIONS & LINE CHART
            item {
                if (isDesktop) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        QuickActionsCard(Modifier.weight(1f), { showTemplateSelector = true }, onNavigateToMyReports)
                        LineChartCard(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        QuickActionsCard(Modifier.fillMaxWidth(), { showTemplateSelector = true }, onNavigateToMyReports)
                        LineChartCard(Modifier.fillMaxWidth())
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (showTemplateSelector) {
        AlertDialog(
            onDismissRequest = { showTemplateSelector = false },
            title = { Text("Escolher Modelo") },
            text = {
                if (templates.isEmpty()) {
                    Text("Você ainda não criou nenhum Modelo. Vá em 'Meus Checklists' para criar o primeiro!")
                } else {
                    LazyColumn {
                        items(templates) { template ->
                            ListItem(
                                headlineContent = { Text(template.name, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(template.description.ifEmpty { "Sem descrição" }) },
                                modifier = Modifier.clickable {
                                    showTemplateSelector = false
                                    onNavigateToFieldMode(template.id)
                                }
                            )
                            HorizontalDivider(color = BorderColor)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateSelector = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun SummaryCard(modifier: Modifier, title: String, value: Int, subtitle: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun DonutChartCard(modifier: Modifier, total: Int, completed: Int, drafts: Int, pending: Int) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text("Relatórios por Status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val strokeWidth = 24f
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)
                        
                        if (total == 0) {
                            drawArc(color = BorderColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeWidth))
                        } else {
                            val cAngle = (completed.toFloat() / total) * 360f
                            val dAngle = (drafts.toFloat() / total) * 360f
                            val pAngle = (pending.toFloat() / total) * 360f
                            
                            var currentAngle = -90f
                            
                            drawArc(color = StatusConforme, startAngle = currentAngle, sweepAngle = cAngle, useCenter = false, style = Stroke(strokeWidth))
                            currentAngle += cAngle
                            
                            drawArc(color = PrimaryBlue, startAngle = currentAngle, sweepAngle = dAngle, useCenter = false, style = Stroke(strokeWidth))
                            currentAngle += dAngle
                            
                            drawArc(color = StatusWarning, startAngle = currentAngle, sweepAngle = pAngle, useCenter = false, style = Stroke(strokeWidth))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(total.toString(), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextPrimary)
                        Text("Total", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.width(32.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendItem("Concluídos", completed, StatusConforme)
                    LegendItem("Rascunhos", drafts, PrimaryBlue)
                    LegendItem("Pendentes", pending, StatusWarning)
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(80.dp))
        Text(value.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun RecentActivityCard(modifier: Modifier, reports: List<com.relatopro.app.data.local.entity.ReportEntity>) {
    Card(
        modifier = modifier.heightIn(min = 230.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Atividade Recente", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text("Ver todos", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val recent = reports.sortedByDescending { it.date }.take(3)
            if (recent.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sem atividades recentes.", color = TextSecondary)
                }
            } else {
                recent.forEach { report ->
                    val isFinal = report.status == "FINALIZED"
                    val badgeColor = if (isFinal) StatusConforme else StatusWarning
                    val badgeBg = badgeColor.copy(alpha = 0.15f)
                    val badgeText = if (isFinal) "Concluído" else "Rascunho"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(report.title.ifEmpty { "Relatório #${report.id}" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text(report.location, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        
                        Box(
                            modifier = Modifier.background(badgeBg, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun QuickActionsCard(modifier: Modifier, onNewReport: () -> Unit, onMyReports: () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text("Ações Rápidas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickActionItem(Icons.Default.Add, "Novo\nRelatório", onNewReport)
                QuickActionItem(Icons.Default.Create, "Novo\nChecklist", { })
                QuickActionItem(Icons.Default.Add, "Adicionar\nFoto", { })
                QuickActionItem(Icons.AutoMirrored.Filled.ListAlt, "Ver\nRelatórios", onMyReports)
            }
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.size(56.dp).background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = TextPrimary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp)
    }
}

@Composable
fun LineChartCard(modifier: Modifier) {
    Card(
        modifier = modifier.heightIn(min = 180.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text("Relatórios por Período", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val points = listOf(10f, 20f, 15f, 35f, 25f, 40f, 30f)
                    val maxPoint = 40f
                    val stepX = size.width / (points.size - 1)
                    
                    val path = Path()
                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = size.height - (value / maxPoint * size.height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    
                    drawPath(
                        path = path,
                        color = PrimaryBlue,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                    
                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = size.height - (value / maxPoint * size.height)
                        drawCircle(color = PrimaryBlue, radius = 6f, center = Offset(x, y))
                        drawCircle(color = Color.White, radius = 4f, center = Offset(x, y))
                    }
                }
            }
        }
    }
}
