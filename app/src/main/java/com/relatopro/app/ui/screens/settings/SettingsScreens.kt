package com.relatopro.app.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)

    var userName by remember { mutableStateOf(prefs.getString("user_name", "")?.ifBlank { "Alexandre Machado" } ?: "Alexandre Machado") }
    val userEmail = prefs.getString("user_email", "usuario.relatopro@gmail.com") ?: "usuario.relatopro@gmail.com"
    var userRole by remember { mutableStateOf(prefs.getString("user_role", "Inspetor Técnico") ?: "Inspetor Técnico") }
    var userCompany by remember { mutableStateOf(prefs.getString("user_company", "") ?: "") }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val initials = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(PrimaryDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials.ifEmpty { "RP" },
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = userName.ifBlank { "Não informado" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = userEmail,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE0E7FF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Conta Google Autenticada", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // User Info Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Informações Profissionais", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryDark)
                            TextButton(onClick = { showEditDialog = true }) {
                                Text("Editar", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = BorderColor)

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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusNaoConforme),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusNaoConforme.copy(alpha = 0.5f))
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
            title = { Text("Editar Perfil", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Nome do Inspetor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempRole,
                        onValueChange = { tempRole = it },
                        label = { Text("Cargo / Função") },
                        placeholder = { Text("Ex: Eng. de Segurança") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempCompany,
                        onValueChange = { tempCompany = it },
                        label = { Text("Empresa") },
                        placeholder = { Text("Ex: Relato Pro Consultoria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite
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
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
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
            // SEÇÃO 1: CONTA
            item {
                SettingsSectionHeader("CONTA DO USUÁRIO")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.Outlined.Person,
                            title = "Meu Perfil",
                            subtitle = "Visualizar dados da conta Google e credenciais",
                            onClick = onNavigateToProfile
                        )
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                        SettingsClickableRow(
                            icon = Icons.AutoMirrored.Outlined.ExitToApp,
                            title = "Sair da Conta",
                            subtitle = "Encerrar sessão no Relato Pro",
                            titleColor = StatusNaoConforme,
                            onClick = { showLogoutDialog = true }
                        )
                    }
                }
            }

            // SEÇÃO 2: RELATÓRIOS E PDF
            item {
                SettingsSectionHeader("PREFERÊNCIAS DE RELATÓRIO E PDF")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
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
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
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
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
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
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
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

            // SEÇÃO 3: SISTEMA E AJUDA
            item {
                SettingsSectionHeader("SISTEMA & SOBRE")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.Outlined.Info,
                            title = "Sobre o Relato Pro",
                            subtitle = "Versão 1.0 • Informações do aplicativo",
                            onClick = { showAboutDialog = true }
                        )
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                        SettingsClickableRow(
                            icon = Icons.Outlined.HelpOutline,
                            title = "Ajuda e Instruções",
                            subtitle = "Como criar relatórios e exportar laudos",
                            onClick = { showHelpDialog = true }
                        )
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Description,
                            title = "Termos de Uso",
                            subtitle = "Condições e termos de serviço",
                            onClick = { showTermsDialog = true }
                        )
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
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
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Relato Pro • Versão 1.0",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Aplicativo Oficial de Vistorias e Relatórios Técnicos",
                        fontSize = 11.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
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
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Ajuda e Suporte", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
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
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Central de Ajuda Relato Pro", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryDark)
                    Text(
                        "Dúvidas frequentes sobre preenchimento, coleta de evidências e emissão de laudos técnicos em PDF.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (titleColor != TextPrimary) titleColor else PrimaryBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = titleColor)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = BorderColor
            )
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoDialog(title: String, content: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary) },
        text = {
            Text(content, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SurfaceWhite
    )
}

@Composable
private fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = StatusNaoConforme)
                Spacer(Modifier.width(8.dp))
                Text("Sair do Relato Pro?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            }
        },
        text = {
            Text("Você precisará entrar novamente com sua conta Google para acessar o aplicativo.\n\nSeus relatórios permanecerão salvos com segurança.", fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = StatusNaoConforme)
            ) {
                Text("Sair", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite
    )
}
