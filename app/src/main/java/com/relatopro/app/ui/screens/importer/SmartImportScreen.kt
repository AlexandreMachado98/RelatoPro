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
    var showAddCategoryDialog by remember { mutableStateOf<String?>(null) } // sectionId
    var showEditHeaderDialog by remember { mutableStateOf(false) }
    var showConflictResolveDialog by remember { mutableStateOf<ParsedItem?>(null) }
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
                                Text("Salvar Modelo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = importState) {
                is ImportProgressState.Idle -> {
                    FileSelectionHub(
                        onSelectFormat = { mimeType ->
                            universalPickerLauncher.launch(mimeType)
                        }
                    )
                }

                is ImportProgressState.Processing -> {
                    ImportProgressDialog(
                        stage = state.stage,
                        percent = state.progressPercent,
                        details = state.details,
                        onCancel = { viewModel.cancelImport() }
                    )
                }

                is ImportProgressState.Error -> {
                    ImportErrorView(
                        errorMessage = state.message,
                        onRetry = { viewModel.resetState() }
                    )
                }

                is ImportProgressState.Success -> {
                    val parsed = checklist ?: state.checklist
                    ChecklistPreviewEditor(
                        checklist = parsed,
                        onEditHeader = { showEditHeaderDialog = true },
                        onAddStatus = { showAddStatusDialog = true },
                        onRemoveStatus = { code -> viewModel.removeStatus(code) },
                        onAddSection = { showAddSectionDialog = true },
                        onAddCategory = { sectionId -> showAddCategoryDialog = sectionId },
                        onRenameSection = { secId, name -> viewModel.renameSection(secId, name) },
                        onDeleteSection = { secId -> viewModel.deleteSection(secId) },
                        onMoveSectionUp = { secId -> viewModel.moveSectionUp(secId) },
                        onMoveSectionDown = { secId -> viewModel.moveSectionDown(secId) },
                        onAddItem = { secId, catId ->
                            showEditItemDialog = Triple(secId, ParsedItem(label = "", sectionName = ""), true)
                        },
                        onEditItem = { secId, catId, item ->
                            showEditItemDialog = Triple(secId, item, false)
                        },
                        onDeleteItem = { secId, catId, itemId ->
                            viewModel.deleteItem(secId, catId, itemId)
                        },
                        onDuplicateItem = { secId, catId, itemId ->
                            viewModel.duplicateItem(secId, catId, itemId)
                        },
                        onMoveItemUp = { secId, itemId ->
                            viewModel.moveItemUp(secId, itemId)
                        },
                        onMoveItemDown = { secId, itemId ->
                            viewModel.moveItemDown(secId, itemId)
                        },
                        onPromoteToSection = { secId, catId, itemId ->
                            viewModel.promoteToSection(secId, catId, itemId)
                        },
                        onResolveConflict = { item ->
                            showConflictResolveDialog = item
                        }
                    )
                }
            }

            // SUCCESS DIALOG AFTER SAVING
            if (savedSuccessTemplateId != null) {
                val tplId = savedSuccessTemplateId!!
                AlertDialog(
                    onDismissRequest = { savedSuccessTemplateId = null },
                    icon = {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.statusConforme, modifier = Modifier.size(36.dp))
                    },
                    title = {
                        Text("Modelo Salvo com Sucesso!", fontWeight = FontWeight.Bold, color = colors.textPrimary)
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

            // CONFLICT RESOLUTION DIALOG
            if (showConflictResolveDialog != null) {
                val item = showConflictResolveDialog!!
                val codes = item.preFilledAnswer?.conflictingCodes ?: listOf("C", "NC")
                AlertDialog(
                    onDismissRequest = { showConflictResolveDialog = null },
                    icon = {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = colors.statusWarning, modifier = Modifier.size(32.dp))
                    },
                    title = {
                        Text("Resolver Conflito de Resposta", fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 15.sp)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Item: \"${item.label}\"", fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                            Text(
                                "Foram encontradas múltiplas marcações no documento original para este item. Escolha o status correto:",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                codes.forEach { code ->
                                    Button(
                                        onClick = {
                                            viewModel.resolveAnswerConflict(item.id, code)
                                            showConflictResolveDialog = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                                    ) {
                                        Text(code, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showConflictResolveDialog = null }) {
                            Text("Cancelar", color = colors.textSecondary)
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
                            viewModel.addItem(secId, null, label, type)
                        } else {
                            viewModel.updateItem(secId, null, item.id, label, type, obs, photo, attach, req)
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
                    title = { Text("Nova Seção Principal", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
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

            // ADD CATEGORY DIALOG
            if (showAddCategoryDialog != null) {
                val secId = showAddCategoryDialog!!
                var categoryNameInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showAddCategoryDialog = null },
                    title = { Text("Nova Categoria / Grupo", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                    text = {
                        OutlinedTextField(
                            value = categoryNameInput,
                            onValueChange = { categoryNameInput = it },
                            placeholder = { Text("Ex: 1.1 Documentos Obrigatórios") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (categoryNameInput.isNotBlank()) {
                                    viewModel.addCategory(secId, categoryNameInput)
                                    showAddCategoryDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Adicionar", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddCategoryDialog = null }) {
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
                            Text("Importação de Alta Precisão", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                            Text("Identificação hierárquica fiel da estrutura do seu documento", fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "O sistema analisa títulos, seções numeradas, categorias, tabelas e cabeçalhos de status sem alterar o texto original técnico, permitindo revisar e personalizar as respostas antes de salvar no aplicativo.",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Text("Selecione o formato do seu modelo:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
        }

        item {
            FormatCard(
                title = "Documento PDF",
                description = "Reconhecimento espacial de páginas, seções e tabelas",
                icon = Icons.Default.PictureAsPdf,
                iconTint = Color(0xFFEF4444),
                badge = "PDF / OCR",
                onClick = { onSelectFormat("application/pdf") }
            )
        }

        item {
            FormatCard(
                title = "Documento Word",
                description = "Estilos de títulos (Headings), tabelas e listas estruturadas",
                icon = Icons.Default.Description,
                iconTint = Color(0xFF2563EB),
                badge = "DOC / DOCX",
                onClick = { onSelectFormat("*/*") }
            )
        }

        item {
            FormatCard(
                title = "Planilha Excel",
                description = "Detecção de células mescladas, abas, colunas C/NC e itens",
                icon = Icons.Default.TableChart,
                iconTint = Color(0xFF10B981),
                badge = "XLS / XLSX",
                onClick = { onSelectFormat("*/*") }
            )
        }

        item {
            FormatCard(
                title = "Arquivo CSV / Texto",
                description = "Delimitadores automáticos com mapeamento de colunas",
                icon = Icons.Default.FormatListBulleted,
                iconTint = Color(0xFFF59E0B),
                badge = "CSV / TXT",
                onClick = { onSelectFormat("text/*") }
            )
        }

        item {
            FormatCard(
                title = "Foto de Checklist / Digitalização",
                description = "Reconhecimento ótico com reconstrução de layout on-device",
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
    onAddCategory: (sectionId: String) -> Unit,
    onRenameSection: (sectionId: String, name: String) -> Unit,
    onDeleteSection: (sectionId: String) -> Unit,
    onMoveSectionUp: (sectionId: String) -> Unit,
    onMoveSectionDown: (sectionId: String) -> Unit,
    onAddItem: (sectionId: String, categoryId: String?) -> Unit,
    onEditItem: (sectionId: String, categoryId: String?, item: ParsedItem) -> Unit,
    onDeleteItem: (sectionId: String, categoryId: String?, itemId: String) -> Unit,
    onDuplicateItem: (sectionId: String, categoryId: String?, itemId: String) -> Unit,
    onMoveItemUp: (sectionId: String, itemId: String) -> Unit,
    onMoveItemDown: (sectionId: String, itemId: String) -> Unit,
    onPromoteToSection: (sectionId: String, categoryId: String?, itemId: String) -> Unit,
    onResolveConflict: (item: ParsedItem) -> Unit
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
                        Text("• ${checklist.totalItemsCount} itens", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                        Text("• Formato: ${checklist.sourceFormat.extensionLabel}", fontSize = 11.sp, color = colors.textSecondary)
                        
                        val confidencePercent = (checklist.totalConfidenceScore * 100).toInt()
                        val confColor = if (confidencePercent >= 90) colors.statusConforme else if (confidencePercent >= 65) colors.statusWarning else colors.statusNaoConforme
                        Box(
                            modifier = Modifier.background(confColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Precisão: $confidencePercent%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = confColor)
                        }
                    }
                }
            }
        }

        // VALIDATION ALERTS PANEL ("REVISÕES NECESSÁRIAS")
        if (checklist.validationAlerts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.statusWarning.copy(alpha = 0.10f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.statusWarning.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = colors.statusWarning, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Revisões Recomendadas (${checklist.validationAlerts.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
                        }

                        checklist.validationAlerts.forEach { alert ->
                            val iconColor = when (alert.severity) {
                                AlertSeverity.CRITICAL -> colors.statusNaoConforme
                                AlertSeverity.WARNING -> colors.statusWarning
                                AlertSeverity.INFO -> colors.primary
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier.size(6.dp).offset(y = 5.dp).background(iconColor, CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = colors.textPrimary)
                                    Text(alert.description, fontSize = 10.sp, color = colors.textSecondary, lineHeight = 13.sp)
                                }
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
                            Text("Opções de Avaliação (Status)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                            Text("Respostas aplicadas aos itens de verificação", fontSize = 11.sp, color = colors.textSecondary)
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
                Text("Estrutura Hierárquica do Checklist", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
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
                    onAddCategory = { onAddCategory(section.id) },
                    onAddItem = { catId -> onAddItem(section.id, catId) },
                    onEditItem = { catId, item -> onEditItem(section.id, catId, item) },
                    onDeleteItem = { catId, itemId -> onDeleteItem(section.id, catId, itemId) },
                    onDuplicateItem = { catId, itemId -> onDuplicateItem(section.id, catId, itemId) },
                    onMoveItemUp = { itemId -> onMoveItemUp(section.id, itemId) },
                    onMoveItemDown = { itemId -> onMoveItemDown(section.id, itemId) },
                    onPromoteToSection = { catId, itemId -> onPromoteToSection(section.id, catId, itemId) },
                    onResolveConflict = onResolveConflict
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
    onAddCategory: () -> Unit,
    onAddItem: (categoryId: String?) -> Unit,
    onEditItem: (categoryId: String?, ParsedItem) -> Unit,
    onDeleteItem: (categoryId: String?, itemId: String) -> Unit,
    onDuplicateItem: (categoryId: String?, itemId: String) -> Unit,
    onMoveItemUp: (itemId: String) -> Unit,
    onMoveItemDown: (itemId: String) -> Unit,
    onPromoteToSection: (categoryId: String?, itemId: String) -> Unit,
    onResolveConflict: (item: ParsedItem) -> Unit
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

            // Direct items under section
            if (section.items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.items.forEachIndexed { iIdx, item ->
                        ItemRow(
                            item = item,
                            index = iIdx,
                            isFirst = iIdx == 0,
                            isLast = iIdx == section.items.lastIndex,
                            onEdit = { onEditItem(null, item) },
                            onDelete = { onDeleteItem(null, item.id) },
                            onDuplicate = { onDuplicateItem(null, item.id) },
                            onMoveUp = { onMoveItemUp(item.id) },
                            onMoveDown = { onMoveItemDown(item.id) },
                            onPromoteToSection = { onPromoteToSection(null, item.id) },
                            onResolveConflict = { onResolveConflict(item) }
                        )
                    }
                }
            }

            // Subcategories under section
            section.categories.forEach { category ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.3f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(category.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textPrimary)
                        }

                        category.items.forEachIndexed { cIdx, item ->
                            ItemRow(
                                item = item,
                                index = cIdx,
                                isFirst = cIdx == 0,
                                isLast = cIdx == category.items.lastIndex,
                                onEdit = { onEditItem(category.id, item) },
                                onDelete = { onDeleteItem(category.id, item.id) },
                                onDuplicate = { onDuplicateItem(category.id, item.id) },
                                onMoveUp = { onMoveItemUp(item.id) },
                                onMoveDown = { onMoveItemDown(item.id) },
                                onPromoteToSection = { onPromoteToSection(category.id, item.id) },
                                onResolveConflict = { onResolveConflict(item) }
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddCategory,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Subcategoria", fontSize = 11.sp)
                }

                Button(
                    onClick = { onAddItem(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant, contentColor = colors.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Pergunta", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                }
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
    onMoveDown: () -> Unit,
    onPromoteToSection: () -> Unit,
    onResolveConflict: () -> Unit
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
                    IconButton(onClick = onPromoteToSection, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DriveFileMove, contentDescription = "Promover a Seção", tint = colors.primary, modifier = Modifier.size(14.dp))
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
                // Confidence badge
                val confColor = when (item.confidenceRating) {
                    ConfidenceRating.HIGH -> colors.statusConforme
                    ConfidenceRating.MEDIUM -> colors.statusWarning
                    ConfidenceRating.LOW -> colors.statusNaoConforme
                }
                Box(
                    modifier = Modifier.background(confColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(item.confidenceRating.label, fontSize = 9.sp, color = confColor, fontWeight = FontWeight.Bold)
                }

                // Pre-filled answer badge (if any)
                if (item.preFilledAnswer != null) {
                    if (item.preFilledAnswer.hasConflict) {
                        Box(
                            modifier = Modifier.clickable(onClick = onResolveConflict)
                                .background(colors.statusNaoConforme.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text("⚠ Conflito: ${item.preFilledAnswer.conflictingCodes.joinToString("/")}", fontSize = 9.sp, color = colors.statusNaoConforme, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier.background(colors.statusConforme.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text("✓ Resposta: ${item.preFilledAnswer.statusShortCode}", fontSize = 9.sp, color = colors.statusConforme, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (item.allowPhoto) {
                    Box(modifier = Modifier.background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("📷 Foto", fontSize = 9.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }

                if (item.allowObservation) {
                    Box(modifier = Modifier.background(colors.surfaceVariant, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("📝 Obs", fontSize = 9.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// =======================================================
// PROGRESS & MODALS
// =======================================================
@Composable
private fun ImportProgressDialog(
    stage: String,
    percent: Int,
    details: String,
    onCancel: () -> Unit
) {
    val colors = AppTheme.colors

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    progress = { percent / 100f },
                    color = colors.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(56.dp)
                )

                Text(stage, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary, textAlign = TextAlign.Center)

                if (details.isNotBlank()) {
                    Text(details, fontSize = 12.sp, color = colors.textSecondary, textAlign = TextAlign.Center)
                }

                LinearProgressIndicator(
                    progress = { percent / 100f },
                    color = colors.primary,
                    trackColor = colors.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )

                Text("$percent%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.primary)

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun ImportErrorView(
    errorMessage: String,
    onRetry: () -> Unit
) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(colors.statusNaoConforme.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = colors.statusNaoConforme, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text("Erro ao Importar Checklist", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)

        Spacer(Modifier.height(8.dp))

        Text(errorMessage, fontSize = 13.sp, color = colors.textSecondary, textAlign = TextAlign.Center, lineHeight = 18.sp)

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tentar Novamente", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EditItemModal(
    item: ParsedItem,
    availableStatuses: List<CustomStatusConfig>,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (label: String, type: String, allowObs: Boolean, allowPhoto: Boolean, allowAttach: Boolean, isRequired: Boolean) -> Unit
) {
    val colors = AppTheme.colors

    var labelInput by remember { mutableStateOf(item.label) }
    var selectedType by remember { mutableStateOf(item.type) }
    var allowObs by remember { mutableStateOf(item.allowObservation) }
    var allowPhoto by remember { mutableStateOf(item.allowPhoto) }
    var allowAttach by remember { mutableStateOf(item.allowAttachment) }
    var isRequired by remember { mutableStateOf(item.isRequired) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isNew) "Adicionar Item de Inspeção" else "Editar Pergunta",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.textPrimary
                )

                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("Texto da Pergunta / Requisito") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tipo de Campo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("C_NC_NA" to "Conformidade (C/NC)", "TEXT" to "Texto", "NUMBER" to "Número").forEach { (typeKey, typeLabel) ->
                            FilterChip(
                                selected = selectedType == typeKey,
                                onClick = { selectedType = typeKey },
                                label = { Text(typeLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Permitir Observação", fontSize = 13.sp, color = colors.textPrimary)
                        Switch(checked = allowObs, onCheckedChange = { allowObs = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Permitir Foto", fontSize = 13.sp, color = colors.textPrimary)
                        Switch(checked = allowPhoto, onCheckedChange = { allowPhoto = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Item Obrigatório", fontSize = 13.sp, color = colors.textPrimary)
                        Switch(checked = isRequired, onCheckedChange = { isRequired = it })
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = colors.textSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (labelInput.isNotBlank()) {
                                onConfirm(labelInput.trim(), selectedType, allowObs, allowPhoto, allowAttach, isRequired)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddStatusModal(
    onDismiss: () -> Unit,
    onConfirm: (name: String, shortCode: String, desc: String, colorHex: String, scoreMultiplier: Float?) -> Unit
) {
    val colors = AppTheme.colors

    var nameInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#3B82F6") }
    var selectedScore by remember { mutableStateOf<Float?>(1.0f) }

    val presetColors = listOf("#10B981", "#F59E0B", "#EF4444", "#3B82F6", "#8B5CF6", "#6B7280", "#EC4899", "#14B8A6")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Adicionar Opção de Resposta", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nome do Status (Ex: Atende com Ressalva)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase() },
                    label = { Text("Sigla / Código Curto (Ex: RES)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text("Descrição / Critério (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Text("Cor do Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { hex ->
                        val color = parseHexColor(hex, colors.primary)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 1.dp,
                                    color = if (selectedColor == hex) colors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = colors.textSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank() && codeInput.isNotBlank()) {
                                onConfirm(nameInput.trim(), codeInput.trim(), descInput.trim(), selectedColor, selectedScore)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Adicionar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}
