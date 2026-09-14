package com.relatopro.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ThumbnailManager {

    private const val THUMB_MAX_WIDTH = 280
    private const val THUMB_MAX_HEIGHT = 280
    private const val THUMB_QUALITY = 75

    fun getThumbnailsDir(context: Context): File {
        val dir = File(context.filesDir, "thumbnails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Retorna o arquivo de thumbnail para o caminho de foto original especificado.
     * Se a thumbnail ainda não existir (por exemplo, fotos antigas de versões anteriores),
     * ela será gerada sob demanda de forma segura.
     */
    fun getThumbnailFile(context: Context, originalPath: String): File {
        val originalFile = File(originalPath)
        if (!originalFile.exists()) return originalFile

        val thumbName = "thumb_" + originalFile.name.substringBeforeLast(".") + ".webp"
        val thumbFile = File(getThumbnailsDir(context), thumbName)

        if (thumbFile.exists() && thumbFile.length() > 0L) {
            return thumbFile
        }

        // Generate thumbnail if missing
        val generated = createThumbnail(originalFile, thumbFile)
        return if (generated && thumbFile.exists()) thumbFile else originalFile
    }

    /**
     * Cria uma micro-thumbnail otimizada para visualização rápida em listas e carrosséis.
     */
    fun createThumbnail(originalFile: File, thumbOutputFile: File): Boolean {
        if (!originalFile.exists()) return false

        try {
            // 1. Read EXIF
            val exif = try {
                ExifInterface(originalFile.absolutePath)
            } catch (e: Exception) {
                null
            }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                ?: ExifInterface.ORIENTATION_UNDEFINED

            // 2. Decode bounds only
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(originalFile.absolutePath, options)

            // Calculate efficient sample size
            options.inSampleSize = calculateInSampleSize(options, THUMB_MAX_WIDTH, THUMB_MAX_HEIGHT)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Low memory footprint for thumbnails

            var bitmap = BitmapFactory.decodeFile(originalFile.absolutePath, options) ?: return false

            // 3. Rotate if needed
            if (orientation != ExifInterface.ORIENTATION_UNDEFINED && orientation != ExifInterface.ORIENTATION_NORMAL) {
                val rotated = rotateBitmap(bitmap, orientation)
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }

            // 4. Scale down precisely to target bounds
            if (bitmap.width > THUMB_MAX_WIDTH || bitmap.height > THUMB_MAX_HEIGHT) {
                val scale = Math.min(
                    THUMB_MAX_WIDTH.toFloat() / bitmap.width.toFloat(),
                    THUMB_MAX_HEIGHT.toFloat() / bitmap.height.toFloat()
                )
                val destW = (bitmap.width * scale).toInt()
                val destH = (bitmap.height * scale).toInt()
                if (destW > 0 && destH > 0) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, destW, destH, true)
                    if (scaled != bitmap) {
                        bitmap.recycle()
                        bitmap = scaled
                    }
                }
            }

            // 5. Save thumbnail WebP
            thumbOutputFile.parentFile?.mkdirs()
            FileOutputStream(thumbOutputFile).use { out ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, THUMB_QUALITY, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, THUMB_QUALITY, out)
                }
            }

            bitmap.recycle()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Remove o arquivo de thumbnail associado quando a foto for excluída.
     */
    fun deleteThumbnail(context: Context, originalPath: String) {
        try {
            val originalFile = File(originalPath)
            val thumbName = "thumb_" + originalFile.name.substringBeforeLast(".") + ".webp"
            val thumbFile = File(getThumbnailsDir(context), thumbName)
            if (thumbFile.exists()) {
                thumbFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize >= reqHeight) && (halfWidth / inSampleSize >= reqWidth)) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
