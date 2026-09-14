package com.relatopro.app.importer.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.relatopro.app.importer.analyzer.ChecklistStructureAnalyzer
import com.relatopro.app.importer.model.ImportFormat
import com.relatopro.app.importer.model.ParsedChecklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfChecklistParser : IChecklistParser {

    override suspend fun parse(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): ParsedChecklist = withContext(Dispatchers.IO) {
        onProgress("Lendo arquivo PDF...", 10, "Carregando páginas...")

        // Copy uri to temp file for PdfRenderer
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

            onProgress("Processando páginas do PDF...", 25, "Encontradas $pageCount páginas.")

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val allParagraphs = mutableListOf<RawParagraph>()
            val allRawLines = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            var extractedTitle = ""

            for (pageIndex in 0 until pageCount) {
                val pagePercent = 25 + ((pageIndex + 1) * 55 / pageCount)
                onProgress("Executando OCR e Extração...", pagePercent, "Página ${pageIndex + 1} de $pageCount")

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

                    if (pageIndex == 0 && blocks.isNotEmpty()) {
                        // The top-most block on the first page is often the title
                        val topBlock = blocks.minByOrNull { it.boundingBox?.top ?: 0 }
                        if (topBlock != null && topBlock.text.isNotBlank() && topBlock.text.length < 120) {
                            extractedTitle = topBlock.text.lines().firstOrNull()?.trim() ?: ""
                        }
                    }

                    for (block in blocks) {
                        for (line in block.lines) {
                            val lineText = line.text.trim()
                            if (lineText.isNotBlank()) {
                                allRawLines.add(lineText)
                                val isHeading = lineText.length < 80 && lineText.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()" }
                                allParagraphs.add(
                                    RawParagraph(
                                        text = lineText,
                                        isHeading = isHeading
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    warnings.add("Aviso na página ${pageIndex + 1}: ${e.localizedMessage}")
                } finally {
                    bitmap.recycle()
                }
            }

            pdfRenderer.close()
            fileDescriptor.close()
            recognizer.close()

            onProgress("Estruturando checklist...", 85, "Organizando seções e itens...")

            val rawDoc = RawDocumentContent(
                suggestedTitle = extractedTitle,
                paragraphs = allParagraphs,
                rawTextLines = allRawLines,
                warnings = warnings,
                ocrUsed = true
            )

            val parsed = ChecklistStructureAnalyzer.analyze(rawDoc, ImportFormat.PDF, fileName)

            onProgress("Finalizando formulário...", 100, "Concluído com sucesso!")
            parsed
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}
