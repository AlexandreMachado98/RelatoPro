package com.relatopro.app.ui.screens.templatebuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.*

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
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Novo Modelo de Relatório", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                actions = {
                    TextButton(onClick = onNavigateBack, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Cancelar", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.saveTemplate(onComplete = onNavigateBack) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text("Salvar Modelo")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            
            // Header Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text("Informações do Modelo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = viewModel::updateName,
                        label = { Text("Nome do Modelo (Ex: Inspeção de Extintores)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Fields Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Campos do Relatório", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                OutlinedButton(
                    onClick = { showAddFieldDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar Campo")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (fields.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                        .background(SurfaceWhite, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextSecondary.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("Este modelo ainda não possui campos.", color = TextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showAddFieldDialog = true }) {
                            Text("Adicionar o primeiro campo", color = PrimaryBlue)
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(fields) { index, field ->
                        val icon = when (field.type) {
                            "TEXT" -> Icons.AutoMirrored.Filled.TextSnippet
                            "PHOTO" -> Icons.Default.CameraAlt
                            else -> Icons.Default.Checklist
                        }
                        val typeLabel = when (field.type) {
                            "TEXT" -> "Texto Livre"
                            "PHOTO" -> "Apenas Foto"
                            else -> "Conforme / Não Conforme"
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(BackgroundLight, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(text = field.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = typeLabel, fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                                IconButton(onClick = { viewModel.removeField(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = TextSecondary)
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
    
    val types = listOf(
        Triple("C_NC_NA", "Conforme / Não Conforme", Icons.Default.Checklist),
        Triple("TEXT", "Texto Livre", Icons.AutoMirrored.Filled.TextSnippet),
        Triple("PHOTO", "Apenas Foto", Icons.Default.CameraAlt)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Campo", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Título da Pergunta / Item") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Tipo de Resposta", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                types.forEach { (typeCode, typeDesc, icon) ->
                    val isSelected = type == typeCode
                    val bgColor = if (isSelected) PrimaryBlue.copy(alpha = 0.05f) else SurfaceWhite
                    val borderColor = if (isSelected) PrimaryBlue else BorderColor
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable { type = typeCode }
                            .padding(12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { type = typeCode },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(icon, contentDescription = null, tint = if (isSelected) PrimaryBlue else TextSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = typeDesc, color = if (isSelected) PrimaryBlue else TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (label.isNotBlank()) onConfirm(label, type) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Adicionar Campo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(16.dp)
    )
}
