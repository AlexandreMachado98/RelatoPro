package com.relatopro.app.ui.screens.templatebuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.relatopro.app.ui.theme.BackgroundLight
import com.relatopro.app.ui.theme.PrimaryBlue
import com.relatopro.app.ui.theme.StatusNaoConforme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateBuilderScreen(
    viewModel: TemplateBuilderViewModel,
    onNavigateBack: () -> Unit,
) {
    val templateName by viewModel.templateName.collectAsState()
    val fields by viewModel.fields.collectAsState()

    var showAddFieldDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar Modelo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White,
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddFieldDialog = true },
                icon = { Icon(Icons.Default.Add, "Adicionar Campo") },
                text = { Text("Adicionar Campo") },
                containerColor = PrimaryBlue,
                contentColor = Color.White
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancelar", color = StatusNaoConforme)
                    }
                    Button(
                        onClick = { viewModel.saveTemplate(onComplete = onNavigateBack) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Salvar Modelo")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = templateName,
                onValueChange = viewModel::updateName,
                label = { Text("Nome do Relatório (Ex: Inspeção Veicular)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (fields.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum campo adicionado. Comece adicionando um novo item.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(fields) { index, field ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = field.label, fontWeight = FontWeight.Bold)
                                    Text(text = "Tipo: ${field.type}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.removeField(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = StatusNaoConforme)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFieldDialog) {
        AddFieldDialog(
            onDismiss = { showAddFieldDialog = false },
            onConfirm = { label, type ->
                viewModel.addField(label, type)
                showAddFieldDialog = false
            }
        )
    }
}

@Composable
fun AddFieldDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("C_NC_NA") }
    
    val types = listOf("C_NC_NA" to "Conforme / Não Conforme", "TEXT" to "Texto Livre", "PHOTO" to "Apenas Foto")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Campo") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Pergunta / Item") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Tipo de Resposta", fontWeight = FontWeight.SemiBold)
                types.forEach { (typeCode, typeDesc) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = type == typeCode,
                            onClick = { type = typeCode }
                        )
                        Text(text = typeDesc)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (label.isNotBlank()) onConfirm(label, type) }) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
