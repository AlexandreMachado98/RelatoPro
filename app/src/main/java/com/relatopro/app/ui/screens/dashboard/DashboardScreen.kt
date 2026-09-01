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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.ui.components.buttons.PrimaryButton
import com.relatopro.app.ui.components.buttons.QuickActionCard
import com.relatopro.app.ui.components.buttons.TextActionButton
import com.relatopro.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToChecklists: () -> Unit,
    onNavigateToFieldMode: (Long, Long?) -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToIndicators: () -> Unit = {},
    onNavigateToCompanies: () -> Unit = {},
    onNavigateToNewChecklist: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val templates by viewModel.templates.collectAsState()
    var showTemplateSelector by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isDesktop = configuration.screenWidthDp >= 800

    val reportsList by viewModel.reports.collectAsState()
    val totalReports = reportsList.size
    val completedReports = reportsList.count { it.status == "FINALIZED" }
    val drafts = reportsList.count { it.status == "DRAFT" }
    val pendingReports = reportsList.count { it.syncStatus == "PENDING" && it.status == "FINALIZED" }

    val latestDraft = remember(reportsList) {
        reportsList.filter { it.status == "DRAFT" }.maxByOrNull { it.date }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. SMART ACTIVE DRAFT BANNER (Se houver rascunho em andamento)
            if (latestDraft != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    val draftDateStr = remember(latestDraft.date) {
                        SimpleDateFormat("dd/MM 'às' HH:mm", Locale.getDefault()).format(Date(latestDraft.date))
                    }
                    val companyName = latestDraft.companyName.ifBlank { "Empresa não informada" }
                    val draftTitle = latestDraft.title.ifBlank { "Vistoria Geral" }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, colors.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(colors.primary, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Inspeção em Andamento", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primary)
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(colors.statusWarning.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Rascunho", color = colors.statusWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text("$draftTitle • $companyName", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1)
                                    Text("Última alteração: $draftDateStr", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = { onNavigateToFieldMode(latestDraft.templateId, latestDraft.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Continuar", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }

            // 2. METRIC SUMMARY CARDS
            item {
                if (latestDraft == null) Spacer(modifier = Modifier.height(16.dp))
                val paddingH = 20.dp
                if (isDesktop) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = paddingH),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryCard(Modifier.weight(1f), "Total de Relatórios", totalReports, "Todos os registros", Icons.AutoMirrored.Filled.ListAlt, colors.primary)
                        SummaryCard(Modifier.weight(1f), "Concluídos", completedReports, "Laudos finalizados", Icons.Default.CheckCircle, colors.statusConforme)
                        SummaryCard(Modifier.weight(1f), "Rascunhos", drafts, "Em andamento", Icons.Default.Edit, colors.statusWarning)
                        SummaryCard(Modifier.weight(1f), "Pendentes", pendingReports, "Aguardando envio", Icons.Default.Info, colors.statusNaoConforme)
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = paddingH),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryCard(Modifier.weight(1f), "Total", totalReports, "Todos os registros", Icons.AutoMirrored.Filled.ListAlt, colors.primary)
                            SummaryCard(Modifier.weight(1f), "Concluídos", completedReports, "Finalizados", Icons.Default.CheckCircle, colors.statusConforme)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryCard(Modifier.weight(1f), "Rascunhos", drafts, "Em andamento", Icons.Default.Edit, colors.statusWarning)
                            SummaryCard(Modifier.weight(1f), "Pendentes", pendingReports, "Aguardando envio", Icons.Default.Info, colors.statusNaoConforme)
                        }
                    }
                }
            }

            // 3. AÇÕES RÁPIDAS (ATALHOS DE 1 TOQUE)
            item {
                val paddingH = 20.dp
                Column(modifier = Modifier.padding(horizontal = paddingH)) {
                    Text(
                        "Ações Rápidas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isDesktop) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            QuickActionCard("Nova Inspeção", "Iniciar vistoria em campo", Icons.Default.AddCircle, colors.primary.copy(alpha = 0.15f), colors.primary, { showTemplateSelector = true }, Modifier.weight(1f))
                            QuickActionCard("Modelos de Checklist", "Gerenciar formulários", Icons.AutoMirrored.Filled.Assignment, colors.cyanAccent.copy(alpha = 0.15f), colors.cyanAccent, onNavigateToChecklists, Modifier.weight(1f))
                            QuickActionCard("Meus Relatórios", "Ver laudos e PDFs", Icons.AutoMirrored.Filled.ListAlt, colors.statusConforme.copy(alpha = 0.15f), colors.statusConforme, onNavigateToMyReports, Modifier.weight(1f))
                            QuickActionCard("Empresas & Clientes", "Unidades inspecionadas", Icons.Default.Business, colors.statusWarning.copy(alpha = 0.15f), colors.statusWarning, onNavigateToCompanies, Modifier.weight(1f))
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickActionCard("Nova Inspeção", "Iniciar em campo", Icons.Default.AddCircle, colors.primary.copy(alpha = 0.15f), colors.primary, { showTemplateSelector = true }, Modifier.weight(1f))
                            QuickActionCard("Checklists", "Modelos ativos", Icons.AutoMirrored.Filled.Assignment, colors.cyanAccent.copy(alpha = 0.15f), colors.cyanAccent, onNavigateToChecklists, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickActionCard("Meus Relatórios", "Laudos & PDFs", Icons.AutoMirrored.Filled.ListAlt, colors.statusConforme.copy(alpha = 0.15f), colors.statusConforme, onNavigateToMyReports, Modifier.weight(1f))
                            QuickActionCard("Empresas", "Clientes cadastrados", Icons.Default.Business, colors.statusWarning.copy(alpha = 0.15f), colors.statusWarning, onNavigateToCompanies, Modifier.weight(1f))
                        }
                    }
                }
            }

            // 4. CHARTS AND RECENT ACTIVITY
            item {
                val paddingH = 20.dp
                if (isDesktop) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = paddingH), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        DonutChartCard(Modifier.weight(1f), totalReports, completedReports, drafts, pendingReports)
                        RecentActivityCard(Modifier.weight(1.2f), reportsList)
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        DonutChartCard(Modifier.fillMaxWidth(), totalReports, completedReports, drafts, pendingReports)
                        RecentActivityCard(Modifier.fillMaxWidth(), reportsList)
                    }
                }
            }
        }
    }

    if (showTemplateSelector) {
        AlertDialog(
            onDismissRequest = { showTemplateSelector = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Checklist, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Selecionar Checklist", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textPrimary)
                }
            },
            text = {
                if (templates.isEmpty()) {
                    Text("Nenhum modelo de checklist encontrado.", color = colors.textSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(templates) { template ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showTemplateSelector = false
                                        onNavigateToFieldMode(template.id, null)
                                    },
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(template.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
                                        if (template.description.isNotBlank()) {
                                            Text(template.description, fontSize = 11.sp, color = colors.textSecondary, maxLines = 1)
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextActionButton(text = "Cancelar", onClick = { showTemplateSelector = false })
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SummaryCard(modifier: Modifier, title: String, value: Int, subtitle: String, icon: ImageVector, iconColor: Color) {
    val colors = AppTheme.colors
    Card(
        modifier = modifier.height(115.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = colors.textSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Box(
                    modifier = Modifier.size(26.dp).background(iconColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(15.dp))
                }
            }
            Text(value.toString(), color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text(subtitle, color = colors.textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
fun DonutChartCard(modifier: Modifier, total: Int, completed: Int, drafts: Int, pending: Int) {
    val colors = AppTheme.colors
    Card(
        modifier = modifier.height(280.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Text("Relatórios por Status", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                    val trackColor = colors.surfaceVariant
                    Canvas(modifier = Modifier.size(140.dp)) {
                        val strokeWidth = 36f
                        val t = if (total > 0) total.toFloat() else 1f
                        val cAngle = (completed.toFloat() / t) * 360f
                        val dAngle = (drafts.toFloat() / t) * 360f
                        val pAngle = (pending.toFloat() / t) * 360f

                        var currentAngle = -90f

                        if (total == 0) {
                            drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeWidth))
                        } else {
                            // Green (Concluídos)
                            drawArc(color = colors.statusConforme, startAngle = currentAngle, sweepAngle = cAngle, useCenter = false, style = Stroke(strokeWidth))
                            currentAngle += cAngle

                            // Blue (Rascunhos)
                            drawArc(color = colors.primary, startAngle = currentAngle, sweepAngle = dAngle, useCenter = false, style = Stroke(strokeWidth))
                            currentAngle += dAngle

                            // Orange (Pendentes)
                            drawArc(color = colors.statusWarning, startAngle = currentAngle, sweepAngle = pAngle, useCenter = false, style = Stroke(strokeWidth))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(total.toString(), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = colors.textPrimary)
                        Text("Total", fontSize = 11.sp, color = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendItem("Concluídos", "$completed", colors.statusConforme)
                    LegendItem("Rascunhos", "$drafts", colors.primary)
                    LegendItem("Pendentes", "$pending", colors.statusWarning)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun LegendItem(label: String, value: String, color: Color) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(72.dp))
        Text(value, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun RecentActivityCard(modifier: Modifier, reports: List<ReportEntity>) {
    val colors = AppTheme.colors
    Card(
        modifier = modifier.height(280.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Text("Atividade Recente", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(14.dp))

            if (reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma vistoria registrada até o momento.", color = colors.textSecondary, fontSize = 12.sp)
                }
            } else {
                val recentReports = reports.sortedByDescending { it.date }.take(4)
                LazyColumn {
                    items(recentReports.size) { idx ->
                        val report = recentReports[idx]
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val dateStr = sdf.format(Date(report.date))

                        val statusText = when(report.status) {
                            "DRAFT" -> "Rascunho"
                            "FINALIZED" -> "Concluído"
                            "SENT" -> "Enviado"
                            else -> report.status
                        }

                        val statusColor = when(report.status) {
                            "DRAFT" -> colors.primary
                            "FINALIZED" -> colors.statusConforme
                            "SENT" -> colors.statusConforme
                            else -> colors.statusWarning
                        }

                        com.relatopro.app.ui.components.animation.AnimatedListItem(index = idx) {
                            RecentRow(report.title.ifEmpty { "Relatório #${report.id}" }, report.companyName.ifBlank { report.location.ifBlank { "Sem local" } }, statusText, statusColor, dateStr)
                        }
                        if (idx < recentReports.size - 1) {
                            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentRow(title: String, subtitle: String, statusText: String, statusColor: Color, date: String) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Description, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary, maxLines = 1)
                Text(subtitle, fontSize = 11.sp, color = colors.textSecondary, maxLines = 1)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(date, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.width(90.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}
