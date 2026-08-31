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

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Visão Geral & Métricas", "Ranking de Empresas", "Recorrência & Ações")

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Indicadores & Inteligência de BI", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                        Text("Métricas gerenciais, rankings e acompanhamento histórico", fontSize = 12.sp, color = colors.textSecondary)
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

            // Company Filter Chips (Todas as Empresas + Lista de Empresas)
            CompanyFilterRow(
                companies = uiState.availableCompanies,
                selectedCompanyId = uiState.selectedCompanyId,
                onSelectCompany = viewModel::setCompanyFilter
            )

            // Primary Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.surface,
                contentColor = colors.primary,
                divider = { HorizontalDivider(color = colors.border.copy(alpha = 0.5f)) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) colors.primary else colors.textSecondary
                            )
                        }
                    )
                }
            }

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
                        Text("Sem dados no período ou filtro selecionado", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
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
                            onClick = {
                                viewModel.setPeriod("TODOS")
                                viewModel.setCompanyFilter(null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Limpar Filtros e Ver Todos", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> GeneralOverviewTab(uiState = uiState)
                    1 -> CompanyRankingsTab(uiState = uiState)
                    2 -> RecurrenceAndActionsTab(uiState = uiState)
                }
            }
        }
    }
}

@Composable
fun GeneralOverviewTab(uiState: IndicatorsUiState) {
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

@Composable
fun CompanyRankingsTab(uiState: IndicatorsUiState) {
    val colors = AppTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Summary Cards
        item {
            Text("Panorama Executivo das Empresas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExecutiveMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Empresas Inspecionadas",
                    value = uiState.totalInspectedCompanies.toString(),
                    icon = Icons.Default.Business,
                    accentColor = colors.primary
                )
                ExecutiveMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Em Melhoria",
                    value = uiState.companiesImprovingCount.toString(),
                    icon = Icons.Default.TrendingUp,
                    accentColor = colors.statusConforme
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExecutiveMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Estáveis",
                    value = uiState.companiesStableCount.toString(),
                    icon = Icons.Default.TrendingFlat,
                    accentColor = colors.textSecondary
                )
                ExecutiveMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Em Piora / Atenção",
                    value = (uiState.companiesWorseningCount + uiState.companiesAttentionCount).toString(),
                    icon = Icons.Default.TrendingDown,
                    accentColor = colors.statusNaoConforme
                )
            }
        }

        // Ranking List Header
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ranking de Desempenho e Tendência", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                Text("${uiState.companyRankings.size} empresas", fontSize = 12.sp, color = colors.textSecondary)
            }
        }

        // Ranking Cards
        items(uiState.companyRankings.size) { index ->
            val comp = uiState.companyRankings[index]
            AnimatedListItem(index = index) {
                CompanyRankingCard(comp = comp)
            }
        }
    }
}

