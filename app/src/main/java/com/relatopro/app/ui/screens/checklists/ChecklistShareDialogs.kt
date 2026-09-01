package com.relatopro.app.ui.screens.checklists

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.ui.theme.AppTheme
import com.relatopro.app.utils.ChecklistShareUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val json = ChecklistShareUtil.serializeChecklistToJson(template, fields)
            val payload = ChecklistShareUtil.encodeChecklistToQrPayload(json)
            qrPayload = payload
            qrBitmap = ChecklistShareUtil.generateQrCodeBitmap(payload, 512)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
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

                // QR Code Container
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code do Checklist",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }

                Text(
                    "Aponte a câmera de outro dispositivo para importar este checklist instantaneamente.",
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
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        if (isExportingFile) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Enviar Arquivo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        Text("Copiar Código", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Arquivo / Imagem, 1 = Digitar Código
    var rawCodeInput by remember { mutableStateOf("") }
    var parsedPackage by remember { mutableStateOf<ChecklistShareUtil.ChecklistPackage?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isReadingFile by remember { mutableStateOf(false) }

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
                        errorMessage = "Arquivo inválido ou corrompido. Certifique-se de selecionar um arquivo .relatopro ou .json exportado pelo Relato Pro."
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
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
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
                            text = { Text("Arquivo / QR", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; errorMessage = null },
                            text = { Text("Digitar Código", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    when (selectedTab) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    if (isReadingFile) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Selecionar Arquivo (.relatopro / .json)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Carregar Foto/Imagem do QR Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        1 -> {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text("Cole o código do checklist abaixo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = rawCodeInput,
                                    onValueChange = { rawCodeInput = it; errorMessage = null },
                                    placeholder = { Text("RELATOPRO:CHK:1:...") },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
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
                                            val pkg = ChecklistShareUtil.decodeChecklistPayload(rawCodeInput) ?: ChecklistShareUtil.parseChecklistJson(rawCodeInput)
                                            if (pkg != null) {
                                                parsedPackage = pkg
                                                errorMessage = null
                                            } else {
                                                errorMessage = "Código inválido. Verifique o texto colado."
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text("Validar Código", fontWeight = FontWeight.Bold)
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
                            pkg.categories.forEachIndexed { idx, cat ->
                                Text("• ${cat.name} (${cat.items.size} itens)", fontSize = 11.sp, color = colors.textSecondary)
                            }
                        }
                    }

                    Text("Deseja importar este formulário como uma cópia independente em 'Meus Checklists'?", fontSize = 12.sp, color = colors.textSecondary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { parsedPackage = null },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                        ) {
                            Text("Voltar", color = colors.textPrimary)
                        }

                        Button(
                            onClick = {
                                onConfirmImport(pkg)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Confirmar Importação", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
