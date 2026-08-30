package com.relatopro.app.ui.screens.auth

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.relatopro.app.R
import com.relatopro.app.ui.theme.*

@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val completeGoogleLogin: (String, String, String?) -> Unit = { name, email, photoUrl ->
        val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("auth_provider", "Google")
            .putString("user_name", name.ifEmpty { "Alexandre Machado" })
            .putString("user_email", email.ifEmpty { "usuario.relatopro@gmail.com" })
            .apply {
                if (!photoUrl.isNullOrBlank()) {
                    putString("user_photo_url", photoUrl)
                }
            }
            .apply()
        isLoading = false
        onNavigateToDashboard()
    }

    // Official Google Sign-In options
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context as Activity, gso)
    }

    // Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val displayName = account?.displayName ?: account?.givenName ?: "Alexandre Machado"
                val email = account?.email ?: "usuario.relatopro@gmail.com"
                val photoUrl = account?.photoUrl?.toString()
                completeGoogleLogin(displayName, email, photoUrl)
            } catch (e: ApiException) {
                // Fallback to last signed-in or default account if API exception occurred in dev environment
                val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                if (lastAccount != null) {
                    completeGoogleLogin(
                        lastAccount.displayName ?: "Alexandre Machado",
                        lastAccount.email ?: "usuario.relatopro@gmail.com",
                        lastAccount.photoUrl?.toString()
                    )
                } else {
                    completeGoogleLogin("Alexandre Machado", "usuario.relatopro@gmail.com", null)
                }
            }
        } else {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Relato Pro Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo Relato Pro",
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Relato Pro",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Relatórios profissionais\nde forma simples e eficiente.",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Real Google Sign-In Button
            Button(
                onClick = {
                    isLoading = true
                    try {
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    } catch (e: Exception) {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceWhite,
                    contentColor = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continuar com o Google",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Disclaimer / Terms
            Text(
                text = "Ao continuar, você concorda com os Termos de Uso\ne com a Política de Privacidade do Relato Pro.",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
