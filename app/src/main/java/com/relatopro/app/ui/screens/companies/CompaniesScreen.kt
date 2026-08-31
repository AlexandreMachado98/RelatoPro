package com.relatopro.app.ui.screens.companies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.data.local.entity.CompanyEntity
import com.relatopro.app.ui.components.animation.AnimatedListItem
import com.relatopro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompaniesScreen(
    viewModel: CompaniesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNewReportForCompany: (Long) -> Unit = {}
) {
    val colors = AppTheme.colors
    val companies by viewModel.companies.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var companyToEdit by remember { mutableStateOf<CompanyEntity?>(null) }
    var companyToDelete by remember { mutableStateOf<CompanyEntity?>(null) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Empresas Inspecionadas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                        Text("Cadastro de clientes, filiais e unidades", fontSize = 12.sp, color = colors.textSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.AddBusiness, contentDescription = "Nova Empresa", tint = colors.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Cadastrar Empresa", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Buscar por Razão Social, Nome Fantasia ou CNPJ...") },
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

            if (companies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(colors.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Nenhuma empresa cadastrada" else "Nenhum resultado encontrado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Cadastre as empresas e suas respectivas unidades para vincular aos relatórios e acompanhar a evolução dos indicadores." else "Tente buscar por outro termo ou CNPJ.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cadastrar Primeira Empresa", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(companies.size) { index ->
                        val company = companies[index]
                        AnimatedListItem(index = index) {
                            CompanyCardItem(
                                company = company,
                                onEdit = { companyToEdit = company },
                                onDelete = { companyToDelete = company }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Add / Edit Company
    if (showAddDialog || companyToEdit != null) {
        val target = companyToEdit
        AddOrEditCompanyDialog(
            company = target,
            onDismiss = {
                showAddDialog = false
                companyToEdit = null
            },
            onSave = { name, tradeName, cnpj, segment, units, contactName, contactEmail, contactPhone ->
                viewModel.saveCompany(
                    id = target?.id ?: 0L,
                    name = name,
                    tradeName = tradeName,
                    cnpj = cnpj,
                    segment = segment,
                    units = units,
                    contactName = contactName,
                    contactEmail = contactEmail,
                    contactPhone = contactPhone
                )
                showAddDialog = false
                companyToEdit = null
            }
        )
    }

    // Modal: Confirm Delete Company
    if (companyToDelete != null) {
        val target = companyToDelete!!
        AlertDialog(
            onDismissRequest = { companyToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = colors.statusNaoConforme)
                    Spacer(Modifier.width(8.dp))
                    Text("Excluir Empresa?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                }
            },
            text = {
                Text(
                    text = "Deseja realmente excluir \"${target.name}\"?\n\nOs relatórios já gerados serão mantidos com os dados arquivados.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCompany(target)
                        companyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.statusNaoConforme)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { companyToDelete = null }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun CompanyCardItem(
    company: CompanyEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AppTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    val unitList = remember(company.units) {
        company.units.split(",").map { it.trim() }.filter { it.isNotBlank() }
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
                            .size(42.dp)
                            .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = company.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )
                        if (company.tradeName.isNotBlank() && company.tradeName != company.name) {
                            Text(
                                text = company.tradeName,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
                            text = { Text("Editar Empresa", color = colors.textPrimary) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = colors.primary) }
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

            Spacer(Modifier.height(10.dp))

            // Badges (CNPJ & Segment)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (company.cnpj.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .background(colors.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("CNPJ: ${company.cnpj}", fontSize = 11.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (company.segment.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(company.segment, fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))

            // Units preview
            Text("Unidades Cadastradas (${unitList.size}):", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(unitList) { unit ->
                    Box(
                        modifier = Modifier
                            .background(colors.surfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(unit, fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOrEditCompanyDialog(
    company: CompanyEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        tradeName: String,
        cnpj: String,
        segment: String,
        units: List<String>,
        contactName: String,
        contactEmail: String,
        contactPhone: String
    ) -> Unit
) {
    val colors = AppTheme.colors

    var name by remember { mutableStateOf(company?.name ?: "") }
    var tradeName by remember { mutableStateOf(company?.tradeName ?: "") }
    var cnpj by remember { mutableStateOf(company?.cnpj ?: "") }
    var segment by remember { mutableStateOf(company?.segment ?: "") }
    var contactName by remember { mutableStateOf(company?.contactName ?: "") }
    var contactEmail by remember { mutableStateOf(company?.contactEmail ?: "") }
    var contactPhone by remember { mutableStateOf(company?.contactPhone ?: "") }

    var unitList by remember {
        mutableStateOf(
            if (company != null && company.units.isNotBlank()) {
                company.units.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf("Matriz")
            }
        )
    }

    var newUnitInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (company == null) "Cadastrar Empresa Inspecionada" else "Editar Empresa",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = colors.textPrimary
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Razão Social / Nome Principal *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Ex: Indústria Metalúrgica ABC Ltda.") },
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

                item {
                    Text("Nome Fantasia (Opcional)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    OutlinedTextField(
                        value = tradeName,
                        onValueChange = { tradeName = it },
                        placeholder = { Text("Ex: Fábrica ABC") },
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

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CNPJ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            OutlinedTextField(
                                value = cnpj,
                                onValueChange = { cnpj = it },
                                placeholder = { Text("00.000.000/0001-00") },
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ramo / Segmento", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            OutlinedTextField(
                                value = segment,
                                onValueChange = { segment = it },
                                placeholder = { Text("Ex: Metalurgia, Logística") },
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
                    }
                }

                item {
                    HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                    Spacer(Modifier.height(4.dp))
                    Text("Unidades / Filiais da Empresa", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("Adicione as unidades para selecionar nas inspeções:", fontSize = 11.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(6.dp))

                    // List of unit chips with remove button
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        unitList.forEach { u ->
                            Box(
                                modifier = Modifier
                                    .background(colors.surfaceVariant, RoundedCornerShape(16.dp))
                                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                    .padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(u, fontSize = 11.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            if (unitList.size > 1) {
                                                unitList = unitList.filter { it != u }
                                            }
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remover", tint = colors.statusNaoConforme, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Input to add unit
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newUnitInput,
                            onValueChange = { newUnitInput = it },
                            placeholder = { Text("Nova unidade (ex: Galpão 2, Filial Sul)") },
                            modifier = Modifier.weight(1f),
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
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (newUnitInput.isNotBlank()) {
                                    unitList = unitList + newUnitInput.trim()
                                    newUnitInput = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Adicionar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            tradeName.trim(),
                            cnpj.trim(),
                            segment.trim(),
                            unitList,
                            contactName.trim(),
                            contactEmail.trim(),
                            contactPhone.trim()
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Salvar Empresa", fontWeight = FontWeight.Bold, color = Color.White)
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
