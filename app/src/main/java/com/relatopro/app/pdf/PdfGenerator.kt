package com.relatopro.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    private val pageWidth = 595 // A4 Width in PostScript points
    private val pageHeight = 842 // A4 Height

    fun generateReportPdf(
        report: ReportEntity,
        fields: List<TemplateFieldEntity>,
        answers: List<ReportAnswerEntity>,
        photos: Map<Long, List<String>>, // FieldId -> List of local paths
    ): File? {
        val document = PdfDocument()
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }
        val normalPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var currentY = 50f
        val startX = 50f

        // Draw Header
        canvas.drawText("RELATÓRIO: ${report.title}", startX, currentY, titlePaint)
        currentY += 20f
        canvas.drawText("Data: ${formatDate(report.date)}", startX, currentY, normalPaint)
        currentY += 20f
        canvas.drawText("Responsável: ${report.responsible}", startX, currentY, normalPaint)
        currentY += 40f

        // Draw Checklist Items
        for (field in fields) {
            if (currentY > (pageHeight - 100f)) {
                // Next page
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f
            }

            val answer = answers.find { it.templateFieldId == field.id }
            val answerText = answer?.answerValue ?: "Não respondido"
            val obsText = answer?.observation ?: ""

            titlePaint.textSize = 14f
            canvas.drawText("${field.orderIndex}. ${field.label}", startX, currentY, titlePaint)
            currentY += 15f
            canvas.drawText("Resposta: $answerText", startX, currentY, normalPaint)
            currentY += 15f
            
            if (obsText.isNotEmpty()) {
                canvas.drawText("Obs: $obsText", startX, currentY, normalPaint)
                currentY += 15f
            }

            // Draw Photos if any
            val itemPhotos = photos[field.id]
            if (!itemPhotos.isNullOrEmpty()) {
                currentY += 10f
                var xOffset = startX
                for (photoPath in itemPhotos) {
                    val bitmap = BitmapFactory.decodeFile(photoPath)
                    if (bitmap != null) {
                        // Scale bitmap to fit
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
                        
                        if ((xOffset + 110) > (pageWidth - 50)) {
                            xOffset = startX
                            currentY += 110f
                            if (currentY > (pageHeight - 100f)) {
                                document.finishPage(page)
                                pageNumber++
                                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                                page = document.startPage(pageInfo)
                                canvas = page.canvas
                                currentY = 50f
                            }
                        }
                        
                        canvas.drawBitmap(scaledBitmap, xOffset, currentY, null)
                        xOffset += 110f
                    }
                }
                currentY += 120f
            }
            currentY += 20f
        }

        document.finishPage(page)

        // Save file
        val outputDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, "Relatorio_${report.id}.pdf")

        return try {
            document.writeTo(FileOutputStream(outputFile))
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            document.close()
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
