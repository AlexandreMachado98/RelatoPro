package com.relatopro.app.importer.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.relatopro.app.importer.analyzer.ChecklistStructureAnalyzer
import com.relatopro.app.importer.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PdfChecklistParser : IChecklistParser {

    override suspend fun parse(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): ParsedChecklist = withContext(Dispatchers.IO) {
        onProgress("Lendo arquivo PDF...", 10, "Carregando páginas...")

        val tempFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.pdf")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Não foi possível abrir o arquivo PDF.")

            val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val pageCount = pdfRenderer.pageCount

            onProgress("Processando páginas do PDF...", 20, "Encontradas $pageCount páginas.")

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val allParagraphs = mutableListOf<RichParagraph>()
            val allTables = mutableListOf<RichTable>()
            val allRawLines = mutableListOf<String>()
            val alerts = mutableListOf<ImportValidationAlert>()
            var extractedTitle = ""
            var extractedSubtitle = ""

            for (pageIndex in 0 until pageCount) {
                val pageNum = pageIndex + 1
                val pagePercent = 20 + (pageNum * 60 / pageCount)
                onProgress("Executando OCR e Extração Estrutural...", pagePercent, "Página $pageNum de $pageCount")

                val page = pdfRenderer.openPage(pageIndex)
                val width = (page.width * 2.0f).toInt()
                val height = (page.height * 2.0f).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val image = InputImage.fromBitmap(bitmap, 0)
                try {
                    val visionText: Text = recognizer.process(image).awaitTask()
                    val blocks: List<Text.TextBlock> = visionText.textBlocks

                    if (blocks.isNotEmpty()) {
                        // Extract lines with spatial coordinates
                        val spatialLines = blocks.flatMap { block ->
                            block.lines.map { line ->
                                SpatialLine(
                                    text = line.text.trim(),
                                    rect = line.boundingBox ?: Rect(0, 0, 0, 0),
                                    pageNumber = pageNum,
                                    isUppercase = line.text.trim().all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()" },
                                    lineLength = line.text.trim().length
                                )
                            }
                        }.filter { it.text.isNotBlank() }

                        // Sort vertically first, then horizontally
                        val sortedLines = spatialLines.sortedWith(
                            compareBy<SpatialLine> { it.rect.top / 15 } // Group into ~15px vertical bands
                                .thenBy { it.rect.left }
                        )

                        // If page 1, check for document title in upper region
                        if (pageIndex == 0) {
                            val topLines = sortedLines.filter { it.rect.top < height * 0.25f }
                            val candidateTitle = topLines.maxByOrNull { it.rect.height() }
                            if (candidateTitle != null && candidateTitle.text.length in 5..120 && !isNoiseText(candidateTitle.text)) {
                                extractedTitle = candidateTitle.text
                            }
                        }

                        // Try to group aligned lines into tabular structures
                        val (detectedTables, remainingLines) = reconstructTablesFromSpatialLines(sortedLines, pageNum)
                        allTables.addAll(detectedTables)

                        // Convert remaining lines to paragraphs with styles
                        for (sLine in remainingLines) {
                            allRawLines.add(sLine.text)
                            val isHeading = sLine.isUppercase && sLine.lineLength in 3..90
                            val style = ElementStyle(
                                isBold = isHeading || sLine.rect.height() > 30,
                                isAllUppercase = sLine.isUppercase,
                                fontSizePt = sLine.rect.height().toFloat(),
                                isHeadingStyle = isHeading,
                                boundsLeft = sLine.rect.left.toFloat(),
                                boundsTop = sLine.rect.top.toFloat(),
                                boundsRight = sLine.rect.right.toFloat(),
                                boundsBottom = sLine.rect.bottom.toFloat()
                            )

                            allParagraphs.add(
                                RichParagraph(
                                    text = sLine.text,
                                    style = style,
                                    isListItem = isListItemPattern(sLine.text),
                                    listNumber = extractLeadingNumber(sLine.text),
                                    location = SourceLocation(
                                        pageNumber = pageNum,
                                        rawText = sLine.text
                                    )
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    alerts.add(
                        ImportValidationAlert(
                            title = "Falha no OCR da Página $pageNum",
                            description = "Não foi possível extrair alguns textos da página $pageNum: ${e.localizedMessage}",
                            severity = AlertSeverity.WARNING
                        )
                    )
                } finally {
                    bitmap.recycle()
                }
            }

            pdfRenderer.close()
            fileDescriptor.close()
            recognizer.close()

            onProgress("Analisando hierarquia estrutural...", 85, "Organizando seções, categorias e itens...")

            val richDoc = RichDocumentContent(
                suggestedTitle = extractedTitle,
                suggestedSubtitle = extractedSubtitle,
                paragraphs = allParagraphs,
                tables = allTables,
                rawTextLines = allRawLines,
                alerts = alerts,
                ocrUsed = true,
                totalPages = pageCount
            )

            val parsed = ChecklistStructureAnalyzer.analyzeRich(richDoc, ImportFormat.PDF, fileName)

            onProgress("Finalizando checklist...", 100, "Concluído com sucesso!")
            parsed
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private data class SpatialLine(
        val text: String,
        val rect: Rect,
        val pageNumber: Int,
        val isUppercase: Boolean,
        val lineLength: Int
    )

    private fun reconstructTablesFromSpatialLines(
        lines: List<SpatialLine>,
        pageNum: Int
    ): Pair<List<RichTable>, List<SpatialLine>> {
        // Look for typical table header rows (e.g., containing "ITEM", "Nº", "C", "NC", "NA", "CONFORME", "OBS")
        val headerKeywords = setOf("ITEM", "Nº", "N.", "DESCRIÇÃO", "C", "NC", "NA", "CONFORME", "OBS", "OBSERVAÇÃO", "STATUS")
        val headerIndex = lines.indexOfFirst { line ->
            val words = line.text.uppercase().split(Regex("""[\s|,\t]+"""))
            val matches = words.count { it in headerKeywords }
            matches >= 2
        }

        if (headerIndex == -1) {
            return emptyList<RichTable>() to lines
        }

        val headerLine = lines[headerIndex]
        val rawHeaderTokens = headerLine.text.split(Regex("""[\s|,\t]{2,}""")).filter { it.isNotBlank() }
        val headers = if (rawHeaderTokens.size >= 2) rawHeaderTokens else listOf("Nº", "Item", "C", "NC", "NA", "Obs")

        val tableRows = mutableListOf<RichRow>()
        val unconsumedLines = mutableListOf<SpatialLine>()
        unconsumedLines.addAll(lines.take(headerIndex))

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i]
            if (isNoiseText(line.text) || isPageFooter(line.text)) {
                continue
            }

            // If line is clearly a new section header, stop table accumulation
            if (line.isUppercase && line.text.length in 4..80 && !isItemFormat(line.text)) {
                unconsumedLines.addAll(lines.subList(i, lines.size))
                break
            }

            // Split columns if multiple parts are found on line
            val tokens = line.text.split(Regex("""[\s|,\t]{2,}""")).filter { it.isNotBlank() }
            val richCells = if (tokens.size >= 2) {
                tokens.mapIndexed { idx, token ->
                    RichCell(
                        text = token.trim(),
                        columnIndex = idx,
                        rowIndex = tableRows.size,
                        style = ElementStyle(boundsTop = line.rect.top.toFloat())
                    )
                }
            } else {
                listOf(
                    RichCell(
                        text = line.text.trim(),
                        columnIndex = 0,
                        rowIndex = tableRows.size,
                        style = ElementStyle(boundsTop = line.rect.top.toFloat())
                    )
                )
            }

            tableRows.add(
                RichRow(
                    rowIndex = tableRows.size,
                    cells = richCells
                )
            )
        }

        val detectedTable = RichTable(
            id = UUID.randomUUID().toString(),
            title = "",
            headers = headers,
            rows = tableRows,
            location = SourceLocation(pageNumber = pageNum)
        )

        return listOf(detectedTable) to unconsumedLines
    }

    private fun isNoiseText(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.length <= 1) return true
        if (lower.contains("cnpj") || lower.contains("telefone") || lower.contains("www.") || lower.contains("http")) return true
        if (lower.startsWith("página ") || lower.startsWith("pagina ") || lower.matches(Regex("""^\d+\s*/\s*\d+$"""))) return true
        return false
    }

    private fun isPageFooter(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower.startsWith("relatório gerado") || lower.startsWith("folha ") || lower.contains("direitos reservados")
    }

    private fun isItemFormat(text: String): Boolean {
        return text.matches(Regex("""^(?:[0-9]{1,3}(?:\.[0-9]{1,3})*|[a-z]\))\s*.+""", RegexOption.IGNORE_CASE))
    }

    private fun isListItemPattern(text: String): Boolean {
        return text.matches(Regex("""^(?:[0-9]{1,3}(?:\.[0-9]{1,3})*|[a-z]\)|[•\-\*])\s*.+""", RegexOption.IGNORE_CASE))
    }

    private fun extractLeadingNumber(text: String): String? {
        val match = Regex("""^([0-9]{1,3}(?:\.[0-9]{1,3})*|[a-z]\))\s*""").find(text.trim())
        return match?.groupValues?.getOrNull(1)
    }
}
