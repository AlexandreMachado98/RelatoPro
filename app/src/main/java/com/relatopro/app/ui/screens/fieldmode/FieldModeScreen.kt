package com.relatopro.app.ui.screens.fieldmode

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.relatopro.app.data.local.entity.PhotoEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.ui.components.signature.SignaturePad
import com.relatopro.app.ui.theme.*
import com.relatopro.app.utils.ImageOptimizer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldModeScreen(
    templateId: Long,
    viewModel: FieldModeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentReport by viewModel.currentReport.collectAsState()
    val fields by viewModel.fields.collectAsState()
    val answers by viewModel.answers.collectAsState()
    val photos by viewModel.photos.collectAsState()

    LaunchedEffect(templateId) {
        viewModel.initializeReportFromTemplate(templateId, "Indústria ABC Lda.", "João da Silva")
    }

    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var activeFieldId by remember { mutableStateOf<Long?>(null) }
    
    var selectedStep by remember { mutableIntStateOf(0) }
    val steps = listOf("Informações", "Checklist", "Evidências", "Observações", "Revisão")

    var isGeneratingPdf by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && currentPhotoFile != null) {
                val optFile = ImageOptimizer.optimizeImageFile(context, currentPhotoFile!!)
                if (optFile != null) {
                    viewModel.savePhoto(activeFieldId, optFile.absolutePath)
                }
            }
        }
    )

    val launchCamera = { fieldId: Long? ->
        activeFieldId = fieldId
        val photosDir = File(context.filesDir, "photos")
        photosDir.mkdirs()
        val tempFile = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
        tempFile.createNewFile()
        currentPhotoFile = tempFile
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        cameraLauncher.launch(uri)
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentReport?.title ?: "Novo Relatório",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Etapa ${selectedStep + 1} de ${steps.size} • ${steps[selectedStep]}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                actions = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Salvar Rascunho", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = SurfaceWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedStep > 0) {
                        OutlinedButton(
                            onClick = { selectedStep-- },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp).weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Voltar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp).weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusNaoConforme),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusNaoConforme.copy(alpha = 0.5f))
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(Modifier.width(16.dp))

                    if (selectedStep < steps.lastIndex) {
                        Button(
                            onClick = { selectedStep++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp).weight(1.3f)
                        ) {
                            Text("Próximo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { 
                                isGeneratingPdf = true
                                viewModel.finalizeReport { 
                                    isGeneratingPdf = false
                                    onNavigateBack() 
                                } 
                            },
                            enabled = !isGeneratingPdf,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusConforme,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp).weight(1.5f)
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Gerando PDF...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Gerar Relatório", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // DYNAMIC 5-STEP STEPPER
            DynamicStepper(
                steps = steps,
                currentStep = selectedStep,
                onStepClick = { stepIndex ->
                    selectedStep = stepIndex
                }
            )
            
            HorizontalDivider(color = BorderColor, thickness = 1.dp)

            // STEP CONTENT (Animated Transition)
            AnimatedContent(
                targetState = selectedStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.fillMaxSize(),
                label = "StepContentAnimation"
            ) { step ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    when (step) {
                        0 -> InfoStepForm(
                            report = currentReport,
                            onUpdateInfo = { title, location, responsible ->
                                viewModel.updateReportInfo(title, location, responsible)
                            }
                        )
                        1 -> ChecklistStepContent(
                            fields = fields,
                            answers = answers,
                            onUpdateAnswer = { fieldId, answerValue, obs ->
                                viewModel.updateAnswer(fieldId, answerValue, obs)
                            },
                            onLaunchCamera = { fieldId -> launchCamera(fieldId) }
                        )
                        2 -> PhotosStepContent(
                            photos = photos,
                            onAddPhotoClick = { launchCamera(null) }
                        )
                        3 -> ObservationsStepContent(
                            observations = currentReport?.generalObservations ?: "",
                            onObservationsChanged = { obs ->
                                viewModel.updateGeneralObservations(obs)
                            }
                        )
                        4 -> SignatureStepContent(
                            report = currentReport,
                            fieldsCount = fields.size,
                            answersCount = answers.size,
                            photosCount = photos.size,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicStepper(
    steps: List<String>,
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 380

    Surface(
        color = SurfaceWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Progress percentage & step label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Etapa ${currentStep + 1} de ${steps.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                val progressPercent = ((currentStep + 1) * 100) / steps.size
                Text(
                    text = "$progressPercent% concluído",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Linear Progress Bar
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / steps.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = PrimaryBlue,
                trackColor = BorderColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step items row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, stepName ->
                    val isPast = index < currentStep
                    val isCurrent = index == currentStep
                    
                    val circleBg = when {
                        isPast -> StatusConforme
                        isCurrent -> PrimaryBlue
                        else -> SurfaceWhite
                    }
                    val circleBorder = when {
                        isPast -> StatusConforme
                        isCurrent -> PrimaryBlue
                        else -> BorderColor
                    }
                    val textColor = when {
                        isCurrent -> TextPrimary
                        isPast -> TextPrimary
                        else -> TextSecondary
                    }
                    val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onStepClick(index) }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(circleBg)
                                .border(1.5.dp, circleBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPast) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Concluído",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = (index + 1).toString(),
                                    color = if (isCurrent) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!isCompact || isCurrent) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stepName,
                                fontSize = 12.sp,
                                fontWeight = fontWeight,
                                color = textColor,
                                maxLines = 1
                            )
                        }

                        if (index < steps.lastIndex) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(if (isCompact) 16.dp else 24.dp)
                                    .height(2.dp)
                                    .background(if (isPast) StatusConforme else BorderColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// ETAPA 1: INFORMAÇÕES GERAIS
// ----------------------------------------------------
@Composable
fun InfoStepForm(
    report: ReportEntity?,
    onUpdateInfo: (title: String, location: String, responsible: String) -> Unit
) {
    var title by remember(report?.title) { mutableStateOf(report?.title ?: "Inspeção de Segurança") }
    var location by remember(report?.location) { mutableStateOf(report?.location ?: "Indústria ABC Lda.") }
    var responsible by remember(report?.responsible) { mutableStateOf(report?.responsible ?: "João da Silva") }
    
    val dateFormatted = remember(report?.date) {
        val d = if (report != null && report.date > 0) Date(report.date) else Date()
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d)
    }
    val timeFormatted = remember(report?.date) {
        val d = if (report != null && report.date > 0) Date(report.date) else Date()
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(d)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Informações Gerais", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Defina o título, local e o responsável pela vistoria.", fontSize = 13.sp, color = TextSecondary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EditableField(
                        label = "Título do Relatório",
                        value = title,
                        onValueChange = {
                            title = it
                            onUpdateInfo(title, location, responsible)
                        },
                        placeholder = "Ex: Inspeção de Segurança Industrial"
                    )

                    EditableField(
                        label = "Local / Empresa",
                        value = location,
                        onValueChange = {
                            location = it
                            onUpdateInfo(title, location, responsible)
                        },
                        placeholder = "Ex: Galpão A - Indústria ABC"
                    )

                    EditableField(
                        label = "Responsável Técnico",
                        value = responsible,
                        onValueChange = {
                            responsible = it
                            onUpdateInfo(title, location, responsible)
                        },
                        placeholder = "Ex: João da Silva - Eng. Segurança"
                    )
                }
            }
        }

        item {
            Text("Data e Horário", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReadOnlyInfoCard(
                    modifier = Modifier.weight(1f),
                    label = "Data da Inspeção",
                    value = dateFormatted,
                    icon = Icons.Default.CalendarToday
                )
                ReadOnlyInfoCard(
                    modifier = Modifier.weight(1f),
                    label = "Horário de Início",
                    value = timeFormatted,
                    icon = Icons.Default.Schedule
                )
            }
        }
    }
}

@Composable
fun EditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(" *", color = StatusNaoConforme, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderColor,
                focusedBorderColor = PrimaryBlue,
                unfocusedContainerColor = SurfaceWhite,
                focusedContainerColor = SurfaceWhite,
                unfocusedTextColor = TextPrimary,
                focusedTextColor = TextPrimary
            )
        )
    }
}

