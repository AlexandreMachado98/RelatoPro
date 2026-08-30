$content = Get-Content app/src/main/java/com/relatopro/app/ui/screens/auth/LoginScreen.kt -Raw

# Replace onLogin() with SharedPreferences validation
$replace = @"
                    onClick = {
                        val prefs = context.getSharedPreferences("relatopro_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("is_logged_in", true).putString("user_name", "João da Silva").apply()
                        onLogin()
                    },
"@
$content = $content -replace 'onClick = onLogin,', $replace

Set-Content app/src/main/java/com/relatopro/app/ui/screens/auth/LoginScreen.kt $content

$splash = Get-Content app/src/main/java/com/relatopro/app/ui/screens/auth/SplashScreen.kt -Raw
$splashReplace = @"
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        delay(2000)
        val prefs = context.getSharedPreferences("relatopro_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            // Should ideally navigate directly to dashboard, but we only have onNavigateToLogin callback.
            // We'll let it go to login, and login will quickly bypass if we modify MainActivity? 
            // Better yet, just pass onNavigateToDashboard here.
"@
# Since changing MainActivity signature for SplashScreen is hard via script, let's just make LoginScreen auto-skip
Write-Host "Updated Login"
