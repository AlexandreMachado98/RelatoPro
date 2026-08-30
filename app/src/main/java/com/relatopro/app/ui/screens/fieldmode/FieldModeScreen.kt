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
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldModeScreen(
    viewModel: FieldModeViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.initializeReportFromTemplate(1L, "Fábrica Central", "João Inspetor")
    }

    val context = LocalContext.current
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var activeFieldId by remember { mutableStateOf<Long?>(null) }

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
        currentPhotoFile = tempFile
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        cameraLauncher.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relatório em Andamento", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
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
                    Button(onClick = { viewModel.finalizeReport { onNavigateBack() } }) {
                        Text("Finalizar e Gerar PDF")
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
                .background(BackgroundLight)
                .padding(paddingValues)
        ) {
            ProgressHeader(
                total = fields.size,
                completed = answers.values.count { it.answerValue != null }
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
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

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Assinatura do Responsável",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SignaturePad(
                        onSignatureCaptured = { _ ->
                            // Here we could store the bitmap to pass to PDF
                        },
                        onClear = {}
                    )
                    Spacer(modifier = Modifier.height(60.dp)) // Extra padding for bottom bar
                }
            }
        }
    }
}

@Composable
fun ProgressHeader(total: Int, completed: Int) {
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
                    text = "$completed/$total itens concluídos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) completed.toFloat() / total.toFloat() else 0f },
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
                    isSelected = initialAnswer == "C",
                    modifier = Modifier.weight(1f)
                ) { onAnswerChange("C", observation) }
                
                AnswerButton(
                    text = "NC",
                    color = StatusNaoConforme,
                    isSelected = initialAnswer == "NC",
                    modifier = Modifier.weight(1f)
                ) { onAnswerChange("NC", observation) }
                
                AnswerButton(
                    text = "NA",
                    color = StatusNaoAplicavel,
                    isSelected = initialAnswer == "NA",
                    modifier = Modifier.weight(1f)
                ) { onAnswerChange("NA", observation) }
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
                        .border(1.dp, PrimaryBlue, RoundedCornerShape(8.dp))
                        .clickable { onAddPhoto() },
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
