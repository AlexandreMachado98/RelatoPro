package com.relatopro.app.ui.screens.fieldmode

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.relatopro.app.ui.components.signature.SignaturePad
import com.relatopro.app.ui.theme.*
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
        viewModel.initializeReportFromTemplate(templateId, "Indústria ABC Lda.", "João da Silva")
    }

    val context = LocalContext.current
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var activeFieldId by remember { mutableStateOf<Long?>(null) }
    
    var selectedStep by remember { mutableIntStateOf(0) }
    val steps = listOf("Informações", "Checklist", "Evidências", "Observações", "Revisão")

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
        topBar = {
            TopAppBar(
                title = { Text("Novo Relatório", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                actions = {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Salvar Rascunho")
                    }
                    if (selectedStep < steps.lastIndex) {
                        Button(
                            onClick = { selectedStep++ },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text("Próximo ➔")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = SurfaceWhite) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedStep > 0) {
                        TextButton(onClick = { selectedStep-- }) {
                            Text("Anterior", color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = onNavigateBack) {
                            Text("Cancelar", color = StatusNaoConforme, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (selectedStep < steps.lastIndex) {
                        Button(
                            onClick = { selectedStep++ },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Próximo")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.finalizeReport { onNavigateBack() } },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusConforme),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Gerar PDF", color = Color.White)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            
            // CUSTOM STEPPER
            Box(
                modifier = Modifier.fillMaxWidth().background(SurfaceWhite).padding(vertical = 16.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    steps.forEachIndexed { index, stepName ->
                        val isSelected = index == selectedStep
                        val isPast = index < selectedStep
                        val circleColor = if (isSelected || isPast) PrimaryBlue else Color.White
                        val textColor = if (isSelected || isPast) Color.White else TextSecondary
                        val borderColor = if (isSelected || isPast) PrimaryBlue else BorderColor
                        val nameColor = if (isSelected) TextPrimary else TextSecondary
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Step Circle
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(circleColor)
                                    .border(1.dp, borderColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPast) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text((index + 1).toString(), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Step Name (Hide on very small screens, show on tablet/desktop, but here we just show it)
                            Text(stepName, color = nameColor, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            
                            if (index < steps.lastIndex) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp).width(30.dp).height(2.dp)
                                        .background(if (isPast) PrimaryBlue else BorderColor)
                                )
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider(color = BorderColor)
            
            // CONTENT
            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                when (selectedStep) {
                    0 -> InfoStepForm()
                    1 -> ChecklistStepContent(viewModel, launchCamera)
                    2 -> PhotosStepContent() // Placeholder for Evidence Gallery
                    3 -> ObservationsStepContent() // Placeholder
                    4 -> SignatureStepContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun InfoStepForm() {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Text("Informações Gerais", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SaaSTextField("Título do Relatório", "Inspeção de Segurança", Modifier.weight(1f))
                SaaSTextField("Tipo de Relatório", "Inspeção", Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SaaSTextField("Local", "Indústria ABC Lda.", Modifier.weight(1f))
                SaaSTextField("Responsável", "João da Silva", Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SaaSTextField("Data da Visita", "30/08/2026", Modifier.weight(1f))
                SaaSTextField("Hora da Visita", "09:30", Modifier.weight(1f))
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Informações Adicionais", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SaaSTextField("Setor / Área", "Produção", Modifier.weight(1f))
                SaaSTextField("Unidade", "Unidade Matriz", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SaaSTextField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row {
            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(" *", color = StatusNaoConforme, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderColor,
                focusedBorderColor = PrimaryBlue,
                unfocusedContainerColor = SurfaceWhite,
                focusedContainerColor = SurfaceWhite,
                unfocusedTextColor = TextSecondary
            )
        )
    }
}

@Composable
fun ChecklistStepContent(viewModel: FieldModeViewModel, launchCamera: (Long) -> Unit) {
    val template by viewModel.template.collectAsState()
    val fields by viewModel.fields.collectAsState()
    val answers by viewModel.answers.collectAsState()
    
    if (template == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(fields.size) { index ->
            val field = fields[index]
            val answer = answers[field.id]
            val answerValue = answer?.answerValue
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = String.format("%02d", index + 1),
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = field.label,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ComplianceChip("Conforme", StatusConforme, answerValue == "Conforme") {
                                viewModel.updateAnswer(field.id, "Conforme", answer?.observation)
                            }
                            ComplianceChip("Não Conforme", StatusNaoConforme, answerValue == "Não Conforme") {
                                viewModel.updateAnswer(field.id, "Não Conforme", answer?.observation)
                            }
                            ComplianceChip("N/A", StatusNaoAplicavel, answerValue == "N/A") {
                                viewModel.updateAnswer(field.id, "N/A", answer?.observation)
                            }
                        }
                        
                        Row {
                            IconButton(onClick = { launchCamera(field.id) }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Foto", tint = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplianceChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) color.copy(alpha = 0.1f) else SurfaceWhite
    val contentColor = if (selected) color else TextSecondary
    val borderColor = if (selected) color else BorderColor
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PhotosStepContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Galeria de Evidências (Mock)", color = TextSecondary)
    }
}

@Composable
fun ObservationsStepContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Observações (Mock)", color = TextSecondary)
    }
}

@Composable
fun SignatureStepContent(viewModel: FieldModeViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Revisão e Assinatura", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Assinatura do Responsável", color = TextSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier.fillMaxWidth().height(250.dp)
        ) {
            SignaturePad(
                modifier = Modifier.fillMaxSize(),
                onSignatureCaptured = { bitmap ->
                    val file = File(context.filesDir, "signature_${System.currentTimeMillis()}.png")
                    file.outputStream().use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    }
                    viewModel.saveSignature(file.absolutePath)
                },
                onClear = {
                    // Nothing extra to do yet
                }
            )
        }
    }
}
