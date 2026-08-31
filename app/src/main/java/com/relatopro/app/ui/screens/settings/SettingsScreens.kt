package com.relatopro.app.ui.screens.settings

import android.content.Context
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.relatopro.app.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
    val colors = AppTheme.colors

    var userName by remember { mutableStateOf(prefs.getString("user_name", "")?.ifBlank { "Alexandre Machado" } ?: "Alexandre Machado") }
    val userEmail = prefs.getString("user_email", "usuario.relatopro@gmail.com") ?: "usuario.relatopro@gmail.com"
    var userRole by remember { mutableStateOf(prefs.getString("user_role", "Inspetor Técnico") ?: "Inspetor Técnico") }
    var userCompany by remember { mutableStateOf(prefs.getString("user_company", "") ?: "") }
    
    var userCustomPhotoPath by remember { mutableStateOf(prefs.getString("user_custom_photo_path", null)) }
    val userGooglePhotoUrl = prefs.getString("user_photo_url", null)

    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val initials = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()

    // Image Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val photoFile = File(context.filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(photoFile)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                userCustomPhotoPath = photoFile.absolutePath
                prefs.edit().putString("user_custom_photo_path", photoFile.absolutePath).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Card with Avatar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with Edit Badge
                        Box(contentAlignment = Alignment.BottomEnd) {
                            val activePhoto = userCustomPhotoPath ?: userGooglePhotoUrl
                            if (!activePhoto.isNullOrBlank()) {
                                AsyncImage(
                                    model = activePhoto,
                                    contentDescription = "Foto de Perfil",
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, colors.primary, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials.ifEmpty { "RP" },
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Camera Edit Button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                                    .clickable { photoPickerLauncher.launch("image/*") }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Alterar Foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = userName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userRole,
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Account Provider Badge
                        Box(
                            modifier = Modifier
                                .background(colors.surfaceVariant, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Conta Google Autenticada",
                                    fontSize = 11.sp,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // User Info Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Informações Profissionais", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                            TextButton(onClick = { showEditDialog = true }) {
                                Text("Editar", color = colors.primary, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

                        ProfileInfoRow("Nome Completo", userName.ifBlank { "Não informado" })
                        ProfileInfoRow("E-mail Google", userEmail)
                        ProfileInfoRow("Cargo / Função Padrão", userRole.ifBlank { "Não informado" })
                        ProfileInfoRow("Empresa / Consultoria", userCompany.ifBlank { "Não informado" })
                    }
                }
            }

            // Logout Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.statusNaoConforme),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.statusNaoConforme.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sair da Conta (Logout)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // Edit Profile Modal
    if (showEditDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempRole by remember { mutableStateOf(userRole) }
        var tempCompany by remember { mutableStateOf(userCompany) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Perfil", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Nome do Inspetor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        placeholder = { Text("Ex: Alexandre Machado") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border
                        )
                    )

                    Text("Cargo / Função", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    OutlinedTextField(
                        value = tempRole,
                        onValueChange = { tempRole = it },
                        placeholder = { Text("Ex: Eng. de Segurança") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border
                        )
                    )

                    Text("Empresa", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    OutlinedTextField(
                        value = tempCompany,
                        onValueChange = { tempCompany = it },
                        placeholder = { Text("Ex: Relato Pro Consultoria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userName = tempName
                        userRole = tempRole
                        userCompany = tempCompany
                        prefs.edit()
                            .putString("user_name", tempName)
                            .putString("user_role", tempRole)
                            .putString("user_company", tempCompany)
                            .apply()
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Salvar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    // Logout Confirmation Modal
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                prefs.edit().clear().apply()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
    val colors = AppTheme.colors

    var pageNumbersEnabled by remember { mutableStateOf(prefs.getBoolean("pdf_page_numbers", true)) }
    var summaryEnabled by remember { mutableStateOf(prefs.getBoolean("pdf_summary_table", true)) }
    var observationsEnabled by remember { mutableStateOf(prefs.getBoolean("pdf_observations", true)) }
    var autoSaveEnabled by remember { mutableStateOf(prefs.getBoolean("auto_save", true)) }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SEÇÃO 1: APARÊNCIA & TEMA
            item {
                SettingsSectionHeader("APARÊNCIA & TEMA")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Tema do Aplicativo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                        Text("Selecione entre o modo claro, escuro ou acompanhe o sistema Android.", fontSize = 12.sp, color = colors.textSecondary)

                        Spacer(Modifier.height(4.dp))

                        val themeOptions = listOf(
                            Triple("LIGHT", "Modo Claro", Icons.Default.LightMode),
                            Triple("DARK", "Modo Escuro", Icons.Default.DarkMode),
                            Triple("SYSTEM", "Automático (Padrão do Sistema)", Icons.Default.SettingsBrightness)
                        )

                        val currentMode = AppTheme.currentMode
                        themeOptions.forEach { (modeKey, modeTitle, modeIcon) ->
                            val isSelected = currentMode == modeKey
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) colors.primary else colors.border, RoundedCornerShape(8.dp))
                                    .clickable {
                                        com.relatopro.app.ui.theme.ThemeManager.setThemeMode(context, modeKey)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modeIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.primary else colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    modeTitle,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.primary else colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        com.relatopro.app.ui.theme.ThemeManager.setThemeMode(context, modeKey)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.primary,
                                        unselectedColor = colors.textSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // SEÇÃO 2: CONTA
            item {
                SettingsSectionHeader("CONTA DO USUÁRIO")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.Outlined.Person,
                            title = "Meu Perfil",
                            subtitle = "Visualizar dados da conta Google, foto e credenciais",
                            onClick = onNavigateToProfile
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        SettingsClickableRow(
                            icon = Icons.AutoMirrored.Outlined.ExitToApp,
                            title = "Sair da Conta",
                            subtitle = "Encerrar sessão no Relato Pro",
                            titleColor = colors.statusNaoConforme,
                            onClick = { showLogoutDialog = true }
                        )
                    }
                }
            }

            // SEÇÃO 3: RELATÓRIOS E PDF
            item {
                SettingsSectionHeader("PREFERÊNCIAS DE RELATÓRIO E PDF")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column {
                        SettingsToggleRow(
                            icon = Icons.Outlined.PictureAsPdf,
                            title = "Numeração de Páginas no PDF",
                            subtitle = "Exibir 'Página X de Y' no rodapé do laudo",
                            checked = pageNumbersEnabled,
                            onCheckedChange = {
                                pageNumbersEnabled = it
                                prefs.edit().putBoolean("pdf_page_numbers", it).apply()
                            }
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        SettingsToggleRow(
                            icon = Icons.Outlined.Checklist,
                            title = "Resumo de Conformidades no Laudo",
                            subtitle = "Incluir tabela com contagem de Conformes/Não Conformes",
                            checked = summaryEnabled,
                            onCheckedChange = {
                                summaryEnabled = it
                                prefs.edit().putBoolean("pdf_summary_table", it).apply()
                            }
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        SettingsToggleRow(
                            icon = Icons.Outlined.Notes,
                            title = "Observações Finais no Laudo",
                            subtitle = "Incluir seção de considerações e recomendações técnicas",
                            checked = observationsEnabled,
                            onCheckedChange = {
                                observationsEnabled = it
                                prefs.edit().putBoolean("pdf_observations", it).apply()
                            }
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        SettingsToggleRow(
                            icon = Icons.Outlined.Save,
                            title = "Salvamento Automático",
                            subtitle = "Salvar alterações de checklists e fotos em tempo real",
                            checked = autoSaveEnabled,
                            onCheckedChange = {
                                autoSaveEnabled = it
                                prefs.edit().putBoolean("auto_save", it).apply()
                            }
                        )
                    }
                }
            }

            // SEÇÃO 4: SISTEMA E AJUDA
            item {
                SettingsSectionHeader("SISTEMA & SOBRE")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.Outlined.Info,
                            title = "Sobre o Relato Pro",
                            subtitle = "Versão 1.0 • Informações do aplicativo",
                            onClick = { showAboutDialog = true }
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        SettingsClickableRow(
                            icon = Icons.Outlined.HelpOutline,
                            title = "Ajuda e Instruções",
                            subtitle = "Como criar relatórios e exportar laudos",
                            onClick = { showHelpDialog = true }
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Description,
                            title = "Termos de Uso",
                            subtitle = "Condições e termos de serviço",
                            onClick = { showTermsDialog = true }
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Security,
                            title = "Política de Privacidade",
                            subtitle = "Proteção de dados e segurança",
                            onClick = { showPrivacyDialog = true }
                        )
                    }
                }
            }

            // FOOTER
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.relatopro.app.R.drawable.logo),
                        contentDescription = "Logo Relato Pro",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Relato Pro • Versão 1.0",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Aplicativo Oficial de Vistorias e Relatórios Técnicos",
                        fontSize = 11.sp,
                        color = colors.textSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // MODALS
    if (showAboutDialog) {
        InfoDialog(
            title = "Sobre o Relato Pro",
            content = "O Relato Pro é uma plataforma desenvolvida para engenheiros, inspetores técnicos e consultores registrarem vistorias, coletarem evidências fotográficas e assinaturas digitais, gerando laudos oficiais em PDF de forma ágil e segura.\n\nVersão: 1.0\nTecnologia: Android Nativo Jetpack Compose",
            onDismiss = { showAboutDialog = false }
        )
    }

    if (showHelpDialog) {
        InfoDialog(
            title = "Ajuda e Suporte",
            content = "1. Para iniciar uma vistoria, toque em 'Novo Relatório' no Dashboard e selecione o modelo de checklist.\n2. Preencha as informações gerais e marque os itens (Conforme, Não Conforme ou N/A).\n3. Anexe fotos de evidências diretamente em cada item.\n4. Na última etapa, colete as assinaturas digitais e toque em 'Gerar Relatório'.\n5. O PDF estará disponível para visualização e compartilhamento no Histórico.",
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showTermsDialog) {
        InfoDialog(
            title = "Termos de Uso",
            content = "Ao utilizar o Relato Pro, você concorda que todos os relatórios, fotos e assinaturas gerados são de responsabilidade técnica do emitente cadastrado. O aplicativo armazena os dados em conformidade com as diretrizes de segurança do dispositivo.",
            onDismiss = { showTermsDialog = false }
        )
    }

    if (showPrivacyDialog) {
        InfoDialog(
            title = "Política de Privacidade",
            content = "O Relato Pro respeita sua privacidade. As informações da conta Google e os relatórios gerados pertencem exclusivamente ao usuário autenticado e não são compartilhados com terceiros sem seu consentimento expresso.",
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                prefs.edit().clear().apply()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    val colors = AppTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Ajuda e Suporte", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Central de Ajuda Relato Pro", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                    Text(
                        "Dúvidas frequentes sobre preenchimento, coleta de evidências e emissão de laudos técnicos em PDF.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = AppTheme.colors
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = AppTheme.colors.textPrimary,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val effectiveTitleColor = if (titleColor == AppTheme.colors.textPrimary) colors.textPrimary else titleColor
    val iconTint = if (titleColor != AppTheme.colors.textPrimary) titleColor else colors.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = effectiveTitleColor)
            Text(subtitle, fontSize = 12.sp, color = colors.textSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
            Text(subtitle, fontSize = 12.sp, color = colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.border
            )
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    val colors = AppTheme.colors
    Column {
        Text(label, fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoDialog(title: String, content: String, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = if (title == "Sobre o Relato Pro") Alignment.CenterHorizontally else Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                if (title == "Sobre o Relato Pro") {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.relatopro.app.R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
            }
        },
        text = {
            Text(content, fontSize = 13.sp, color = colors.textSecondary, lineHeight = 19.sp)
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Fechar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colors.surface
    )
}

@Composable
private fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = colors.statusNaoConforme)
                Spacer(Modifier.width(8.dp))
                Text("Sair do Relato Pro?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
            }
        },
        text = {
            Text("Você precisará entrar novamente com sua conta Google para acessar o aplicativo.\n\nSeus relatórios permanecerão salvos com segurança.", fontSize = 13.sp, color = colors.textSecondary, lineHeight = 18.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.statusNaoConforme,
                    contentColor = Color.White
                )
            ) {
                Text("Sair", color = Color.White, fontWeight = FontWeight.Bold)
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
