package com.relatopro.app.ui.screens.fieldmode

import android.content.Context
import android.content.Intent
import androidx.compose.ui.window.Dialog
import com.relatopro.app.data.local.entity.CompanyEntity
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
import com.relatopro.app.pdf.PdfGenerator
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
    reportId: Long = 0L,
    viewModel: FieldModeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val currentReport by viewModel.currentReport.collectAsState()
    val fields by viewModel.fields.collectAsState()
    val answers by viewModel.answers.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val isAutoSaving by viewModel.isAutoSaving.collectAsState()
    val lastSavedTime by viewModel.lastSavedTime.collectAsState()

    val prefs = remember { context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE) }
    val loggedInName = remember { prefs.getString("user_name", "")?.ifBlank { "Alexandre Machado" } ?: "Alexandre Machado" }
    val loggedInCompany = remember { prefs.getString("user_company", "") ?: "" }

    LaunchedEffect(templateId, reportId) {
        if (reportId > 0L) {
            viewModel.loadExistingReport(reportId)
        } else {
            viewModel.initializeReportFromTemplate(templateId, loggedInCompany, loggedInName)
        }
    }

    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var activeFieldId by remember { mutableStateOf<Long?>(null) }

    var selectedStep by remember { mutableIntStateOf(0) }
    val steps = listOf("Informações", "Checklist", "Evidências", "Observações", "Revisão")

    var isGeneratingPdf by remember { mutableStateOf(false) }
    var pdfProgressStage by remember { mutableStateOf("Preparando...") }
    var pdfProgressCurrent by remember { mutableIntStateOf(0) }
    var pdfProgressTotal by remember { mutableIntStateOf(0) }
    var pdfResultDialog by remember { mutableStateOf<PdfGenerator.PdfGenerationResult?>(null) }

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
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentReport?.title ?: "Novo Relatório",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Etapa ${selectedStep + 1}/${steps.size} • ${steps[selectedStep]}",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                            if (isAutoSaving) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "• Salvando...",
                                    fontSize = 11.sp,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (lastSavedTime > 0L) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "• Salvo automaticamente",
                                    fontSize = 11.sp,
                                    color = colors.statusConforme
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface),
                actions = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Salvar Rascunho", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                pdfProgressCurrent = 0
                                pdfProgressTotal = 0
                                pdfProgressStage = "Iniciando processamento das evidências..."
                                viewModel.finalizeReport(
                                    onProgress = { cur, tot, stg ->
                                        pdfProgressCurrent = cur
                                        pdfProgressTotal = tot
                                        pdfProgressStage = stg
                                    },
                                    onPdfGenerated = { result ->
                                        isGeneratingPdf = false
                                        if (result != null) {
                                            pdfResultDialog = result
                                        } else {
                                            onNavigateBack()
                                        }
                                    }
                                )
                            },
                            enabled = !isGeneratingPdf,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.statusConforme,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp).weight(1.5f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Gerar Relatório", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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

            // STEP CONTENT (Smooth Directional Animated Transition)
            AnimatedContent(
                targetState = selectedStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (androidx.compose.animation.slideInHorizontally(animationSpec = androidx.compose.animation.core.tween(240)) { width -> width / 3 } + fadeIn(androidx.compose.animation.core.tween(200)))
                            .togetherWith(androidx.compose.animation.slideOutHorizontally(animationSpec = androidx.compose.animation.core.tween(240)) { width -> -width / 3 } + fadeOut(androidx.compose.animation.core.tween(160)))
                    } else {
                        (androidx.compose.animation.slideInHorizontally(animationSpec = androidx.compose.animation.core.tween(240)) { width -> -width / 3 } + fadeIn(androidx.compose.animation.core.tween(200)))
                            .togetherWith(androidx.compose.animation.slideOutHorizontally(animationSpec = androidx.compose.animation.core.tween(240)) { width -> width / 3 } + fadeOut(androidx.compose.animation.core.tween(160)))
                    }
                },
                modifier = Modifier.fillMaxSize(),
                label = "StepContentAnimation"
            ) { step ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    when (step) {
                        0 -> {
                            val companyList by viewModel.companies.collectAsState()
                            InfoStepForm(
                                report = currentReport,
                                companies = companyList,
                                onQuickCreateCompany = { name, unit, onCreated ->
                                    viewModel.quickCreateCompany(name, unit, onCreated)
                                },
                                onUpdateCompanyAndLocation = { companyId, companyName, unit, location, responsible, title ->
                                    viewModel.updateReportCompanyAndLocation(companyId, companyName, unit, location, responsible, title)
                                }
                            )
                        }
                        1 -> ChecklistStepContent(
                            fields = fields,
                            answers = answers,
                            onUpdateAnswer = { fieldId, answerValue, obs ->
                                viewModel.updateAnswer(fieldId, answerValue, obs)
                            },
                            onLaunchCamera = { fieldId -> launchCamera(fieldId) },
                            onMarkAllConforme = { viewModel.markAllConforme() }
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

    // PDF GENERATION PROGRESS MODAL
    if (isGeneratingPdf) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                    Text("Gerando Relatório Técnico...", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                    Text(pdfProgressStage, fontSize = 13.sp, color = colors.primary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Medium)

                    if (pdfProgressTotal > 0) {
                        LinearProgressIndicator(
                            progress = { if (pdfProgressTotal > 0) pdfProgressCurrent.toFloat() / pdfProgressTotal.toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = colors.primary,
                            trackColor = colors.surfaceVariant
                        )
                        Text(
                            "$pdfProgressCurrent / $pdfProgressTotal fotos processadas",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }

                    Text(
                        "Otimizando imagens e montando estrutura do PDF...",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    // PDF GENERATION RESULT MODAL
    val currentResult = pdfResultDialog
    if (currentResult != null) {
        val result = currentResult
        val openPdf = {
            val file = result.file
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Abrir PDF"))
            }
        }

        val sharePdf = {
            val file = result.file
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

        AlertDialog(
            onDismissRequest = {
                pdfResultDialog = null
                onNavigateBack()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(colors.statusConforme.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.statusConforme, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("PDF Gerado com Sucesso!", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tamanho do Arquivo:", fontSize = 12.sp, color = colors.textSecondary)
                                Text(result.fileSizeFormatted, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fotos Processadas:", fontSize = 12.sp, color = colors.textSecondary)
                                Text("${result.photosCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            }
                        }
                    }
                    Text(
                        "O documento foi comprimido e salvo com alta fidelidade visual no seu dispositivo.",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { openPdf() },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ver PDF", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            sharePdf()
                            pdfResultDialog = null
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Compartilhar", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pdfResultDialog = null
                    onNavigateBack()
                }) {
                    Text("Concluir", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
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
// ETAPA 1: INFORMAÇÕES GERAIS (EMPRESA -> UNIDADE -> LOCAL)
// ----------------------------------------------------
@Composable
fun InfoStepForm(
    report: ReportEntity?,
    companies: List<CompanyEntity>,
    onQuickCreateCompany: (name: String, unit: String, onCreated: (CompanyEntity) -> Unit) -> Unit,
    onUpdateCompanyAndLocation: (
        companyId: Long?,
        companyName: String,
        unit: String,
        location: String,
        responsible: String,
        title: String
    ) -> Unit
) {
    val colors = AppTheme.colors

    var title by remember(report?.title) { mutableStateOf(report?.title ?: "Inspeção de Segurança") }
    var selectedCompanyId by remember(report?.companyId) { mutableStateOf(report?.companyId) }
    var selectedCompanyName by remember(report?.companyName) { mutableStateOf(report?.companyName ?: "Empresa não informada") }
    var selectedUnit by remember(report?.unit) { mutableStateOf(report?.unit ?: "Matriz") }
    var location by remember(report?.location) { mutableStateOf(report?.location ?: "Setor de Produção") }
    var responsible by remember(report?.responsible) { mutableStateOf(report?.responsible ?: "João da Silva") }

    var showCompanySelectorModal by remember { mutableStateOf(false) }
    var showQuickCreateModal by remember { mutableStateOf(false) }

    val dateFormatted = remember(report?.date) {
        val d = if (report != null && report.date > 0) Date(report.date) else Date()
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d)
    }
    val timeFormatted = remember(report?.date) {
        val d = if (report != null && report.date > 0) Date(report.date) else Date()
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(d)
    }

    val selectedCompany = companies.find { it.id == selectedCompanyId }
    val availableUnits = remember(selectedCompany) {
        selectedCompany?.units?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: listOf("Matriz")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Identificação da Inspeção", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Vincule a empresa inspecionada, unidade, setor e dados do laudo.", fontSize = 13.sp, color = colors.textSecondary)
        }

        // 1. Bloco de Empresa Inspecionada & Unidade
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Empresa Inspecionada
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Empresa Inspecionada *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                        }
                        TextButton(onClick = { showCompanySelectorModal = true }) {
                            Text(if (selectedCompanyId != null) "Trocar Empresa" else "Selecionar", color = colors.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Card da Empresa Selecionada
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant)
                            .border(1.dp, if (selectedCompanyId != null) colors.primary.copy(alpha = 0.5f) else colors.border, RoundedCornerShape(8.dp))
                            .clickable { showCompanySelectorModal = true }
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = selectedCompanyName.ifBlank { "Clique para selecionar a empresa inspecionada" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (selectedCompanyId != null) colors.textPrimary else colors.textSecondary
                            )
                            if (selectedCompany != null && selectedCompany.cnpj.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text("CNPJ: ${selectedCompany.cnpj} • ${selectedCompany.segment.ifBlank { "Geral" }}", fontSize = 11.sp, color = colors.textSecondary)
                            }
                        }
                    }

                    // Seletor de Unidade
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Unidade / Filial *", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        var unitMenuExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedUnit,
                                onValueChange = {
                                    selectedUnit = it
                                    onUpdateCompanyAndLocation(selectedCompanyId, selectedCompanyName, selectedUnit, location, responsible, title)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Ex: Matriz, Unidade Brasilândia...") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { unitMenuExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar Unidade", tint = colors.textSecondary)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = colors.border,
                                    focusedBorderColor = colors.primary,
                                    unfocusedContainerColor = colors.surface,
                                    focusedContainerColor = colors.surface,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedTextColor = colors.textPrimary
                                )
                            )

                            DropdownMenu(
                                expanded = unitMenuExpanded,
                                onDismissRequest = { unitMenuExpanded = false },
                                modifier = Modifier.background(colors.surface)
                            ) {
                                availableUnits.forEach { unitItem ->
                                    DropdownMenuItem(
                                        text = { Text(unitItem, color = colors.textPrimary) },
                                        onClick = {
                                            selectedUnit = unitItem
                                            unitMenuExpanded = false
                                            onUpdateCompanyAndLocation(selectedCompanyId, selectedCompanyName, selectedUnit, location, responsible, title)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Local / Setor
                    EditableField(
                        label = "Local / Setor da Inspeção",
                        value = location,
                        onValueChange = {
                            location = it
                            onUpdateCompanyAndLocation(selectedCompanyId, selectedCompanyName, selectedUnit, location, responsible, title)
                        },
                        placeholder = "Ex: Setor de Produção, Galpão B, Linha 2"
                    )
                }
            }
        }

        // 2. Bloco de Dados do Laudo & Responsável
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    EditableField(
                        label = "Título do Relatório",
                        value = title,
                        onValueChange = {
                            title = it
                            onUpdateCompanyAndLocation(selectedCompanyId, selectedCompanyName, selectedUnit, location, responsible, title)
                        },
                        placeholder = "Ex: Inspeção de Segurança Industrial"
                    )

                    EditableField(
                        label = "Responsável Técnico",
                        value = responsible,
                        onValueChange = {
                            responsible = it
                            onUpdateCompanyAndLocation(selectedCompanyId, selectedCompanyName, selectedUnit, location, responsible, title)
                        },
                        placeholder = "Ex: Alexandre Machado - Eng. Segurança"
                    )
                }
            }
        }

        // 3. Bloco de Data e Horário
        item {
            Text("Data e Horário", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
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

    // Modal: Company Selector
    if (showCompanySelectorModal) {
        var query by remember { mutableStateOf("") }
        val filtered = companies.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.tradeName.contains(query, ignoreCase = true) ||
            it.cnpj.contains(query)
        }

        AlertDialog(
            onDismissRequest = { showCompanySelectorModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Selecionar Empresa", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textPrimary)
                    TextButton(onClick = {
                        showCompanySelectorModal = false
                        showQuickCreateModal = true
                    }) {
                        Text("+ Nova", fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Buscar por nome ou CNPJ...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Nenhuma empresa encontrada.", fontSize = 13.sp, color = colors.textSecondary)
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        showCompanySelectorModal = false
                                        showQuickCreateModal = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                                ) {
                                    Text("Cadastrar Nova Empresa", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filtered.size) { idx ->
                                val comp = filtered[idx]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCompanyId = comp.id
                                            selectedCompanyName = comp.name
                                            val firstUnit = comp.units.split(",").firstOrNull()?.trim() ?: "Matriz"
                                            selectedUnit = firstUnit
                                            onUpdateCompanyAndLocation(comp.id, comp.name, selectedUnit, location, responsible, title)
                                            showCompanySelectorModal = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedCompanyId == comp.id) colors.primary.copy(alpha = 0.12f) else colors.surfaceVariant
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selectedCompanyId == comp.id) colors.primary else colors.border
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(comp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
                                            if (comp.cnpj.isNotBlank()) {
                                                Text("CNPJ: ${comp.cnpj}", fontSize = 11.sp, color = colors.textSecondary)
                                            }
                                        }
                                        if (selectedCompanyId == comp.id) {
                                            Icon(Icons.Default.Check, contentDescription = "Selecionada", tint = colors.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCompanySelectorModal = false }) {
                    Text("Fechar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    // Modal: Quick Create Company
    if (showQuickCreateModal) {
        var newName by remember { mutableStateOf("") }
        var newUnit by remember { mutableStateOf("Matriz") }

        AlertDialog(
            onDismissRequest = { showQuickCreateModal = false },
            title = { Text("Cadastro Rápido de Empresa", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Razão Social / Nome da Empresa *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Ex: Indústria XYZ Ltda.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    Text("Unidade Inicial", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    OutlinedTextField(
                        value = newUnit,
                        onValueChange = { newUnit = it },
                        placeholder = { Text("Ex: Matriz, Planta 1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onQuickCreateCompany(newName.trim(), newUnit.trim()) { created ->
                                selectedCompanyId = created.id
                                selectedCompanyName = created.name
                                selectedUnit = newUnit.trim().ifBlank { "Matriz" }
                                onUpdateCompanyAndLocation(created.id, created.name, selectedUnit, location, responsible, title)
                                showQuickCreateModal = false
                            }
                        }
                    },
                    enabled = newName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Cadastrar e Vincular", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickCreateModal = false }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun EditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val colors = AppTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(" *", color = colors.statusNaoConforme, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = colors.textSecondary.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.border,
                focusedBorderColor = colors.primary,
                unfocusedContainerColor = colors.surface,
                focusedContainerColor = colors.surface,
                unfocusedTextColor = colors.textPrimary,
                focusedTextColor = colors.textPrimary
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
    onLaunchCamera: (fieldId: Long) -> Unit,
    onMarkAllConforme: () -> Unit = {}
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
    val categoriesGrouped = fields.groupBy { it.category.ifBlank { "Geral" } }

    var totalC = 0
    var totalNC = 0
    var totalNA = 0
    answers.values.forEach { ans ->
        when (ans.answerValue?.trim()?.uppercase()) {
            "C", "CONFORME", "TRUE", "SIM" -> totalC++
            "NC", "NÃO CONFORME", "NAO CONFORME", "FALSE", "NÃO", "NAO" -> totalNC++
            "NA", "N/A", "NÃO APLICÁVEL", "NAO APLICAVEL" -> totalNA++
        }
    }
    val applicable = totalC + totalNC
    val generalComp = if (applicable > 0) (totalC.toFloat() / applicable.toFloat() * 100f) else null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Top Toolbar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Itens de Verificação", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Text("$answeredCount de ${fields.size} respondidos", fontSize = 12.sp, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onMarkAllConforme,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusConforme.copy(alpha = 0.12f), contentColor = StatusConforme),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Todos C", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("${(answeredCount * 100) / fields.size}%", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Category Groups
        categoriesGrouped.forEach { (catName, catFields) ->
            var catC = 0
            var catNC = 0
            var catNA = 0
            catFields.forEach { f ->
                val ans = answers[f.id]
                when (ans?.answerValue?.trim()?.uppercase()) {
                    "C", "CONFORME", "TRUE", "SIM" -> catC++
                    "NC", "NÃO CONFORME", "NAO CONFORME", "FALSE", "NÃO", "NAO" -> catNC++
                    "NA", "N/A", "NÃO APLICÁVEL", "NAO APLICAVEL" -> catNA++
                }
            }
            val catApp = catC + catNC
            val catCompPercent = if (catApp > 0) String.format(Locale.getDefault(), "%.0f%%", (catC.toFloat() / catApp.toFloat() * 100f)) else "—"

            item(key = "cat_header_$catName") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryDark.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(catName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryDark)
                        }
                        Text("C: $catC • NC: $catNC • Conf: $catCompPercent", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(catFields.size) { itemIndex ->
                val field = catFields[itemIndex]
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
                                    text = String.format("%02d", itemIndex + 1),
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
                                ComplianceChip("Conforme", "C", StatusConforme, answerValue?.uppercase() in listOf("C", "CONFORME", "TRUE", "SIM")) {
                                    onUpdateAnswer(field.id, "C", answer?.observation)
                                }
                                ComplianceChip("Não Conforme", "NC", StatusNaoConforme, answerValue?.uppercase() in listOf("NC", "NÃO CONFORME", "NAO CONFORME", "FALSE", "NÃO", "NAO")) {
                                    onUpdateAnswer(field.id, "NC", answer?.observation)
                                }
                                ComplianceChip("N/A", "NA", StatusNaoAplicavel, answerValue?.uppercase() in listOf("NA", "N/A", "NÃO APLICÁVEL", "NAO APLICAVEL")) {
                                    onUpdateAnswer(field.id, "NA", answer?.observation)
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

        // Bottom Inspection Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Resultado Geral da Vistoria", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        if (generalComp != null) {
                            Text("${String.format(Locale.getDefault(), "%.1f", generalComp)}% Conformidade", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (generalComp >= 80f) StatusConforme else StatusNaoConforme)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Total: ${fields.size} itens • C: $totalC • NC: $totalNC • NA: $totalNA", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
    
    if (showBottomSheet && selectedFieldId != null) {
        val field = fields.find { it.id == selectedFieldId }
        val currentAnswer = answers[selectedFieldId]
        var obsText by remember { mutableStateOf(currentAnswer?.observation ?: "") }

        val speechLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                val spoken = matches?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    obsText = if (obsText.isBlank()) spoken else "$obsText. $spoken"
                }
            }
        }

        val launchSpeech = {
            try {
                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Fale a observação técnica...")
                }
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = SurfaceWhite
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Observações do Item", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = launchSpeech) {
                        Icon(Icons.Default.Mic, contentDescription = "Ditar por Voz", tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(field?.label ?: "", fontSize = 13.sp, color = TextSecondary, lineHeight = 16.sp)
                
                Spacer(modifier = Modifier.height(14.dp))

                // Quick Suggestion Chips
                Text("Sugestões Rápidas de Campo:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val suggestions = listOf(
                        "Substituição imediata",
                        "Recarga / Manutenção",
                        "Sinalização ausente",
                        "Obstruído / Bloqueado",
                        "Conforme norma técnica"
                    )
                    suggestions.forEach { chipText ->
                        Box(
                            modifier = Modifier
                                .background(BackgroundLight, RoundedCornerShape(16.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                .clickable {
                                    obsText = if (obsText.isBlank()) chipText else "$obsText. $chipText"
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(chipText, fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                OutlinedTextField(
                    value = obsText,
                    onValueChange = { obsText = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Descreva anomalias, motivos ou ações corretivas...", fontSize = 13.sp) },
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        IconButton(onClick = launchSpeech) {
                            Icon(Icons.Default.Mic, contentDescription = "Ditar", tint = PrimaryBlue)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )
                
                Spacer(modifier = Modifier.height(18.dp))
                
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
    val animatedBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) color else SurfaceWhite,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "ChipBg"
    )
    val animatedContent by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) Color.White else color,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "ChipContent"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(animatedBg)
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (selected) "✓ $shortLabel" else shortLabel,
            color = animatedContent,
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

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                obsText = if (obsText.isBlank()) spoken else "$obsText. $spoken"
                onObservationsChanged(obsText)
            }
        }
    }

    val launchSpeech = {
        try {
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Fale as conclusões e recomendações...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Observações Finais", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Text("Recomendações técnicas e prazos de adequação.", fontSize = 12.sp, color = TextSecondary)
            }
            IconButton(onClick = launchSpeech) {
                Icon(Icons.Default.Mic, contentDescription = "Ditar Observações", tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))

        // Quick Recommendation Chips
        Text("Modelos de Parecer Rápido:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val conclusions = listOf(
                "Instalações conformes com a legislação",
                "Prazo de 30 dias para adequações",
                "Interdição preventiva recomendada",
                "Sem não conformidades críticas detectadas"
            )
            conclusions.forEach { chipText ->
                Box(
                    modifier = Modifier
                        .background(SurfaceWhite, RoundedCornerShape(16.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .clickable {
                            obsText = if (obsText.isBlank()) chipText else "$obsText\n• $chipText"
                            onObservationsChanged(obsText)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(chipText, fontSize = 11.sp, color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        
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
            trailingIcon = {
                IconButton(onClick = launchSpeech) {
                    Icon(Icons.Default.Mic, contentDescription = "Ditar", tint = PrimaryBlue)
                }
            },
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
