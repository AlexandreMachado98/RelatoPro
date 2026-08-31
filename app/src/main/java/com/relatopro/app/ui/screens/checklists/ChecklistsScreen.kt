package com.relatopro.app.ui.screens.checklists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.ui.components.animation.AnimatedListItem
import com.relatopro.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistsScreen(
    viewModel: ChecklistsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToStartReport: (Long) -> Unit
) {
    val colors = AppTheme.colors
    val templates by viewModel.templates.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val fieldsCountMap by viewModel.fieldsCountMap.collectAsState()

    var templateToDelete by remember { mutableStateOf<TemplateEntity?>(null) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Checklists", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                        Text("Gerencie modelos e seus checklists de campo", fontSize = 12.sp, color = colors.textSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = colors.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Criar Checklist", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Buscar checklist por nome, categoria...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = colors.textSecondary)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChecklistCategoryChip(
                    title = "Todos",
                    selected = selectedTab == "TODOS",
                    onClick = { viewModel.setTab("TODOS") }
                )
                ChecklistCategoryChip(
                    title = "Meus Checklists",
                    selected = selectedTab == "MEUS_CHECKLISTS",
                    onClick = { viewModel.setTab("MEUS_CHECKLISTS") }
                )
                ChecklistCategoryChip(
                    title = "Modelos Oficiais",
                    selected = selectedTab == "MODELOS",
                    onClick = { viewModel.setTab("MODELOS") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Checklists List
            if (templates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedTab == "MEUS_CHECKLISTS") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(colors.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Você ainda não possui checklists próprios", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Crie um novo checklist personalizado ou duplique um dos modelos oficiais para editar como preferir.",
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onNavigateToCreate,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Criar Checklist", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.setTab("MODELOS") },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary)
                                ) {
                                    Text("Ver Modelos")
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Checklist, contentDescription = null, tint = colors.textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Nenhum checklist encontrado", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("Tente ajustar o termo pesquisado.", fontSize = 13.sp, color = colors.textSecondary)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(templates.size) { index ->
                        val template = templates[index]
                        val itemCount = fieldsCountMap[template.id] ?: 0
                        val isOfficial = template.isGlobal && template.userId.isBlank() && template.id <= 3L
                        val isUserCreated = !isOfficial

                        AnimatedListItem(index = index) {
                            ChecklistCardItem(
                                template = template,
                                itemCount = itemCount,
                                isUserCreated = isUserCreated,
                                onStartReport = { onNavigateToStartReport(template.id) },
                                onEdit = { onNavigateToEdit(template.id) },
                                onDuplicate = {
                                    viewModel.duplicateChecklist(template.id) {
                                        viewModel.setTab("MEUS_CHECKLISTS")
                                    }
                                },
                                onDelete = { templateToDelete = template }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (templateToDelete != null) {
        val target = templateToDelete!!
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = colors.statusNaoConforme)
                    Spacer(Modifier.width(8.dp))
                    Text("Excluir Checklist?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                }
            },
            text = {
                Text(
                    text = "Deseja realmente excluir o checklist \"${target.name}\"?\n\nEssa ação é irreversível e removerá o modelo de sua lista pessoal.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChecklist(target.id)
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.statusNaoConforme)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun ChecklistCategoryChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val bgColor = if (selected) colors.primary else colors.surface
    val contentColor = if (selected) Color.White else colors.textPrimary
    val borderColor = if (selected) colors.primary else colors.border

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
fun ChecklistCardItem(
    template: TemplateEntity,
    itemCount: Int,
    isUserCreated: Boolean,
    onStartReport: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AppTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }
    val dateStr = remember(template.updatedAt) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(template.updatedAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isUserCreated) colors.primary.copy(alpha = 0.12f) else colors.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = if (isUserCreated) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = template.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = template.category.ifBlank { "Geral" },
                            fontSize = 12.sp,
                            color = if (isUserCreated) colors.primary else colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opções", tint = colors.textSecondary)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar", color = colors.textPrimary) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = colors.primary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicar", color = colors.textPrimary) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.primary) }
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        DropdownMenuItem(
                            text = { Text("Excluir", color = colors.statusNaoConforme) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.statusNaoConforme) }
                        )
                    }
                }
            }

            if (template.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = template.description,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(colors.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$itemCount ${if (itemCount == 1) "item" else "itens"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    Text(
                        text = "Atualizado em $dateStr",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }

                Button(
                    onClick = onStartReport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Usar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
