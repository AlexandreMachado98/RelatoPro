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

class ExcelChecklistParser : IChecklistParser {

    override suspend fun parse(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): ParsedChecklist = withContext(Dispatchers.IO) {
        onProgress("Lendo planilha Excel...", 15, "Abrindo arquivo $fileName...")

        val isXlsx = fileName.endsWith(".xlsx", ignoreCase = true)
        val richDoc = if (isXlsx) {
            parseXlsxRich(context, uri, onProgress)
        } else {
            parseXlsBinaryRich(context, uri, onProgress)
        }

        onProgress("Estruturando hierarquia...", 85, "Organizando colunas, seções e itens...")
        val parsed = ChecklistStructureAnalyzer.analyzeRich(richDoc, ImportFormat.EXCEL, fileName)
        onProgress("Concluído!", 100, "Planilha importada com sucesso.")
        parsed
    }

    private fun parseXlsxRich(
        context: Context,
        uri: Uri,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): RichDocumentContent {
        val sharedStringsBytes: ByteArray?
        val sheetEntries = mutableMapOf<String, ByteArray>()
        var stylesXmlBytes: ByteArray? = null

        var sstBytes: ByteArray? = null
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "xl/sharedStrings.xml") {
                        sstBytes = zis.readBytes()
                    } else if (name == "xl/styles.xml") {
                        stylesXmlBytes = zis.readBytes()
                    } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                        sheetEntries[name] = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }
        } ?: throw IllegalStateException("Não foi possível abrir o arquivo Excel.")

        sharedStringsBytes = sstBytes
        val sharedStrings = parseSharedStrings(sharedStringsBytes)
        val boldStyleIndices = parseBoldStyleIndices(stylesXmlBytes)

        onProgress("Processando abas e linhas...", 50, "Extraindo dados estruturados...")

        val tables = mutableListOf<RichTable>()
        val rawLines = mutableListOf<String>()

        sheetEntries.forEach { (sheetName, sheetXml) ->
            val parsedTable = parseSheetRich(sheetName, sheetXml, sharedStrings, boldStyleIndices)
            if (parsedTable.rows.isNotEmpty()) {
                tables.add(parsedTable)
                parsedTable.headers.forEach { if (it.isNotBlank()) rawLines.add(it) }
                parsedTable.rows.forEach { row ->
                    row.cells.forEach { cell ->
                        if (cell.text.isNotBlank()) rawLines.add(cell.text)
                    }
                }
            }
        }

        val suggestedTitle = tables.firstOrNull()?.title?.ifBlank { "" } ?: ""

        return RichDocumentContent(
            suggestedTitle = suggestedTitle,
            tables = tables,
            rawTextLines = rawLines,
            ocrUsed = false
        )
    }

    private fun parseBoldStyleIndices(bytes: ByteArray?): Set<Int> {
        if (bytes == null) return emptySet()
        val boldIndices = mutableSetOf<Int>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
            var eventType = parser.eventType
            var inCellXfs = false
            var currentXfIndex = 0

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = if (parser.name != null) parser.name.substringAfter(":") else ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tag == "cellXfs") {
                            inCellXfs = true
                            currentXfIndex = 0
                        } else if (tag == "xf" && inCellXfs) {
                            val applyFont = parser.getAttributeValue(null, "applyFont")
                            val fontId = parser.getAttributeValue(null, "fontId")?.toIntOrNull()
                            if (fontId != null && fontId > 0) {
                                boldIndices.add(currentXfIndex)
                            }
                            currentXfIndex++
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag == "cellXfs") {
                            inCellXfs = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return boldIndices
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val list = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
            var eventType = parser.eventType
            var currentText = StringBuilder()
            var inTextNode = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = if (parser.name != null) parser.name.substringAfter(":") else ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tag == "t") {
                            inTextNode = true
                            currentText = StringBuilder()
                        } else if (tag == "si") {
                            currentText = StringBuilder()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inTextNode) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag == "t") {
                            inTextNode = false
                        } else if (tag == "si") {
                            list.add(currentText.toString())
                            currentText = StringBuilder()
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseSheetRich(
        sheetPath: String,
        xmlBytes: ByteArray,
        sharedStrings: List<String>,
        boldStyleIndices: Set<Int>
    ): RichTable {
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var eventType = parser.eventType
        val allRows = mutableListOf<RichRow>()
        var currentCells = mutableListOf<RichCell>()
        var currentCellValue = StringBuilder()
        var isStringCell = false
        var cellColRef = 0
        var styleIndex = 0
        val mergedCellRanges = mutableListOf<String>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = if (parser.name != null) parser.name.substringAfter(":") else ""
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tag) {
                        "row" -> {
                            currentCells = mutableListOf()
                        }
                        "c" -> {
                            val type = parser.getAttributeValue(null, "t") ?: ""
                            isStringCell = (type == "s")
                            styleIndex = parser.getAttributeValue(null, "s")?.toIntOrNull() ?: 0
                            currentCellValue = StringBuilder()
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            cellColRef = columnRefToIndex(ref)
                        }
                        "mergeCell" -> {
                            val ref = parser.getAttributeValue(null, "ref")
                            if (ref != null) {
                                mergedCellRanges.add(ref)
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    currentCellValue.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (tag) {
                        "c" -> {
                            val rawVal = currentCellValue.toString().trim()
                            val finalVal = if (isStringCell) {
                                val sstIndex = rawVal.toIntOrNull()
                                if (sstIndex != null && sstIndex in sharedStrings.indices) {
                                    sharedStrings[sstIndex]
                                } else rawVal
                            } else {
                                rawVal
                            }

                            if (finalVal.isNotBlank()) {
                                val isBold = styleIndex in boldStyleIndices
                                val cellStyle = ElementStyle(
                                    isBold = isBold,
                                    isAllUppercase = finalVal.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()" }
                                )
                                currentCells.add(
                                    RichCell(
                                        text = finalVal,
                                        columnIndex = cellColRef,
                                        rowIndex = allRows.size,
                                        style = cellStyle
                                    )
                                )
                            }
                        }
                        "row" -> {
                            if (currentCells.isNotEmpty()) {
                                val nonBlank = currentCells.filter { it.text.isNotBlank() }
                                val isSectionBreak = nonBlank.size == 1 && nonBlank[0].text.length in 3..90
                                allRows.add(
                                    RichRow(
                                        rowIndex = allRows.size,
                                        cells = currentCells.toList(),
                                        isSectionBreakRow = isSectionBreak
                                    )
                                )
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val headers = allRows.firstOrNull()?.cells?.map { it.text } ?: emptyList()
        val dataRows = if (allRows.size > 1) allRows.subList(1, allRows.size) else allRows

        val sheetTitle = sheetPath.substringAfterLast("/").substringBefore(".xml")

        return RichTable(
            id = UUID.randomUUID().toString(),
            title = sheetTitle,
            headers = headers,
            rows = dataRows,
            location = SourceLocation(sheetName = sheetTitle)
        )
    }

    private fun columnRefToIndex(cellRef: String): Int {
        val colLetters = cellRef.filter { it.isLetter() }.uppercase()
        var index = 0
        for (char in colLetters) {
            index = index * 26 + (char - 'A' + 1)
        }
        return if (index > 0) index - 1 else 0
    }

    private fun parseXlsBinaryRich(
        context: Context,
        uri: Uri,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): RichDocumentContent {
        onProgress("Lendo arquivo .xls legado...", 40, "Extraindo tabelas...")
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Não foi possível ler o arquivo .xls.")

        val sb = StringBuilder()
        var currentRun = StringBuilder()
        for (b in bytes) {
            val char = (b.toInt() and 0xFF).toChar()
            if (char.isLetterOrDigit() || char.isWhitespace() || char in ".,;:?!()[]{}/\\-_@#%&*+=\"'") {
                currentRun.append(char)
            } else {
                if (currentRun.length >= 3) {
                    sb.append(currentRun.toString().trim()).append("\n")
                }
                currentRun = StringBuilder()
            }
        }

        val lines = sb.toString().lines().map { it.trim() }.filter { it.length > 2 }
        val rows = lines.mapIndexed { idx, line ->
            RichRow(
                rowIndex = idx,
                cells = listOf(RichCell(text = line, columnIndex = 0, rowIndex = idx))
            )
        }

        return RichDocumentContent(
            tables = listOf(RichTable(title = "Planilha", headers = listOf("Item"), rows = rows)),
            rawTextLines = lines,
            alerts = listOf(
                ImportValidationAlert(
                    title = "Formato .XLS Legado",
                    description = "Planilha importada através de extração de cadeias de caracteres.",
                    severity = AlertSeverity.INFO
                )
            ),
            ocrUsed = false
        )
    }
}
