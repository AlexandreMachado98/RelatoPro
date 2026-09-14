package com.relatopro.app.importer.parser

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.relatopro.app.importer.analyzer.ChecklistStructureAnalyzer
import com.relatopro.app.importer.model.ImportFormat
import com.relatopro.app.importer.model.ParsedChecklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageChecklistParser : IChecklistParser {

    override suspend fun parse(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): ParsedChecklist = withContext(Dispatchers.IO) {
        onProgress("Carregando imagem...", 20, "Decodificando foto do checklist...")

        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalStateException("Não foi possível carregar a imagem do checklist.")

        onProgress("Executando OCR na imagem...", 50, "Identificando textos e itens manuscritos/impressos...")

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        val visionText: Text = recognizer.process(image).awaitTask()
        recognizer.close()
        bitmap.recycle()

        val allParagraphs = mutableListOf<RawParagraph>()
        val allRawLines = mutableListOf<String>()
        var suggestedTitle = ""

        val blocks: List<Text.TextBlock> = visionText.textBlocks.sortedBy { it.boundingBox?.top ?: 0 }

        if (blocks.isNotEmpty()) {
            val topBlock = blocks.first()
            if (topBlock.text.isNotBlank() && topBlock.text.length < 100) {
                suggestedTitle = topBlock.text.lines().firstOrNull()?.trim() ?: ""
            }
        }

        for (block in blocks) {
            for (line in block.lines) {
                val text = line.text.trim()
                if (text.isNotBlank()) {
                    allRawLines.add(text)
                    val isHeading = text.length < 80 && text.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()" }
                    allParagraphs.add(
                        RawParagraph(
                            text = text,
                            isHeading = isHeading
                        )
                    )
                }
            }
        }

        onProgress("Estruturando checklist...", 85, "Montando formulário e seções...")

        val rawDoc = RawDocumentContent(
            suggestedTitle = suggestedTitle,
            paragraphs = allParagraphs,
            rawTextLines = allRawLines,
            ocrUsed = true,
            warnings = listOf("Checklist importado a partir de imagem via OCR on-device.")
        )

        val parsed = ChecklistStructureAnalyzer.analyze(rawDoc, ImportFormat.IMAGE, fileName)
        onProgress("Concluído!", 100, "Imagem digitalizada com sucesso.")
        parsed
    }
}
