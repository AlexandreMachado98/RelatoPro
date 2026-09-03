package com.relatopro.app.ui.screens.templatebuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateBuilderScreen(
    templateId: Long = 0L,
    viewModel: TemplateBuilderViewModel,
    onNavigateBack: () -> Unit
) {
    val colors = AppTheme.colors
    val name by viewModel.templateName.collectAsState()
    val description by viewModel.templateDescription.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val fields by viewModel.fields.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<String?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    var itemDialogTargetCategory by remember { mutableStateOf<String?>(null) }
    var itemToEditIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(templateId) {
        if (templateId > 0L) {
            viewModel.loadTemplate(templateId)
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (templateId > 0L) "Editar Checklist" else "Novo Checklist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveTemplate(onNavigateBack) },
                        enabled = name.isNotBlank() && !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Salvar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Checklist Basic Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Dados Gerais do Modelo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = viewModel::updateName,
                            label = { Text("Nome do Checklist *") },
                            placeholder = { Text("Ex: Inspeção de Segurança Industrial") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.border,
                                focusedContainerColor = colors.surface,
                                unfocusedContainerColor = colors.surface,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = viewModel::updateDescription,
                            label = { Text("Descrição / Instruções (Opcional)") },
                            placeholder = { Text("Instruções de aplicação para o inspetor...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 2,
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
                }
            }

            // 2. Categories and their Items
            itemsIndexed(categories) { catIndex, categoryName ->
                val categoryFields = fields.filter { 
                    it.category.equals(categoryName, ignoreCase = true) || (categoryName == "Geral" && it.category.isBlank())
                }

                CategoryCardBlock(
                    categoryName = categoryName,
                    catIndex = catIndex,
                    totalCategories = categories.size,
                    fields = fields,
                    categoryFields = categoryFields,
                    onMoveCategoryUp = { viewModel.moveCategoryUp(categoryName) },
                    onMoveCategoryDown = { viewModel.moveCategoryDown(categoryName) },
                    onRenameCategory = { categoryToEdit = categoryName },
                    onDuplicateCategory = { viewModel.duplicateCategory(categoryName) },
                    onDeleteCategory = { categoryToDelete = categoryName },
                    onAddItem = { itemDialogTargetCategory = categoryName },
                    onEditItem = { field ->
                        val globalIdx = fields.indexOf(field)
                        if (globalIdx != -1) itemToEditIndex = globalIdx
                    },
                    onDuplicateItem = { field ->
                        val globalIdx = fields.indexOf(field)
                        if (globalIdx != -1) viewModel.duplicateItem(globalIdx)
                    },
                    onMoveItemUp = { field ->
                        val globalIdx = fields.indexOf(field)
                        if (globalIdx != -1) viewModel.moveItemUp(globalIdx)
                    },
                    onMoveItemDown = { field ->
                        val globalIdx = fields.indexOf(field)
                        if (globalIdx != -1) viewModel.moveItemDown(globalIdx)
                    },
                    onRemoveItem = { field ->
                        val globalIdx = fields.indexOf(field)
                        if (globalIdx != -1) viewModel.removeItem(globalIdx)
                    }
                )
            }

            // 3. Add Category Button
            item {
                OutlinedButton(
                    onClick = { showAddCategoryDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.primary)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("+ ADICIONAR CATEGORIA", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // Modal: Add Category
    if (showAddCategoryDialog) {
        AddOrEditCategoryDialog(
            title = "Nova Categoria",
            initialName = "",
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { newCat ->
                viewModel.addCategory(newCat)
                showAddCategoryDialog = false
            }
        )
    }

    // Modal: Rename Category
    if (categoryToEdit != null) {
        val cat = categoryToEdit!!
        AddOrEditCategoryDialog(
            title = "Editar Categoria",
            initialName = cat,
            onDismiss = { categoryToEdit = null },
            onConfirm = { updated ->
                viewModel.renameCategory(cat, updated)
                categoryToEdit = null
            }
        )
    }

    // Modal: Confirm Delete Category
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        val count = fields.count { it.category.equals(cat, ignoreCase = true) || (cat == "Geral" && it.category.isBlank()) }
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Excluir Categoria?", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Text(
                    text = if (count > 0) {
                        "Esta categoria possui $count item(ns). Ao excluí-la, todos os itens pertencentes a ela também serão removidos."
                    } else {
                        "Deseja excluir a categoria \"$cat\"?"
                    },
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.statusNaoConforme)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    // Modal: Add Item to Category
    if (itemDialogTargetCategory != null) {
        val targetCat = itemDialogTargetCategory!!
        AddOrEditItemDialog(
            dialogTitle = "Novo Item em $targetCat",
            initialLabel = "",
            initialType = "C_NC_NA",
            onDismiss = { itemDialogTargetCategory = null },
            onConfirm = { label, type ->
                viewModel.addItemToCategory(targetCat, label, type)
                itemDialogTargetCategory = null
            }
        )
    }

    // Modal: Edit Item
    if (itemToEditIndex != null) {
        val idx = itemToEditIndex!!
        val field = fields.getOrNull(idx)
        if (field != null) {
            AddOrEditItemDialog(
                dialogTitle = "Editar Item",
                initialLabel = field.label,
                initialType = field.type,
                onDismiss = { itemToEditIndex = null },
                onConfirm = { label, type ->
                    viewModel.updateItem(idx, label, type)
                    itemToEditIndex = null
                }
            )
        }
    }
}

@Composable
fun CategoryCardBlock(
    categoryName: String,
    catIndex: Int,
    totalCategories: Int,
    fields: List<TemplateFieldEntity>,
    categoryFields: List<TemplateFieldEntity>,
    onMoveCategoryUp: () -> Unit,
    onMoveCategoryDown: () -> Unit,
    onRenameCategory: () -> Unit,
    onDuplicateCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (TemplateFieldEntity) -> Unit,
    onDuplicateItem: (TemplateFieldEntity) -> Unit,
    onMoveItemUp: (TemplateFieldEntity) -> Unit,
    onMoveItemDown: (TemplateFieldEntity) -> Unit,
    onRemoveItem: (TemplateFieldEntity) -> Unit
) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Category Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = categoryName.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "${categoryFields.size} item(ns)",
                            fontSize = 10.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                // Category Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (catIndex > 0) {
                        IconButton(onClick = onMoveCategoryUp, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir Categoria", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (catIndex < totalCategories - 1) {
                        IconButton(onClick = onMoveCategoryDown, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer Categoria", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    var catMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { catMenuExpanded = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opções da Categoria", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = catMenuExpanded,
                            onDismissRequest = { catMenuExpanded = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Renomear Categoria", color = colors.textPrimary) },
                                onClick = {
                                    catMenuExpanded = false
                                    onRenameCategory()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = colors.primary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicar Categoria", color = colors.textPrimary) },
                                onClick = {
                                    catMenuExpanded = false
                                    onDuplicateCategory()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.primary) }
                            )
                            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                            DropdownMenuItem(
                                text = { Text("Excluir Categoria", color = colors.statusNaoConforme) },
                                onClick = {
                                    catMenuExpanded = false
                                    onDeleteCategory()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.statusNaoConforme) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.border.copy(alpha = 0.6f))

            // Items List
            if (categoryFields.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum item cadastrado nesta categoria.", fontSize = 12.sp, color = colors.textSecondary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryFields.forEachIndexed { itemIdx, field ->
                        ItemRowCard(
                            itemIndex = itemIdx,
                            totalItemsInCategory = categoryFields.size,
                            field = field,
                            onEdit = { onEditItem(field) },
                            onDuplicate = { onDuplicateItem(field) },
                            onMoveUp = { onMoveItemUp(field) },
                            onMoveDown = { onMoveItemDown(field) },
                            onRemove = { onRemoveItem(field) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Add Item inside this category button
            Button(
                onClick = onAddItem,
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant, contentColor = colors.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("+ Adicionar Item em $categoryName", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ItemRowCard(
    itemIndex: Int,
    totalItemsInCategory: Int,
    field: TemplateFieldEntity,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = AppTheme.colors
    var itemMenuExpanded by remember { mutableStateOf(false) }
    val typeBadge = when (field.type) {
        "TEXT" -> "Texto Livre"
        "PHOTO" -> "Foto"
        else -> "C / NC / NA"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(colors.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${itemIndex + 1}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = colors.primary)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = field.label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Resposta: $typeBadge",
                        fontSize = 11.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (itemIndex > 0) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                if (itemIndex < totalItemsInCategory - 1) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                Box {
                    IconButton(onClick = { itemMenuExpanded = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opções do Item", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = itemMenuExpanded,
                        onDismissRequest = { itemMenuExpanded = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar Item", color = colors.textPrimary) },
                            onClick = {
                                itemMenuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = colors.primary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicar Item", color = colors.textPrimary) },
                            onClick = {
                                itemMenuExpanded = false
                                onDuplicate()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.primary) }
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        DropdownMenuItem(
                            text = { Text("Remover Item", color = colors.statusNaoConforme) },
                            onClick = {
                                itemMenuExpanded = false
                                onRemove()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.statusNaoConforme) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrEditCategoryDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = AppTheme.colors
    var categoryName by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 17.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Título da Categoria", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    placeholder = { Text("Ex: Máquinas e Equipamentos, EPI, Sinalização...") },
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
                onClick = { if (categoryName.isNotBlank()) onConfirm(categoryName.trim()) },
                enabled = categoryName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Salvar Categoria", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface
    )
}

@Composable
fun AddOrEditItemDialog(
    dialogTitle: String,
    initialLabel: String,
    initialType: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val colors = AppTheme.colors
    var label by remember { mutableStateOf(initialLabel) }
    var type by remember { mutableStateOf(initialType) }

    val types = listOf(
        Triple("C_NC_NA", "Conforme / Não Conforme (C/NC/NA)", Icons.Default.Checklist),
        Triple("TEXT", "Texto Livre / Observação", Icons.AutoMirrored.Filled.TextSnippet),
        Triple("PHOTO", "Apenas Foto", Icons.Default.CameraAlt)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 17.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Text("Título da Pergunta / Item *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        placeholder = { Text("Ex: As máquinas possuem proteção adequada?") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
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

                Column {
                    Text("Tipo de Resposta", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    types.forEach { (typeKey, typeTitle, typeIcon) ->
                        val isSelected = type == typeKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
                                .border(1.dp, if (isSelected) colors.primary else colors.border, RoundedCornerShape(8.dp))
                                .clickable { type = typeKey }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(typeIcon, contentDescription = null, tint = if (isSelected) colors.primary else colors.textSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(typeTitle, fontSize = 12.sp, color = if (isSelected) colors.primary else colors.textPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (label.isNotBlank()) onConfirm(label.trim(), type) },
                enabled = label.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Salvar Item", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface
    )
}
