package com.relatopro.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.BackgroundLight
import com.relatopro.app.ui.theme.BorderColor
import com.relatopro.app.ui.theme.PrimaryBlue
import com.relatopro.app.ui.theme.SidebarDark
import com.relatopro.app.ui.theme.StatusConforme
import com.relatopro.app.ui.theme.StatusNaoConforme
import com.relatopro.app.ui.theme.StatusWarning
import com.relatopro.app.ui.theme.SurfaceWhite
import com.relatopro.app.ui.theme.TextPrimary
import com.relatopro.app.ui.theme.TextSecondary

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

    // Mock data for visual fidelity to the reference image
    val totalReports = 32
    val completedReports = 24
    val drafts = 5
    val pendingReports = 3

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Bottom padding for Mobile Nav Bar
        ) {
            item {
                if (isDesktop) {
                    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)) {
                        Text("Olá, João! 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Aqui está o resumo das suas atividades.", fontSize = 14.sp, color = TextSecondary)
                    }
                } else {
                    // Mobile Custom Header (White)
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = "Logo", tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Relato Pro", color = PrimaryBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.Notifications, contentDescription = "Notificações", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Olá, João! 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Aqui está o resumo das suas atividades.", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            }
            
            // 4 SUMMARY CARDS
            item {
                val paddingH = 24.dp
                if (isDesktop) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = paddingH), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SummaryCard(Modifier.weight(1f), "Relatórios", totalReports, "Total de relatórios", Icons.Default.Description, PrimaryBlue)
                        SummaryCard(Modifier.weight(1f), "Concluídos", completedReports, "Neste período", Icons.Default.CheckCircle, StatusConforme)
                        SummaryCard(Modifier.weight(1f), "Rascunhos", drafts, "Em andamento", Icons.Default.Edit, StatusWarning) // Used Edit as mock
                        SummaryCard(Modifier.weight(1f), "Pendentes", pendingReports, "Aguardando envio", Icons.Default.Info, StatusNaoConforme)
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                Spacer(modifier = Modifier.height(24.dp))
                val paddingH = 24.dp
                if (isDesktop) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = paddingH), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        DonutChartCard(Modifier.weight(1f))
                        RecentActivityCard(Modifier.weight(1.2f))
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        DonutChartCard(Modifier.fillMaxWidth())
                        RecentActivityCard(Modifier.fillMaxWidth())
                    }
                }
            }

            // QUICK ACTIONS & LINE CHART
            item {
                Spacer(modifier = Modifier.height(24.dp))
                val paddingH = 24.dp
                if (isDesktop) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = paddingH), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        QuickActionsCard(Modifier.weight(1f), { showTemplateSelector = true }, onNavigateToMyReports)
                        Spacer(modifier = Modifier.weight(1.2f)) // Placeholder for balance
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        QuickActionsCard(Modifier.fillMaxWidth(), { showTemplateSelector = true }, onNavigateToMyReports)
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
fun DonutChartCard(modifier: Modifier) {
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
                // Thick Donut Chart
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                    Canvas(modifier = Modifier.size(140.dp)) {
                        val strokeWidth = 36f
                        val cAngle = (24f / 32f) * 360f // 75%
                        val dAngle = (5f / 32f) * 360f  // 16%
                        val pAngle = (3f / 32f) * 360f  // 9%
                        
                        var currentAngle = -90f
                        
                        // Green (Concluídos)
                        drawArc(color = StatusConforme, startAngle = currentAngle, sweepAngle = cAngle, useCenter = false, style = Stroke(strokeWidth))
                        currentAngle += cAngle
                        
                        // Blue (Rascunhos)
                        drawArc(color = PrimaryBlue, startAngle = currentAngle, sweepAngle = dAngle, useCenter = false, style = Stroke(strokeWidth))
                        currentAngle += dAngle
                        
                        // Orange (Pendentes)
                        drawArc(color = StatusWarning, startAngle = currentAngle, sweepAngle = pAngle, useCenter = false, style = Stroke(strokeWidth))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("32", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = TextPrimary)
                        Text("Total", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.width(32.dp))
                
                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem("Concluídos", "24 (75%)", StatusConforme)
                    LegendItem("Rascunhos", "5 (16%)", PrimaryBlue)
                    LegendItem("Pendentes", "3 (9%)", StatusWarning)
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
fun RecentActivityCard(modifier: Modifier) {
    Card(
        modifier = modifier.height(280.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Atividade Recente", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Box(modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Ver todos", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mocking the exact rows from the image
            RecentRow("Relatório #1024", "Inspeção de Segurança - Indústria ABC", "Concluído", StatusConforme, "Hoje, 09:30")
            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
            RecentRow("Checklist Manutenção", "Setor Produção", "Rascunho", PrimaryBlue, "Ontem, 15:45")
            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
            RecentRow("Inspeção Operacional", "Área de Logística", "Pendente", StatusWarning, "28/08/2026")
            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
            RecentRow("Relatório #1023", "Visita Técnica - Cliente XYZ", "Concluído", StatusConforme, "28/08/2026")
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
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
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
            Text(date, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(70.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}

@Composable
fun QuickActionsCard(modifier: Modifier, onNewReport: () -> Unit, onMyReports: () -> Unit) {
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
                QuickActionItem(Icons.Default.Create, "Novo\nChecklist") { }
                QuickActionItem(Icons.Default.CameraAlt, "Adicionar\nFoto") { }
                QuickActionItem(Icons.AutoMirrored.Filled.ListAlt, "Ver\nRelatórios", onMyReports)
            }
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.width(60.dp)
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


