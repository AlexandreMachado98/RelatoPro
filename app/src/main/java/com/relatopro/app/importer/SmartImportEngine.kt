package com.relatopro.app.importer

import android.content.Context
import android.net.Uri
import com.relatopro.app.importer.model.ImportFormat
import com.relatopro.app.importer.model.ParsedChecklist
import com.relatopro.app.importer.parser.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmartImportEngine {

    private val pdfParser = PdfChecklistParser()
    private val wordParser = WordChecklistParser()
    private val excelParser = ExcelChecklistParser()
    private val csvParser = CsvChecklistParser()
    private val imageParser = ImageChecklistParser()

    suspend fun parseDocument(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): ParsedChecklist = withContext(Dispatchers.IO) {
        val format = detectFormat(context, uri, fileName)
        onProgress("Iniciando importação de ${format.displayName}...", 5, "Arquivo: $fileName")

        when (format) {
            ImportFormat.PDF -> pdfParser.parse(context, uri, fileName, onProgress)
            ImportFormat.WORD -> wordParser.parse(context, uri, fileName, onProgress)
            ImportFormat.EXCEL -> excelParser.parse(context, uri, fileName, onProgress)
            ImportFormat.CSV -> csvParser.parse(context, uri, fileName, onProgress)
            ImportFormat.IMAGE -> imageParser.parse(context, uri, fileName, onProgress)
            ImportFormat.UNKNOWN -> {
                // Try PDF or Text as fallback
                try {
                    pdfParser.parse(context, uri, fileName, onProgress)
                } catch (e: Exception) {
                    csvParser.parse(context, uri, fileName, onProgress)
                }
            }
        }
    }

    fun detectFormat(context: Context, uri: Uri, fileName: String): ImportFormat {
        val lowerName = fileName.lowercase()
        val mimeType = context.contentResolver.getType(uri)?.lowercase() ?: ""

        return when {
            lowerName.endsWith(".pdf") || mimeType.contains("pdf") -> ImportFormat.PDF
            lowerName.endsWith(".docx") || lowerName.endsWith(".doc") || mimeType.contains("word") || mimeType.contains("officedocument.wordprocessingml") -> ImportFormat.WORD
            lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") || mimeType.contains("excel") || mimeType.contains("spreadsheetml") -> ImportFormat.EXCEL
            lowerName.endsWith(".csv") || lowerName.endsWith(".txt") || mimeType.contains("csv") || mimeType.contains("text/plain") -> ImportFormat.CSV
            lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || mimeType.startsWith("image/") -> ImportFormat.IMAGE
            else -> ImportFormat.UNKNOWN
        }
    }
}
