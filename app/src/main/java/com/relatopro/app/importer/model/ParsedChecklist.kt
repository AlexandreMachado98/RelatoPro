package com.relatopro.app.importer.model

import java.util.UUID

enum class ImportFormat(val displayName: String, val extensionLabel: String) {
    PDF("Documento PDF", "PDF / OCR"),
    WORD("Documento Word", "DOC / DOCX"),
    EXCEL("Planilha Excel", "XLS / XLSX"),
    CSV("Arquivo CSV", "CSV"),
    IMAGE("Imagem / Digitalização", "JPG / PNG / WEBP"),
    UNKNOWN("Desconhecido", "Outro")
}

data class CustomStatusConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val shortCode: String,
    val description: String = "",
    val colorHex: String = "#10B981",
    val scoreMultiplier: Float? = 1.0f, // 1.0 = C, 0.5 = Parcial, 0.0 = NC, null = NA
    val isSystemDefault: Boolean = false
) {
    companion object {
        val DEFAULT_STATUSES = listOf(
            CustomStatusConfig(
                name = "Conforme",
                shortCode = "C",
                description = "Item em conformidade com os requisitos de segurança e operação.",
                colorHex = "#10B981",
                scoreMultiplier = 1.0f,
                isSystemDefault = true
            ),
            CustomStatusConfig(
                name = "Parcial",
                shortCode = "PARCIAL",
                description = "Atende parcialmente com pequenas pendências ou ressalvas.",
                colorHex = "#F59E0B",
                scoreMultiplier = 0.5f,
                isSystemDefault = true
            ),
            CustomStatusConfig(
                name = "Não Conforme",
                shortCode = "NC",
                description = "Item em não conformidade crítica ou irregularidade detectada.",
                colorHex = "#EF4444",
                scoreMultiplier = 0.0f,
                isSystemDefault = true
            ),
            CustomStatusConfig(
                name = "Não Aplicável",
                shortCode = "NA",
                description = "Item não se aplica a este equipamento ou instalação.",
                colorHex = "#6B7280",
                scoreMultiplier = null,
                isSystemDefault = true
            )
        )
    }
}

data class ParsedItem(
    val id: String = UUID.randomUUID().toString(),
    val sectionName: String = "Geral",
    val numberPrefix: String? = null,
    val label: String,
    val type: String = "C_NC_NA", // C_NC_NA, TEXT, NUMBER, CHECKBOX, PHOTO, SIGNATURE
    val allowedStatuses: List<String> = listOf("C", "PARCIAL", "NC", "NA"),
    val allowObservation: Boolean = true,
    val allowPhoto: Boolean = true,
    val allowAttachment: Boolean = false,
    val isRequired: Boolean = true,
    val requireObservationOnNC: Boolean = true,
    val requirePhotoOnNC: Boolean = false,
    val maxPhotos: Int = 5,
    val warning: String? = null // Aviso ou alerta se houve ambiguidade na leitura
)

data class ParsedSection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val orderIndex: Int = 0,
    val items: List<ParsedItem> = emptyList()
)

data class ParsedChecklist(
    val title: String,
    val subtitle: String = "",
    val category: String = "Segurança do Trabalho",
    val description: String = "",
    val sections: List<ParsedSection> = emptyList(),
    val availableStatuses: List<CustomStatusConfig> = CustomStatusConfig.DEFAULT_STATUSES,
    val sourceFormat: ImportFormat = ImportFormat.UNKNOWN,
    val fileName: String = "",
    val warnings: List<String> = emptyList(),
    val totalConfidenceScore: Float = 1.0f
) {
    val totalItemsCount: Int
        get() = sections.sumOf { it.items.size }
}

sealed class ImportProgressState {
    object Idle : ImportProgressState()
    data class Processing(
        val stage: String,
        val progressPercent: Int,
        val details: String = ""
    ) : ImportProgressState()
    data class Success(val checklist: ParsedChecklist) : ImportProgressState()
    data class Error(val message: String) : ImportProgressState()
}
