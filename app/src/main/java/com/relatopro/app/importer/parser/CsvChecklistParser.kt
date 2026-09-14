package com.relatopro.app.importer.parser

import android.content.Context
import android.net.Uri
import com.relatopro.app.importer.analyzer.ChecklistStructureAnalyzer
import com.relatopro.app.importer.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.UUID

class CsvChecklistParser : IChecklistParser {

    override suspend fun parse(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): ParsedChecklist = withContext(Dispatchers.IO) {
        onProgress("Lendo arquivo CSV...", 20, "Detectando codificação e delimitadores...")

        val rawLines = mutableListOf<String>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    rawLines.add(line)
                    line = reader.readLine()
                }
            }
        } ?: throw IllegalStateException("Não foi possível ler o arquivo CSV.")

        if (rawLines.isEmpty()) {
            throw IllegalStateException("O arquivo CSV está vazio.")
        }

        onProgress("Analisando colunas CSV...", 50, "Processando linhas e células...")

        val delimiter = detectDelimiter(rawLines.take(10))
        val parsedRows = parseCsvLines(rawLines, delimiter)

        val headers = parsedRows.firstOrNull()?.map { it.trim() } ?: emptyList()
        val dataRows = if (parsedRows.size > 1) parsedRows.subList(1, parsedRows.size) else parsedRows

        val richRows = dataRows.mapIndexed { rowIdx, rowValues ->
            val nonBlank = rowValues.filter { it.isNotBlank() }
            val isSectionBreak = nonBlank.size == 1 && nonBlank[0].length in 3..90
            val richCells = rowValues.mapIndexed { colIdx, v ->
                val trimmed = v.trim()
                val isUpper = trimmed.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()" }
                RichCell(
                    text = trimmed,
                    columnIndex = colIdx,
                    rowIndex = rowIdx,
                    style = ElementStyle(isBold = isUpper || isSectionBreak, isAllUppercase = isUpper)
                )
            }
            RichRow(
                rowIndex = rowIdx,
                cells = richCells,
                isSectionBreakRow = isSectionBreak
            )
        }

        val table = RichTable(
            id = UUID.randomUUID().toString(),
            title = "Checklist CSV",
            headers = headers,
            rows = richRows,
            location = SourceLocation(sheetName = "CSV")
        )

        onProgress("Estruturando hierarquia...", 85, "Organizando seções e itens...")

        val richDoc = RichDocumentContent(
            suggestedTitle = fileName.substringBeforeLast("."),
            tables = listOf(table),
            rawTextLines = rawLines,
            ocrUsed = false
        )

        val parsed = ChecklistStructureAnalyzer.analyzeRich(richDoc, ImportFormat.CSV, fileName)
        onProgress("Concluído!", 100, "CSV estruturado com sucesso.")
        parsed
    }

    private fun detectDelimiter(sampleLines: List<String>): Char {
        val candidates = listOf(';', ',', '\t', '|')
        val counts = candidates.associateWith { delim ->
            sampleLines.sumOf { line -> line.count { it == delim } }
        }
        return counts.maxByOrNull { it.value }?.key ?: ';'
    }

    private fun parseCsvLines(lines: List<String>, delimiter: Char): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var inQuotes = false
        var currentToken = StringBuilder()
        var currentRow = mutableListOf<String>()

        val fullText = lines.joinToString("\n")
        var i = 0
        while (i < fullText.length) {
            val c = fullText[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < fullText.length && fullText[i + 1] == '"') {
                        currentToken.append('"')
                        i++ // skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    currentRow.add(currentToken.toString())
                    currentToken = StringBuilder()
                }
                c == '\n' && !inQuotes -> {
                    currentRow.add(currentToken.toString())
                    if (currentRow.any { it.isNotBlank() }) {
                        result.add(currentRow)
                    }
                    currentRow = mutableListOf()
                    currentToken = StringBuilder()
                }
                c == '\r' && !inQuotes -> {
                    // Ignore carriage return
                }
                else -> {
                    currentToken.append(c)
                }
            }
            i++
        }

        if (currentToken.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentToken.toString())
            if (currentRow.any { it.isNotBlank() }) {
                result.add(currentRow)
            }
        }

        return result
    }
}
