package com.relatopro.app.utils

import android.content.Context
import android.content.SharedPreferences

enum class PhotoQuality(val key: String, val title: String, val description: String, val maxDimension: Int, val jpegQuality: Int) {
    HIGH(
        key = "HIGH",
        title = "Alta",
        description = "Maior fidelidade visual com resolução até 1600px e arquivos ligeiramente maiores.",
        maxDimension = 1600,
        jpegQuality = 85
    ),
    BALANCED(
        key = "BALANCED",
        title = "Equilibrada (Recomendada)",
        description = "Excelente qualidade visual e tamanho de PDF drasticamente reduzido (~700KB a 1.5MB).",
        maxDimension = 1080,
        jpegQuality = 75
    ),
    ECONOMY(
        key = "ECONOMY",
        title = "Econômica",
        description = "Maior taxa de compressão e arquivos ultraleves. Ideal para vistorias com dezenas de evidências.",
        maxDimension = 800,
        jpegQuality = 60
    );

    companion object {
        fun fromKey(key: String?): PhotoQuality {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: BALANCED
        }
    }
}

enum class CameraResolution(val key: String, val title: String, val description: String, val maxWidth: Int, val maxHeight: Int) {
    HIGH(
        key = "HIGH",
        title = "Alta (1080p+)",
        description = "Captura com nitidez estendida para documentos e textos minúsculos.",
        maxWidth = 1920,
        maxHeight = 1440
    ),
    MEDIUM(
        key = "MEDIUM",
        title = "Média (Recomendada)",
        description = "Equilíbrio perfeito de velocidade de captura e leitura de placas/etiquetas.",
        maxWidth = 1280,
        maxHeight = 960
    ),
    ECONOMY(
        key = "ECONOMY",
        title = "Econômica",
        description = "Otimizado para processamento instantâneo e economia de armazenamento.",
        maxWidth = 1024,
        maxHeight = 768
    );

    companion object {
        fun fromKey(key: String?): CameraResolution {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: MEDIUM
        }
    }
}

object PreferenceUtils {
    private const val PREFS_NAME = "relatopro_prefs"
    private const val KEY_PHOTO_QUALITY = "pdf_photo_quality"
    private const val KEY_CAMERA_RESOLUTION = "camera_resolution"
    private const val KEY_AUTO_SAVE = "auto_save"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPhotoQuality(context: Context): PhotoQuality {
        val key = getPrefs(context).getString(KEY_PHOTO_QUALITY, PhotoQuality.BALANCED.key)
        return PhotoQuality.fromKey(key)
    }

    fun setPhotoQuality(context: Context, quality: PhotoQuality) {
        getPrefs(context).edit().putString(KEY_PHOTO_QUALITY, quality.key).apply()
    }

    fun getCameraResolution(context: Context): CameraResolution {
        val key = getPrefs(context).getString(KEY_CAMERA_RESOLUTION, CameraResolution.MEDIUM.key)
        return CameraResolution.fromKey(key)
    }

    fun setCameraResolution(context: Context, resolution: CameraResolution) {
        getPrefs(context).edit().putString(KEY_CAMERA_RESOLUTION, resolution.key).apply()
    }

    fun isAutoSaveEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_SAVE, true)
    }

    fun setAutoSaveEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_SAVE, enabled).apply()
    }
}
