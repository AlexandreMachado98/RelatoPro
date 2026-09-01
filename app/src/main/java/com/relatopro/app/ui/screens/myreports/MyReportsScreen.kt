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
        val matchesQuery = it.title.contains(searchQuery, ignoreCase = true) ||
                           it.location.contains(searchQuery, ignoreCase = true) ||
                           it.id.toString().contains(searchQuery)
        val matchesStatus = when (selectedStatus) {
            "Todos" -> true
            "Concluídos" -> it.status == "FINALIZED"
            "Rascunhos" -> it.status == "DRAFT"
            "Enviados" -> it.status == "SENT"
            else -> true
        }
        matchesQuery && matchesStatus
    }.sortedByDescending { it.date }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Meus Relatórios", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("Gerencie, importe e compartilhe suas inspeções.", fontSize = 13.sp, color = colors.textSecondary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { csvFilePicker.launch("*/*") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        if (isParsingCsv) {
                            CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Importar CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                        }
                    }

                    Button(
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
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Exportar CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Filtros / Toolbar
            val draftsCount = reports.count { it.status == "DRAFT" }
            val completedCount = reports.count { it.status == "FINALIZED" }
            val sentCount = reports.count { it.status == "SENT" }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pesquisar por título, ID ou local...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = colors.textSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = colors.textSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
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
            
            Spacer(modifier = Modifier.height(16.dp))
            // Row of Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem("Todos", reports.size, selectedStatus == "Todos") { selectedStatus = "Todos" }
                FilterChipItem("Rascunhos", draftsCount, selectedStatus == "Rascunhos") { selectedStatus = "Rascunhos" }
                FilterChipItem("Concluídos", completedCount, selectedStatus == "Concluídos") { selectedStatus = "Concluídos" }
                FilterChipItem("Enviados", sentCount, selectedStatus == "Enviados") { selectedStatus = "Enviados" }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Content
            if (filteredReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = colors.textSecondary.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("Nenhum relatório encontrado.", color = colors.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
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
                    Button(
                        onClick = {
                            viewModel.importParsedReports(parseResult.validReports)
                            csvParseResult = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirmar Importação (${parseResult.validReports.size})", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { csvParseResult = null }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun FilterChipItem(title: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val bgColor = if (isSelected) colors.primary else Color.Transparent
    val contentColor = if (isSelected) Color.White else colors.textSecondary
    val borderColor = if (isSelected) colors.primary else colors.border
    
    Box(
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .background(bgColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = contentColor, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier.background(
                    if (isSelected) Color.White.copy(alpha = 0.25f) else colors.surfaceVariant,
                    RoundedCornerShape(10.dp)
                ).padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(count.toString(), color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ID", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                Text("TÍTULO", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                Text("LOCAL", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                Text("DATA", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                Text("STATUS", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(48.dp))
            }
            HorizontalDivider(color = colors.border)

            // Table Rows
            LazyColumn {
                items(reports) { report ->
                    var expanded by remember { mutableStateOf(false) }
                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(report.date))
                    
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
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${report.id}", modifier = Modifier.width(60.dp), color = colors.textPrimary, fontSize = 14.sp)
                        Text(report.title.ifEmpty { "Inspeção Padrão" }, modifier = Modifier.weight(2f), color = colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(report.location, modifier = Modifier.weight(2f), color = colors.textSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(dateStr, modifier = Modifier.weight(1f), color = colors.textSecondary, fontSize = 14.sp)
                        
                        Box(modifier = Modifier.weight(1f)) {
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

    if (reports.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = colors.textSecondary.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(12.dp))
                Text("Nenhum relatório encontrado", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Crie seu primeiro relatório ou ajuste os filtros.", fontSize = 13.sp, color = colors.textSecondary)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(reports.size) { index ->
                val report = reports[index]
                var expanded by remember { mutableStateOf(false) }
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.date))
                
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(report.title.ifEmpty { "Relatório #${report.id}" }, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 16.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(report.location, color = colors.textSecondary, fontSize = 14.sp)
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
                            Spacer(Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(dateStr, color = colors.textSecondary, fontSize = 12.sp)
                                StatusBadge(report.status)
                            }
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
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(6.dp))
            Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
