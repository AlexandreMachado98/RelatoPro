package com.relatopro.app.ui.screens.importer

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.relatopro.app.importer.model.*
import com.relatopro.app.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartImportScreen(
    viewModel: SmartImportViewModel,
    onNavigateBack: () -> Unit,
    onStartReportWithTemplate: (templateId: Long) -> Unit,
    onNavigateToTemplates: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val importState by viewModel.importState.collectAsState()
    val checklist by viewModel.parsedChecklist.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var showEditItemDialog by remember { mutableStateOf<Triple<String, ParsedItem, Boolean>?>(null) } // (sectionId, item, isNew)
    var showAddStatusDialog by remember { mutableStateOf(false) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showEditHeaderDialog by remember { mutableStateOf(false) }
    var savedSuccessTemplateId by remember { mutableStateOf<Long?>(null) }

    // File pickers for different formats
    val universalPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                viewModel.startImport(uri)
            }
        }
    )

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (checklist == null) "Importar Checklist Inteligente" else "Revisar e Personalizar Checklist",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (checklist == null) "PDF, Word, Excel, CSV ou Imagem" else "${checklist?.totalItemsCount ?: 0} itens em ${checklist?.sections?.size ?: 0} seções",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (checklist != null) {
                            viewModel.resetState()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                },
                actions = {
                    if (checklist != null) {
                        Button(
                            onClick = {
                                viewModel.saveAsNativeTemplate(
                                    onSuccess = { tplId ->
                                        savedSuccessTemplateId = tplId
                                    },
                                    onError = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp).height(36.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Salvar Modelo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (checklist == null) {
                // FILE SELECTION HUB
                FileSelectionHub(
                    onSelectFormat = { mimeType ->
                        universalPickerLauncher.launch(mimeType)
                    }
                )
            } else {
                // INTERACTIVE PREVIEW & EDITOR
                ChecklistPreviewEditor(
                    checklist = checklist!!,
                    onEditHeader = { showEditHeaderDialog = true },
                    onAddStatus = { showAddStatusDialog = true },
                    onRemoveStatus = { statusShortCode ->
                        val success = viewModel.removeStatus(statusShortCode)
                        if (!success) {
                            Toast.makeText(context, "Não foi possível remover o status.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAddSection = { showAddSectionDialog = true },
                    onRenameSection = { secId, name -> viewModel.renameSection(secId, name) },
                    onDeleteSection = { secId -> viewModel.deleteSection(secId) },
                    onMoveSectionUp = { secId -> viewModel.moveSectionUp(secId) },
                    onMoveSectionDown = { secId -> viewModel.moveSectionDown(secId) },
                    onAddItem = { secId ->
                        val dummyItem = ParsedItem(label = "", sectionName = "")
                        showEditItemDialog = Triple(secId, dummyItem, true)
                    },
                    onEditItem = { secId, item ->
                        showEditItemDialog = Triple(secId, item, false)
                    },
                    onDeleteItem = { secId, itemId -> viewModel.deleteItem(secId, itemId) },
                    onDuplicateItem = { secId, itemId -> viewModel.duplicateItem(secId, itemId) },
                    onMoveItemUp = { secId, itemId -> viewModel.moveItemUp(secId, itemId) },
                    onMoveItemDown = { secId, itemId -> viewModel.moveItemDown(secId, itemId) }
                )
            }

            // PROCESSING OVERLAY
            if (importState is ImportProgressState.Processing) {
                val proc = importState as ImportProgressState.Processing
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
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                            Text(proc.stage, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary, textAlign = TextAlign.Center)
                            Text(proc.details, fontSize = 12.sp, color = colors.textSecondary, textAlign = TextAlign.Center)

                            LinearProgressIndicator(
                                progress = { proc.progressPercent.toFloat() / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = colors.primary,
                                trackColor = colors.surfaceVariant
                            )

                            Text("${proc.progressPercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.primary)

                            Spacer(Modifier.height(6.dp))

                            OutlinedButton(
                                onClick = { viewModel.cancelImport() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.statusNaoConforme),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.statusNaoConforme.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancelar Importação", fontSize = 12.sp, color = colors.statusNaoConforme)
                            }
                        }
                    }
                }
            }

            // ERROR DIALOG
            if (importState is ImportProgressState.Error) {
                val err = importState as ImportProgressState.Error
                AlertDialog(
                    onDismissRequest = { viewModel.resetState() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = colors.statusNaoConforme)
                            Spacer(Modifier.width(8.dp))
                            Text("Falha na Importação", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                    },
                    text = { Text(err.message, color = colors.textSecondary, fontSize = 13.sp) },
                    confirmButton = {
                        Button(onClick = { viewModel.resetState() }, colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) {
                            Text("Tentar Novamente", color = Color.White)
                        }
                    },
                    containerColor = colors.surface
                )
            }

            // SUCCESS DIALOG
            if (savedSuccessTemplateId != null) {
                val tplId = savedSuccessTemplateId!!
                AlertDialog(
                    onDismissRequest = {
                        savedSuccessTemplateId = null
                        onNavigateToTemplates()
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.statusConforme)
                            Spacer(Modifier.width(8.dp))
                            Text("Modelo Salvo com Sucesso!", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                    },
                    text = {
                        Text(
                            "O checklist foi estruturado e adicionado aos seus modelos nativos do Relato Pro.\n\nVocê já pode utilizá-lo para realizar novas inspeções em campo!",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                savedSuccessTemplateId = null
                                onStartReportWithTemplate(tplId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Usar Agora", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            savedSuccessTemplateId = null
                            onNavigateToTemplates()
                        }) {
                            Text("Ver Modelos", color = colors.textSecondary)
                        }
                    },
                    containerColor = colors.surface
                )
            }

            // EDIT ITEM DIALOG
            if (showEditItemDialog != null) {
                val (secId, item, isNew) = showEditItemDialog!!
                EditItemModal(
                    item = item,
                    availableStatuses = checklist?.availableStatuses ?: emptyList(),
                    isNew = isNew,
                    onDismiss = { showEditItemDialog = null },
                    onConfirm = { label, type, obs, photo, attach, req ->
                        if (isNew) {
                            viewModel.addItem(secId, label, type)
                        } else {
                            viewModel.updateItem(secId, item.id, label, type, obs, photo, attach, req)
                        }
                        showEditItemDialog = null
                    }
                )
            }

            // ADD STATUS DIALOG
            if (showAddStatusDialog) {
                AddStatusModal(
                    onDismiss = { showAddStatusDialog = false },
                    onConfirm = { name, code, desc, color, score ->
                        viewModel.addCustomStatus(name, code, desc, color, score)
                        showAddStatusDialog = false
                    }
                )
            }

            // ADD SECTION DIALOG
            if (showAddSectionDialog) {
                var sectionNameInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showAddSectionDialog = false },
                    title = { Text("Nova Seção", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                    text = {
                        OutlinedTextField(
                            value = sectionNameInput,
                            onValueChange = { sectionNameInput = it },
                            placeholder = { Text("Ex: 3. EQUIPAMENTOS DE COMBATE A INCÊNDIO") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (sectionNameInput.isNotBlank()) {
                                    viewModel.addSection(sectionNameInput)
                                    showAddSectionDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Adicionar", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddSectionDialog = false }) {
                            Text("Cancelar", color = colors.textSecondary)
                        }
                    },
                    containerColor = colors.surface
                )
            }

            // EDIT HEADER DIALOG
            if (showEditHeaderDialog && checklist != null) {
                var titleInput by remember { mutableStateOf(checklist!!.title) }
                var catInput by remember { mutableStateOf(checklist!!.category) }
                var descInput by remember { mutableStateOf(checklist!!.description) }

                AlertDialog(
                    onDismissRequest = { showEditHeaderDialog = false },
                    title = { Text("Editar Informações Gerais", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text("Nome do Checklist") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = catInput,
                                onValueChange = { catInput = it },
                                label = { Text("Categoria") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = descInput,
                                onValueChange = { descInput = it },
                                label = { Text("Descrição / Instruções") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.updateTitle(titleInput)
                                viewModel.updateCategory(catInput)
                                viewModel.updateDescription(descInput)
                                showEditHeaderDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Salvar", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditHeaderDialog = false }) {
                            Text("Cancelar", color = colors.textSecondary)
                        }
                    },
                    containerColor = colors.surface
                )
            }
        }
    }
}

// =======================================================
// FILE SELECTION HUB
// =======================================================
@Composable
private fun FileSelectionHub(
    onSelectFormat: (mimeType: String) -> Unit
) {
    val colors = AppTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).background(colors.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Importação Inteligente", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                            Text("Transforme seus arquivos existentes em formulários nativos", fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "O sistema interpreta títulos, seções, tabelas e perguntas, permitindo revisar e personalizar as respostas antes de salvar no aplicativo.",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Text("Selecione o formato do documento:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
        }

        item {
            FormatCard(
                title = "Documento PDF",
                description = "PDFs com texto selecionável, tabelas ou digitalizados (OCR)",
                icon = Icons.Default.PictureAsPdf,
                iconTint = Color(0xFFEF4444),
                badge = "PDF / OCR",
                onClick = { onSelectFormat("application/pdf") }
            )
        }

        item {
            FormatCard(
                title = "Documento Word (.docx / .doc)",
                description = "Textos, tabelas, parágrafos e listas estruturadas",
                icon = Icons.Default.Description,
                iconTint = Color(0xFF2563EB),
                badge = "DOC / DOCX",
                onClick = { onSelectFormat("application/*") }
            )
        }

        item {
            FormatCard(
                title = "Planilha Excel (.xlsx / .xls)",
                description = "Planilhas com linhas, colunas, cabeçalhos dinâmicos e abas",
                icon = Icons.Default.TableChart,
                iconTint = Color(0xFF10B981),
                badge = "XLS / XLSX",
                onClick = { onSelectFormat("application/*") }
            )
        }

        item {
            FormatCard(
                title = "Arquivo CSV / Texto",
                description = "Separadores automáticos (vírgula, ponto e vírgula, tab)",
                icon = Icons.Default.ListAlt,
                iconTint = Color(0xFFF59E0B),
                badge = "CSV / TXT",
                onClick = { onSelectFormat("text/*") }
            )
        }

        item {
            FormatCard(
                title = "Foto de Checklist / Digitalização",
                description = "Reconhecimento ótico de caracteres (OCR on-device)",
                icon = Icons.Default.CameraEnhance,
                iconTint = Color(0xFF8B5CF6),
                badge = "JPG / PNG / WEBP",
                onClick = { onSelectFormat("image/*") }
            )
        }
    }
}

@Composable
private fun FormatCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    badge: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                    Box(
                        modifier = Modifier.background(colors.surfaceVariant, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(description, fontSize = 11.sp, color = colors.textSecondary, lineHeight = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textSecondary)
        }
    }
}

// =======================================================
// INTERACTIVE PREVIEW & EDITOR
// =======================================================
@Composable
private fun ChecklistPreviewEditor(
    checklist: ParsedChecklist,
    onEditHeader: () -> Unit,
    onAddStatus: () -> Unit,
    onRemoveStatus: (String) -> Unit,
    onAddSection: () -> Unit,
    onRenameSection: (sectionId: String, name: String) -> Unit,
    onDeleteSection: (sectionId: String) -> Unit,
    onMoveSectionUp: (sectionId: String) -> Unit,
    onMoveSectionDown: (sectionId: String) -> Unit,
    onAddItem: (sectionId: String) -> Unit,
    onEditItem: (sectionId: String, item: ParsedItem) -> Unit,
    onDeleteItem: (sectionId: String, itemId: String) -> Unit,
    onDuplicateItem: (sectionId: String, itemId: String) -> Unit,
    onMoveItemUp: (sectionId: String, itemId: String) -> Unit,
    onMoveItemDown: (sectionId: String, itemId: String) -> Unit
) {
    val colors = AppTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // HEADER CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.background(colors.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(checklist.category.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                        }

                        IconButton(onClick = onEditHeader, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Cabeçalho", tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(checklist.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)

                    if (checklist.description.isNotBlank()) {
                        Text(checklist.description, fontSize = 12.sp, color = colors.textSecondary)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• ${checklist.sections.size} seções", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                        Text("• ${checklist.totalItemsCount} perguntas", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                        Text("• Formato: ${checklist.sourceFormat.extensionLabel}", fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }
        }

        // WARNINGS BANNER (IF ANY)
        if (checklist.warnings.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.statusWarning.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.statusWarning.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = colors.statusWarning, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Avisos da Interpretação:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textPrimary)
                            checklist.warnings.forEach { warn ->
                                Text("• $warn", fontSize = 11.sp, color = colors.textSecondary, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // STATUS CUSTOMIZATION PANEL
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Personalize as Respostas (Status)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                            Text("Opções de avaliação aplicadas aos itens de verificação", fontSize = 11.sp, color = colors.textSecondary)
                        }
                        IconButton(onClick = onAddStatus, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Adicionar Status", tint = colors.primary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        checklist.availableStatuses.forEach { status ->
                            val color = parseHexColor(status.colorHex, colors.primary)
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${status.name} (${status.shortCode})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                    if (!status.isSystemDefault) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remover",
                                            tint = color,
                                            modifier = Modifier.size(12.dp).clickable { onRemoveStatus(status.shortCode) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTIONS HEADER & ADD SECTION BUTTON
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Estrutura de Seções e Perguntas", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                OutlinedButton(
                    onClick = onAddSection,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Nova Seção", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // SECTIONS LIST
        checklist.sections.forEachIndexed { sIdx, section ->
            item(key = "section_${section.id}") {
                SectionCard(
                    section = section,
                    isFirst = sIdx == 0,
                    isLast = sIdx == checklist.sections.lastIndex,
                    onMoveUp = { onMoveSectionUp(section.id) },
                    onMoveDown = { onMoveSectionDown(section.id) },
                    onDelete = { onDeleteSection(section.id) },
                    onAddItem = { onAddItem(section.id) },
                    onEditItem = { item -> onEditItem(section.id, item) },
                    onDeleteItem = { itemId -> onDeleteItem(section.id, itemId) },
                    onDuplicateItem = { itemId -> onDuplicateItem(section.id, itemId) },
                    onMoveItemUp = { itemId -> onMoveItemUp(section.id, itemId) },
                    onMoveItemDown = { itemId -> onMoveItemDown(section.id, itemId) }
                )
            }
        }

        item {
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionCard(
    section: ParsedSection,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (ParsedItem) -> Unit,
    onDeleteItem: (itemId: String) -> Unit,
    onDuplicateItem: (itemId: String) -> Unit,
    onMoveItemUp: (itemId: String) -> Unit,
    onMoveItemDown: (itemId: String) -> Unit
) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Section Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = section.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isFirst) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Subir Seção", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (!isLast) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Descer Seção", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir Seção", tint = colors.statusNaoConforme, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            // Items List
            if (section.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum item nesta seção. Toque abaixo para adicionar.", fontSize = 11.sp, color = colors.textSecondary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.items.forEachIndexed { iIdx, item ->
                        ItemRow(
                            item = item,
                            index = iIdx,
                            isFirst = iIdx == 0,
                            isLast = iIdx == section.items.lastIndex,
                            onEdit = { onEditItem(item) },
                            onDelete = { onDeleteItem(item.id) },
                            onDuplicate = { onDuplicateItem(item.id) },
                            onMoveUp = { onMoveItemUp(item.id) },
                            onMoveDown = { onMoveItemDown(item.id) }
                        )
                    }
                }
            }

            // Add Item button
            Button(
                onClick = onAddItem,
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant, contentColor = colors.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("+ Adicionar Pergunta nesta Seção", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.primary)
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: ParsedItem,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.size(22.dp).background(colors.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.label,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = colors.textPrimary,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isFirst) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (!isLast) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir", tint = colors.statusNaoConforme, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status choices preview
                Text(
                    text = "[ ${item.allowedStatuses.joinToString(" ")} ]",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                if (item.allowPhoto) {
                    Box(modifier = Modifier.background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("📷 Foto", fontSize = 9.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }

                if (item.allowObservation) {
                    Box(modifier = Modifier.background(colors.cyanAccent.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("💬 Obs", fontSize = 9.sp, color = colors.cyanAccent, fontWeight = FontWeight.Bold)
                    }
                }

                if (item.isRequired) {
                    Box(modifier = Modifier.background(colors.statusWarning.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("Obrigatório", fontSize = 9.sp, color = colors.statusWarning, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =======================================================
// DIALOGS: EDIT ITEM, ADD STATUS
// =======================================================
@Composable
private fun EditItemModal(
    item: ParsedItem,
    availableStatuses: List<CustomStatusConfig>,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (label: String, type: String, allowObs: Boolean, allowPhoto: Boolean, allowAttach: Boolean, isReq: Boolean) -> Unit
) {
    val colors = AppTheme.colors
    var labelInput by remember { mutableStateOf(item.label) }
    var typeSelection by remember { mutableStateOf(item.type) }
    var allowObs by remember { mutableStateOf(item.allowObservation) }
    var allowPhoto by remember { mutableStateOf(item.allowPhoto) }
    var allowAttach by remember { mutableStateOf(item.allowAttachment) }
    var isReq by remember { mutableStateOf(item.isRequired) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
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
                    Text(if (isNew) "Adicionar Pergunta" else "Editar Pergunta", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = colors.textSecondary)
                    }
                }

                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("Texto da Pergunta / Item") },
                    placeholder = { Text("Ex: Extintor está desobstruído e sinalizado?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )

                // Type selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tipo de Resposta:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "C_NC_NA" to "Status (C/NC/P/NA)",
                            "TEXT" to "Texto",
                            "NUMBER" to "Número",
                            "PHOTO" to "Apenas Foto"
                        ).forEach { (typeVal, typeLabel) ->
                            val selected = typeSelection == typeVal
                            FilterChip(
                                selected = selected,
                                onClick = { typeSelection = typeVal },
                                label = { Text(typeLabel, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                // Toggles
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { allowPhoto = !allowPhoto }) {
                        Checkbox(checked = allowPhoto, onCheckedChange = { allowPhoto = it })
                        Text("Permitir anexar fotos da evidência", fontSize = 12.sp, color = colors.textPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { allowObs = !allowObs }) {
                        Checkbox(checked = allowObs, onCheckedChange = { allowObs = it })
                        Text("Permitir observação técnica", fontSize = 12.sp, color = colors.textPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isReq = !isReq }) {
                        Checkbox(checked = isReq, onCheckedChange = { isReq = it })
                        Text("Item de resposta obrigatória", fontSize = 12.sp, color = colors.textPrimary)
                    }
                }

                Spacer(Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (labelInput.isNotBlank()) {
                            onConfirm(labelInput.trim(), typeSelection, allowObs, allowPhoto, allowAttach, isReq)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Salvar Item", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddStatusModal(
    onDismiss: () -> Unit,
    onConfirm: (name: String, shortCode: String, desc: String, colorHex: String, score: Float?) -> Unit
) {
    val colors = AppTheme.colors
    var nameInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#8B5CF6") }

    val colorOptions = listOf("#10B981", "#F59E0B", "#EF4444", "#3B82F6", "#8B5CF6", "#EC4899", "#6B7280")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Adicionar Status Customizado", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nome do Status (ex: Regular, Pendente)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase() },
                    label = { Text("Sigla Curta (ex: REG, PND, OK)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text("Descrição / Comportamento") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Text("Cor do Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { hex ->
                        val col = parseHexColor(hex, Color.Gray)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(if (selectedColor == hex) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && codeInput.isNotBlank()) {
                            onConfirm(nameInput.trim(), codeInput.trim(), descInput.trim(), selectedColor, 1.0f)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Adicionar Status", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        val clean = hex.removePrefix("#")
        Color(android.graphics.Color.parseColor("#$clean"))
    } catch (e: Exception) {
        defaultColor
    }
}
