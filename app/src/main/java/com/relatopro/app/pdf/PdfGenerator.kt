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
import com.relatopro.app.utils.PhotoQuality
import com.relatopro.app.utils.PreferenceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    data class PdfGenerationResult(
        val file: File,
        val fileSizeBytes: Long,
        val fileSizeFormatted: String,
        val photosCount: Int
    )

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
        photoEntities: List<PhotoEntity> = emptyList(),
        previousReport: ReportEntity? = null,
        previousAnswers: List<ReportAnswerEntity> = emptyList(),
        onProgress: ((current: Int, total: Int, stage: String) -> Unit)? = null
    ): PdfGenerationResult? = withContext(Dispatchers.IO) {
        onProgress?.invoke(0, 100, "Iniciando estruturação do relatório...")

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
            color = Color.parseColor("#93C5FD")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        canvas.drawText("RELATO PRO", margin + 16f, margin + 30f, whiteTitle)
        canvas.drawText("SISTEMA PROFISSIONAL DE AUDITORIA & INSPEÇÃO", margin + 16f, margin + 44f, whiteSub)

        // Right box: Report Number & Date
        val rightInfoPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val rightMutedPaint = TextPaint().apply {
            color = Color.parseColor("#93C5FD")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val rightX = pageWidth - margin - 16f
        canvas.drawText("LAUDO TÉCNICO", rightX, margin + 26f, rightInfoPaint)
        canvas.drawText("Nº ${report.reportNumber.ifEmpty { "#${report.id}" }}", rightX, margin + 40f, rightInfoPaint)
        canvas.drawText("Emissão: $dateStr", rightX, margin + 54f, rightMutedPaint)
        canvas.drawText("Status: FINALIZADO", rightX, margin + 68f, rightMutedPaint)

        currentY = margin + 112f

        // ==========================================
        // 2. DADOS DA EMPRESA & INSPEÇÃO
        // ==========================================
        canvas.drawText("1. DADOS DA EMPRESA & INSPEÇÃO", margin, currentY, sectionTitlePaint)
        currentY += 10f

        val infoBoxHeight = 84f
        bgPaint.color = bgLight
        canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + infoBoxHeight), 6f, 6f, bgPaint)
        canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + infoBoxHeight), 6f, 6f, linePaint)

        val col1X = margin + 14f
        val col2X = margin + (usableWidth / 2f) + 8f

        fun drawInfoField(x: Float, y: Float, label: String, value: String) {
            canvas.drawText(label.uppercase(Locale.getDefault()), x, y, bodyMutedPaint.apply { textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            val valLayout = @Suppress("DEPRECATION") StaticLayout(
                value.ifBlank { "Não informado" },
                bodyPaint.apply { textSize = 9f },
                (usableWidth / 2f - 24f).toInt(),
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                false
            )
            canvas.save()
            canvas.translate(x, y + 2f)
            valLayout.draw(canvas)
            canvas.restore()
        }

        val companyDisplayName = report.companyName.ifBlank { "Empresa não informada" }
        val unitDisplayName = report.unit.ifBlank { "Matriz" }
        drawInfoField(col1X, currentY + 12f, "Empresa Inspecionada", companyDisplayName)
        drawInfoField(col2X, currentY + 12f, "Unidade / Filial", unitDisplayName)

        drawInfoField(col1X, currentY + 38f, "Título do Relatório", report.title.ifBlank { "Vistoria Geral" })
        drawInfoField(col2X, currentY + 38f, "Local / Setor / Frente de Trabalho", report.location.ifBlank { "Não especificado" })

        drawInfoField(col1X, currentY + 64f, "Responsável Técnico", report.responsible.ifBlank { "Alexandre Machado" })
        drawInfoField(col2X, currentY + 64f, "Data e Hora da Inspeção", dateStr)

        currentY += infoBoxHeight + 16f

        // ==========================================
        // 3. QUADRO RESUMO DE CONFORMIDADE
        // ==========================================
        var countConforme = 0
        var countNaoConforme = 0
        var countNA = 0

        answers.forEach { ans ->
            when (normalizeAnswer(ans.answerValue)) {
                "C" -> countConforme++
                "NC" -> countNaoConforme++
                else -> countNA++
            }
        }

        val totalEvaluated = countConforme + countNaoConforme
        val conformidadePct = if (totalEvaluated > 0) {
            (countConforme.toFloat() / totalEvaluated.toFloat()) * 100f
        } else {
            100f
        }
        val compPctStr = String.format(Locale.getDefault(), "%.1f%%", conformidadePct)

        // Summary Metric Cards
        val cardWidth = (usableWidth - 24f) / 4f
        val cardHeight = 44f

        fun drawStatCard(x: Float, y: Float, label: String, value: String, valueColor: Int) {
            bgPaint.color = bgLight
            canvas.drawRoundRect(RectF(x, y, x + cardWidth, y + cardHeight), 6f, 6f, bgPaint)
            canvas.drawRoundRect(RectF(x, y, x + cardWidth, y + cardHeight), 6f, 6f, linePaint)

            val valPaint = TextPaint().apply {
                color = valueColor
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val lblPaint = TextPaint().apply {
                color = textMuted
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText(value, x + (cardWidth / 2), y + 20f, valPaint)
            canvas.drawText(label, x + (cardWidth / 2), y + 34f, lblPaint)
        }

        drawStatCard(margin, currentY, "TOTAL ITENS", "${fields.size}", primaryDark)
        drawStatCard(margin + cardWidth + 8f, currentY, "CONFORMES (C)", "$countConforme", colorConforme)
        drawStatCard(margin + (cardWidth * 2) + 16f, currentY, "NÃO CONF. (NC)", "$countNaoConforme", colorNaoConforme)
        drawStatCard(margin + (cardWidth * 3) + 24f, currentY, "ÍNDICE CONFORMIDADE", compPctStr, if (conformidadePct >= 80f) colorConforme else colorNaoConforme)

        currentY += cardHeight + 20f

        // ==========================================
        // 4. CHECKLIST DETALHADO POR CATEGORIAS
        // ==========================================
        canvas.drawText("2. CHECKLIST DE INSPEÇÃO DETALHADO", margin, currentY, sectionTitlePaint)
        currentY += 12f

        val groupedFields = fields.groupBy { it.category.ifBlank { "GERAL" } }

        for ((catName, catFields) in groupedFields) {
            var catC = 0
            var catNC = 0
            var catNA = 0

            // Check if category header fits
            if (currentY + 60f > pageHeight - margin - 30f) {
                startNewPage()
            }

            // Category Banner
            bgPaint.color = primaryDark
            canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + 20f), 4f, 4f, bgPaint)
            val catHeaderPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("CATEGORIA: ${catName.uppercase(Locale.getDefault())} (${catFields.size} ITENS)", margin + 8f, currentY + 14f, catHeaderPaint)
            currentY += 24f

            // Table Header
            val thHeight = 16f
            bgPaint.color = Color.parseColor("#E2E8F0")
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + thHeight, bgPaint)

            val c1 = margin + 6f
            val c2 = margin + 30f
            val c3 = margin + usableWidth - 190f
            val c4 = margin + usableWidth - 120f

            canvas.drawText("Nº", c1, currentY + 11.5f, headerPaint.apply { textSize = 8f })
            canvas.drawText("ITEM / REQUISITO AVALIADO", c2, currentY + 11.5f, headerPaint)
            canvas.drawText("STATUS", c3, currentY + 11.5f, headerPaint)
            canvas.drawText("OBSERVAÇÕES", c4, currentY + 11.5f, headerPaint)
            currentY += thHeight

            for ((fIndex, field) in catFields.withIndex()) {
                val answer = answers.find { it.templateFieldId == field.id }
                val normStatus = normalizeAnswer(answer?.answerValue)
                when (normStatus) {
                    "C" -> catC++
                    "NC" -> catNC++
                    else -> catNA++
                }

                val orderStr = String.format(Locale.getDefault(), "%02d", fIndex + 1)
                val itemLabelText = field.label

                val questionWidth = (c3 - c2 - 12f).toInt()
                val obsWidth = (pageWidth - margin - c4 - 6f).toInt()

                @Suppress("DEPRECATION")
                val questionLayout = StaticLayout(
                    itemLabelText,
                    bodyPaint.apply { textSize = 8.5f },
                    questionWidth,
                    Layout.Alignment.ALIGN_NORMAL,
                    1.1f,
                    0.0f,
                    false
                )

                val obsText = answer?.observation?.ifBlank { "-" } ?: "-"
                @Suppress("DEPRECATION")
                val obsLayout = StaticLayout(
                    obsText,
                    bodyMutedPaint.apply { textSize = 8f },
                    obsWidth,
                    Layout.Alignment.ALIGN_NORMAL,
                    1.1f,
                    0.0f,
                    false
                )

                val rowHeight = maxOf(22f, maxOf(questionLayout.height.toFloat(), obsLayout.height.toFloat()) + 8f)

                if (currentY + rowHeight > pageHeight - margin - 30f) {
                    startNewPage()
                }

                // Zebra row background
                if (fIndex % 2 == 1) {
                    bgPaint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, bgPaint)
                }

                // Draw Nº
                canvas.drawText(orderStr, c1, currentY + 12f, headerPaint.apply { textSize = 8f })

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

            // Category Subtotal Footer
            val catApp = catC + catNC
            val catCompPct = if (catApp > 0) String.format(Locale.getDefault(), "%.1f%%", (catC.toFloat() / catApp.toFloat() * 100f)) else "100%"
            val catSubtotalText = "Subtotal $catName: C: $catC | NC: $catNC | NA: $catNA | Conformidade: $catCompPct"

            bgPaint.color = Color.parseColor("#F1F5F9")
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 16f, bgPaint)
            canvas.drawText(catSubtotalText, margin + 8f, currentY + 11.5f, bodyMutedPaint.apply { textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            currentY += 22f
        }

        currentY += 16f

        // ==========================================
        // 5. OBSERVAÇÕES FINAIS E RECOMENDAÇÕES
        // ==========================================
        val generalObs = report.generalObservations?.trim() ?: ""
        if (generalObs.isNotEmpty()) {
            @Suppress("DEPRECATION")
            val obsGeneralLayout = StaticLayout(
                generalObs,
                bodyPaint.apply { textSize = 9f },
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
        // 6. EVIDÊNCIAS FOTOGRÁFICAS VINCULADAS AOS ITENS
        // ==========================================
        data class ItemEvidenceGroup(
            val fieldId: Long,
            val categoryName: String,
            val itemOrder: Int,
            val questionText: String,
            val status: String,
            val observation: String,
            val photoPaths: List<String>
        )

        val evidenceGroups = mutableListOf<ItemEvidenceGroup>()

        // Group photos by Field / Question
        val allFieldsMap = fields.associateBy { it.id }
        val answersMap = answers.associateBy { it.templateFieldId }

        // 1. From PhotoEntity
        val photosByField = photoEntities.groupBy { it.templateFieldId }
        for ((fieldId, peList) in photosByField) {
            val field = allFieldsMap[fieldId]
            val answer = answersMap[fieldId]
            val validPaths = peList.map { it.localPath }.filter { File(it).exists() }
            if (validPaths.isNotEmpty()) {
                evidenceGroups.add(
                    ItemEvidenceGroup(
                        fieldId = fieldId ?: 0L,
                        categoryName = field?.category?.ifBlank { "GERAL" } ?: "GERAL",
                        itemOrder = field?.orderIndex ?: 0,
                        questionText = field?.label ?: "Item não especificado",
                        status = normalizeAnswer(answer?.answerValue),
                        observation = answer?.observation?.ifBlank { peList.firstNotNullOfOrNull { it.description } ?: "" } ?: "",
                        photoPaths = validPaths
                    )
                )
            }
        }

        // 2. Fallback to photos map if empty
        if (evidenceGroups.isEmpty() && photos.isNotEmpty()) {
            for ((fieldId, pList) in photos) {
                val field = allFieldsMap[fieldId]
                val answer = answersMap[fieldId]
                val validPaths = pList.filter { File(it).exists() }
                if (validPaths.isNotEmpty()) {
                    evidenceGroups.add(
                        ItemEvidenceGroup(
                            fieldId = fieldId,
                            categoryName = field?.category?.ifBlank { "GERAL" } ?: "GERAL",
                            itemOrder = field?.orderIndex ?: 0,
                            questionText = field?.label ?: "Item não especificado",
                            status = normalizeAnswer(answer?.answerValue),
                            observation = answer?.observation ?: "",
                            photoPaths = validPaths
                        )
                    )
                }
            }
        }

        val totalPhotosCount = evidenceGroups.sumOf { it.photoPaths.size }
        var processedPhotosCount = 0

        if (evidenceGroups.isNotEmpty()) {
            if (currentY + 140f > pageHeight - margin - 30f) {
                startNewPage()
            }

            canvas.drawText("4. EVIDÊNCIAS FOTOGRÁFICAS E CONSTATAÇÕES", margin, currentY, sectionTitlePaint)
            currentY += 14f

            for ((groupIndex, group) in evidenceGroups.withIndex()) {
                val statusText = when (group.status) {
                    "C" -> "CONFORME"
                    "NC" -> "NÃO CONFORME"
                    else -> "N/A"
                }
                val statusColor = when (group.status) {
                    "C" -> colorConforme
                    "NC" -> colorNaoConforme
                    else -> colorNA
                }

                // Layout item header
                val headerTitleText = "ITEM ${String.format(Locale.getDefault(), "%02d", group.itemOrder + 1)} • ${group.categoryName.uppercase(Locale.getDefault())}"
                val questionFullText = group.questionText

                @Suppress("DEPRECATION")
                val qLayout = StaticLayout(
                    questionFullText,
                    bodyPaint.apply { textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) },
                    (usableWidth - 85f).toInt(),
                    Layout.Alignment.ALIGN_NORMAL,
                    1.1f,
                    0.0f,
                    false
                )

                var headerHeight = qLayout.height + 24f
                if (group.observation.isNotBlank()) {
                    headerHeight += 18f
                }

                // Check if group header fits on current page
                if (currentY + headerHeight + 120f > pageHeight - margin - 30f) {
                    startNewPage()
                    canvas.drawText("4. EVIDÊNCIAS FOTOGRÁFICAS (Continuação)", margin, currentY, sectionTitlePaint)
                    currentY += 14f
                }

                // Draw Evidence Header Container
                bgPaint.color = Color.parseColor("#F1F5F9")
                canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + headerHeight), 6f, 6f, bgPaint)
                canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + headerHeight), 6f, 6f, linePaint)

                // Category & Item index badge
                canvas.drawText(headerTitleText, margin + 8f, currentY + 11f, bodyMutedPaint.apply { textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

                // Result Badge in top-right
                val badgeW = 74f
                bgPaint.color = statusColor
                canvas.drawRoundRect(RectF(pageWidth - margin - badgeW - 6f, currentY + 4f, pageWidth - margin - 6f, currentY + 18f), 3f, 3f, bgPaint)
                val badgeTp = TextPaint().apply {
                    color = Color.WHITE
                    textSize = 7.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(statusText, pageWidth - margin - 6f - (badgeW / 2), currentY + 14f, badgeTp)

                // Draw full question text
                canvas.save()
                canvas.translate(margin + 8f, currentY + 15f)
                qLayout.draw(canvas)
                canvas.restore()

                // Draw observation if available
                if (group.observation.isNotBlank()) {
                    val obsPrefix = "Constatação / Observação: " + group.observation
                    val obsTrunc = if (obsPrefix.length > 120) obsPrefix.take(117) + "..." else obsPrefix
                    canvas.drawText(obsTrunc, margin + 8f, currentY + headerHeight - 6f, bodyMutedPaint.apply { textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) })
                }

                currentY += headerHeight + 8f

                // Draw photo grid (2 photos per row)
                val gridCardWidth = (usableWidth - 12f) / 2f
                val gridCardHeight = 150f
                val gridImgHeight = 118f

                val photoChunks = group.photoPaths.chunked(2)
                for (chunk in photoChunks) {
                    if (currentY + gridCardHeight > pageHeight - margin - 30f) {
                        startNewPage()
                        canvas.drawText("4. EVIDÊNCIAS FOTOGRÁFICAS (Continuação)", margin, currentY, sectionTitlePaint)
                        currentY += 14f
                    }

                    for ((colIdx, pPath) in chunk.withIndex()) {
                        val cardX = margin + (colIdx * (gridCardWidth + 12f))
                        val cardY = currentY

                        // Draw card border & background
                        bgPaint.color = Color.WHITE
                        canvas.drawRoundRect(RectF(cardX, cardY, cardX + gridCardWidth, cardY + gridCardHeight), 6f, 6f, bgPaint)
                        canvas.drawRoundRect(RectF(cardX, cardY, cardX + gridCardWidth, cardY + gridCardHeight), 6f, 6f, linePaint)

                        // Top bar inside photo card
                        bgPaint.color = primaryDark
                        canvas.drawRoundRect(RectF(cardX, cardY, cardX + gridCardWidth, cardY + 16f), 6f, 6f, bgPaint)
                        canvas.drawRect(cardX, cardY + 8f, cardX + gridCardWidth, cardY + 16f, bgPaint)

                        val photoTitle = "EVIDÊNCIA FOTOGRÁFICA #${processedPhotosCount + 1}"
                        val phTitlePaint = TextPaint().apply {
                            color = Color.WHITE
                            textSize = 7.5f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            isAntiAlias = true
                        }
                        canvas.drawText(photoTitle, cardX + 6f, cardY + 11.5f, phTitlePaint)

                        // Load and compress photo bitmap using PdfImageCompressor
                        processedPhotosCount++
                        onProgress?.invoke(
                            processedPhotosCount,
                            totalPhotosCount,
                            "Otimizando foto $processedPhotosCount de $totalPhotosCount..."
                        )

                        val optBitmap = PdfImageCompressor.loadOptimizedBitmapForPdf(
                            context = context,
                            filePath = pPath,
                            targetWidth = 600,
                            targetHeight = 450
                        )

                        if (optBitmap != null) {
                            try {
                                val imgRect = RectF(cardX + 4f, cardY + 19f, cardX + gridCardWidth - 4f, cardY + 19f + gridImgHeight)
                                val scale = maxOf(imgRect.width() / optBitmap.width.toFloat(), imgRect.height() / optBitmap.height.toFloat())
                                val scaledW = optBitmap.width * scale
                                val scaledH = optBitmap.height * scale
                                val offsetX = (imgRect.width() - scaledW) / 2f
                                val offsetY = (imgRect.height() - scaledH) / 2f

                                canvas.save()
                                canvas.clipRect(imgRect)
                                canvas.translate(imgRect.left + offsetX, imgRect.top + offsetY)
                                canvas.scale(scale, scale)
                                val bmpPaint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
                                canvas.drawBitmap(optBitmap, 0f, 0f, bmpPaint)
                                canvas.restore()
                            } finally {
                                optBitmap.recycle()
                            }
                        } else {
                            // Fallback placeholder box
                            bgPaint.color = Color.parseColor("#E2E8F0")
                            canvas.drawRect(cardX + 4f, cardY + 19f, cardX + gridCardWidth - 4f, cardY + 19f + gridImgHeight, bgPaint)
                            canvas.drawText("Imagem não encontrada", cardX + 20f, cardY + 70f, bodyMutedPaint)
                        }

                        // Footer date/time
                        val photoFile = File(pPath)
                        val lastMod = if (photoFile.exists()) dateOnlyDf.format(Date(photoFile.lastModified())) else dateOnlyStr
                        canvas.drawText("Registro: $lastMod • Vistoria Técnica", cardX + 6f, cardY + gridCardHeight - 4f, bodyMutedPaint.apply { textSize = 7f })
                    }

                    currentY += gridCardHeight + 10f
                }

                currentY += 8f
            }
        }

        // ==========================================
        // 7. ASSINATURAS DIGITAIS (LADO A LADO)
        // ==========================================
        onProgress?.invoke(totalPhotosCount, totalPhotosCount, "Finalizando montagem do documento...")

        val sigBoxHeight = 110f
        if (currentY + sigBoxHeight + 30f > pageHeight - margin - 30f) {
            startNewPage()
        }

        canvas.drawText("5. VALIDAÇÃO & ASSINATURAS", margin, currentY, sectionTitlePaint)
        currentY += 12f

        val sigRespRelatorio = signatures.find {
            it.role == "RESPONSAVEL_RELATORIO" || it.role.startsWith("RESPONSAVEL") || it.role == "Inspetor Técnico"
        } ?: signatures.firstOrNull()

        val sigPresenteOp = signatures.find {
            it.role == "PRESENTE_OPERACAO" || it.role.startsWith("PRESENTE") || it.role == "Acompanhante"
        } ?: signatures.getOrNull(1)

        val sigWidth = (usableWidth - 16f) / 2f

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
            bgPaint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(RectF(x, y, x + sigWidth, y + 20f), 6f, 6f, bgPaint)
            canvas.drawRect(x, y + 12f, x + sigWidth, y + 20f, bgPaint)

            val sigHeaderPaint = TextPaint().apply {
                color = primaryDark
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(title, x + 8f, y + 13.5f, sigHeaderPaint)

            // Signature stroke / bitmap
            if (sig?.localPath != null && File(sig.localPath).exists()) {
                val sigBmp = BitmapFactory.decodeFile(sig.localPath)
                if (sigBmp != null) {
                    try {
                        val sRect = RectF(x + 10f, y + 24f, x + sigWidth - 10f, y + 74f)
                        val scale = minOf(sRect.width() / sigBmp.width.toFloat(), sRect.height() / sigBmp.height.toFloat())
                        val sW = sigBmp.width * scale
                        val sH = sigBmp.height * scale
                        val sOffX = (sRect.width() - sW) / 2f
                        val sOffY = (sRect.height() - sH) / 2f

                        canvas.save()
                        canvas.translate(sRect.left + sOffX, sRect.top + sOffY)
                        canvas.scale(scale, scale)
                        canvas.drawBitmap(sigBmp, 0f, 0f, null)
                        canvas.restore()
                    } finally {
                        sigBmp.recycle()
                    }
                }
            } else {
                // Line for manual signature
                canvas.drawLine(x + 20f, y + 68f, x + sigWidth - 20f, y + 68f, linePaint)
            }

            // Name & Role text
            val name = sig?.name?.ifBlank { defaultName } ?: defaultName
            val rawRole = sig?.role?.ifBlank { defaultRole } ?: defaultRole
            val displayRole = when {
                rawRole.contains("#") -> rawRole.substringAfter("#").ifBlank { defaultRole }
                rawRole == "RESPONSAVEL_RELATORIO" -> "Inspetor Técnico"
                rawRole == "PRESENTE_OPERACAO" -> defaultRole
                else -> rawRole
            }
            canvas.drawText("Nome: $name", x + 10f, y + 90f, bodyPaint.apply { textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            canvas.drawText("Cargo: $displayRole  •  Data: $dateOnlyStr", x + 10f, y + 102f, bodyMutedPaint.apply { textSize = 7.5f })
        }

        drawSignatureBox(
            x = margin,
            y = currentY,
            title = "RESPONSÁVEL PELO RELATÓRIO",
            sig = sigRespRelatorio,
            defaultName = report.responsible.ifBlank { "Inspetor Técnico" },
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
            FileOutputStream(outputFile).use { fos ->
                document.writeTo(fos)
            }
            val sizeBytes = outputFile.length()
            val formattedSize = formatFileSize(sizeBytes)
            PdfGenerationResult(
                file = outputFile,
                fileSizeBytes = sizeBytes,
                fileSizeFormatted = formattedSize,
                photosCount = totalPhotosCount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            document.close()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }

    private fun normalizeAnswer(answerValue: String?): String {
        if (answerValue == null) return "NA"
        val trimmed = answerValue.trim().uppercase(Locale.getDefault())
        return when {
            trimmed == "C" || trimmed == "CONFORME" || trimmed == "SIM" -> "C"
            trimmed == "NC" || trimmed == "NÃO CONFORME" || trimmed == "NAO CONFORME" || trimmed == "NÃO" -> "NC"
            else -> "NA"
        }
    }
}
