package com.relatopro.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.relatopro.app.utils.PhotoQuality
import com.relatopro.app.utils.PreferenceUtils
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min

object PdfImageCompressor {

    /**
     * Carrega e otimiza uma imagem especificamente para incorporação no documento PDF.
     * Redimensiona para o tamanho ideal do card no PDF (ex: 600x450 max) e comprime com JPEG
     * de acordo com a qualidade configurada, reduzindo drasticamente o tamanho do PDF final.
     */
    fun loadOptimizedBitmapForPdf(
        context: Context,
        filePath: String,
        targetWidth: Int = 600,
        targetHeight: Int = 450,
        qualityOverride: PhotoQuality? = null
    ): Bitmap? {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val quality = qualityOverride ?: PreferenceUtils.getPhotoQuality(context)
            val maxDim = min(quality.maxDimension, max(targetWidth, targetHeight))

            // 1. Read EXIF orientation
            val exif = try {
                ExifInterface(file.absolutePath)
            } catch (e: Exception) {
                null
            }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            ) ?: ExifInterface.ORIENTATION_UNDEFINED

            // 2. Decode bounds to compute optimal inSampleSize
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return null

            // 3. Compute sample size
            var inSampleSize = 1
            if (rawHeight > maxDim || rawWidth > maxDim) {
                val halfHeight = rawHeight / 2
                val halfWidth = rawWidth / 2
                while ((halfHeight / inSampleSize) >= maxDim && (halfWidth / inSampleSize) >= maxDim) {
                    inSampleSize *= 2
                }
            }

            // 4. Decode downsampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // 2 bytes per pixel instead of 4 (50% memory savings)
                inDither = true
            }
            var bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            // 5. Rotate according to EXIF if needed
            bitmap = rotateBitmapIfNeeded(bitmap, orientation)

            // 6. Scale precisely to target bounds maintaining aspect ratio
            val currentW = bitmap.width
            val currentH = bitmap.height
            if (currentW > maxDim || currentH > maxDim) {
                val scaleRatio = min(maxDim.toFloat() / currentW.toFloat(), maxDim.toFloat() / currentH.toFloat())
                val destW = (currentW * scaleRatio).toInt()
                val destH = (currentH * scaleRatio).toInt()
                if (destW > 0 && destH > 0) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, destW, destH, true)
                    if (scaled != bitmap) {
                        bitmap.recycle()
                        bitmap = scaled
                    }
                }
            }

            // 7. JPEG compression cycle to strip metadata and optimize byte stream
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, baos)
            val compressedBytes = baos.toByteArray()
            baos.close()

            val finalOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val finalBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size, finalOptions)
            bitmap.recycle()

            finalBitmap
        } catch (e: OutOfMemoryError) {
            System.gc()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.postRotate(180f)
                matrix.postScale(-1f, 1f)
            }
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
