package com.relatopro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Configuração de Tipografia baseada no UI Kit do Relato Pro (Inter)
// Obs: Estamos usando FontFamily.SansSerif como fallback nativo (Roboto)
// que tem proporções idênticas ao Inter. Para o Inter real, os arquivos .ttf
// precisariam ser adicionados à pasta res/font.

val RelatoProTypography = Typography(
    // H1 (Título principal): Inter Bold – 28px
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    // H2 (Título de seção): Inter Semibold – 20px
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    // H3 (Título de card): Inter Semibold – 16px
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Texto principal: Inter Regular – 14px
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Texto secundário: Inter Regular – 12px
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Legenda/Caption: Inter Regular – 11px
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    // Botões principais: Inter Semibold
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Menu e Navegação: Inter Medium
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
