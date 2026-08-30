package com.relatopro.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTemplateBuilder: () -> Unit,
    onNavigateToFieldMode: (Long) -> Unit,
    onNavigateToMyReports: () -> Unit
) {
    val templates by viewModel.templates.collectAsState()
    var showTemplateSelector by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTemplateSelector = true },
                icon = { Icon(Icons.Default.Add, "Novo Relatório") },
                text = { Text("Novo Relatório") },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column {
                    Text("Bom dia, Inspetor", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Gerencie seus relatórios e atividades", fontSize = 14.sp, color = TextSecondary)
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardCard(title = "Relatórios", value = "32", color = PrimaryBlue, modifier = Modifier.weight(1f))
                    DashboardCard(title = "Concluídos", value = "24", color = StatusConforme, modifier = Modifier.weight(1f))
                    DashboardCard(title = "Pendentes", value = "3", color = StatusWarning, modifier = Modifier.weight(1f))
                }
            }

            item {
                Text("Atividade Recente", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column {
                        RecentActivityItem("Relatório #1024", "Hoje", StatusConforme)
                        HorizontalDivider(color = BorderColor)
                        RecentActivityItem("Checklist Segurança", "Ontem", StatusWarning)
                        HorizontalDivider(color = BorderColor)
                        RecentActivityItem("Inspeção Operacional", "28/08", StatusConforme)
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
fun DashboardCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
fun RecentActivityItem(title: String, date: String, statusColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(statusColor, shape = androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Text(text = date, color = TextSecondary, fontSize = 12.sp)
    }
}
