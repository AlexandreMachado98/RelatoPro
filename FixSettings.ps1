$settings = @"
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
"@

Set-Content app/src/main/java/com/relatopro/app/ui/screens/settings/SettingsScreens.kt $settings
Write-Host "Fixed SettingsScreens imports and syntax"
