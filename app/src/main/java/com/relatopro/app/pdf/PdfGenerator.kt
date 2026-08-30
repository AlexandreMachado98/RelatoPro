package com.relatopro.app.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.relatopro.app.data.local.entity.PhotoEntity
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.SignatureEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    // A4 Portrait dimensions in PostScript points (595 x 842 pt)
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 36f
    private val usableWidth = pageWidth - (margin * 2)

    // Palette
    private val primaryDark = Color.parseColor("#0B2A5B")
    private val primaryBlue = Color.parseColor("#2563EB")
    private val bgLight = Color.parseColor("#F8FAFC")
    private val textDark = Color.parseColor("#0F172A")
    private val textMuted = Color.parseColor("#64748B")
    private val colorConforme = Color.parseColor("#16A34A") // Emerald Green
    private val colorNaoConforme = Color.parseColor("#DC2626") // Red
    private val colorNA = Color.parseColor("#94A3B8") // Slate
    private val borderLight = Color.parseColor("#E2E8F0")

    suspend fun generateReportPdf(
        report: ReportEntity,
        fields: List<TemplateFieldEntity>,
        answers: List<ReportAnswerEntity>,
        photos: Map<Long, List<String>>,
        signatures: List<SignatureEntity> = emptyList(),
        photoEntities: List<PhotoEntity> = emptyList()
    ): File? = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        val titlePaint = TextPaint().apply {
            color = primaryDark
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionTitlePaint = TextPaint().apply {
            color = primaryDark
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerPaint = TextPaint().apply {
            color = textDark
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = TextPaint().apply {
            color = textDark
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val bodyMutedPaint = TextPaint().apply {
            color = textMuted
            textSize = 8.5f
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

        var currentY = margin + 110f

        fun drawHeaderAndFooter(isCover: Boolean = false) {
            if (!isCover) {
                // Top header line
                bgPaint.color = primaryDark
                canvas.drawRect(margin, margin - 15f, pageWidth - margin, margin - 13f, bgPaint)
                
                canvas.drawText("RELATO PRO • Relatório de Inspeção Técnica", margin, margin - 20f, sectionTitlePaint.apply { textSize = 9f })
                sectionTitlePaint.textSize = 13f // restore
                
                val reportNumText = "Nº ${report.reportNumber.ifEmpty { "#${report.id}" }} • $dateOnlyStr"
                val reportNumWidth = bodyMutedPaint.measureText(reportNumText)
                canvas.drawText(reportNumText, pageWidth - margin - reportNumWidth, margin - 20f, bodyMutedPaint)
            }

            // Bottom Footer
            val footerY = pageHeight - margin + 18f
            canvas.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f, linePaint)
            
            val footerLeft = "Relato Pro — Documento gerado eletronicamente"
            canvas.drawText(footerLeft, margin, footerY, bodyMutedPaint)

            val pageText = "Página $pageNumber"
            val pageTextWidth = bodyMutedPaint.measureText(pageText)
            canvas.drawText(pageText, pageWidth - margin - pageTextWidth, footerY, bodyMutedPaint)
        }

        fun startNewPage() {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            drawHeaderAndFooter(isCover = false)
            currentY = margin + 15f
        }

        // ==========================================
        // 1. CAPA / CABEÇALHO DO RELATÓRIO
        // ==========================================
        // Top Banner
        bgPaint.color = primaryDark
        canvas.drawRoundRect(RectF(margin, margin, pageWidth - margin, margin + 95f), 8f, 8f, bgPaint)

        // Banner content
        val whiteTitle = TextPaint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val whiteSub = TextPaint().apply {
            color = Color.WHITE
            alpha = 220
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        canvas.drawText("RELATO PRO", margin + 18f, margin + 30f, whiteTitle.apply { textSize = 14f; color = Color.parseColor("#93C5FD") })
        whiteTitle.color = Color.WHITE
        whiteTitle.textSize = 18f
        canvas.drawText(report.title.ifEmpty { "RELATÓRIO DE INSPEÇÃO TÉCNICA" }.uppercase(Locale.getDefault()), margin + 18f, margin + 55f, whiteTitle)
        canvas.drawText("Documento Oficial de Vistoria e Conformidade • Nº ${report.reportNumber.ifEmpty { "#${report.id}" }}", margin + 18f, margin + 78f, whiteSub)

        currentY = margin + 110f

        // Informações Gerais Card
        val infoCardHeight = 75f
        bgPaint.color = bgLight
        canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + infoCardHeight), 6f, 6f, bgPaint)
        canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + infoCardHeight), 6f, 6f, linePaint)

        val colA = margin + 14f
        val colB = margin + (usableWidth / 2) + 10f

        fun drawMetaItem(x: Float, y: Float, label: String, value: String) {
            canvas.drawText(label, x, y, bodyMutedPaint)
            canvas.drawText(value, x, y + 12f, headerPaint)
        }

        drawMetaItem(colA, currentY + 20f, "EMPRESA / LOCAL:", report.location.ifEmpty { "Indústria ABC Lda." })
        drawMetaItem(colA, currentY + 50f, "RESPONSÁVEL TÉCNICO:", report.responsible.ifEmpty { "João da Silva" })
        drawMetaItem(colB, currentY + 20f, "DATA E HORA DA VISTORIA:", dateStr)
        val statusDisplay = when (report.status) {
            "FINALIZED" -> "CONCLUÍDO"
            "SENT" -> "ENVIADO"
            "DRAFT" -> "RASCUNHO"
            else -> report.status.uppercase(Locale.getDefault())
        }
        drawMetaItem(colB, currentY + 50f, "STATUS DO LAUDO:", statusDisplay)

        currentY += infoCardHeight + 20f

        // ==========================================
        // 2. RESUMO DE CONFORMIDADES (COMPLIANCE STATS)
        // ==========================================
        var conformeCount = 0
        var naoConformeCount = 0
        var naCount = 0

        for (field in fields) {
            val answer = answers.find { it.templateFieldId == field.id }
            when (normalizeAnswer(answer?.answerValue)) {
                "C" -> conformeCount++
                "NC" -> naoConformeCount++
                else -> naCount++
            }
        }
        val totalCount = fields.size
        val complianceRate = if (totalCount > 0) ((conformeCount.toFloat() / totalCount.toFloat()) * 100).toInt() else 100

        canvas.drawText("1. RESUMO DA INSPEÇÃO", margin, currentY, sectionTitlePaint)
        currentY += 12f

        val summaryCardHeight = 44f
        bgPaint.color = Color.WHITE
        canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + summaryCardHeight), 6f, 6f, bgPaint)
        canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + summaryCardHeight), 6f, 6f, linePaint)

        val statWidth = usableWidth / 4f

        fun drawStatBox(index: Int, label: String, count: Int, color: Int) {
            val startX = margin + (index * statWidth)
            if (index > 0) {
                canvas.drawLine(startX, currentY + 8f, startX, currentY + summaryCardHeight - 8f, linePaint)
            }
            val numText = count.toString()
            val statPaint = TextPaint().apply {
                this.color = color
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(numText, startX + 14f, currentY + 22f, statPaint)
            canvas.drawText(label, startX + 14f, currentY + 36f, bodyMutedPaint)
        }

        drawStatBox(0, "Total de Itens", totalCount, primaryDark)
        drawStatBox(1, "Conformes", conformeCount, colorConforme)
        drawStatBox(2, "Não Conformes", naoConformeCount, colorNaoConforme)
        drawStatBox(3, "Taxa de Conformidade", complianceRate, primaryBlue)

        currentY += summaryCardHeight + 22f

        // ==========================================
        // 3. TABELA DO CHECKLIST
        // ==========================================
        canvas.drawText("2. ITENS DE VERIFICAÇÃO (CHECKLIST)", margin, currentY, sectionTitlePaint)
        currentY += 14f

        // Table Header
        val thHeight = 22f
        bgPaint.color = primaryDark
        canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + thHeight), 4f, 4f, bgPaint)
        
        val thTextPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val c1 = margin + 8f // Nº
        val c2 = margin + 34f // Descrição
        val c3 = margin + 300f // Status
        val c4 = margin + 380f // Observações

        canvas.drawText("Nº", c1, currentY + 15f, thTextPaint)
        canvas.drawText("ITEM / DESCRIÇÃO", c2, currentY + 15f, thTextPaint)
        canvas.drawText("STATUS", c3, currentY + 15f, thTextPaint)
        canvas.drawText("OBSERVAÇÕES TÉCNICAS", c4, currentY + 15f, thTextPaint)

        currentY += thHeight + 4f

        for ((index, field) in fields.withIndex()) {
            val answer = answers.find { it.templateFieldId == field.id }
            val normStatus = normalizeAnswer(answer?.answerValue)
            val obsText = answer?.observation?.trim() ?: ""
            val orderStr = String.format(Locale.getDefault(), "%02d", index + 1)

            @Suppress("DEPRECATION")
            val questionLayout = StaticLayout(field.label, bodyPaint, 255, Layout.Alignment.ALIGN_NORMAL, 1.1f, 0.0f, false)
            @Suppress("DEPRECATION")
            val obsLayout = StaticLayout(if (obsText.isEmpty()) "—" else obsText, bodyMutedPaint, (usableWidth - 390).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.1f, 0.0f, false)

            val rowHeight = maxOf(questionLayout.height, obsLayout.height) + 14f

            if (currentY + rowHeight > pageHeight - margin - 30f) {
                startNewPage()
                // Re-draw table header on new page
                bgPaint.color = primaryDark
                canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + thHeight), 4f, 4f, bgPaint)
                canvas.drawText("Nº", c1, currentY + 15f, thTextPaint)
                canvas.drawText("ITEM / DESCRIÇÃO", c2, currentY + 15f, thTextPaint)
                canvas.drawText("STATUS", c3, currentY + 15f, thTextPaint)
                canvas.drawText("OBSERVAÇÕES TÉCNICAS", c4, currentY + 15f, thTextPaint)
                currentY += thHeight + 4f
            }

            // Alternating row background
            if (index % 2 == 1) {
                bgPaint.color = bgLight
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, bgPaint)
            }

            // Draw Nº
            canvas.drawText(orderStr, c1, currentY + 12f, headerPaint)

            // Draw Question
            canvas.save()
            canvas.translate(c2, currentY + 3f)
            questionLayout.draw(canvas)
            canvas.restore()

            // Draw Status Badge
            val badgeColor = when (normStatus) {
                "C" -> colorConforme
                "NC" -> colorNaoConforme
                else -> colorNA
            }
            val badgeText = when (normStatus) {
                "C" -> "CONFORME"
                "NC" -> "NÃO CONF."
                else -> "N/A"
            }
            bgPaint.color = badgeColor
            val badgeWidth = 62f
            canvas.drawRoundRect(RectF(c3, currentY + 2f, c3 + badgeWidth, currentY + 18f), 3f, 3f, bgPaint)
            
            val badgeTextPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(badgeText, c3 + (badgeWidth / 2), currentY + 13f, badgeTextPaint)

            // Draw Obs
            canvas.save()
            canvas.translate(c4, currentY + 3f)
            obsLayout.draw(canvas)
            canvas.restore()

            currentY += rowHeight
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
        }

        currentY += 24f

        // ==========================================
        // 4. OBSERVAÇÕES FINAIS (GENERAL OBSERVATIONS)
        // ==========================================
        val generalObs = report.generalObservations?.trim() ?: ""
        if (generalObs.isNotEmpty()) {
            @Suppress("DEPRECATION")
            val obsGeneralLayout = StaticLayout(
                generalObs,
                bodyPaint,
                (usableWidth - 24).toInt(),
                Layout.Alignment.ALIGN_NORMAL,
                1.2f,
                0.0f,
                false
            )
            val obsBoxHeight = obsGeneralLayout.height + 28f

            if (currentY + obsBoxHeight + 30f > pageHeight - margin - 30f) {
                startNewPage()
            }

            canvas.drawText("3. OBSERVAÇÕES FINAIS E RECOMENDAÇÕES", margin, currentY, sectionTitlePaint)
            currentY += 12f

            bgPaint.color = bgLight
            canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + obsBoxHeight), 6f, 6f, bgPaint)
            canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + obsBoxHeight), 6f, 6f, linePaint)

            canvas.save()
            canvas.translate(margin + 12f, currentY + 14f)
            obsGeneralLayout.draw(canvas)
            canvas.restore()

            currentY += obsBoxHeight + 24f
        }

        // ==========================================
        // 5. EVIDÊNCIAS FOTOGRÁFICAS (GRID DE 2 POR LINHA)
        // ==========================================
        data class PhotoItem(
            val localPath: String,
            val itemNumber: String?,
            val itemLabel: String?,
            val status: String?,
            val description: String?
        )

        val photoItems = mutableListOf<PhotoItem>()

        // 1. Photos from PhotoEntity list
        for (pe in photoEntities) {
            val field = fields.find { it.id == pe.templateFieldId }
            val answer = if (field != null) answers.find { it.templateFieldId == field.id } else null
            val desc = if (!pe.description.isNullOrBlank()) pe.description else answer?.observation
            photoItems.add(
                PhotoItem(
                    localPath = pe.localPath,
                    itemNumber = field?.let { "Item ${String.format(Locale.getDefault(), "%02d", it.orderIndex + 1)}" },
                    itemLabel = field?.label,
                    status = normalizeAnswer(answer?.answerValue),
                    description = desc
                )
            )
        }

        // 2. Fallback to photos map if photoEntities was empty
        if (photoItems.isEmpty()) {
            for (field in fields) {
                val pList = photos[field.id]
                val answer = answers.find { it.templateFieldId == field.id }
                pList?.forEach { path ->
                    photoItems.add(
                        PhotoItem(
                            localPath = path,
                            itemNumber = "Item ${String.format(Locale.getDefault(), "%02d", field.orderIndex + 1)}",
                            itemLabel = field.label,
                            status = normalizeAnswer(answer?.answerValue),
                            description = answer?.observation
                        )
                    )
                }
            }
        }

        if (photoItems.isNotEmpty()) {
            if (currentY + 120f > pageHeight - margin - 30f) {
                startNewPage()
            }

            canvas.drawText("4. EVIDÊNCIAS FOTOGRÁFICAS", margin, currentY, sectionTitlePaint)
            currentY += 14f

            val cardWidth = (usableWidth - 16f) / 2f
            val cardHeight = 175f
            val imgHeight = 110f

            var colIndex = 0

            for ((pIdx, item) in photoItems.withIndex()) {
                val file = File(item.localPath)
                if (!file.exists()) continue

                // Check if row fits
                if (colIndex == 0 && (currentY + cardHeight > pageHeight - margin - 30f)) {
                    startNewPage()
                    canvas.drawText("4. EVIDÊNCIAS FOTOGRÁFICAS (Continuação)", margin, currentY, sectionTitlePaint)
                    currentY += 14f
                }

                val cardX = margin + (colIndex * (cardWidth + 16f))
                val cardY = currentY

                // Draw Card Container
                bgPaint.color = Color.WHITE
                canvas.drawRoundRect(RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight), 6f, 6f, bgPaint)
                canvas.drawRoundRect(RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight), 6f, 6f, linePaint)

                // Top Badge: "EVIDÊNCIA 01"
                bgPaint.color = primaryDark
                canvas.drawRoundRect(RectF(cardX, cardY, cardX + cardWidth, cardY + 18f), 6f, 6f, bgPaint)
                canvas.drawRect(cardX, cardY + 10f, cardX + cardWidth, cardY + 18f, bgPaint) // flat bottom

                val evTitlePaint = TextPaint().apply {
                    color = Color.WHITE
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val evNum = String.format(Locale.getDefault(), "EVIDÊNCIA %02d", pIdx + 1)
                canvas.drawText(evNum, cardX + 8f, cardY + 13f, evTitlePaint)

                // Render Image
                try {
                    val bitmap = BitmapFactory.decodeFile(item.localPath)
                    if (bitmap != null) {
                        val imgRect = RectF(cardX + 4f, cardY + 22f, cardX + cardWidth - 4f, cardY + 22f + imgHeight)
                        val scale = maxOf(imgRect.width() / bitmap.width, imgRect.height() / bitmap.height)
                        val scaledW = bitmap.width * scale
                        val scaledH = bitmap.height * scale
                        val offsetX = (imgRect.width() - scaledW) / 2f
                        val offsetY = (imgRect.height() - scaledH) / 2f

                        canvas.save()
                        canvas.clipRect(imgRect)
                        canvas.translate(imgRect.left + offsetX, imgRect.top + offsetY)
                        canvas.scale(scale, scale)
                        canvas.drawBitmap(bitmap, 0f, 0f, null)
                        canvas.restore()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Info Footer inside card
                val footerY = cardY + 22f + imgHeight + 8f
                val itemTitle = item.itemNumber ?: "Registro Geral"
                canvas.drawText(itemTitle, cardX + 8f, footerY + 6f, headerPaint)

                if (item.status != null) {
                    val statusText = when (item.status) {
                        "C" -> "Conforme"
                        "NC" -> "Não Conforme"
                        else -> "N/A"
                    }
                    val statusColor = when (item.status) {
                        "C" -> colorConforme
                        "NC" -> colorNaoConforme
                        else -> colorNA
                    }
                    val stPaint = TextPaint().apply {
                        color = statusColor
                        textSize = 8f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val stWidth = stPaint.measureText(statusText)
                    canvas.drawText(statusText, cardX + cardWidth - 8f - stWidth, footerY + 6f, stPaint)
                }

                val desc = item.description ?: item.itemLabel ?: ""
                val truncatedDesc = if (desc.length > 42) desc.take(40) + "..." else desc
                canvas.drawText(truncatedDesc, cardX + 8f, footerY + 18f, bodyMutedPaint)

                colIndex++
                if (colIndex == 2) {
                    colIndex = 0
                    currentY += cardHeight + 14f
                }
            }

            if (colIndex != 0) {
                currentY += cardHeight + 14f
            }
            currentY += 14f
        }

        // ==========================================
        // 6. DUAS ASSINATURAS (SIDE BY SIDE)
        // ==========================================
        val sigBoxHeight = 110f
        if (currentY + sigBoxHeight + 30f > pageHeight - margin - 30f) {
            startNewPage()
        }

        canvas.drawText("5. ASSINATURAS E RESPONSABILIDADES", margin, currentY, sectionTitlePaint)
        currentY += 14f

        val sigWidth = (usableWidth - 16f) / 2f

        // Get the 2 signatures
        val sigRespRelatorio = signatures.firstOrNull { 
            it.role.contains("RESPONSAVEL", ignoreCase = true) || 
            it.role.contains("Inspetor", ignoreCase = true) ||
            it.role.contains("Técnico", ignoreCase = true)
        } ?: signatures.firstOrNull()

        val sigPresenteOp = signatures.firstOrNull { 
            (it.role.contains("PRESENTE", ignoreCase = true) || 
             it.role.contains("OPERACAO", ignoreCase = true) || 
             it.role.contains("Acompanhante", ignoreCase = true) || 
             it.role.contains("Supervisor", ignoreCase = true) || 
             it.role.contains("Gerente", ignoreCase = true) ||
             it.role.contains("Cliente", ignoreCase = true)) && it.id != (sigRespRelatorio?.id ?: -1L)
        } ?: signatures.firstOrNull { it.id != (sigRespRelatorio?.id ?: -1L) }

        fun drawSignatureBox(
            x: Float,
            y: Float,
            title: String,
            sig: SignatureEntity?,
            defaultName: String,
            defaultRole: String
        ) {
            bgPaint.color = Color.WHITE
            canvas.drawRoundRect(RectF(x, y, x + sigWidth, y + sigBoxHeight), 6f, 6f, bgPaint)
            canvas.drawRoundRect(RectF(x, y, x + sigWidth, y + sigBoxHeight), 6f, 6f, linePaint)

            // Header of box
            canvas.drawText(title, x + 10f, y + 16f, headerPaint)
            canvas.drawLine(x + 10f, y + 22f, x + sigWidth - 10f, y + 22f, linePaint)

            // Draw Signature Bitmap if present
            var drawn = false
            if (sig != null && !sig.localPath.isNullOrBlank()) {
                val file = File(sig.localPath)
                if (file.exists()) {
                    try {
                        val sigBmp = BitmapFactory.decodeFile(sig.localPath)
                        if (sigBmp != null) {
                            val sigRect = RectF(x + 15f, y + 26f, x + sigWidth - 15f, y + 74f)
                            val scale = minOf(sigRect.width() / sigBmp.width, sigRect.height() / sigBmp.height)
                            val scaledW = sigBmp.width * scale
                            val scaledH = sigBmp.height * scale
                            val offsetX = (sigRect.width() - scaledW) / 2f
                            val offsetY = (sigRect.height() - scaledH) / 2f

                            val dest = RectF(sigRect.left + offsetX, sigRect.top + offsetY, sigRect.left + offsetX + scaledW, sigRect.top + offsetY + scaledH)
                            canvas.drawBitmap(sigBmp, null, dest, null)
                            drawn = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (!drawn) {
                canvas.drawText("[ Assinatura Digital Pendente ]", x + 20f, y + 54f, bodyMutedPaint)
            }

            // Divider before name
            canvas.drawLine(x + 10f, y + 78f, x + sigWidth - 10f, y + 78f, linePaint)

            val name = sig?.name?.ifEmpty { defaultName } ?: defaultName
            val rawRole = sig?.role?.ifEmpty { defaultRole } ?: defaultRole
            val displayRole = when (rawRole) {
                "RESPONSAVEL_RELATORIO" -> "Inspetor Técnico"
                "PRESENTE_OPERACAO" -> defaultRole
                else -> rawRole
            }
            canvas.drawText("Nome: $name", x + 10f, y + 92f, bodyPaint)
            canvas.drawText("Cargo: $displayRole  •  Data: $dateOnlyStr", x + 10f, y + 104f, bodyMutedPaint)
        }

        drawSignatureBox(
            x = margin,
            y = currentY,
            title = "RESPONSÁVEL PELO RELATÓRIO",
            sig = sigRespRelatorio,
            defaultName = report.responsible.ifEmpty { "João da Silva" },
            defaultRole = "Inspetor Técnico"
        )

        drawSignatureBox(
            x = margin + sigWidth + 16f,
            y = currentY,
            title = "PRESENTE NA OPERAÇÃO / ACOMPANHANTE",
            sig = sigPresenteOp,
            defaultName = "Responsável no Local",
            defaultRole = "Acompanhante / Supervisor"
        )

        // Draw header and footer on the final page
        drawHeaderAndFooter(isCover = pageNumber == 1)
        document.finishPage(page)

        val outputDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val safeLocation = report.location.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val dateForName = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(report.date))
        val outputFile = File(outputDir, "RELATO-PRO_Relatorio_${safeLocation}_${dateForName}_${report.id}.pdf")

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

    private fun normalizeAnswer(answerValue: String?): String {
        if (answerValue == null) return "NA"
        val trimmed = answerValue.trim().uppercase(Locale.getDefault())
        return when {
            trimmed == "C" || trimmed == "CONFORME" -> "C"
            trimmed == "NC" || trimmed == "NÃO CONFORME" || trimmed == "NAO CONFORME" -> "NC"
            else -> "NA"
        }
    }
}
