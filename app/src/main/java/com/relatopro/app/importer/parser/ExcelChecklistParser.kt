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
        val rawDoc = if (isXlsx) {
            parseXlsx(context, uri, onProgress)
        } else {
            parseXlsBinary(context, uri, onProgress)
        }

        onProgress("Estruturando checklist...", 85, "Organizando colunas, abas e itens...")
        val parsed = ChecklistStructureAnalyzer.analyze(rawDoc, ImportFormat.EXCEL, fileName)
        onProgress("Concluído!", 100, "Planilha importada com sucesso.")
        parsed
    }

    private fun parseXlsx(
        context: Context,
        uri: Uri,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): RawDocumentContent {
        val sharedStringsBytes: ByteArray?
        val sheetEntries = mutableMapOf<String, ByteArray>()

        var sstBytes: ByteArray? = null
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "xl/sharedStrings.xml") {
                        sstBytes = zis.readBytes()
                    } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                        sheetEntries[name] = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }
        } ?: throw IllegalStateException("Não foi possível abrir o arquivo Excel.")

        sharedStringsBytes = sstBytes
        val sharedStrings = parseSharedStrings(sharedStringsBytes)

        onProgress("Processando abas e linhas...", 50, "Extraindo dados estruturados...")

        val tables = mutableListOf<RawTable>()
        val rawLines = mutableListOf<String>()

        sheetEntries.forEach { (sheetName, sheetXml) ->
            val parsedTable = parseSheet(sheetName, sheetXml, sharedStrings)
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

        return RawDocumentContent(
            suggestedTitle = tables.firstOrNull()?.title?.ifBlank { "" } ?: "",
            tables = tables,
            rawTextLines = rawLines,
            ocrUsed = false
        )
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

    private fun parseSheet(
        sheetPath: String,
        xmlBytes: ByteArray,
        sharedStrings: List<String>
    ): RawTable {
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var eventType = parser.eventType
        val allRows = mutableListOf<RawRow>()
        var currentCells = mutableListOf<RawCell>()
        var currentCellValue = StringBuilder()
        var isStringCell = false
        var isInlineString = false
        var cellColRef = 0

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
                            isInlineString = (type == "inlineStr")
                            currentCellValue = StringBuilder()
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            cellColRef = columnRefToIndex(ref)
                        }
                        "v", "t" -> {
                            // Text inside value or inline string
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
                                currentCells.add(RawCell(text = finalVal, columnIndex = cellColRef))
                            }
                        }
                        "row" -> {
                            if (currentCells.isNotEmpty()) {
                                allRows.add(RawRow(cells = currentCells.toList()))
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

        return RawTable(
            title = sheetTitle,
            headers = headers,
            rows = dataRows
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

    private fun parseXlsBinary(
        context: Context,
        uri: Uri,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): RawDocumentContent {
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
        val rows = lines.map { RawRow(listOf(RawCell(text = it, columnIndex = 0))) }

        return RawDocumentContent(
            tables = listOf(RawTable(title = "Planilha", headers = listOf("Item"), rows = rows)),
            rawTextLines = lines,
            ocrUsed = false,
            warnings = listOf("Arquivo no formato .xls importado por compatibilidade de texto estruturado.")
        )
    }
}
