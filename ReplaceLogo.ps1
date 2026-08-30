$files = @(
    "app/src/main/java/com/relatopro/app/ui/screens/auth/LoginScreen.kt",
    "app/src/main/java/com/relatopro/app/ui/screens/auth/RegisterScreen.kt",
    "app/src/main/java/com/relatopro/app/ui/screens/auth/ForgotPasswordScreen.kt",
    "app/src/main/java/com/relatopro/app/ui/screens/auth/SplashScreen.kt",
    "app/src/main/java/com/relatopro/app/ui/screens/dashboard/DashboardScreen.kt"
)

foreach ($file in $files) {
    $content = Get-Content $file -Raw
    
    # Remove Security icon import
    $content = $content -replace 'import androidx.compose.material.icons.filled.Security\r?\n?', ''
    
    # Add Image and painterResource if not exists
    if ($content -notmatch 'import androidx.compose.foundation.Image') {
        $content = $content -replace 'import androidx.compose.foundation.layout', "import androidx.compose.foundation.Image`nimport androidx.compose.foundation.layout"
    }
    if ($content -notmatch 'import androidx.compose.ui.res.painterResource') {
        $content = $content -replace 'import androidx.compose.ui.unit', "import androidx.compose.ui.res.painterResource`nimport androidx.compose.ui.unit"
    }
    
    # Replace Icon usage (SplashScreen is multi-line)
    $content = $content -replace 'Icon\(\s*imageVector\s*=\s*Icons\.Default\.Security,\s*contentDescription\s*=\s*"Logo",\s*tint\s*=\s*Color\.White,\s*modifier\s*=\s*Modifier\.size\(100\.dp\)\s*\)', 'Image(painterResource(id = com.relatopro.app.R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(100.dp))'
    
    # Replace Icon usage in others
    $content = $content -replace 'Icon\(Icons\.Default\.Security,\s*contentDescription\s*=\s*"Logo",\s*tint\s*=\s*PrimaryBlue,\s*modifier\s*=\s*(.*?)\)', 'Image(painterResource(id = com.relatopro.app.R.drawable.logo), contentDescription = "Logo", modifier = $1)'
    
    Set-Content $file $content
}
Write-Host "Replaced."
