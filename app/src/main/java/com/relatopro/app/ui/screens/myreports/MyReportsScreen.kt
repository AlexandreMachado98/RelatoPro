package com.relatopro.app.ui.screens.myreports

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.ui.components.buttons.PrimaryButton
import com.relatopro.app.ui.components.buttons.SecondaryButton
import com.relatopro.app.ui.components.buttons.TextActionButton
import com.relatopro.app.ui.components.feedback.EmptyStateView
import com.relatopro.app.ui.theme.*
import com.relatopro.app.utils.CsvImporter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    viewModel: MyReportsViewModel,
    initialFilter: String = "Todos",
    onNavigateBack: () -> Unit,
    onNavigateToEditDraft: (templateId: Long, reportId: Long) -> Unit = { _, _ -> }
) {
    val colors = AppTheme.colors
    val reports by viewModel.reports.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(initialFilter) }

    val configuration = LocalConfiguration.current
    val isDesktop = configuration.screenWidthDp >= 600

    var isExporting by remember { mutableStateOf(false) }
    var isParsingCsv by remember { mutableStateOf(false) }
    var csvParseResult by remember { mutableStateOf<CsvImporter.CsvParseResult?>(null) }

    val csvFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                isParsingCsv = true
                scope.launch {
                    val result = CsvImporter.parseCsvFromUri(context, uri)
                    isParsingCsv = false
                    csvParseResult = result
                }
            }
        }
    )

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

    val filteredReports = reports.filter {
        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) ||
                it.location.contains(searchQuery, ignoreCase = true) ||
                it.companyName.contains(searchQuery, ignoreCase = true) ||
                it.id.toString().contains(searchQuery)
        val matchesFilter = when (selectedStatus) {
            "Todos" -> true
            "Rascunhos" -> it.status == "DRAFT"
            "Concluídos" -> it.status == "FINALIZED"
            "Enviados" -> it.status == "SENT"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isDesktop) 24.dp else 16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Meus Relatórios",
                        fontSize = if (isDesktop) 24.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        "Gerencie e exporte suas vistorias e laudos técnicos",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SecondaryButton(
                        text = "Importar CSV",
                        icon = Icons.Default.UploadFile,
                        onClick = { csvFilePicker.launch("*/*") },
                        height = 40.dp
                    )

                    PrimaryButton(
                        text = "Exportar CSV",
                        icon = Icons.Default.TableChart,
                        onClick = {
                            if (!isExporting && reports.isNotEmpty()) {
                                isExporting = true
                                viewModel.exportReportsCsv(context) { file ->
                                    isExporting = false
                                    if (file != null) {
                                        com.relatopro.app.utils.ReportExportUtil.shareCsvFile(context, file)
                                    }
                                }
                            }
                        },
                        isLoading = isExporting,
                        enabled = reports.isNotEmpty(),
                        height = 40.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Search Bar
            val draftsCount = reports.count { it.status == "DRAFT" }
            val completedCount = reports.count { it.status == "FINALIZED" }
            val sentCount = reports.count { it.status == "SENT" }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pesquisar por título, empresa, setor ou #ID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = colors.textSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = colors.textSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Row of Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem("Todos", reports.size, selectedStatus == "Todos") { selectedStatus = "Todos" }
                FilterChipItem("Rascunhos", draftsCount, selectedStatus == "Rascunhos") { selectedStatus = "Rascunhos" }
                FilterChipItem("Concluídos", completedCount, selectedStatus == "Concluídos") { selectedStatus = "Concluídos" }
                FilterChipItem("Enviados", sentCount, selectedStatus == "Enviados") { selectedStatus = "Enviados" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            if (filteredReports.isEmpty()) {
                val emptyDesc = if (searchQuery.isNotBlank() || selectedStatus != "Todos") {
                    "Nenhum relatório corresponde aos filtros aplicados. Tente alterar o termo ou limpar os filtros."
                } else {
                    "Você ainda não realizou nenhuma inspeção. Inicie sua primeira vistoria em campo através do botão abaixo."
                }

                EmptyStateView(
                    icon = if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.Description,
                    title = if (searchQuery.isNotBlank()) "Nenhum resultado encontrado" else "Nenhum relatório registrado",
                    description = emptyDesc,
                    actionButtonText = if (searchQuery.isNotBlank() || selectedStatus != "Todos") "Limpar Filtros" else null,
                    onActionClick = {
                        searchQuery = ""
                        selectedStatus = "Todos"
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                if (isDesktop) {
                    DesktopReportsTable(
                        reports = filteredReports,
                        onOpenPdf = openPdf,
                        onSharePdf = sharePdf,
                        onEditDraft = onNavigateToEditDraft,
                        onDeleteReport = { viewModel.deleteReport(it) }
                    )
                } else {
                    MobileReportsList(
                        reports = filteredReports,
                        onOpenPdf = openPdf,
                        onSharePdf = sharePdf,
                        onEditDraft = onNavigateToEditDraft,
                        onDeleteReport = { viewModel.deleteReport(it) }
                    )
                }
            }
        }
    }

    // CSV IMPORT PREVIEW DIALOG
    if (csvParseResult != null) {
        val parseResult = csvParseResult!!
        AlertDialog(
            onDismissRequest = { csvParseResult = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (parseResult.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (parseResult.isValid) colors.statusConforme else colors.statusWarning
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Importar Relatórios CSV", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Total de registros identificados: ${parseResult.totalRows}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Text("Relatórios válidos para importação: ${parseResult.validReports.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                            if (parseResult.errors.isNotEmpty()) {
                                Text("Erros / Linhas ignoradas: ${parseResult.errors.size}", fontSize = 12.sp, color = colors.statusNaoConforme)
                            }
                        }
                    }

                    if (parseResult.validReports.isNotEmpty()) {
                        Text("Prévia dos laudos encontrados:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            parseResult.validReports.take(4).forEach { rep ->
                                Text("• ${rep.title} (${rep.companyName})", fontSize = 11.sp, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (parseResult.validReports.size > 4) {
                                Text("... e mais ${parseResult.validReports.size - 4} relatórios.", fontSize = 11.sp, color = colors.primary)
                            }
                        }
                    }

                    if (parseResult.errors.isNotEmpty()) {
                        Text("Avisos e Erros:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.statusNaoConforme)
                        parseResult.errors.take(2).forEach { err ->
                            Text("Linha ${err.line}: ${err.message}", fontSize = 11.sp, color = colors.statusNaoConforme)
                        }
                    }
                }
            },
            confirmButton = {
                if (parseResult.validReports.isNotEmpty()) {
                    PrimaryButton(
                        text = "Confirmar Importação (${parseResult.validReports.size})",
                        onClick = {
                            viewModel.importParsedReports(parseResult.validReports)
                            csvParseResult = null
                        },
                        height = 42.dp
                    )
                }
            },
            dismissButton = {
                TextActionButton(text = "Cancelar", onClick = { csvParseResult = null })
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun FilterChipItem(title: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val bgColor = if (isSelected) colors.primary else colors.surface
    val contentColor = if (isSelected) Color.White else colors.textSecondary
    val borderColor = if (isSelected) colors.primary else colors.border

    Box(
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .background(bgColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = contentColor, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.25f) else colors.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(count.toString(), color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DesktopReportsTable(
    reports: List<ReportEntity>,
    onOpenPdf: (String) -> Unit,
    onSharePdf: (String, Long) -> Unit,
    onEditDraft: (Long, Long) -> Unit,
    onDeleteReport: (ReportEntity) -> Unit
) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceVariant)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ID", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                Text("TÍTULO / EMPRESA", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                Text("SETOR / LOCAL", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                Text("DATA", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                Text("STATUS", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(48.dp))
            }
            HorizontalDivider(color = colors.border)

            // Table Rows
            LazyColumn {
                items(reports) { report ->
                    var expanded by remember { mutableStateOf(false) }
                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(report.date))
                    val titleDisplay = report.title.ifEmpty { "Inspeção #${report.id}" }
                    val companyDisplay = report.companyName.ifBlank { "Sem empresa" }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (report.status == "DRAFT") {
                                    onEditDraft(report.templateId, report.id)
                                } else {
                                    report.pdfLocalPath?.let { onOpenPdf(it) }
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${report.id}", modifier = Modifier.width(60.dp), color = colors.textSecondary, fontSize = 13.sp)
                        Column(modifier = Modifier.weight(2f)) {
                            Text(titleDisplay, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(companyDisplay, color = colors.primary, fontSize = 11.sp, maxLines = 1)
                        }
                        Text(report.location.ifBlank { "Não informado" }, modifier = Modifier.weight(1.8f), color = colors.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(dateStr, modifier = Modifier.weight(1f), color = colors.textSecondary, fontSize = 13.sp)

                        Box(modifier = Modifier.weight(1.2f)) {
                            StatusBadge(report.status)
                        }

                        // Menu Contextual
                        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.CenterEnd) {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Ações", tint = colors.textSecondary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(colors.surface)) {
                                if (report.status == "DRAFT") {
                                    DropdownMenuItem(
                                        text = { Text("Editar e Continuar", color = colors.primary, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            expanded = false
                                            onEditDraft(report.templateId, report.id)
                                        },
                                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = colors.primary) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Visualizar PDF", color = colors.textPrimary) },
                                    onClick = { expanded = false; report.pdfLocalPath?.let { onOpenPdf(it) } },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.primary) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Compartilhar", color = colors.textPrimary) },
                                    onClick = { expanded = false; report.pdfLocalPath?.let { onSharePdf(it, report.id) } },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = colors.primary) }
                                )
                                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                                DropdownMenuItem(
                                    text = { Text("Excluir", color = colors.statusNaoConforme) },
                                    onClick = { expanded = false; onDeleteReport(report) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.statusNaoConforme) }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun MobileReportsList(
    reports: List<ReportEntity>,
    onOpenPdf: (String) -> Unit,
    onSharePdf: (String, Long) -> Unit,
    onEditDraft: (Long, Long) -> Unit,
    onDeleteReport: (ReportEntity) -> Unit
) {
    val colors = AppTheme.colors

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(reports.size) { index ->
            val report = reports[index]
            var expanded by remember { mutableStateOf(false) }
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.date))
            val companyDisplay = report.companyName.ifBlank { "Empresa não informada" }
            val titleDisplay = report.title.ifEmpty { "Inspeção #${report.id}" }

            com.relatopro.app.ui.components.animation.AnimatedListItem(index = index) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (report.status == "DRAFT") {
                                onEditDraft(report.templateId, report.id)
                            } else {
                                report.pdfLocalPath?.let { onOpenPdf(it) }
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    titleDisplay,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    companyDisplay,
                                    color = colors.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                if (report.location.isNotBlank()) {
                                    Text(
                                        "Local: ${report.location}",
                                        color = colors.textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Ações", tint = colors.textSecondary)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(colors.surface)) {
                                    if (report.status == "DRAFT") {
                                        DropdownMenuItem(
                                            text = { Text("Editar e Continuar", color = colors.primary, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                expanded = false
                                                onEditDraft(report.templateId, report.id)
                                            },
                                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = colors.primary) }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Visualizar PDF", color = colors.textPrimary) },
                                        onClick = { expanded = false; report.pdfLocalPath?.let { onOpenPdf(it) } },
                                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.primary) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Compartilhar", color = colors.textPrimary) },
                                        onClick = { expanded = false; report.pdfLocalPath?.let { onSharePdf(it, report.id) } },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = colors.primary) }
                                    )
                                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                                    DropdownMenuItem(
                                        text = { Text("Excluir", color = colors.statusNaoConforme) },
                                        onClick = { expanded = false; onDeleteReport(report) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.statusNaoConforme) }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dateStr, color = colors.textSecondary, fontSize = 11.sp)
                            StatusBadge(report.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val colors = AppTheme.colors
    val color = when(status) {
        "FINALIZED", "SENT" -> colors.statusConforme
        "DRAFT" -> colors.primary
        else -> colors.statusWarning
    }
    val text = when(status) {
        "FINALIZED" -> "Concluído"
        "SENT" -> "Enviado"
        "DRAFT" -> "Rascunho"
        else -> status
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(6.dp))
            Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
