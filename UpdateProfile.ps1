$settings = Get-Content app/src/main/java/com/relatopro/app/ui/screens/settings/SettingsScreens.kt -Raw

$profileReplace = @"
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
"@
$settings = $settings -replace '(?s)@Composable\s*fun ProfileScreen.*?\}', $profileReplace

$settingsReplace = @"
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
"@
$settings = $settings -replace '(?s)@Composable\s*fun SettingsScreen.*?\}', $settingsReplace

Set-Content app/src/main/java/com/relatopro/app/ui/screens/settings/SettingsScreens.kt $settings
Write-Host "Updated SettingsScreens with functional Profile and Settings"
