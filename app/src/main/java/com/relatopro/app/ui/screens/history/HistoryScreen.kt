package com.relatopro.app.ui.screens.history

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val reports by viewModel.reports.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableIntStateOf(-1) } // -1 = Todos, 0 = Janeiro, 11 = Dezembro
    var selectedYear by remember { mutableIntStateOf(-1) } // -1 = Todos
    var selectedStatus by remember { mutableStateOf("TODOS") }
    var sortOrder by remember { mutableStateOf("RECENTES") } // RECENTES, ANTIGOS, ALFABETICO

    var selectedReportForDetails by remember { mutableStateOf<ReportEntity?>(null) }

    val openPdf = { localPath: String ->
        val file = File(localPath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Abrir PDF"))
        }
    }

    val sharePdf = { localPath: String, reportId: Long ->
        val file = File(localPath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório"))
            viewModel.markAsSent(reportId)
        }
    }

    // Available years computed from data
    val availableYears = remember(reports) {
        val cal = Calendar.getInstance()
        val yearsSet = mutableSetOf<Int>()
        reports.forEach {
            cal.timeInMillis = it.date
            yearsSet.add(cal.get(Calendar.YEAR))
        }
        yearsSet.sortedDescending()
    }

    val monthsList = listOf(
        "Todos", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    // Filter & sort reports
    val filteredReports = remember(reports, searchQuery, selectedMonth, selectedYear, selectedStatus, sortOrder) {
        val cal = Calendar.getInstance()
        reports.filter { r ->
            cal.timeInMillis = r.date
            val rMonth = cal.get(Calendar.MONTH)
            val rYear = cal.get(Calendar.YEAR)

            val matchesQuery = searchQuery.isBlank() ||
                    r.title.contains(searchQuery, ignoreCase = true) ||
                    r.location.contains(searchQuery, ignoreCase = true) ||
                    r.responsible.contains(searchQuery, ignoreCase = true) ||
                    r.reportNumber.contains(searchQuery, ignoreCase = true) ||
                    r.id.toString().contains(searchQuery)

            val matchesMonth = selectedMonth == -1 || rMonth == selectedMonth
            val matchesYear = selectedYear == -1 || rYear == selectedYear

            val matchesStatus = when (selectedStatus) {
                "CONCLUIDOS" -> r.status == "FINALIZED"
                "RASCUNHOS" -> r.status == "DRAFT"
                "ENVIADOS" -> r.status == "SENT"
                else -> true
            }

            matchesQuery && matchesMonth && matchesYear && matchesStatus
        }.let { list ->
            when (sortOrder) {
                "ANTIGOS" -> list.sortedBy { it.date }
                "ALFABETICO" -> list.sortedBy { it.title.lowercase(Locale.getDefault()) }
                else -> list.sortedByDescending { it.date }
            }
        }
    }

    // Grouping by Month Year (e.g. "AGOSTO 2026")
    val groupedReports = remember(filteredReports) {
        val cal = Calendar.getInstance()
        val map = linkedMapOf<String, MutableList<ReportEntity>>()
        val sdfGroup = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("pt-BR"))

        filteredReports.forEach { report ->
            cal.timeInMillis = report.date
            val groupKey = sdfGroup.format(Date(report.date)).uppercase(Locale.forLanguageTag("pt-BR"))
            map.getOrPut(groupKey) { mutableListOf() }.add(report)
        }
        map
    }

    // Summary counts for current filter
    val totalCount = filteredReports.size
    val completedCount = filteredReports.count { it.status == "FINALIZED" }
    val draftCount = filteredReports.count { it.status == "DRAFT" }
    val sentCount = filteredReports.count { it.status == "SENT" }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Histórico de Relatórios", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Text("Consulta mensal e filtros avançados", fontSize = 12.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // 1. TOP SUMMARY METRICS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HistoryStatCard(Modifier.weight(1f), "Total", totalCount, PrimaryBlue)
                    HistoryStatCard(Modifier.weight(1f), "Concluídos", completedCount, StatusConforme)
                    HistoryStatCard(Modifier.weight(1f), "Rascunhos", draftCount, StatusWarning)
                    HistoryStatCard(Modifier.weight(1f), "Enviados", sentCount, Color(0xFF06B6D4))
                }
            }

            // 2. SEARCH BAR
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pesquisar por título, nº, empresa ou local...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    )
                )
            }

            // 3. MONTH CHIPS ROW
            item {
                Column {
                    Text("Filtrar por Mês", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        monthsList.forEachIndexed { idx, name ->
                            val mIndex = idx - 1
                            val isSelected = selectedMonth == mIndex
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMonth = mIndex },
                                label = { Text(name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = SurfaceWhite,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) PrimaryBlue else BorderColor,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }
            }

            // 4. YEAR & STATUS & ORDER FILTERS ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Filter
                    var statusMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { statusMenuExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite, contentColor = TextPrimary),
                            modifier = Modifier.height(40.dp)
                        ) {
                            val statusLabel = when (selectedStatus) {
                                "CONCLUIDOS" -> "Concluídos"
                                "RASCUNHOS" -> "Rascunhos"
                                "ENVIADOS" -> "Enviados"
                                else -> "Status: Todos"
                            }
                            Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = statusMenuExpanded, onDismissRequest = { statusMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Todos os Status") }, onClick = { selectedStatus = "TODOS"; statusMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Apenas Concluídos") }, onClick = { selectedStatus = "CONCLUIDOS"; statusMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Apenas Rascunhos") }, onClick = { selectedStatus = "RASCUNHOS"; statusMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Apenas Enviados") }, onClick = { selectedStatus = "ENVIADOS"; statusMenuExpanded = false })
                        }
                    }

                    // Year Filter
                    if (availableYears.isNotEmpty()) {
                        var yearMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { yearMenuExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite, contentColor = TextPrimary),
                                modifier = Modifier.height(40.dp)
                            ) {
                                val yearLabel = if (selectedYear == -1) "Ano: Todos" else selectedYear.toString()
                                Text(yearLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = yearMenuExpanded, onDismissRequest = { yearMenuExpanded = false }) {
                                DropdownMenuItem(text = { Text("Todos os Anos") }, onClick = { selectedYear = -1; yearMenuExpanded = false })
                                availableYears.forEach { yr ->
                                    DropdownMenuItem(text = { Text(yr.toString()) }, onClick = { selectedYear = yr; yearMenuExpanded = false })
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Sort Order Button
                    var sortMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = PrimaryBlue)
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Mais Recentes Primeiro") }, onClick = { sortOrder = "RECENTES"; sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Mais Antigos Primeiro") }, onClick = { sortOrder = "ANTIGOS"; sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Ordem Alfabética (A-Z)") }, onClick = { sortOrder = "ALFABETICO"; sortMenuExpanded = false })
                        }
                    }
                }
            }

            // 5. GROUPED REPORTS LIST
            if (groupedReports.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(SurfaceWhite, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextSecondary.copy(alpha = 0.5f))
                            Spacer(Modifier.height(12.dp))
                            Text("Nenhum relatório encontrado para os filtros selecionados.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                groupedReports.forEach { (monthYearKey, reportsInMonth) ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthYearKey,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PrimaryDark
                            )
                            Text(
                                text = "${reportsInMonth.size} ${if (reportsInMonth.size == 1) "relatório" else "relatórios"}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 6.dp))
                    }

                    items(reportsInMonth) { report ->
                        HistoryReportCard(
                            report = report,
                            onCardClick = { selectedReportForDetails = report },
                            onOpenPdf = { path -> openPdf(path) },
                            onSharePdf = { path, id -> sharePdf(path, id) },
                            onDelete = { viewModel.deleteReport(report) }
                        )
                    }
                }
            }
        }
    }

    // Report Details Bottom Sheet
    if (selectedReportForDetails != null) {
        val rep = selectedReportForDetails!!
        val dateFormatted = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()).format(Date(rep.date))
        
        ModalBottomSheet(
            onDismissRequest = { selectedReportForDetails = null },
            containerColor = SurfaceWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rep.title.ifEmpty { "Relatório #${rep.id}" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Text(rep.reportNumber.ifEmpty { "#${rep.id}" }, fontSize = 12.sp, color = TextSecondary)
                    }
                    HistoryStatusBadge(rep.status)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailItemRow("Local / Obra", rep.location)
                        DetailItemRow("Responsável Técnico", rep.responsible)
                        DetailItemRow("Data da Inspeção", dateFormatted)
                        if (!rep.generalObservations.isNullOrBlank()) {
                            DetailItemRow("Observações Finais", rep.generalObservations)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!rep.pdfLocalPath.isNullOrBlank()) {
                        Button(
                            onClick = {
                                openPdf(rep.pdfLocalPath)
                                selectedReportForDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Ver PDF", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                sharePdf(rep.pdfLocalPath, rep.id)
                                selectedReportForDetails = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Compartilhar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            "PDF deste relatório ainda não foi gerado.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun HistoryStatCard(modifier: Modifier = Modifier, label: String, count: Int, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(count.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

@Composable
fun HistoryReportCard(
    report: ReportEntity,
    onCardClick: () -> Unit,
    onOpenPdf: (String) -> Unit,
    onSharePdf: (String, Long) -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.date))
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.title.ifEmpty { "Relatório #${report.id}" },
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = report.location,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Ações", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (!report.pdfLocalPath.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = { Text("Visualizar PDF") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenPdf(report.pdfLocalPath)
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartilhar") },
                                onClick = {
                                    menuExpanded = false
                                    onSharePdf(report.pdfLocalPath, report.id)
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                            HorizontalDivider()
                        }
                        DropdownMenuItem(
                            text = { Text("Excluir", color = StatusNaoConforme) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusNaoConforme) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(dateStr, color = TextSecondary, fontSize = 11.sp)
                }

                HistoryStatusBadge(report.status)
            }
        }
    }
}

@Composable
fun HistoryStatusBadge(status: String) {
    val color = when (status) {
        "FINALIZED" -> StatusConforme
        "SENT" -> Color(0xFF06B6D4)
        "DRAFT" -> PrimaryBlue
        else -> StatusWarning
    }
    val text = when (status) {
        "FINALIZED" -> "Concluído"
        "SENT" -> "Enviado"
        "DRAFT" -> "Rascunho"
        else -> status
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailItemRow(label: String, value: String) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
