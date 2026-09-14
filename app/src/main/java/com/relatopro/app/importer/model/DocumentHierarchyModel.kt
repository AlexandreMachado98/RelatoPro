package com.relatopro.app.importer.model

import java.util.UUID

/**
 * Nível hierárquico identificado para um elemento do documento
 */
enum class HierarchyLevel(val displayName: String) {
    DOCUMENT_TITLE("Título do Documento"),
    DOCUMENT_SUBTITLE("Subtítulo / Descrição"),
    SECTION("Seção Principal (Nível 1)"),
    CATEGORY("Categoria (Nível 2)"),
    SUBCATEGORY("Subcategoria (Nível 3)"),
    ITEM("Item de Verificação / Pergunta"),
    TABLE_HEADER("Cabeçalho de Tabela"),
    AUXILIARY_FIELD("Campo Auxiliar / Observação"),
    METADATA_NOISE("Informação Administrativa / Ruído")
}

/**
 * Nível de confiança na classificação estrutural de um elemento
 */
enum class ConfidenceRating(val label: String, val minScore: Float) {
    HIGH("Alta Confiança", 0.90f),
    MEDIUM("Revisão Recomendada", 0.65f),
    LOW("Revisão Necessária", 0.0f)
}

/**
 * Estilo visual e geométrico extraído do documento original
 */
data class ElementStyle(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isAllUppercase: Boolean = false,
    val fontSizePt: Float? = null,
    val isHeadingStyle: Boolean = false,
    val headingLevel: Int = 0,
    val isCentered: Boolean = false,
    val backgroundColorHex: String? = null,
    val isMergedCell: Boolean = false,
    val boundsLeft: Float? = null,
    val boundsTop: Float? = null,
    val boundsRight: Float? = null,
    val boundsBottom: Float? = null
)

/**
 * Informações sobre a localização exata de origem no documento
 */
data class SourceLocation(
    val pageNumber: Int? = null,
    val sheetName: String? = null,
    val rowNumber: Int? = null,
    val columnNumber: Int? = null,
    val rawText: String = ""
)

/**
 * Resposta pré-preenchida identificada no documento original (ex: X na coluna Conforme)
 */
data class ExtractedAnswer(
    val statusShortCode: String, // C, NC, PARCIAL, NA
    val statusName: String,
    val rawMarking: String, // "X", "SIM", "✓", "1"
    val hasConflict: Boolean = false,
    val conflictingCodes: List<String> = emptyList()
)

/**
 * Alerta estrutural ou de validação emitido pelo analisador
 */
data class ImportValidationAlert(
    val id: String = UUID.randomUUID().toString(),
    val elementId: String? = null,
    val elementLabel: String? = null,
    val title: String,
    val description: String,
    val severity: AlertSeverity = AlertSeverity.WARNING,
    val suggestedAction: String? = null
)

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Célula bruta estruturada com estilos e mesclagem
 */
data class RichCell(
    val text: String,
    val columnIndex: Int = 0,
    val rowIndex: Int = 0,
    val isHeader: Boolean = false,
    val style: ElementStyle = ElementStyle(),
    val colSpan: Int = 1,
    val rowSpan: Int = 1
)

/**
 * Linha bruta com células ricas
 */
data class RichRow(
    val rowIndex: Int = 0,
    val cells: List<RichCell> = emptyList(),
    val isHeaderRow: Boolean = false,
    val isSectionBreakRow: Boolean = false
)

/**
 * Tabela estruturada com metadados
 */
data class RichTable(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val headers: List<String> = emptyList(),
    val rows: List<RichRow> = emptyList(),
    val location: SourceLocation = SourceLocation()
)

/**
 * Parágrafo estruturado com estilos e numeração
 */
data class RichParagraph(
    val text: String,
    val style: ElementStyle = ElementStyle(),
    val isListItem: Boolean = false,
    val listNumber: String? = null,
    val location: SourceLocation = SourceLocation()
)

/**
 * Conteúdo rico consolidado do documento antes da análise hierárquica
 */
data class RichDocumentContent(
    val suggestedTitle: String = "",
    val suggestedSubtitle: String = "",
    val paragraphs: List<RichParagraph> = emptyList(),
    val tables: List<RichTable> = emptyList(),
    val rawTextLines: List<String> = emptyList(),
    val alerts: List<ImportValidationAlert> = emptyList(),
    val ocrUsed: Boolean = false,
    val totalPages: Int = 1
)