@Composable
fun RecurrenceAndActionsTab(uiState: IndicatorsUiState) {
    val colors = AppTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Corrective Action Effectiveness Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Efetividade das Ações Corretivas", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("Resolução de Não Conformidades identificadas nas vistorias", fontSize = 12.sp, color = colors.textSecondary)
                    
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", uiState.actionEffectivenessPercent),
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = colors.primary
                            )
                            Text("Taxa de Efetividade / Resolução", fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.background(colors.statusConforme.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("${uiState.resolvedActions} Resolvidas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.statusConforme)
                            }
                            Box(
                                modifier = Modifier.background(colors.statusWarning.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("${uiState.pendingActions} Pendentes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.statusWarning)
                            }
                        }
                    }
                }
            }
        }

        // Recurrent NCs Header
        item {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = colors.statusWarning, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Não Conformidades Recorrentes (Reincidentes)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
            }
            Spacer(Modifier.height(2.dp))
            Text("Itens que continuam apresentando NC em diferentes inspeções da mesma empresa:", fontSize = 12.sp, color = colors.textSecondary)
        }

        if (uiState.recurrentNonConformities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nenhuma não conformidade reincidente detectada no período.", fontSize = 13.sp, color = colors.textSecondary)
                    }
                }
            }
        } else {
            items(uiState.recurrentNonConformities.size) { index ->
                val rNc = uiState.recurrentNonConformities[index]
                AnimatedListItem(index = index) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.statusWarning.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.background(colors.statusWarning.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Reincidência: ${rNc.recurrenceCount}x vistorias", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.statusWarning)
                                }
                                Text(rNc.category, fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(rNc.itemLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("Empresa: ${rNc.companyName}", fontSize = 12.sp, color = colors.primary, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            Text("Identificada nos laudos: ${rNc.affectedReports.joinToString(", ")}", fontSize = 11.sp, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    val colors = AppTheme.colors
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary, maxLines = 1)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }
    }
}

@Composable
fun CompanyRankingCard(comp: CompanyRankingItem) {
    val colors = AppTheme.colors

    val trendColor = when (comp.trend) {
        "Melhorando" -> colors.statusConforme
        "Piorando" -> colors.statusNaoConforme
        "Estável" -> colors.primary
        else -> colors.textSecondary
    }

    val trendIcon = when (comp.trend) {
        "Melhorando" -> Icons.Default.TrendingUp
        "Piorando" -> Icons.Default.TrendingDown
        "Estável" -> Icons.Default.TrendingFlat
        else -> Icons.Default.Info
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(comp.companyName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text("Unidade: ${comp.unit} • ${comp.totalReports} inspeções no período", fontSize = 12.sp, color = colors.textSecondary)
                }

                // Trend badge
                Box(
                    modifier = Modifier
                        .background(trendColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(comp.trend, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = trendColor)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Metrics row: Conformidade %, NC %, Variação pp
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Conformidade", fontSize = 11.sp, color = colors.textSecondary)
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", comp.compliancePercent),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (comp.compliancePercent >= 80f) colors.statusConforme else colors.statusNaoConforme
                    )
                }

                Column {
                    Text("Não Conformidades", fontSize = 11.sp, color = colors.textSecondary)
                    Text(
                        text = "${comp.naoConformeCount} NCs (${String.format(Locale.getDefault(), "%.1f%%", comp.ncPercent)})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Variação Histórica", fontSize = 11.sp, color = colors.textSecondary)
                    val varText = if (comp.variationPp != null) {
                        val sign = if (comp.variationPp > 0) "+" else ""
                        String.format(Locale.getDefault(), "%s%.1f pp", sign, comp.variationPp)
                    } else {
                        "N/A"
                    }
                    Text(
                        text = varText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }
        }
    }
}

@Composable
fun CompanyFilterRow(
    companies: List<com.relatopro.app.data.local.entity.CompanyEntity>,
    selectedCompanyId: Long?,
    onSelectCompany: (Long?) -> Unit
) {
    val colors = AppTheme.colors
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            val isAll = selectedCompanyId == null
            Box(
                modifier = Modifier
                    .background(if (isAll) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant, RoundedCornerShape(16.dp))
                    .border(1.dp, if (isAll) colors.primary else colors.border, RoundedCornerShape(16.dp))
                    .clickable { onSelectCompany(null) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("🏢 Todas as Empresas", fontSize = 11.sp, fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal, color = if (isAll) colors.primary else colors.textPrimary)
            }
        }

        items(companies) { comp ->
            val isSelected = selectedCompanyId == comp.id
            Box(
                modifier = Modifier
                    .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant, RoundedCornerShape(16.dp))
                    .border(1.dp, if (isSelected) colors.primary else colors.border, RoundedCornerShape(16.dp))
                    .clickable { onSelectCompany(comp.id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(comp.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) colors.primary else colors.textPrimary)
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = uiState.generalStatus,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${uiState.totalReports} vistorias realizadas (${uiState.completedReports} concluídas)",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(colors.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun MetricsGrid(uiState: IndicatorsUiState) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricSquareCard(
            modifier = Modifier.weight(1f),
            title = "Itens Conformes",
            count = uiState.totalConforme,
            percent = uiState.compliancePercent,
            color = colors.statusConforme,
            icon = Icons.Default.CheckCircle
        )
        MetricSquareCard(
            modifier = Modifier.weight(1f),
            title = "Não Conformes",
            count = uiState.totalNaoConforme,
            percent = uiState.nonCompliancePercent,
            color = colors.statusNaoConforme,
            icon = Icons.Default.Cancel
        )
        MetricSquareCard(
            modifier = Modifier.weight(1f),
            title = "Não Aplicáveis",
            count = uiState.totalNA,
            percent = uiState.naPercent,
            color = colors.textSecondary,
            icon = Icons.Default.RemoveCircleOutline
        )
    }
}

@Composable
fun MetricSquareCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    percent: Float?,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val colors = AppTheme.colors
    val pctStr = if (percent != null) String.format(Locale.getDefault(), "%.0f%%", percent) else "0%"
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text(pctStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.height(10.dp))
            Text(count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(title, fontSize = 11.sp, color = colors.textSecondary, maxLines = 1)
        }
    }
}

