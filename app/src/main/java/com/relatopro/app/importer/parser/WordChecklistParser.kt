package com.relatopro.app.importer.parser

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.relatopro.app.importer.analyzer.ChecklistStructureAnalyzer
import com.relatopro.app.importer.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.zip.ZipInputStream

class WordChecklistParser : IChecklistParser {

    override suspend fun parse(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): ParsedChecklist = withContext(Dispatchers.IO) {
        onProgress("Lendo documento Word...", 15, "Abrindo arquivo $fileName...")

        val isDocx = fileName.endsWith(".docx", ignoreCase = true)
        val richDoc = if (isDocx) {
            parseDocxRich(context, uri, onProgress)
        } else {
            parseDocBinaryRich(context, uri, onProgress)
        }

        onProgress("Estruturando hierarquia...", 85, "Analisando seções, categorias e itens...")
        val parsed = ChecklistStructureAnalyzer.analyzeRich(richDoc, ImportFormat.WORD, fileName)
        onProgress("Concluído!", 100, "Checklist pronto para revisão.")
        parsed
    }

    private fun parseDocxRich(
        context: Context,
        uri: Uri,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): RichDocumentContent {
        var documentXmlBytes: ByteArray? = null
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        documentXmlBytes = zis.readBytes()
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } ?: throw IllegalStateException("Não foi possível abrir o arquivo .docx.")

        if (documentXmlBytes == null) {
            throw IllegalStateException("Arquivo .docx inválido ou sem conteúdo document.xml.")
        }

        onProgress("Interpretando estilos e tabelas...", 45, "Lendo parágrafos, estilos e tabelas...")

