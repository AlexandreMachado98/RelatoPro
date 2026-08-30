package com.relatopro.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.*

@Composable
fun ProfileScreen(onNavigateBack: () -> Unit) {
    GenericSettingsScreen(title = "Perfil", onNavigateBack = onNavigateBack) {
        Text("Configurações de Perfil (Em breve)", color = TextSecondary)
    }
}

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    GenericSettingsScreen(title = "Configurações", onNavigateBack = onNavigateBack) {
        Text("Opções do Sistema (Em breve)", color = TextSecondary)
    }
}

@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    GenericSettingsScreen(title = "Ajuda e Suporte", onNavigateBack = onNavigateBack) {
        Text("Central de Ajuda (Em breve)", color = TextSecondary)
    }
}

@Composable
fun EvidenceGalleryScreen(onNavigateBack: () -> Unit) {
    GenericSettingsScreen(title = "Fotos e Anexos", onNavigateBack = onNavigateBack) {
        Text("Galeria Geral (Em breve)", color = TextSecondary)
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