@Composable
fun ReadOnlyInfoCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(label, fontSize = 11.sp, color = TextSecondary)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
    }
}

// ----------------------------------------------------
// ETAPA 2: CHECKLIST
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistStepContent(
    fields: List<TemplateFieldEntity>,
    answers: Map<Long, com.relatopro.app.data.local.entity.ReportAnswerEntity>,
    onUpdateAnswer: (fieldId: Long, answerValue: String?, observation: String?) -> Unit,
    onLaunchCamera: (fieldId: Long) -> Unit
) {
    var selectedFieldId by remember { mutableStateOf<Long?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    if (fields.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Checklist, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Nenhum item configurado neste checklist.", color = TextSecondary, fontSize = 14.sp)
            }
        }
        return
    }

    val answeredCount = answers.values.count { !it.answerValue.isNullOrBlank() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Itens de Verificação", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Text("$answeredCount de ${fields.size} respondidos", fontSize = 12.sp, color = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${(answeredCount * 100) / fields.size}%", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(fields.size) { index ->
            val field = fields[index]
            val answer = answers[field.id]
            val answerValue = answer?.answerValue
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(BackgroundLight, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format("%02d", index + 1),
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = field.label,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                            lineHeight = 18.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ComplianceChip("Conforme", "C", StatusConforme, answerValue == "Conforme") {
                                onUpdateAnswer(field.id, "Conforme", answer?.observation)
                            }
                            ComplianceChip("Não Conforme", "NC", StatusNaoConforme, answerValue == "Não Conforme") {
                                onUpdateAnswer(field.id, "Não Conforme", answer?.observation)
                            }
                            ComplianceChip("N/A", "NA", StatusNaoAplicavel, answerValue == "N/A") {
                                onUpdateAnswer(field.id, "N/A", answer?.observation)
                            }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onLaunchCamera(field.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Foto", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { 
                                    selectedFieldId = field.id
                                    showBottomSheet = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Comentário",
                                    tint = if (answer?.observation.isNullOrBlank()) TextSecondary else PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (!answer?.observation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundLight, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Obs: ${answer?.observation}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showBottomSheet && selectedFieldId != null) {
        val field = fields.find { it.id == selectedFieldId }
        val currentAnswer = answers[selectedFieldId]
        
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = SurfaceWhite
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("Observações do Item", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(field?.label ?: "", fontSize = 13.sp, color = TextSecondary, lineHeight = 16.sp)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                var obsText by remember { mutableStateOf(currentAnswer?.observation ?: "") }
                
                OutlinedTextField(
                    value = obsText,
                    onValueChange = { obsText = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Descreva anomalias, motivos ou ações corretivas...", fontSize = 13.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        onUpdateAnswer(selectedFieldId!!, currentAnswer?.answerValue, obsText)
                        showBottomSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Salvar Observação", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ComplianceChip(fullLabel: String, shortLabel: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) color else SurfaceWhite
    val contentColor = if (selected) Color.White else color
    val borderColor = color

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shortLabel,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ----------------------------------------------------
// ETAPA 3: EVIDÊNCIAS FOTOGRÁFICAS
// ----------------------------------------------------
@Composable
fun PhotosStepContent(
    photos: List<PhotoEntity>,
    onAddPhotoClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Evidências Fotográficas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Text("${photos.size} fotos registradas", fontSize = 12.sp, color = TextSecondary)
            }
            Button(
                onClick = onAddPhotoClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tirar Foto", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SurfaceWhite, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .clickable { onAddPhotoClick() }
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nenhuma evidência capturada ainda", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Toque aqui para abrir a câmera e registrar uma foto.", textAlign = TextAlign.Center, color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos) { photo ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column {
                            val file = File(photo.localPath)
                            if (file.exists()) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "Evidência",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(BorderColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = TextSecondary)
                                }
                            }
                            val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(photo.timestamp))
                            Text(
                                text = dateStr,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// ETAPA 4: OBSERVAÇÕES GERAIS
// ----------------------------------------------------
@Composable
fun ObservationsStepContent(
    observations: String,
    onObservationsChanged: (String) -> Unit
) {
    var obsText by remember(observations) { mutableStateOf(observations) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Observações Finais", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Adicione recomendações técnicas, prazos de adequação ou observações finais para o relatório.", fontSize = 13.sp, color = TextSecondary, lineHeight = 17.sp)
        
        Spacer(modifier = Modifier.height(20.dp))
        
        OutlinedTextField(
            value = obsText,
            onValueChange = {
                obsText = it
                onObservationsChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Digite suas considerações e conclusões técnicas aqui...", fontSize = 14.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ----------------------------------------------------
// ETAPA 5: REVISÃO E DUAS ASSINATURAS
// ----------------------------------------------------
@Composable
fun SignatureStepContent(
    report: ReportEntity?,
    fieldsCount: Int,
    answersCount: Int,
    photosCount: Int,
    viewModel: FieldModeViewModel
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE) }
    val profileName = prefs.getString("user_name", "") ?: ""
    val profileRole = prefs.getString("user_role", "") ?: ""

    val inspectorSig by viewModel.inspectorSignature.collectAsState()
    val operationSig by viewModel.operationSignature.collectAsState()

    var inspectorName by remember(report?.responsible, profileName) { 
        mutableStateOf(report?.responsible?.ifBlank { profileName } ?: profileName) 
    }
    var inspectorRole by remember(profileRole) { 
        mutableStateOf(profileRole.ifBlank { "Inspetor Técnico" }) 
    }
    var operationName by remember { mutableStateOf("") }
    var operationRole by remember { mutableStateOf("Supervisor no Local") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Revisão & Assinaturas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Confira os dados e colete as assinaturas digitais antes de gerar o laudo.", fontSize = 13.sp, color = TextSecondary)
        }

        // Profile Missing Warnings
        if (profileName.isBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Complete seus dados", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                            Text("Para finalizar o relatório, informe seu nome e sua função.", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                    }
                }
            }
        } else if (profileRole.isBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Função não informada", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                            Text("Informe sua função para continuar.", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                    }
                }
            }
        }

        // Resumo do Relatório Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resumo da Vistoria", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryBlue)
                    HorizontalDivider(color = BorderColor)
                    
                    SummaryRow("Título", report?.title ?: "Inspeção Técnica")
                    SummaryRow("Local", report?.location ?: "Local da Inspeção")
                    SummaryRow("Responsável", inspectorName.ifBlank { report?.responsible ?: "Inspetor Técnico" })
                    SummaryRow("Itens Respondidos", "$answersCount de $fieldsCount")
                    SummaryRow("Fotos Anexadas", "$photosCount fotos")
                }
            }
        }

        // Assinatura 1: Responsável pelo Relatório
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (inspectorSig != null) StatusConforme else BorderColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("1. Responsável pelo Relatório", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Text("Inspetor / Engenheiro Técnico", fontSize = 12.sp, color = TextSecondary)
                        }
                        if (inspectorSig != null) {
                            Box(
                                modifier = Modifier
                                    .background(StatusConforme.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Assinado ✓", color = StatusConforme, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Nome do Responsável", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = inspectorName,
                        onValueChange = { 
                            inspectorName = it
                            viewModel.updateReportInfo(report?.title ?: "", report?.location ?: "", it)
                        },
                        placeholder = { Text("Nome completo do responsável", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Cargo / Função", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = inspectorRole,
                        onValueChange = { inspectorRole = it },
                        placeholder = { Text("Digite o cargo ou função", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Desenhe a assinatura no quadro abaixo:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    SignaturePad(
                        modifier = Modifier.fillMaxWidth(),
                        onSignatureCaptured = { bitmap ->
                            viewModel.saveSignature(bitmap, context, inspectorName, "RESPONSAVEL_RELATORIO", inspectorRole)
                        },
                        onClear = {
                            viewModel.clearSignature("RESPONSAVEL_RELATORIO")
                        }
                    )
                }
            }
        }

        // Assinatura 2: Presente na Operação / Acompanhante
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (operationSig != null) StatusConforme else BorderColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("2. Presente na Operação", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Text("Acompanhante / Supervisor no Local", fontSize = 12.sp, color = TextSecondary)
                        }
                        if (operationSig != null) {
                            Box(
                                modifier = Modifier
                                    .background(StatusConforme.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Assinado ✓", color = StatusConforme, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Nome do Acompanhante / Cliente", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = operationName,
                        onValueChange = { operationName = it },
                        placeholder = { Text("Nome do responsável no local", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Cargo / Função", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = operationRole,
                        onValueChange = { operationRole = it },
                        placeholder = { Text("Digite o cargo ou função", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Desenhe a assinatura no quadro abaixo:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    SignaturePad(
                        modifier = Modifier.fillMaxWidth(),
                        onSignatureCaptured = { bitmap ->
                            viewModel.saveSignature(bitmap, context, operationName.ifBlank { "Responsável no Local" }, "PRESENTE_OPERACAO", operationRole.ifBlank { "Acompanhante / Supervisor" })
                        },
                        onClear = {
                            viewModel.clearSignature("PRESENTE_OPERACAO")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
