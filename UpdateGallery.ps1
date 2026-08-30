$settings = Get-Content app/src/main/java/com/relatopro/app/ui/screens/settings/SettingsScreens.kt -Raw

$replaceGallery = @"
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
"@

# Replace the old EvidenceGalleryScreen using a regex that captures everything to the next fun
$settings = $settings -replace '(?s)@Composable\s*fun EvidenceGalleryScreen.*?\}', $replaceGallery

Set-Content app/src/main/java/com/relatopro/app/ui/screens/settings/SettingsScreens.kt $settings
Write-Host "Updated SettingsScreens"