        val paragraphs = mutableListOf<RichParagraph>()
        val tables = mutableListOf<RichTable>()
        val rawLines = mutableListOf<String>()
        val alerts = mutableListOf<ImportValidationAlert>()
        var suggestedTitle = ""

        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(documentXmlBytes), "UTF-8")

        var eventType = parser.eventType
        var inTable = false
        var currentTableRows = mutableListOf<RichRow>()
        var currentCells = mutableListOf<RichCell>()
        var currentText = StringBuilder()
        
        // Formatting flags
        var isBold = false
        var isItalic = false
        var isHeading = false
        var headingLevel = 0
        var isListItem = false
        var fontSizePt: Float? = null
        var colSpan = 1
        var isTableHeaderRow = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = if (parser.name != null) parser.name.substringAfter(":") else ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName) {
                        "tbl" -> {
                            inTable = true
                            currentTableRows = mutableListOf()
                        }
                        "tr" -> {
                            currentCells = mutableListOf()
                            isTableHeaderRow = false
                        }
                        "tblHeader" -> {
                            isTableHeaderRow = true
                        }
                        "tc" -> {
                            currentText = StringBuilder()
                            colSpan = 1
                        }
                        "gridSpan" -> {
                            val spanVal = parser.getAttributeValue(null, "val")?.toIntOrNull()
                            if (spanVal != null && spanVal > 1) {
                                colSpan = spanVal
                            }
                        }
                        "p" -> {
                            if (!inTable) {
                                currentText = StringBuilder()
                                isHeading = false
                                headingLevel = 0
                                isListItem = false
                                isBold = false
                                isItalic = false
                                fontSizePt = null
                            }
                        }
                        "pStyle" -> {
                            val styleVal = parser.getAttributeValue(null, "val") ?: ""
                            when {
                                styleVal.contains("Heading1", ignoreCase = true) || styleVal.contains("Titulo1", ignoreCase = true) -> {
                                    isHeading = true
                                    headingLevel = 1
                                }
                                styleVal.contains("Heading2", ignoreCase = true) || styleVal.contains("Titulo2", ignoreCase = true) -> {
                                    isHeading = true
                                    headingLevel = 2
                                }
                                styleVal.contains("Heading3", ignoreCase = true) || styleVal.contains("Titulo3", ignoreCase = true) -> {
                                    isHeading = true
                                    headingLevel = 3
                                }
                                styleVal.contains("Heading", ignoreCase = true) || styleVal.contains("Titulo", ignoreCase = true) || styleVal.contains("Title", ignoreCase = true) -> {
                                    isHeading = true
                                    headingLevel = 1
                                }
                            }
                        }
                        "numPr" -> {
                            isListItem = true
                        }
                        "b" -> {
                            val valAttr = parser.getAttributeValue(null, "val")
                            isBold = (valAttr == null || valAttr == "1" || valAttr.equals("true", ignoreCase = true))
                        }
                        "i" -> {
                            val valAttr = parser.getAttributeValue(null, "val")
                            isItalic = (valAttr == null || valAttr == "1" || valAttr.equals("true", ignoreCase = true))
                        }
                        "sz" -> {
                            val szVal = parser.getAttributeValue(null, "val")?.toFloatOrNull()
                            if (szVal != null) {
                                fontSizePt = szVal / 2.0f // Word sz is in half-points
                            }
                        }
                        "t" -> {
                            val text = parser.nextText()
                            currentText.append(text)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tagName) {
                        "tc" -> {
                            val cellText = currentText.toString().trim()
                            val cellStyle = ElementStyle(
                                isBold = isBold,
                                isItalic = isItalic,
                                fontSizePt = fontSizePt,
                                isMergedCell = colSpan > 1
                            )
                            currentCells.add(
                                RichCell(
                                    text = cellText,
                                    columnIndex = currentCells.size,
                                    rowIndex = currentTableRows.size,
                                    style = cellStyle,
                                    colSpan = colSpan
                                )
                            )
                            currentText = StringBuilder()
                        }
                        "tr" -> {
                            if (currentCells.isNotEmpty()) {
                                currentTableRows.add(
                                    RichRow(
                                        rowIndex = currentTableRows.size,
                                        cells = currentCells.toList(),
                                        isHeaderRow = isTableHeaderRow
                                    )
                                )
                            }
                        }
                        "tbl" -> {
                            inTable = false
                            if (currentTableRows.isNotEmpty()) {
                                val firstRow = currentTableRows.firstOrNull()?.cells?.map { it.text } ?: emptyList()
                                val dataRows = if (currentTableRows.size > 1) currentTableRows.subList(1, currentTableRows.size) else currentTableRows
                                tables.add(
                                    RichTable(
                                        id = UUID.randomUUID().toString(),
                                        title = "",
                                        headers = firstRow,
                                        rows = dataRows
                                    )
                                )
                            }
                        }
                        "p" -> {
                            if (!inTable) {
                                val pText = currentText.toString().trim()
                                if (pText.isNotBlank()) {
                                    rawLines.add(pText)
                                    val isUpper = pText.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()" }
                                    if (suggestedTitle.isBlank() && (isHeading || pText.length in 5..90)) {
                                        suggestedTitle = pText
                                    }

                                    val pStyle = ElementStyle(
                                        isBold = isBold || isHeading,
                                        isItalic = isItalic,
                                        isAllUppercase = isUpper,
                                        fontSizePt = fontSizePt,
                                        isHeadingStyle = isHeading,
                                        headingLevel = headingLevel
                                    )

                                    paragraphs.add(
                                        RichParagraph(
                                            text = pText,
                                            style = pStyle,
                                            isListItem = isListItem,
                                            listNumber = extractLeadingNumber(pText),
                                            location = SourceLocation(rawText = pText)
                                        )
                                    )
                                }
                                currentText = StringBuilder()
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return RichDocumentContent(
            suggestedTitle = suggestedTitle,
            paragraphs = paragraphs,
            tables = tables,
            rawTextLines = rawLines,
            alerts = alerts,
            ocrUsed = false
        )
    }

    private fun parseDocBinaryRich(
        context: Context,
        uri: Uri,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): RichDocumentContent {
        onProgress("Lendo arquivo .doc binário...", 40, "Extraindo textos...")
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Não foi possível ler o arquivo .doc.")

        val textContent = extractReadableStrings(rawBytes)
        val lines = textContent.lines().map { it.trim() }.filter { it.length > 2 }

        val paragraphs = lines.map { line ->
            val isUpper = line.length in 4..90 && line.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()" }
            RichParagraph(
                text = line,
                style = ElementStyle(
                    isBold = isUpper,
                    isAllUppercase = isUpper,
                    isHeadingStyle = isUpper
                ),
                isListItem = line.matches(Regex("""^(?:[0-9]{1,3}(?:\.[0-9]{1,3})*|[a-z]\))\s*.+""")),
                listNumber = extractLeadingNumber(line),
                location = SourceLocation(rawText = line)
            )
        }

        val suggestedTitle = lines.firstOrNull { it.length in 5..80 } ?: ""

        return RichDocumentContent(
            suggestedTitle = suggestedTitle,
            paragraphs = paragraphs,
            rawTextLines = lines,
            alerts = listOf(
                ImportValidationAlert(
                    title = "Formato Legado .DOC",
                    description = "Arquivo .doc importado via extração de texto binário.",
                    severity = AlertSeverity.INFO
                )
            ),
            ocrUsed = false
        )
    }

    private fun extractReadableStrings(bytes: ByteArray): String {
        val sb = StringBuilder()
        var currentRun = StringBuilder()
        for (b in bytes) {
            val char = (b.toInt() and 0xFF).toChar()
            if (char.isLetterOrDigit() || char.isWhitespace() || char in ".,;:?!()[]{}/\\-_@#%&*+=\"'") {
                currentRun.append(char)
            } else {
                if (currentRun.length >= 4) {
                    val str = currentRun.toString().trim()
                    if (str.isNotBlank()) {
                        sb.append(str).append("\n")
                    }
                }
                currentRun = StringBuilder()
            }
        }
        if (currentRun.length >= 4) {
            sb.append(currentRun.toString().trim())
        }
        return sb.toString()
    }

    private fun extractLeadingNumber(text: String): String? {
        val match = Regex("""^([0-9]{1,3}(?:\.[0-9]{1,3})*|[a-z]\))\s*""").find(text.trim())
        return match?.groupValues?.getOrNull(1)
    }
}
