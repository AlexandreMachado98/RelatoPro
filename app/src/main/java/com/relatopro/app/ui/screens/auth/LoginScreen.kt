package com.relatopro.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(id = com.relatopro.app.R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Relato Pro", color = PrimaryBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Bem-vindo de volta!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Faça login para gerenciar suas vistorias", fontSize = 14.sp, color = TextSecondary)
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // E-mail Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("E-mail", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("seu@email.com", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Senha", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("••••••••", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Esconder senha" else "Mostrar senha", tint = TextSecondary)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Esqueci minha senha
        Text(
            text = "Esqueci minha senha",
            color = PrimaryBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onNavigateToForgotPassword() }
                .padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Entrar Button
        Button(
            onClick = {
                val prefs = context.getSharedPreferences("relatopro_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("user_name", "João da Silva")
                    .putString("user_email", email.ifEmpty { "joao.silva@relatopro.com" })
                    .apply()
                onNavigateToDashboard()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Entrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // Separator
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
            Text("ou continuar com", modifier = Modifier.padding(horizontal = 16.dp), color = TextSecondary, fontSize = 12.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Social Login Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = {
                    val prefs = context.getSharedPreferences("relatopro_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("is_logged_in", true).putString("user_name", "João da Silva").apply()
                    onNavigateToDashboard()
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Text("Google", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = {
                    val prefs = context.getSharedPreferences("relatopro_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("is_logged_in", true).putString("user_name", "João da Silva").apply()
                    onNavigateToDashboard()
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Text("Apple", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Cadastre-se
        Row(modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Não tem uma conta? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                "Cadastre-se",
                color = PrimaryBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}
