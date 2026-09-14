package com.relatopro.app.importer.parser

import android.content.Context
import android.net.Uri
import com.relatopro.app.importer.model.ParsedChecklist

data class RawCell(
    val text: String,
    val columnIndex: Int = 0,
    val isHeader: Boolean = false
)

data class RawRow(
    val cells: List<RawCell>
)

data class RawTable(
    val title: String = "",
    val headers: List<String> = emptyList(),
    val rows: List<RawRow> = emptyList()
)

data class RawParagraph(
    val text: String,
    val isHeading: Boolean = false,
    val headingLevel: Int = 0,
    val isListItem: Boolean = false,
    val listNumber: String? = null
)

data class RawDocumentContent(
    val suggestedTitle: String = "",
    val suggestedSubtitle: String = "",
    val paragraphs: List<RawParagraph> = emptyList(),
    val tables: List<RawTable> = emptyList(),
    val rawTextLines: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val ocrUsed: Boolean = false
)

interface IChecklistParser {
    suspend fun parse(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): ParsedChecklist
}
