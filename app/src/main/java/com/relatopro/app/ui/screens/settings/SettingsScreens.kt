package com.relatopro.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

@Composable
fun ProfileScreen(onNavigateBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("relatopro_prefs", android.content.Context.MODE_PRIVATE)
    val userName = prefs.getString("user_name", "João da Silva") ?: "João da Silva"
    val userEmail = prefs.getString("user_email", "joao.silva@email.com") ?: "joao.silva@email.com"

    GenericSettingsScreen(title = "Perfil do Usuário", onNavigateBack = onNavigateBack) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(60.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(userEmail, fontSize = 16.sp, color = TextSecondary)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    prefs.edit().clear().apply()
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusNaoConforme),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Sair da Conta (Logout)", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("relatopro_prefs", android.content.Context.MODE_PRIVATE)
    
    var syncWifi by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(prefs.getBoolean("sync_wifi", true)) }
    var autoSave by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(prefs.getBoolean("auto_save", true)) }

    GenericSettingsScreen(title = "Configurações", onNavigateBack = onNavigateBack) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Sincronização", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Sincronizar apenas via Wi-Fi", fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("Economiza dados móveis", fontSize = 12.sp, color = TextSecondary)
                }
                Switch(
                    checked = syncWifi,
                    onCheckedChange = { 
                        syncWifi = it
                        prefs.edit().putBoolean("sync_wifi", it).apply()
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = BorderColor)
            
            Text("Relatórios", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Salvamento Automático", fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("Salvar rascunhos em tempo real", fontSize = 12.sp, color = TextSecondary)
                }
                Switch(
                    checked = autoSave,
                    onCheckedChange = { 
                        autoSave = it
                        prefs.edit().putBoolean("auto_save", it).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    GenericSettingsScreen(title = "Ajuda e Suporte", onNavigateBack = onNavigateBack) {
        Text("Central de Ajuda (Em breve)", color = TextSecondary)
    }
}

@Composable
fun EvidenceGalleryScreen(
    onNavigateBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val photos by viewModel.photos.collectAsState()

    GenericSettingsScreen(title = "Fotos e Anexos", onNavigateBack = onNavigateBack) {
        if (photos.isEmpty()) {
            Text("Nenhuma foto encontrada.", color = TextSecondary)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos) { photo ->
                    val file = File(photo.localPath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Evidência",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenericSettingsScreen(title: String, onNavigateBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(SurfaceWhite).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        HorizontalDivider(color = BorderColor)
        
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}
