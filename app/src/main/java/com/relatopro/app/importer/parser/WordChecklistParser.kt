package com.relatopro.app.importer.parser

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.relatopro.app.importer.analyzer.ChecklistStructureAnalyzer
import com.relatopro.app.importer.model.ImportFormat
import com.relatopro.app.importer.model.ParsedChecklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
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
        val rawDoc = if (isDocx) {
            parseDocx(context, uri, onProgress)
        } else {
            parseDocBinary(context, uri, onProgress)
        }

        onProgress("Estruturando checklist...", 85, "Organizando seções, tabelas e itens...")
        val parsed = ChecklistStructureAnalyzer.analyze(rawDoc, ImportFormat.WORD, fileName)
        onProgress("Concluído!", 100, "Checklist pronto para revisão.")
        parsed
    }

    private fun parseDocx(
        context: Context,
        uri: Uri,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): RawDocumentContent {
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

        onProgress("Interpretando elementos Word...", 45, "Lendo parágrafos e tabelas...")

        val paragraphs = mutableListOf<RawParagraph>()
        val tables = mutableListOf<RawTable>()
        val rawLines = mutableListOf<String>()
        var suggestedTitle = ""

        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(documentXmlBytes), "UTF-8")

        var eventType = parser.eventType
        var inTable = false
        var currentTableRows = mutableListOf<RawRow>()
        var currentCells = mutableListOf<RawCell>()
        var currentText = StringBuilder()
        var isHeading = false
        var isListItem = false

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
                        }
                        "tc" -> {
                            currentText = StringBuilder()
                        }
                        "p" -> {
                            if (!inTable) {
                                currentText = StringBuilder()
                                isHeading = false
                                isListItem = false
                            }
                        }
                        "pStyle" -> {
                            val styleVal = parser.getAttributeValue(null, "val") ?: ""
                            if (styleVal.contains("Heading", ignoreCase = true) || styleVal.contains("Titulo", ignoreCase = true) || styleVal.contains("Title", ignoreCase = true)) {
                                isHeading = true
                            }
                        }
                        "numPr" -> {
                            isListItem = true
                        }
                        "t" -> {
                            // Text node
                            val text = parser.nextText()
                            currentText.append(text)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tagName) {
                        "tc" -> {
                            val cellText = currentText.toString().trim()
                            currentCells.add(RawCell(text = cellText, columnIndex = currentCells.size))
                            currentText = StringBuilder()
                        }
                        "tr" -> {
                            if (currentCells.isNotEmpty()) {
                                currentTableRows.add(RawRow(cells = currentCells.toList()))
                            }
                        }
                        "tbl" -> {
                            inTable = false
                            if (currentTableRows.isNotEmpty()) {
                                val firstRow = currentTableRows.firstOrNull()?.cells?.map { it.text } ?: emptyList()
                                val dataRows = if (currentTableRows.size > 1) currentTableRows.subList(1, currentTableRows.size) else currentTableRows
                                tables.add(
                                    RawTable(
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
                                    if (suggestedTitle.isBlank() && (isHeading || pText.length in 4..90)) {
                                        suggestedTitle = pText
                                    }
                                    paragraphs.add(
                                        RawParagraph(
                                            text = pText,
                                            isHeading = isHeading,
                                            isListItem = isListItem
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

        return RawDocumentContent(
            suggestedTitle = suggestedTitle,
            paragraphs = paragraphs,
            tables = tables,
            rawTextLines = rawLines,
            ocrUsed = false
        )
    }

    private fun parseDocBinary(
        context: Context,
        uri: Uri,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): RawDocumentContent {
        onProgress("Lendo arquivo .doc binário...", 40, "Extraindo textos...")
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Não foi possível ler o arquivo .doc.")

        // Extract readable UTF-8 and ASCII string runs from binary doc
        val textContent = extractReadableStrings(rawBytes)
        val lines = textContent.lines().map { it.trim() }.filter { it.length > 2 }

        val paragraphs = lines.map { line ->
            RawParagraph(
                text = line,
                isHeading = line.length < 80 && line.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()" }
            )
        }

        val suggestedTitle = lines.firstOrNull { it.length in 5..80 } ?: ""

        return RawDocumentContent(
            suggestedTitle = suggestedTitle,
            paragraphs = paragraphs,
            rawTextLines = lines,
            ocrUsed = false,
            warnings = listOf("Arquivo no formato legado .doc importado por fluxo de compatibilidade.")
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
}
