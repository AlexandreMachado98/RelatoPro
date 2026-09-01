package com.relatopro.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageOptimizer {

    /**
     * Otimiza a imagem salva pela câmera: redimensiona e comprime conforme as preferências
     * de resolução e qualidade configuradas no aplicativo.
     */
    fun optimizeImageFile(context: Context, originalFile: File): File? {
        try {
            val cameraRes = PreferenceUtils.getCameraResolution(context)
            val photoQual = PreferenceUtils.getPhotoQuality(context)

            // 1. Read EXIF rotation
            val exif = try {
                ExifInterface(originalFile.absolutePath)
            } catch (e: Exception) {
                null
            }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                ?: ExifInterface.ORIENTATION_UNDEFINED

            // 2. Decode with bounds to avoid OutOfMemory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(originalFile.absolutePath, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, cameraRes.maxWidth, cameraRes.maxHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            var bitmap = BitmapFactory.decodeFile(originalFile.absolutePath, options) ?: return null

            // 3. Rotate if necessary
            bitmap = rotateBitmap(bitmap, orientation)

            // 4. Scale precisely if needed
            if (bitmap.width > cameraRes.maxWidth || bitmap.height > cameraRes.maxHeight) {
                val scale = Math.min(
                    cameraRes.maxWidth.toFloat() / bitmap.width.toFloat(),
                    cameraRes.maxHeight.toFloat() / bitmap.height.toFloat()
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

            // 5. Save optimized file
            val optimizedFile = File(
                context.filesDir,
                "photos/opt_${System.currentTimeMillis()}.webp",
            )
            optimizedFile.parentFile?.mkdirs()

            FileOutputStream(optimizedFile).use { out ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, photoQual.jpegQuality, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, photoQual.jpegQuality, out)
                }
            }

            // Delete the huge original file from cache to save space
            if (originalFile.exists()) {
                originalFile.delete()
            }

            bitmap.recycle()

            return optimizedFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if ((height > reqHeight) || (width > reqWidth)) {
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
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return bitmap
        }

        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }
}
