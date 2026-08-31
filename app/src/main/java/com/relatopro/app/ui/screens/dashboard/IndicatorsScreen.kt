package com.relatopro.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.components.animation.AnimatedListItem
import com.relatopro.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorsScreen(
    viewModel: IndicatorsViewModel,
    onNavigateBack: () -> Unit
) {
    val colors = AppTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Indicadores & Estatísticas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                        Text("Métricas de conformidade e segurança técnica", fontSize = 12.sp, color = colors.textSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Period Filter Chips
            PeriodFilterRow(
                selectedPeriod = uiState.selectedPeriod,
                onSelectPeriod = viewModel::setPeriod
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.7f))

            if (uiState.totalReports == 0 && !uiState.isLoading) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(colors.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Sem dados no período selecionado", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Realize ou finalize vistorias e laudos no aplicativo para acompanhar os indicadores de conformidade.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.setPeriod("TODOS") },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Ver Todo o Histórico", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. Overall Compliance Index Banner
                    item {
                        OverallComplianceBanner(uiState)
                    }

                    // 2. Metrics 4-Grid Cards
                    item {
                        MetricsGrid(uiState)
                    }

                    // 3. Segmented Distribution Bar
                    item {
                        DistributionBarCard(uiState)
                    }

                    // 4. Time Series Evolution Chart
                    if (uiState.timePoints.isNotEmpty()) {
                        item {
                            TemporalEvolutionCard(uiState.timePoints)
                        }
                    }

                    // 5. Category Breakdown
                    if (uiState.categories.isNotEmpty()) {
                        item {
                            CategoryBreakdownSection(uiState.categories)
                        }
                    }

                    // 6. Top Non-Conformities Ranking
                    if (uiState.topNonConformities.isNotEmpty()) {
                        item {
                            TopNonConformitiesRankingSection(uiState.topNonConformities)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodFilterRow(
    selectedPeriod: String,
    onSelectPeriod: (String) -> Unit
) {
    val colors = AppTheme.colors
    val periods = listOf(
        "HOJE" to "Hoje",
        "7_DIAS" to "Últimos 7 dias",
        "30_DIAS" to "Últimos 30 dias",
        "ESTE_MES" to "Este mês",
        "TODOS" to "Todos os registros"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(periods) { (code, label) ->
            val isSelected = selectedPeriod == code
            val bgColor = if (isSelected) colors.primary else colors.surfaceVariant
            val textColor = if (isSelected) Color.White else colors.textPrimary
            val borderColor = if (isSelected) colors.primary else colors.border

            Box(
                modifier = Modifier
                    .background(bgColor, RoundedCornerShape(20.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable { onSelectPeriod(code) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun OverallComplianceBanner(uiState: IndicatorsUiState) {
    val colors = AppTheme.colors
    val compliance = uiState.compliancePercent
    val complianceStr = if (compliance != null) String.format(Locale.getDefault(), "%.1f%%", compliance) else "N/A"
    
    val statusColor = when (uiState.generalStatus) {
        "Excelente" -> colors.statusConforme
        "Bom" -> colors.primary
        "Atenção" -> colors.statusWarning
        "Crítico" -> colors.statusNaoConforme
        else -> colors.textSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ÍNDICE GERAL DE CONFORMIDADE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = complianceStr,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = uiState.generalStatus.uppercase(),
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (compliance != null) "Cálculo: C / (C + NC) × 100 • ${uiState.totalEvaluatedItems} itens analisados" else "Sem dados suficientes para cálculo",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun MetricsGrid(uiState: IndicatorsUiState) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Relatórios",
                value = uiState.totalReports.toString(),
                subText = "${uiState.completedReports} concluídos • ${uiState.draftReports} rascunhos",
                icon = Icons.Default.Description,
                iconColor = colors.primary
            )
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Itens Avaliados",
                value = uiState.totalEvaluatedItems.toString(),
                subText = "Em todas as vistorias",
                icon = Icons.Default.Checklist,
                iconColor = colors.primary
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val cPct = uiState.compliancePercent?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "—"
            val ncPct = uiState.nonCompliancePercent?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "—"

            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Conformes (C)",
                value = uiState.totalConforme.toString(),
                subText = "$cPct de conformidade",
                icon = Icons.Default.CheckCircle,
                iconColor = colors.statusConforme
            )
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Não Conformes (NC)",
                value = uiState.totalNaoConforme.toString(),
                subText = "$ncPct de não conformidade",
                icon = Icons.Default.Cancel,
                iconColor = colors.statusNaoConforme
            )
        }
    }
}

@Composable
fun MetricStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    val colors = AppTheme.colors
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subText, fontSize = 11.sp, color = colors.textSecondary, maxLines = 1)
        }
    }
}

@Composable
fun DistributionBarCard(uiState: IndicatorsUiState) {
    val colors = AppTheme.colors
    val total = uiState.totalEvaluatedItems
    val c = uiState.totalConforme
    val nc = uiState.totalNaoConforme
    val na = uiState.totalNA

    val cFrac = if (total > 0) c.toFloat() / total.toFloat() else 0f
    val ncFrac = if (total > 0) nc.toFloat() / total.toFloat() else 0f
    val naFrac = if (total > 0) na.toFloat() / total.toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Distribuição das Respostas", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))

            // Multi-segment progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.surfaceVariant)
            ) {
                if (cFrac > 0f) {
                    Box(modifier = Modifier.weight(cFrac).fillMaxHeight().background(colors.statusConforme))
                }
                if (ncFrac > 0f) {
                    Box(modifier = Modifier.weight(ncFrac).fillMaxHeight().background(colors.statusNaoConforme))
                }
                if (naFrac > 0f) {
                    Box(modifier = Modifier.weight(naFrac).fillMaxHeight().background(colors.statusNaoAplicavel))
                }
                if (total == 0) {
                    Box(modifier = Modifier.fillMaxSize().background(colors.border))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Legend
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendItem(color = colors.statusConforme, label = "Conforme: $c")
                LegendItem(color = colors.statusNaoConforme, label = "Não Conforme: $nc")
                LegendItem(color = colors.statusNaoAplicavel, label = "Não Aplicável: $na")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TemporalEvolutionCard(timePoints: List<TimePoint>) {
    val colors = AppTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Evolução da Conformidade (%)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                Text("Histórico Real", fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))

            val lineColor = colors.primary
            val dotCenterColor = colors.surface

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (timePoints.size < 2) {
                        val singlePoint = timePoints.firstOrNull()?.compliancePercent ?: 100f
                        val y = size.height - (singlePoint / 100f * size.height)
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawCircle(color = lineColor, radius = 5.dp.toPx(), center = Offset(size.width / 2, y))
                        return@Canvas
                    }

                    val stepX = size.width / (timePoints.size - 1)
                    val path = Path()
                    val fillPath = Path()

                    timePoints.forEachIndexed { index, pt ->
                        val x = index * stepX
                        val y = size.height - ((pt.compliancePercent.coerceIn(0f, 100f) / 100f) * size.height)
                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, size.height)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }

                        if (index == timePoints.lastIndex) {
                            fillPath.lineTo(x, size.height)
                            fillPath.close()
                        }
                    }

                    drawPath(path = fillPath, color = lineColor.copy(alpha = 0.12f), style = Fill)
                    drawPath(path = path, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

                    timePoints.forEachIndexed { index, pt ->
                        val x = index * stepX
                        val y = size.height - ((pt.compliancePercent.coerceIn(0f, 100f) / 100f) * size.height)
                        drawCircle(color = dotCenterColor, radius = 5.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = Offset(x, y))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                timePoints.take(6).forEach { pt ->
                    Text(pt.label, color = colors.textSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownSection(categories: List<CategoryIndicator>) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Indicadores por Categoria", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)

        categories.forEach { cat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.categoryName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
                        val compStr = String.format(Locale.getDefault(), "%.1f%%", cat.compliancePercent)
                        Text(
                            text = "$compStr Conf.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (cat.compliancePercent >= 80f) colors.statusConforme else colors.statusNaoConforme
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    val app = cat.conformeCount + cat.naoConformeCount
                    val frac = if (app > 0) cat.conformeCount.toFloat() / app.toFloat() else 1f
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.weight(frac.coerceAtLeast(0.01f)).fillMaxHeight().background(colors.statusConforme))
                        Box(modifier = Modifier.weight((1f - frac).coerceAtLeast(0.01f)).fillMaxHeight().background(colors.statusNaoConforme))
                    }
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("C: ${cat.conformeCount} • NC: ${cat.naoConformeCount} • NA: ${cat.naCount}", fontSize = 11.sp, color = colors.textSecondary)
                        Text("Total: ${cat.totalItems} itens", fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun TopNonConformitiesRankingSection(ranking: List<NonConformityItem>) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Principais Não Conformidades", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ranking.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(colors.statusNaoConforme.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = colors.statusNaoConforme, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = colors.textPrimary, maxLines = 1)
                            Text(item.category, fontSize = 10.sp, color = colors.textSecondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(colors.statusNaoConforme.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("${item.count} NC", color = colors.statusNaoConforme, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    if (index < ranking.lastIndex) {
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
