package com.relatopro.app.utils

import android.content.Context
import android.net.Uri
import com.relatopro.app.data.local.entity.ReportEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvImporter {

    data class CsvError(
        val line: Int,
        val field: String,
        val message: String
    )

    data class CsvParseResult(
        val isValid: Boolean,
        val totalRows: Int,
        val validReports: List<ReportEntity>,
        val errors: List<CsvError>,
        val warnings: List<String>
    )

    suspend fun parseCsvFromUri(context: Context, uri: Uri): CsvParseResult = withContext(Dispatchers.IO) {
        val validList = mutableListOf<ReportEntity>()
        val errors = mutableListOf<CsvError>()
        val warnings = mutableListOf<String>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext CsvParseResult(false, 0, emptyList(), listOf(CsvError(0, "Arquivo", "Não foi possível abrir o arquivo CSV selecionado.")), emptyList())

            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                val rawLines = reader.readLines().map { it.trim() }.filter { it.isNotEmpty() }
                if (rawLines.isEmpty()) {
                    return@withContext CsvParseResult(false, 0, emptyList(), listOf(CsvError(0, "Conteúdo", "O arquivo CSV está vazio.")), emptyList())
                }

                // Detect delimiter (; or ,)
                var headerLine = rawLines[0]
                // Strip UTF-8 BOM if present
                if (headerLine.startsWith("\uFEFF")) {
                    headerLine = headerLine.substring(1)
                }

                val delimiter = if (headerLine.contains(";")) ";" else ","
                val headerTokens = parseCsvRow(headerLine, delimiter).map { it.trim().lowercase(Locale.getDefault()) }

                // Determine column indices
                val idIdx = headerTokens.indexOfFirst { it == "id" || it == "codigo" }
                val numIdx = headerTokens.indexOfFirst { it.contains("numero") || it.contains("laudo") || it.contains("relatorio") || it == "nr" }
                val titleIdx = headerTokens.indexOfFirst { it.contains("titulo") || it.contains("nome") || it.contains("assunto") }
                val dateIdx = headerTokens.indexOfFirst { it.contains("data") || it.contains("date") || it.contains("emissao") }
                val respIdx = headerTokens.indexOfFirst { it.contains("responsavel") || it.contains("inspetor") || it.contains("tecnico") }
                val locIdx = headerTokens.indexOfFirst { it.contains("local") || it.contains("setor") || it.contains("obra") }
                val statusIdx = headerTokens.indexOfFirst { it.contains("status") || it.contains("situacao") }
                val compIdx = headerTokens.indexOfFirst { it.contains("empresa") || it.contains("cliente") }
                val unitIdx = headerTokens.indexOfFirst { it.contains("unidade") || it.contains("filial") }

                if (titleIdx == -1 && locIdx == -1) {
                    errors.add(CsvError(1, "Cabeçalho", "O cabeçalho não possui colunas obrigatórias como 'Titulo' ou 'Local'. Colunas encontradas: ${headerTokens.joinToString(", ")}"))
                    return@withContext CsvParseResult(false, rawLines.size - 1, emptyList(), errors, emptyList())
                }

                val sdfList = listOf(
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                )

                for (lineIndex in 1 until rawLines.size) {
                    val line = rawLines[lineIndex]
                    val tokens = parseCsvRow(line, delimiter)
                    if (tokens.isEmpty()) continue

                    try {
                        val title = if (titleIdx != -1 && titleIdx < tokens.size) tokens[titleIdx].trim() else "Relatório Importado #${lineIndex}"
                        val location = if (locIdx != -1 && locIdx < tokens.size) tokens[locIdx].trim() else "Local não informado"
                        val responsible = if (respIdx != -1 && respIdx < tokens.size) tokens[respIdx].trim() else "Responsável não informado"
                        val reportNumber = if (numIdx != -1 && numIdx < tokens.size) tokens[numIdx].trim() else "IMP-${System.currentTimeMillis().toString().takeLast(6)}"
                        val companyName = if (compIdx != -1 && compIdx < tokens.size) tokens[compIdx].trim() else "Empresa não informada"
                        val unit = if (unitIdx != -1 && unitIdx < tokens.size) tokens[unitIdx].trim() else "Matriz"

                        // Parse Date
                        var parsedDate = System.currentTimeMillis()
                        if (dateIdx != -1 && dateIdx < tokens.size) {
                            val dateText = tokens[dateIdx].trim()
                            if (dateText.isNotBlank()) {
                                for (sdf in sdfList) {
                                    try {
                                        val d = sdf.parse(dateText)
                                        if (d != null) {
                                            parsedDate = d.time
                                            break
                                        }
                                    } catch (e: Exception) {
                                        // Try next pattern
                                    }
                                }
                            }
                        }

                        // Parse Status
                        var status = "FINALIZED"
                        if (statusIdx != -1 && statusIdx < tokens.size) {
                            val rawStatus = tokens[statusIdx].trim().uppercase(Locale.getDefault())
                            status = when {
                                rawStatus.contains("DRAFT") || rawStatus.contains("RASCUNHO") -> "DRAFT"
                                rawStatus.contains("SENT") || rawStatus.contains("ENVIADO") -> "SENT"
                                else -> "FINALIZED"
                            }
                        }

                        if (title.isBlank() && location.isBlank()) {
                            errors.add(CsvError(lineIndex + 1, "Dados", "Linha com título e local vazios."))
                            continue
                        }

                        val reportEntity = ReportEntity(
                            id = 0L, // Auto-generated ID in database
                            templateId = 1L,
                            companyId = null,
                            companyName = companyName,
                            unit = unit,
                            title = title.ifBlank { "Vistoria Técnica #${lineIndex}" },
                            reportNumber = reportNumber.ifBlank { "LAUDO-${lineIndex}" },
                            date = parsedDate,
                            responsible = responsible.ifBlank { "Inspetor Técnico" },
                            location = location.ifBlank { "Frente de Trabalho" },
                            lat = null,
                            lng = null,
                            status = status,
                            generalObservations = "Relatório importado via arquivo CSV em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}",
                            pdfLocalPath = null,
                            syncStatus = "SYNCED"
                        )
                        validList.add(reportEntity)
                    } catch (e: Exception) {
                        errors.add(CsvError(lineIndex + 1, "Linha", "Erro ao processar dados da linha: ${e.localizedMessage}"))
                    }
                }
            }
        } catch (e: Exception) {
            errors.add(CsvError(0, "Arquivo", "Falha de leitura do CSV: ${e.localizedMessage}"))
        }

        return@withContext CsvParseResult(
            isValid = errors.isEmpty() && validList.isNotEmpty(),
            totalRows = validList.size,
            validReports = validList,
            errors = errors,
            warnings = warnings
        )
    }

    private fun parseCsvRow(row: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false

        var i = 0
        while (i < row.length) {
            val c = row[i]
            if (c == '"') {
                if (insideQuotes && i + 1 < row.length && row[i + 1] == '"') {
                    sb.append('"')
                    i++ // skip escaped quote
                } else {
                    insideQuotes = !insideQuotes
                }
            } else if (c.toString() == delimiter && !insideQuotes) {
                result.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }
}
