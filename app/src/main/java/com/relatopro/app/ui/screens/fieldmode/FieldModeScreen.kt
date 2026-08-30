package com.relatopro.app.ui.screens.fieldmode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldModeScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relatório em Andamento", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("Voltar")
                    }
                    Button(onClick = { /* TODO */ }) {
                        Text("Finalizar e Gerar PDF")
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
        ) {
            ProgressHeader()
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ChecklistItemCard(
                        number = "08",
                        title = "Extintor de incêndio acessível e sinalizado?",
                        initialAnswer = "NC",
                        initialObservation = "Extintor sem identificação adequada."
                    )
                }
                item {
                    ChecklistItemCard(
                        number = "09",
                        title = "Equipamentos de Proteção Individual (EPI) em uso?",
                        initialAnswer = "C",
                        initialObservation = ""
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressHeader() {
    Surface(
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "12/35 itens concluídos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge("C: 8", StatusConforme)
                    StatusBadge("NC: 3", StatusNaoConforme)
                    StatusBadge("NA: 1", StatusNaoAplicavel)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 12f / 35f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = StatusConforme,
                trackColor = Color.LightGray
            )
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistItemCard(
    number: String,
    title: String,
    initialAnswer: String?,
    initialObservation: String
) {
    var selectedAnswer by remember { mutableStateOf(initialAnswer) }
    var observation by remember { mutableStateOf(initialObservation) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$number - $title",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // C / NC / NA Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnswerButton(
                    text = "C",
                    color = StatusConforme,
                    isSelected = selectedAnswer == "C",
                    modifier = Modifier.weight(1f)
                ) { selectedAnswer = "C" }
                
                AnswerButton(
                    text = "NC",
                    color = StatusNaoConforme,
                    isSelected = selectedAnswer == "NC",
                    modifier = Modifier.weight(1f)
                ) { selectedAnswer = "NC" }
                
                AnswerButton(
                    text = "NA",
                    color = StatusNaoAplicavel,
                    isSelected = selectedAnswer == "NA",
                    modifier = Modifier.weight(1f)
                ) { selectedAnswer = "NA" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = observation,
                onValueChange = { observation = it },
                label = { Text("Observação") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Photos Row (Mock)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Foto 1", fontSize = 10.sp)
                }
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PrimaryBlue, RoundedCornerShape(8.dp))
                        .clickable { /* TODO: Abrir câmera */ },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Adicionar Foto", tint = PrimaryBlue)
                        Text("Adicionar", fontSize = 10.sp, color = PrimaryBlue)
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerButton(
    text: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) color else Color.White
    val contentColor = if (isSelected) Color.White else color
    val borderColor = if (isSelected) Color.Transparent else color

    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = contentColor
            )
        }
    }
}
