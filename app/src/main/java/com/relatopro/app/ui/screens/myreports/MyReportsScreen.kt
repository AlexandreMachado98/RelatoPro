package com.relatopro.app.ui.screens.myreports

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    viewModel: MyReportsViewModel,
    onNavigateBack: () -> Unit
) {
    val reports by viewModel.reports.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Todos") }

    val configuration = LocalConfiguration.current
    val isDesktop = configuration.screenWidthDp >= 600

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

    val sharePdf = { localPath: String ->
        val file = File(localPath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório"))
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
            else -> true
        }
        matchesQuery && matchesStatus
    }.sortedByDescending { it.date }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
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
                Column {
                    Text("Meus Relatórios", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Gerencie e compartilhe suas inspeções.", fontSize = 14.sp, color = TextSecondary)
                }
                if (isDesktop) {
                    Button(
                        onClick = { /* TODO Navigate to Template Builder or Dashboard to create */ },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Novo Relatório")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Filtros / Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Pesquisar por título, ID ou local...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    ),
                    singleLine = true
                )

                if (isDesktop) {
                    StatusFilterDropdown(selectedStatus) { selectedStatus = it }
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mais Filtros")
                    }
                }
            }
            
            if (!isDesktop) {
                Spacer(modifier = Modifier.height(16.dp))
                StatusFilterDropdown(selectedStatus) { selectedStatus = it }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Content
            if (filteredReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextSecondary.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("Nenhum relatório encontrado.", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                if (isDesktop) {
                    DesktopReportsTable(filteredReports, openPdf, sharePdf)
                } else {
                    MobileReportsList(filteredReports, openPdf, sharePdf)
                }
            }
        }
    }
}

@Composable
fun StatusFilterDropdown(selectedStatus: String, onStatusSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val statuses = listOf("Todos", "Concluídos", "Rascunhos")

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceWhite,
                contentColor = TextPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.height(56.dp)
        ) {
            Text(selectedStatus)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            statuses.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DesktopReportsTable(
    reports: List<ReportEntity>,
    onOpenPdf: (String) -> Unit,
    onSharePdf: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundLight)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ID", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp)
                Text("TÍTULO", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp)
                Text("LOCAL", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp)
                Text("DATA", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp)
                Text("STATUS", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(48.dp)) // Actions
            }
            HorizontalDivider(color = BorderColor)

            // Table Rows
            LazyColumn {
                items(reports) { report ->
                    var expanded by remember { mutableStateOf(false) }
                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(report.date))
                    val isFinal = report.status == "FINALIZED"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { report.pdfLocalPath?.let { onOpenPdf(it) } }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${report.id}", modifier = Modifier.width(60.dp), color = TextPrimary, fontSize = 14.sp)
                        Text(report.title.ifEmpty { "Inspeção Padrão" }, modifier = Modifier.weight(2f), color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(report.location, modifier = Modifier.weight(2f), color = TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(dateStr, modifier = Modifier.weight(1f), color = TextSecondary, fontSize = 14.sp)
                        
                        Box(modifier = Modifier.weight(1f)) {
                            StatusBadge(isFinal)
                        }

                        // Menu Contextual
                        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.CenterEnd) {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Ações", tint = TextSecondary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Visualizar PDF") },
                                    onClick = { expanded = false; report.pdfLocalPath?.let { onOpenPdf(it) } },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Compartilhar") },
                                    onClick = { expanded = false; report.pdfLocalPath?.let { onSharePdf(it) } },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Excluir", color = StatusNaoConforme) },
                                    onClick = { expanded = false },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusNaoConforme) }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun MobileReportsList(
    reports: List<ReportEntity>,
    onOpenPdf: (String) -> Unit,
    onSharePdf: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(reports) { report ->
            var expanded by remember { mutableStateOf(false) }
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.date))
            val isFinal = report.status == "FINALIZED"
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { report.pdfLocalPath?.let { onOpenPdf(it) } },
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(report.title.ifEmpty { "Relatório #${report.id}" }, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(report.location, color = TextSecondary, fontSize = 14.sp)
                        }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Ações", tint = TextSecondary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Visualizar PDF") },
                                    onClick = { expanded = false; report.pdfLocalPath?.let { onOpenPdf(it) } },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Compartilhar") },
                                    onClick = { expanded = false; report.pdfLocalPath?.let { onSharePdf(it) } },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Excluir", color = StatusNaoConforme) },
                                    onClick = { expanded = false },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusNaoConforme) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(dateStr, color = TextSecondary, fontSize = 12.sp)
                        StatusBadge(isFinal)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(isFinal: Boolean) {
    val color = if (isFinal) StatusConforme else StatusWarning
    val text = if (isFinal) "Concluído" else "Rascunho"
    
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(6.dp))
            Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
