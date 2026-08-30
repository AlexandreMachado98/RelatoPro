package com.relatopro.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.BackgroundLight
import com.relatopro.app.ui.theme.PrimaryBlue
import com.relatopro.app.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTemplateBuilder: () -> Unit,
    onNavigateToFieldMode: () -> Unit,
    onNavigateToMyReports: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("RelatoPro", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Seu relatório. Do seu jeito.", fontSize = 12.sp, color = Color.LightGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White,
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToFieldMode,
                icon = { Icon(Icons.Default.Add, "Novo Relatório") },
                text = { Text("Novo Relatório") },
                containerColor = PrimaryBlue,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Visão Geral", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardCard(title = "Relatórios", value = "12", modifier = Modifier.weight(1f))
                    DashboardCard(title = "Pendentes", value = "3", modifier = Modifier.weight(1f))
                    DashboardCard(title = "Sincronizados", value = "9", modifier = Modifier.weight(1f))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Acesso Rápido", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Meus Relatórios") },
                            supportingContent = { Text("Visualizar e exportar relatórios anteriores") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToMyReports() }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Meus Checklists") },
                            supportingContent = { Text("Criar e gerenciar modelos de formulários") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToTemplateBuilder() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = PrimaryBlue)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
