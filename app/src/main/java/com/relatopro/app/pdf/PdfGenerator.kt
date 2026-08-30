package com.relatopro.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    // A4 Landscape dimensions in PostScript points
    private val pageWidth = 842
    private val pageHeight = 595
    private val margin = 40f
    private val usableWidth = pageWidth - (margin * 2)

    // Colors
    private val primaryBlue = Color.parseColor("#1E3A8A")
    private val lightBlue = Color.parseColor("#DBEAFE")
    private val textDark = Color.parseColor("#0F172A")
    private val textGray = Color.parseColor("#64748B")
    private val colorConforme = Color.parseColor("#10B981")
    private val colorNaoConforme = Color.parseColor("#EF4444")
    private val colorNA = Color.parseColor("#94A3B8")
    private val borderLight = Color.parseColor("#E2E8F0")

    suspend fun generateReportPdf(
        report: ReportEntity,
        fields: List<TemplateFieldEntity>,
        answers: List<ReportAnswerEntity>,
        photos: Map<Long, List<String>>
    ): File? = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        val titlePaint = TextPaint().apply {
            color = primaryBlue
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerPaint = TextPaint().apply {
            color = textDark
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val normalPaint = TextPaint().apply {
            color = textDark
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val normalGrayPaint = TextPaint().apply {
            color = textGray
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = borderLight
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val bgPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val df = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        val dateOnlyDf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = df.format(Date(report.date))
        val dateOnlyStr = dateOnlyDf.format(Date(report.date))

        // --- DRAW COVER PAGE ---
        // Blue Top Banner
        bgPaint.color = primaryBlue
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 180f, bgPaint)
        
        // Banner Text
        val bannerTitle = TextPaint().apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("RELATO PRO", margin, 70f, bannerTitle)
        bannerTitle.textSize = 24f
        canvas.drawText("RELATÓRIO DE INSPEÇÃO TÉCNICA", margin, 120f, bannerTitle)
        bannerTitle.textSize = 14f
        bannerTitle.alpha = 200
        canvas.drawText("Documento Oficial de Vistoria e Conformidade", margin, 150f, bannerTitle)

        // Details Block
        var currentY = 230f
        val detailsStartX = margin + 20f

        fun drawDetail(label: String, value: String) {
            canvas.drawText(label, detailsStartX, currentY, headerPaint)
            canvas.drawText(value, detailsStartX + 200f, currentY, normalGrayPaint)
            currentY += 15f
            canvas.drawLine(detailsStartX, currentY, pageWidth - margin - 20f, currentY, linePaint)
            currentY += 25f
        }

        drawDetail("Local / Obra / Fazenda:", report.location)
        drawDetail("Inspetor Responsável:", report.responsible)
        drawDetail("Data e Hora:", dateStr)
        drawDetail("ID do Relatório:", "#${report.id}")
        drawDetail("Status:", report.status)

        document.finishPage(page)

        // --- DRAW INTERNAL PAGES ---
        fun startNewPage() {
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            
            // Draw Header
            canvas.drawText("Relatório de Inspeção Técnica - #${report.id}", margin, margin, headerPaint)
            val dateWidth = normalGrayPaint.measureText(dateOnlyStr)
            canvas.drawText(dateOnlyStr, pageWidth - margin - dateWidth, margin, normalGrayPaint)
            canvas.drawLine(margin, margin + 10f, pageWidth - margin, margin + 10f, linePaint.apply { color = primaryBlue; strokeWidth = 2f })
            
            currentY = margin + 40f
            linePaint.color = borderLight
            linePaint.strokeWidth = 1f
        }

        startNewPage()

        canvas.drawText("Resultados do Checklist", margin, currentY, titlePaint)
        currentY += 20f

        // Table Header
        bgPaint.color = lightBlue
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 30f, bgPaint)
        currentY += 20f
        
        val col1 = margin + 5f // #
        val col2 = margin + 40f // Item
        val col3 = margin + 350f // Status
        val col4 = margin + 470f // Obs
        
        canvas.drawText("#", col1, currentY, headerPaint)
        canvas.drawText("Item / Descrição", col2, currentY, headerPaint)
        canvas.drawText("Conformidade", col3, currentY, headerPaint)
        canvas.drawText("Observações Apontadas", col4, currentY, headerPaint)
        currentY += 10f

        // Table Rows
        for (field in fields) {
            val answer = answers.find { it.templateFieldId == field.id }
            val answerValue = answer?.answerValue ?: "NA"
            val obsText = answer?.observation ?: ""
            val orderStr = String.format(Locale.getDefault(), "%02d", field.orderIndex)

            // Measure texts
            val questionLayout = StaticLayout(field.label, normalPaint, 300, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
            val obsLayout = StaticLayout(if (obsText.isEmpty()) "-" else obsText, normalGrayPaint, usableWidth - 470, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
            
            val rowHeight = maxOf(questionLayout.height, obsLayout.height) + 20f

            if (currentY + rowHeight > pageHeight - margin) {
                document.finishPage(page)
                startNewPage()
            }

            // Draw Number
            canvas.drawText(orderStr, col1, currentY + 15f, normalPaint)
            
            // Draw Question
            canvas.save()
            canvas.translate(col2, currentY + 5f)
            questionLayout.draw(canvas)
            canvas.restore()

            // Draw Badge
            val badgeColor = when (answerValue) {
                "C" -> colorConforme
                "NC" -> colorNaoConforme
                else -> colorNA
            }
            val badgeText = when (answerValue) {
                "C" -> "CONFORME"
                "NC" -> "NÃO CONF."
                else -> "N/A"
            }
            bgPaint.color = badgeColor
            val badgeWidth = headerPaint.measureText(badgeText) + 16f
            canvas.drawRoundRect(RectF(col3, currentY + 5f, col3 + badgeWidth, currentY + 25f), 4f, 4f, bgPaint)
            headerPaint.color = Color.WHITE
            canvas.drawText(badgeText, col3 + 8f, currentY + 19f, headerPaint)
            headerPaint.color = textDark // restore

            // Draw Observation
            canvas.save()
            canvas.translate(col4, currentY + 5f)
            obsLayout.draw(canvas)
            canvas.restore()

            currentY += rowHeight
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
        }

        // --- DRAW PHOTOS ---
        data class PhotoData(val path: String, val label: String, val isSignature: Boolean)
        val allPhotos = mutableListOf<PhotoData>()
        
        for (field in fields) {
            val pList = photos[field.id]
            pList?.forEach { 
                allPhotos.add(PhotoData(it, "Item ${String.format(Locale.getDefault(), "%02d", field.orderIndex)}", false))
            }
        }
        val sigList = photos[-1L]
        sigList?.lastOrNull()?.let {
            allPhotos.add(PhotoData(it, "Assinatura do Inspetor", true))
        }

        if (allPhotos.isNotEmpty()) {
            if (currentY + 100f > pageHeight - margin) {
                document.finishPage(page)
                startNewPage()
            } else {
                currentY += 30f
            }

            canvas.drawText("Evidências Fotográficas", margin, currentY, titlePaint)
            currentY += 30f

            val photoSize = 240f
            val spacing = 20f
            var photoX = margin

            for (photo in allPhotos) {
                if (photoX + photoSize > pageWidth - margin) {
                    photoX = margin
                    currentY += photoSize + 40f
                    if (currentY + photoSize + 40f > pageHeight - margin) {
                        document.finishPage(page)
                        startNewPage()
                    }
                }

                try {
                    val bitmap = BitmapFactory.decodeFile(photo.path)
                    if (bitmap != null) {
                        // Draw Image
                        val destRect = RectF(photoX, currentY, photoX + photoSize, currentY + photoSize - 30f)
                        if (photo.isSignature) {
                            canvas.drawBitmap(bitmap, null, destRect, null)
                        } else {
                            // Center crop logic for standard photos
                            val scale = maxOf(destRect.width() / bitmap.width, destRect.height() / bitmap.height)
                            val scaledWidth = bitmap.width * scale
                            val scaledHeight = bitmap.height * scale
                            val left = (destRect.width() - scaledWidth) / 2
                            val top = (destRect.height() - scaledHeight) / 2
                            
                            canvas.save()
                            canvas.clipRect(destRect)
                            canvas.translate(destRect.left + left, destRect.top + top)
                            canvas.scale(scale, scale)
                            canvas.drawBitmap(bitmap, 0f, 0f, null)
                            canvas.restore()
                            
                            // Border
                            canvas.drawRect(destRect, linePaint)
                        }
                        
                        // Draw Label
                        canvas.drawText(photo.label, photoX, currentY + photoSize - 10f, headerPaint)
                        
                        photoX += photoSize + spacing
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            currentY += photoSize + 40f
        }

        document.finishPage(page)

        val outputDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val safeLocation = report.location.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val dateForName = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(report.date))
        val outputFile = File(outputDir, "RELATO-PRO_Relatorio-Inspecao_${safeLocation}_${dateForName}_${report.id}.pdf")

        return@withContext try {
            document.writeTo(FileOutputStream(outputFile))
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            document.close()
        }
    }
}
