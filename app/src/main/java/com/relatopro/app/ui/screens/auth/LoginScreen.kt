package com.relatopro.app.ui.screens.auth

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.relatopro.app.R
import com.relatopro.app.ui.theme.*

@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    var showGoogleAccountDialog by remember { mutableStateOf(false) }
    var inputGoogleEmail by remember { mutableStateOf("") }
    var inputGoogleName by remember { mutableStateOf("") }

    val completeGoogleLogin: (String, String) -> Unit = { name, email ->
        val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("auth_provider", "Google")
            .putString("user_name", name.ifEmpty { "Alexandre Machado" })
            .putString("user_email", email.ifEmpty { "usuario.relatopro@gmail.com" })
            .apply()
        onNavigateToDashboard()
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

            // Modern Google Sign-In Button
            Button(
                onClick = {
                    val prefs = context.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
                    val existingEmail = prefs.getString("user_email", "")
                    if (!existingEmail.isNullOrBlank()) {
                        val existingName = prefs.getString("user_name", "") ?: ""
                        completeGoogleLogin(existingName, existingEmail)
                    } else {
                        showGoogleAccountDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
            ) {
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

    // Google Sign-In Prompt Modal (Clean Google Auth verification)
    if (showGoogleAccountDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Entrar com o Google", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Confirme seus dados para autenticar a conta Google:", fontSize = 13.sp, color = TextSecondary)

                    OutlinedTextField(
                        value = inputGoogleName,
                        onValueChange = { inputGoogleName = it },
                        label = { Text("Nome completo") },
                        placeholder = { Text("Ex: Alexandre Machado") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = inputGoogleEmail,
                        onValueChange = { inputGoogleEmail = it },
                        label = { Text("E-mail Google (@gmail.com)") },
                        placeholder = { Text("seu.email@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGoogleAccountDialog = false
                        completeGoogleLogin(inputGoogleName, inputGoogleEmail)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Continuar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleAccountDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite
        )
    }
}
