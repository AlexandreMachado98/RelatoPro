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
import com.relatopro.app.ui.components.signature.SignaturePad
import com.relatopro.app.ui.theme.*

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.relatopro.app.utils.ImageOptimizer
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldModeScreen(
    templateId: Long,
    viewModel: FieldModeViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(templateId) {
        viewModel.initializeReportFromTemplate(templateId, "Fábrica Central", "João Inspetor")
    }

    val context = LocalContext.current
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var activeFieldId by remember { mutableStateOf<Long?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Informações", "Checklist", "Assinatura")

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && currentPhotoFile != null && activeFieldId != null) {
                val optFile = ImageOptimizer.optimizeImageFile(context, currentPhotoFile!!)
                if (optFile != null) {
                    viewModel.savePhoto(activeFieldId!!, optFile.absolutePath)
                }
            }
        }
    )

    val launchCamera = { fieldId: Long ->
        activeFieldId = fieldId
        val photosDir = File(context.filesDir, "photos")
        photosDir.mkdirs()
        val tempFile = File(photosDir, "temp_photo_${System.currentTimeMillis()}.jpg")
        tempFile.createNewFile()
        currentPhotoFile = tempFile
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        cameraLauncher.launch(uri)
    }

    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            BottomAppBar(containerColor = SurfaceWhite) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedTabIndex > 0) {
                        TextButton(onClick = { selectedTabIndex -= 1 }) {
                            Text("Anterior", color = TextSecondary)
                        }
                    } else {
                        TextButton(onClick = onNavigateBack) {
                            Text("Cancelar", color = StatusNaoConforme)
                        }
                    }
                    
                    if (selectedTabIndex < tabs.lastIndex) {
                        Button(
                            onClick = { selectedTabIndex += 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Próximo")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.finalizeReport { onNavigateBack() } },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusConforme),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Gerar Relatório")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        val fields by viewModel.fields.collectAsState()
        val answers by viewModel.answers.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = SurfaceWhite,
                contentColor = PrimaryBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Medium) }
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (selectedTabIndex) {
                    0 -> {
                        // Informações
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Detalhes da Vistoria", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            OutlinedTextField(
                                value = "João Inspetor",
                                onValueChange = {},
                                label = { Text("Responsável") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = BorderColor,
                                    disabledTextColor = TextPrimary,
                                    disabledLabelColor = TextSecondary
                                )
                            )
                            OutlinedTextField(
                                value = "Fábrica Central",
                                onValueChange = {},
                                label = { Text("Local / Cliente") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = BorderColor,
                                    disabledTextColor = TextPrimary,
                                    disabledLabelColor = TextSecondary
                                )
                            )
                        }
                    }
                    1 -> {
                        // Checklist
                        Column {
                            ProgressHeader(
                                total = fields.size,
                                completed = answers.values.count { it.answerValue != null }
                            )
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(count = fields.size) { index ->
                                    val field = fields[index]
                                    val answer = answers[field.id]
                                    ChecklistItemCard(
                                        number = String.format(java.util.Locale.getDefault(), "%02d", field.orderIndex),
                                        title = field.label,
                                        initialAnswer = answer?.answerValue,
                                        initialObservation = answer?.observation ?: "",
                                        onAnswerChange = { newAns, newObs ->
                                            viewModel.updateAnswer(field.id, newAns, newObs)
                                        },
                                        onAddPhoto = { launchCamera(field.id) }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Assinatura
                        Column {
                            Text("Conclusão e Assinatura", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Assine abaixo para validar as evidências.", color = TextSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    SignaturePad(
                                        onSignatureCaptured = { bitmap ->
                                            viewModel.saveSignature(bitmap, context)
                                        },
                                        onClear = {}
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

@Composable
fun ProgressHeader(total: Int, completed: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Progresso", fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("$completed / $total", color = PrimaryBlue, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) completed.toFloat() / total.toFloat() else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PrimaryBlue,
            trackColor = BorderColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistItemCard(
    number: String,
    title: String,
    initialAnswer: String?,
    initialObservation: String,
    onAnswerChange: (String?, String) -> Unit,
    onAddPhoto: () -> Unit
) {
    var observation by remember(initialObservation) { mutableStateOf(initialObservation) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(BackgroundLight, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = number, color = TextSecondary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // C / NC / NA Buttons (Modern Segmented-like)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnswerButton(
                    text = "Conforme",
                    selected = initialAnswer == "C",
                    selectedColor = StatusConforme,
                    onClick = { onAnswerChange(if (initialAnswer == "C") null else "C", observation) },
                    modifier = Modifier.weight(1f)
                )
                AnswerButton(
                    text = "Não Conforme",
                    selected = initialAnswer == "NC",
                    selectedColor = StatusNaoConforme,
                    onClick = { onAnswerChange(if (initialAnswer == "NC") null else "NC", observation) },
                    modifier = Modifier.weight(1.2f)
                )
                AnswerButton(
                    text = "N/A",
                    selected = initialAnswer == "NA",
                    selectedColor = StatusNaoAplicavel,
                    onClick = { onAnswerChange(if (initialAnswer == "NA") null else "NA", observation) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = observation,
                onValueChange = { 
                    observation = it
                    onAnswerChange(initialAnswer, it) 
                },
                label = { Text("Observação") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onAddPhoto,
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.CameraAlt, contentDescription = "Foto", tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar Evidência (Foto)", color = PrimaryBlue)
            }
        }
    }
}

@Composable
fun AnswerButton(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) selectedColor else SurfaceWhite
    val contentColor = if (selected) Color.White else TextSecondary
    val borderCol = if (selected) selectedColor else BorderColor

    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
