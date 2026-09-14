package com.relatopro.app.importer.parser

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.relatopro.app.importer.analyzer.ChecklistStructureAnalyzer
import com.relatopro.app.importer.model.*
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

        val allParagraphs = mutableListOf<RichParagraph>()
        val allRawLines = mutableListOf<String>()
        var suggestedTitle = ""

        val blocks: List<Text.TextBlock> = visionText.textBlocks.sortedBy { it.boundingBox?.top ?: 0 }

        if (blocks.isNotEmpty()) {
            val topBlock = blocks.first()
            if (topBlock.text.isNotBlank() && topBlock.text.length < 100) {
                suggestedTitle = topBlock.text.lines().firstOrNull()?.trim() ?: ""
            }
        }

        // Group into spatial lines sorted vertically then horizontally
        val lines = blocks.flatMap { it.lines }
            .map { line ->
                val box = line.boundingBox ?: Rect(0, 0, 0, 0)
                val text = line.text.trim()
                val isUpper = text.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()" }
                RichParagraph(
                    text = text,
                    style = ElementStyle(
                        isBold = isUpper || box.height() > 28,
                        isAllUppercase = isUpper,
                        fontSizePt = box.height().toFloat(),
                        boundsLeft = box.left.toFloat(),
                        boundsTop = box.top.toFloat(),
                        boundsRight = box.right.toFloat(),
                        boundsBottom = box.bottom.toFloat()
                    ),
                    isListItem = text.matches(Regex("""^(?:[0-9]{1,3}(?:\.[0-9]{1,3})*|[a-z]\)|[•\-\*])\s*.+""", RegexOption.IGNORE_CASE)),
                    listNumber = Regex("""^([0-9]{1,3}(?:\.[0-9]{1,3})*|[a-z]\))\s*""").find(text)?.groupValues?.getOrNull(1),
                    location = SourceLocation(pageNumber = 1, rawText = text)
                )
            }
            .filter { it.text.isNotBlank() }
            .sortedWith(
                compareBy<RichParagraph> { (it.style.boundsTop ?: 0f) / 15f }
                    .thenBy { it.style.boundsLeft ?: 0f }
            )

        lines.forEach {
            allRawLines.add(it.text)
            allParagraphs.add(it)
        }

        onProgress("Estruturando checklist...", 85, "Montando formulário e seções...")

        val richDoc = RichDocumentContent(
            suggestedTitle = suggestedTitle,
            paragraphs = allParagraphs,
            rawTextLines = allRawLines,
            alerts = listOf(
                ImportValidationAlert(
                    title = "OCR em Imagem",
                    description = "Texto e hierarquia reconstruídos a partir de análise espacial da imagem.",
                    severity = AlertSeverity.INFO
                )
            ),
            ocrUsed = true
        )

        val parsed = ChecklistStructureAnalyzer.analyzeRich(richDoc, ImportFormat.IMAGE, fileName)
        onProgress("Concluído!", 100, "Imagem digitalizada com sucesso.")
        parsed
    }
}
