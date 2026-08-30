package com.relatopro.app.ui.screens.myreports

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.relatopro.app.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    viewModel: MyReportsViewModel,
    onNavigateBack: () -> Unit
) {
    val reports by viewModel.reports.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

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

    Scaffold(
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Meus Relatórios", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Barra de Pesquisa
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pesquisar relatório ou local...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val filteredReports = reports.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.location.contains(searchQuery, ignoreCase = true) ||
                it.id.toString().contains(searchQuery)
            }
            
            if (filteredReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nenhum relatório encontrado.", color = TextSecondary, fontSize = 16.sp)
                        if (searchQuery.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Tente buscar por outro termo.", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredReports) { report ->
                        var expanded by remember { mutableStateOf(false) }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { report.pdfLocalPath?.let { openPdf(it) } },
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = report.title.ifEmpty { "Relatório #${report.id}" },
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Responsável: ${report.responsible} | Local: ${report.location}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.date))
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Box {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Mais opções", tint = TextSecondary)
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Visualizar PDF") },
                                            onClick = { 
                                                expanded = false
                                                report.pdfLocalPath?.let { openPdf(it) } 
                                            },
                                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryBlue) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Compartilhar") },
                                            onClick = { 
                                                expanded = false
                                                report.pdfLocalPath?.let { sharePdf(it) } 
                                            },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryBlue) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Arquivar") },
                                            onClick = { expanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Excluir", color = StatusWarning) },
                                            onClick = { expanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