@Composable
fun DistributionBarCard(uiState: IndicatorsUiState) {
    val colors = AppTheme.colors
    val total = uiState.totalEvaluatedItems
    val cFrac = if (total > 0) uiState.totalConforme.toFloat() / total.toFloat() else 0f
    val ncFrac = if (total > 0) uiState.totalNaoConforme.toFloat() / total.toFloat() else 0f
    val naFrac = if (total > 0) uiState.totalNA.toFloat() / total.toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Distribuição Total dos Itens Avaliados", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))

            // Segmented Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceVariant)
            ) {
                if (cFrac > 0f) Box(modifier = Modifier.weight(cFrac).fillMaxHeight().background(colors.statusConforme))
                if (ncFrac > 0f) Box(modifier = Modifier.weight(ncFrac).fillMaxHeight().background(colors.statusNaoConforme))
                if (naFrac > 0f) Box(modifier = Modifier.weight(naFrac).fillMaxHeight().background(colors.textSecondary.copy(alpha = 0.5f)))
            }

            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendItem("Conforme (${uiState.totalConforme})", colors.statusConforme)
                LegendItem("Não Conforme (${uiState.totalNaoConforme})", colors.statusNaoConforme)
                LegendItem("Não Aplicável (${uiState.totalNA})", colors.textSecondary)
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TemporalEvolutionCard(timePoints: List<TimePoint>) {
    val colors = AppTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Evolução Histórica da Conformidade", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                    Text("Desempenho ao longo do tempo", fontSize = 11.sp, color = colors.textSecondary)
                }
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = colors.primary)
            }

            Spacer(Modifier.height(16.dp))

            // Canvas Line Graph
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val w = size.width
                val h = size.height
                val padding = 20f

                // Draw background grid lines
                val gridPaint = Color.Gray.copy(alpha = 0.15f)
                drawLine(gridPaint, Offset(0f, padding), Offset(w, padding), 1f)
                drawLine(gridPaint, Offset(0f, h / 2f), Offset(w, h / 2f), 1f)
                drawLine(gridPaint, Offset(0f, h - padding), Offset(w, h - padding), 1f)

                if (timePoints.size > 1) {
                    val stepX = (w - (padding * 2)) / (timePoints.size - 1)
                    val points = timePoints.mapIndexed { idx, tp ->
                        val x = padding + (idx * stepX)
                        val y = h - padding - ((tp.compliancePercent / 100f) * (h - (padding * 2)))
                        Offset(x, y)
                    }

                    // Draw filled path under the line
                    val fillPath = Path().apply {
                        moveTo(points.first().x, h - padding)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, h - padding)
                        close()
                    }
                    drawPath(fillPath, brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(colors.primary.copy(alpha = 0.25f), colors.primary.copy(alpha = 0.02f))
                    ), style = Fill)

                    // Draw line
                    val strokePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(strokePath, color = colors.primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

                    // Draw dots
                    points.forEach { pt ->
                        drawCircle(color = colors.surface, radius = 5.dp.toPx(), center = pt)
                        drawCircle(color = colors.primary, radius = 3.5.dp.toPx(), center = pt)
                    }
                } else if (timePoints.size == 1) {
                    val center = Offset(w / 2f, h / 2f)
                    drawCircle(color = colors.primary, radius = 6.dp.toPx(), center = center)
                }
            }

            Spacer(Modifier.height(10.dp))

            // X-Axis labels
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                timePoints.forEach { tp ->
                    Text(tp.label, fontSize = 10.sp, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownSection(categories: List<CategoryIndicator>) {
    val colors = AppTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Desempenho por Categoria", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)

            categories.forEach { cat ->
                val compPercent = cat.compliancePercent
                val compFormatted = String.format(Locale.getDefault(), "%.0f%%", compPercent)
                val compColor = if (compPercent >= 80f) colors.statusConforme else if (compPercent >= 60f) colors.statusWarning else colors.statusNaoConforme

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.categoryName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary)
                        Text("$compFormatted conforme • ${cat.naoConformeCount} NC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = compColor)
                    }

                    LinearProgressIndicator(
                        progress = { compPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = compColor,
                        trackColor = colors.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TopNonConformitiesRankingSection(items: List<NonConformityItem>) {
    val colors = AppTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Principais Itens Não Conformes (Top Falhas)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)

            items.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(22.dp).background(colors.statusNaoConforme.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.statusNaoConforme)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary, maxLines = 1)
                            Text(item.category, fontSize = 11.sp, color = colors.textSecondary)
                        }
                    }
                    Box(
                        modifier = Modifier.background(colors.statusNaoConforme.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("${item.count}x (${String.format(Locale.getDefault(), "%.0f%%", item.percentOfTotalNC)})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.statusNaoConforme)
                    }
                }
                if (idx < items.lastIndex) {
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                }
            }
        }
    }
}
