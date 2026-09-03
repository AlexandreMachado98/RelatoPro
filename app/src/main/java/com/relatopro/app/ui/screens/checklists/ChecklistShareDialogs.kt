package com.relatopro.app.ui.screens.checklists

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.ui.theme.AppTheme
import com.relatopro.app.utils.ChecklistShareUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
fun ShareChecklistDialog(
    template: TemplateEntity,
    fields: List<TemplateFieldEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrPayload by remember { mutableStateOf("") }
    var isExportingFile by remember { mutableStateOf(false) }

    LaunchedEffect(template.id) {
        withContext(Dispatchers.Default) {
            val payload = ChecklistShareUtil.encodeChecklistToQrPayload(template, fields)
            qrPayload = payload
            qrBitmap = ChecklistShareUtil.generateQrCodeBitmap(payload, 600)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Compartilhar Formulário", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textPrimary)
                        Text(template.name, fontSize = 13.sp, color = colors.textSecondary, maxLines = 1)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = colors.textSecondary)
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

                // QR Code Display Container
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(2.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code do Checklist",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(32.dp))
                            Text("Gerando QR Code...", fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                }

                Text(
                    "Aponte a câmera de outro dispositivo com o Relato Pro para importar este checklist instantaneamente sem necessidade de internet.",
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 14.sp
                )

                // Buttons: Share as File (.relatopro) & Copy Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isExportingFile) {
                                isExportingFile = true
                                scope.launch {
                                    val file = ChecklistShareUtil.exportChecklistToFile(context, template, fields)
                                    isExportingFile = false
                                    if (file != null) {
                                        ChecklistShareUtil.shareChecklistFile(context, file, template.name)
                                    } else {
                                        Toast.makeText(context, "Erro ao exportar arquivo de checklist.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        if (isExportingFile) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Enviar Arquivo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (qrPayload.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Checklist Relato Pro", qrPayload)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Código copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar Código", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ImportChecklistDialog(
    onDismiss: () -> Unit,
    onConfirmImport: (ChecklistShareUtil.ChecklistPackage) -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Câmera QR, 1 = Arquivo / Foto, 2 = Digitar Código
    var rawCodeInput by remember { mutableStateOf("") }
    var parsedPackage by remember { mutableStateOf<ChecklistShareUtil.ChecklistPackage?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isReadingFile by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                errorMessage = "Permissão de câmera necessária para escanear QR Code em tempo real."
            }
        }
    )

    // File Picker for .relatopro or .json files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                isReadingFile = true
                errorMessage = null
                scope.launch {
                    val pkg = ChecklistShareUtil.parseChecklistFromFileUri(context, uri)
                    isReadingFile = false
                    if (pkg != null) {
                        parsedPackage = pkg
                    } else {
                        errorMessage = "Arquivo inválido ou corrompido. Selecione um arquivo .relatopro ou .json válido."
                    }
                }
            }
        }
    )

    // Image Picker for QR Code screenshots/photos
    val qrImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                isReadingFile = true
                errorMessage = null
                scope.launch(Dispatchers.IO) {
                    try {
                        val input = context.contentResolver.openInputStream(uri)
                        val bmp = BitmapFactory.decodeStream(input)
                        input?.close()
                        if (bmp != null) {
                            val decoded = ChecklistShareUtil.decodeQrBitmap(bmp)
                            if (decoded != null) {
                                val pkg = ChecklistShareUtil.decodeChecklistPayload(decoded)
                                withContext(Dispatchers.Main) {
                                    isReadingFile = false
                                    if (pkg != null) {
                                        triggerVibration(context)
                                        parsedPackage = pkg
                                    } else {
                                        errorMessage = "QR Code lido não é um checklist válido do Relato Pro."
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    isReadingFile = false
                                    errorMessage = "Nenhum QR Code legível foi identificado nesta imagem."
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                isReadingFile = false
                                errorMessage = "Não foi possível carregar a imagem selecionada."
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            isReadingFile = false
                            errorMessage = "Erro ao processar imagem: ${e.localizedMessage}"
                        }
                    }
                }
            }
        }
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Importar Formulário", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = colors.textSecondary)
                    }
                }

                if (parsedPackage == null) {
                    // Selection Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.primary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; errorMessage = null },
                            text = { Text("Escanear", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; errorMessage = null },
                            text = { Text("Arquivo / Foto", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2; errorMessage = null },
                            text = { Text("Código", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    when (selectedTab) {
                        0 -> {
                            // LIVE CAMERAX QR CODE SCANNER
                            if (hasCameraPermission) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CameraLiveScanner(
                                        onQrDetected = { payload ->
                                            val pkg = ChecklistShareUtil.decodeChecklistPayload(payload)
                                            if (pkg != null) {
                                                triggerVibration(context)
                                                parsedPackage = pkg
                                                errorMessage = null
                                            } else {
                                                errorMessage = "QR Code lido não é um checklist compatível."
                                            }
                                        }
                                    )

                                    // Viewfinder Overlay
                                    Box(
                                        modifier = Modifier
                                            .size(180.dp)
                                            .border(2.dp, colors.primary, RoundedCornerShape(12.dp))
                                    )

                                    Text(
                                        "Posicione o QR Code no quadrado",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 10.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(40.dp), tint = colors.primary)
                                    Text("Permissão de Câmera Necessária", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                                    Text("Permita o acesso à câmera para ler QR Codes diretamente.", fontSize = 12.sp, color = colors.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Ativar Câmera", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                        1 -> {
                            // ARQUIVO & FOTO DA GALERIA
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    if (isReadingFile) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Selecionar Arquivo (.relatopro / .json)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
                                    Text("OU", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
                                }

                                OutlinedButton(
                                    onClick = { qrImagePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Carregar Foto do QR Code da Galeria", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textPrimary)
                                }
                            }
                        }
                        2 -> {
                            // DIGITAR / COLAR CÓDIGO
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text("Cole o código do checklist abaixo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = rawCodeInput,
                                    onValueChange = { rawCodeInput = it; errorMessage = null },
                                    placeholder = { Text("RPRO:2:... ou RELATOPRO:CHK:1:...") },
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.border,
                                        focusedContainerColor = colors.surfaceVariant,
                                        unfocusedContainerColor = colors.surfaceVariant
                                    )
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        if (rawCodeInput.isNotBlank()) {
                                            val pkg = ChecklistShareUtil.decodeChecklistPayload(rawCodeInput)
                                            if (pkg != null) {
                                                triggerVibration(context)
                                                parsedPackage = pkg
                                                errorMessage = null
                                            } else {
                                                errorMessage = "Código inválido. Verifique o texto colado."
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text("Validar Código", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.statusNaoConforme.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, colors.statusNaoConforme.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(errorMessage!!, color = colors.statusNaoConforme, fontSize = 12.sp)
                        }
                    }
                } else {
                    // PREVIEW OF PARSED CHECKLIST
                    val pkg = parsedPackage!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(colors.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Checklist, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(pkg.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                    Text("${pkg.categories.size} categorias • ${pkg.totalQuestions} perguntas", fontSize = 12.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (pkg.description.isNotBlank()) {
                                Text(pkg.description, fontSize = 12.sp, color = colors.textSecondary)
                            }

                            HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

                            Text("Estrutura do Formulário:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            pkg.categories.forEach { cat ->
                                Text("• ${cat.name} (${cat.items.size} itens)", fontSize = 11.sp, color = colors.textSecondary)
                            }
                        }
                    }

                    Text("Deseja importar este formulário como um modelo em 'Meus Checklists'?", fontSize = 12.sp, color = colors.textSecondary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { parsedPackage = null },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                        ) {
                            Text("Voltar", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                onConfirmImport(pkg)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Confirmar Importação", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Componente de câmera em tempo real que analisa cada frame com o ZXing
 */
@Composable
fun CameraLiveScanner(
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isScanningActive by remember { mutableStateOf(true) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val multiFormatReader = MultiFormatReader().apply {
                    setHints(mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                        DecodeHintType.CHARACTER_SET to "UTF-8"
                    ))
                }

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (!isScanningActive) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val plane = imageProxy.planes.firstOrNull()
                    if (plane != null) {
                        val buffer: ByteBuffer = plane.buffer
                        val data = ByteArray(buffer.remaining())
                        buffer.get(data)

                        val width = imageProxy.width
                        val height = imageProxy.height
                        val source = PlanarYUVLuminanceSource(
                            data, width, height, 0, 0, width, height, false
                        )
                        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                        try {
                            val result = multiFormatReader.decodeWithState(binaryBitmap)
                            val text = result.text
                            if (!text.isNullOrBlank()) {
                                isScanningActive = false
                                ContextCompat.getMainExecutor(ctx).execute {
                                    onQrDetected(text)
                                }
                            }
                        } catch (e: Exception) {
                            // Frame sem QR Code válido, continua escaneando
                        } finally {
                            multiFormatReader.reset()
                        }
                    }
                    imageProxy.close()
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun triggerVibration(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
