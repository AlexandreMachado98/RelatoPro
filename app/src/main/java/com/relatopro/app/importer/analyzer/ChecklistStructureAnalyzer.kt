package com.relatopro.app.importer.analyzer

import com.relatopro.app.importer.model.*
import com.relatopro.app.importer.parser.RawDocumentContent
import com.relatopro.app.importer.parser.RawTable
import java.util.UUID

object ChecklistStructureAnalyzer {

    // Regex for Section / Group Headers (e.g. "1. DOCUMENTAÇÃO", "SEÇÃO 2: EQUIPAMENTOS", "MÓDULO I - VEÍCULO")
    private val SECTION_HEADER_REGEX = Regex(
        """^(?:(?:SEÇÃO|SECAO|GRUPO|MODULO|MÓDULO|BLOCO|PARTE|ETAPA|ITEM)\s*(?:[0-9IVXLCDM]+|[A-Z])[:\-–.]?\s+)?([0-9IVXLCDM]+[.\-–\s]+[A-ZÀ-Ú\s\-_/()]{3,}|[A-ZÀ-Ú\s\-_/()]{4,})$""",
        RegexOption.IGNORE_CASE
    )

    // Regex for Item numbering (e.g. "1.", "1.1", "01.", "a)", "Item 02:")
    private val ITEM_NUMBER_REGEX = Regex(
        """^((?:ITEM\s*)?(?:[0-9]{1,3}(?:\.[0-9]{1,3})*|[a-z]\))\s*[:.\-–]?)\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )

    // Regex for typical compliance / status column keywords
    private val CONFORME_KEYWORDS = setOf("C", "CONFORME", "SIM", "OK", "BOM", "ADEQUADO", "ATENDE", "COMPLIANT", "TRUE", "S")
    private val NAO_CONFORME_KEYWORDS = setOf("NC", "NÃO CONFORME", "NAO CONFORME", "NÃO", "NAO", "NOK", "RUIM", "IRREGULAR", "NÃO ATENDE", "NON-COMPLIANT", "FALSE", "N")
    private val PARCIAL_KEYWORDS = setOf("P", "PARCIAL", "PARCIALMENTE CONFORME", "PARCIALMENTE", "RESSALVA", "REGULAR", "PARTIAL")
    private val NA_KEYWORDS = setOf("NA", "N/A", "NÃO APLICÁVEL", "NAO APLICAVEL", "NÃO SE APLICA", "EXCLUÍDO", "NOT APPLICABLE")
    private val OBS_KEYWORDS = setOf("OBS", "OBSERVAÇÃO", "OBSERVACOES", "OBSERVAÇÕES", "COMENTÁRIO", "RESSALVAS", "AÇÃO", "NOTAS", "EVIDÊNCIA")

    fun analyze(
        raw: RawDocumentContent,
        format: ImportFormat,
        fileName: String
    ): ParsedChecklist {
        val warnings = mutableListOf<String>()
        warnings.addAll(raw.warnings)

        // 1. Determine Title & Subtitle & Category
        var title = raw.suggestedTitle.trim()
        if (title.isBlank()) {
            title = sanitizeFileNameToTitle(fileName)
        }
        val category = inferCategory(title, raw)
        val description = raw.suggestedSubtitle.trim()

        val sectionsMap = linkedMapOf<String, MutableList<ParsedItem>>()
        var currentSection = "1. Itens de Verificação"

        // 2. First analyze Structured Tables (if any)
        if (raw.tables.isNotEmpty()) {
            for (table in raw.tables) {
                if (table.title.isNotBlank()) {
                    currentSection = cleanSectionName(table.title)
                }

                // Analyze table columns
                val colMapping = analyzeTableColumns(table.headers)

                for (row in table.rows) {
                    if (row.cells.isEmpty()) continue

                    // Check if entire row is a section header spanning columns
                    val nonBlankCells = row.cells.map { it.text.trim() }.filter { it.isNotBlank() }
                    if (nonBlankCells.size == 1 && isSectionHeader(nonBlankCells[0])) {
                        currentSection = cleanSectionName(nonBlankCells[0])
                        continue
                    }

                    // Extract question / label
                    val questionCell = if (colMapping.questionColIdx in row.cells.indices) {
                        row.cells[colMapping.questionColIdx].text.trim()
                    } else if (nonBlankCells.isNotEmpty()) {
                        nonBlankCells.maxByOrNull { it.length } ?: ""
                    } else ""

                    if (questionCell.isBlank() || isTableNoise(questionCell)) continue

                    // Check if this looks like a sub-header or item
                    if (isSectionHeader(questionCell) && nonBlankCells.size <= 2) {
                        currentSection = cleanSectionName(questionCell)
                        continue
                    }

                    val numberPrefix = if (colMapping.numberColIdx in row.cells.indices) {
                        row.cells[colMapping.numberColIdx].text.trim().ifBlank { null }
                    } else {
                        extractNumberPrefix(questionCell)?.first
                    }

                    val cleanLabel = if (numberPrefix != null && questionCell.startsWith(numberPrefix)) {
                        questionCell.substring(numberPrefix.length).trim().removePrefix("-").removePrefix(".").removePrefix(":").trim()
                    } else {
                        questionCell
                    }

                    val item = ParsedItem(
                        id = UUID.randomUUID().toString(),
                        sectionName = currentSection,
                        numberPrefix = numberPrefix,
                        label = if (cleanLabel.isNotBlank()) cleanLabel else questionCell,
                        type = "C_NC_NA",
                        allowedStatuses = listOf("C", "PARCIAL", "NC", "NA"),
                        allowObservation = true,
                        allowPhoto = true,
                        allowAttachment = false,
                        isRequired = true,
                        requireObservationOnNC = true,
                        requirePhotoOnNC = false
                    )

                    val list = sectionsMap.getOrPut(currentSection) { mutableListOf() }
                    list.add(item)
                }
            }
        }

        // 3. Analyze Paragraphs & Raw Text Lines (if no table items or additional text)
        if (raw.paragraphs.isNotEmpty() || raw.rawTextLines.isNotEmpty()) {
            val linesToProcess = if (raw.paragraphs.isNotEmpty()) {
                raw.paragraphs.map { p ->
                    LineCandidate(
                        text = p.text.trim(),
                        isHeading = p.isHeading,
                        isListItem = p.isListItem,
                        listNumber = p.listNumber
                    )
                }
            } else {
                raw.rawTextLines.map { LineCandidate(text = it.trim()) }
            }

            for (candidate in linesToProcess) {
                val text = candidate.text
                if (text.isBlank() || isTableNoise(text)) continue

                // Check for Section Header
                if (candidate.isHeading || isSectionHeader(text)) {
                    currentSection = cleanSectionName(text)
                    continue
                }

                // Check for Item / Question
                val (prefix, label) = extractNumberPrefix(text) ?: (null to text)
                val isQuestionLike = candidate.isListItem || prefix != null || text.endsWith("?") || isInspectionItem(text)

                if (isQuestionLike && label.length >= 3) {
                    val item = ParsedItem(
                        id = UUID.randomUUID().toString(),
                        sectionName = currentSection,
                        numberPrefix = prefix ?: candidate.listNumber,
                        label = label,
                        type = "C_NC_NA",
                        allowedStatuses = listOf("C", "PARCIAL", "NC", "NA"),
                        allowObservation = true,
                        allowPhoto = true,
                        allowAttachment = false,
                        isRequired = true,
                        requireObservationOnNC = true
                    )

                    val list = sectionsMap.getOrPut(currentSection) { mutableListOf() }
                    list.add(item)
                }
            }
        }

        // 4. Fallback if no items were detected: parse line by line with relaxed rules
        if (sectionsMap.values.sumOf { it.size } == 0 && raw.rawTextLines.isNotEmpty()) {
            currentSection = "Itens da Vistoria"
            for (line in raw.rawTextLines) {
                val trimmed = line.trim()
                if (trimmed.length > 5 && !isTableNoise(trimmed)) {
                    val item = ParsedItem(
                        id = UUID.randomUUID().toString(),
                        sectionName = currentSection,
                        label = trimmed,
                        type = "C_NC_NA",
                        allowedStatuses = listOf("C", "PARCIAL", "NC", "NA"),
                        allowObservation = true,
                        allowPhoto = true,
                        isRequired = true
                    )
                    val list = sectionsMap.getOrPut(currentSection) { mutableListOf() }
                    list.add(item)
                }
            }
            if (sectionsMap.isNotEmpty()) {
                warnings.add("A estrutura foi montada a partir do texto contínuo do documento.")
            }
        }

        // 5. Build Ordered ParsedSections
        val parsedSections = sectionsMap.entries.mapIndexed { index, entry ->
            val sectionName = entry.key
            val items = entry.value
            // Check for duplicate questions
            val duplicates = items.groupBy { it.label.lowercase().trim() }.filter { it.value.size > 1 }
            if (duplicates.isNotEmpty()) {
                warnings.add("Foram encontradas ${duplicates.size} perguntas duplicadas na seção '$sectionName'.")
            }

            ParsedSection(
                id = UUID.randomUUID().toString(),
                name = sectionName,
                orderIndex = index,
                items = items
            )
        }.filter { it.items.isNotEmpty() }

        if (parsedSections.isEmpty()) {
            warnings.add("Não foram identificados itens de verificação com clareza. Você pode adicionar itens manualmente na prévia.")
        }

        return ParsedChecklist(
            title = title,
            subtitle = description,
            category = category,
            description = description,
            sections = parsedSections,
            availableStatuses = CustomStatusConfig.DEFAULT_STATUSES,
            sourceFormat = format,
            fileName = fileName,
            warnings = warnings,
            totalConfidenceScore = if (parsedSections.isNotEmpty()) 0.95f else 0.5f
        )
    }

    private data class LineCandidate(
        val text: String,
        val isHeading: Boolean = false,
        val isListItem: Boolean = false,
        val listNumber: String? = null
    )

    private data class TableColumnMapping(
        val numberColIdx: Int = -1,
        val questionColIdx: Int = 0,
        val conformeColIdx: Int = -1,
        val naoConformeColIdx: Int = -1,
        val parcialColIdx: Int = -1,
        val naColIdx: Int = -1,
        val obsColIdx: Int = -1
    )

    private fun analyzeTableColumns(headers: List<String>): TableColumnMapping {
        var numIdx = -1
        var questionIdx = -1
        var cIdx = -1
        var ncIdx = -1
        var pIdx = -1
        var naIdx = -1
        var obsIdx = -1

        headers.forEachIndexed { index, header ->
            val upper = header.trim().uppercase()
            when {
                upper in listOf("Nº", "NO", "N.", "#", "ITEM", "ORDEM", "COD", "CÓDIGO") -> numIdx = index
                upper in CONFORME_KEYWORDS -> cIdx = index
                upper in NAO_CONFORME_KEYWORDS -> ncIdx = index
                upper in PARCIAL_KEYWORDS -> pIdx = index
                upper in NA_KEYWORDS -> naIdx = index
                upper in OBS_KEYWORDS || upper.contains("OBS") || upper.contains("RESSALVA") -> obsIdx = index
                upper.contains("DESCRIÇÃO") || upper.contains("DESCRICAO") || upper.contains("PERGUNTA") ||
                        upper.contains("REQUISITO") || upper.contains("VERIFICAÇÃO") || upper.contains("ITEM") -> {
                    if (questionIdx == -1) questionIdx = index
                }
            }
        }

        // If question column wasn't explicitly found, select the most descriptive non-status column
        if (questionIdx == -1) {
            val candidates = headers.indices.filter { it != numIdx && it != cIdx && it != ncIdx && it != pIdx && it != naIdx && it != obsIdx }
            questionIdx = candidates.firstOrNull() ?: 0
        }

        return TableColumnMapping(
            numberColIdx = numIdx,
            questionColIdx = questionIdx,
            conformeColIdx = cIdx,
            naoConformeColIdx = ncIdx,
            parcialColIdx = pIdx,
            naColIdx = naIdx,
            obsColIdx = obsIdx
        )
    }

    private fun isSectionHeader(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 3 || trimmed.length > 90) return false
        if (trimmed.endsWith("?") || trimmed.endsWith(".")) return false
        if (SECTION_HEADER_REGEX.matches(trimmed)) return true
        if (trimmed.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()&" } && trimmed.length >= 4) {
            return true
        }
        return false
    }

    private fun cleanSectionName(text: String): String {
        return text.trim()
            .replace(Regex("""\s+"""), " ")
            .take(100)
    }

    private fun extractNumberPrefix(text: String): Pair<String, String>? {
        val match = ITEM_NUMBER_REGEX.find(text.trim()) ?: return null
        val prefix = match.groupValues[1].trim()
        val rest = match.groupValues[2].trim()
        return prefix to rest
    }

    private fun isInspectionItem(text: String): Boolean {
        val lower = text.lowercase()
        return lower.startsWith("verificar") ||
                lower.startsWith("inspecionar") ||
                lower.startsWith("avaliar") ||
                lower.startsWith("conferir") ||
                lower.startsWith("checar") ||
                lower.startsWith("analisar") ||
                lower.startsWith("identificar") ||
                lower.startsWith("testar") ||
                lower.startsWith("garantir") ||
                lower.contains("está conforme") ||
                lower.contains("em boas condições") ||
                lower.contains("dentro da validade")
    }

    private fun isTableNoise(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.length <= 1) return true
        if (lower in listOf("total", "subtotal", "assinatura", "responsável", "data", "hora", "página", "pagina")) return true
        if (lower.startsWith("página ") || lower.startsWith("pagina ")) return true
        return false
    }

    private fun sanitizeFileNameToTitle(fileName: String): String {
        val nameWithoutExt = fileName.substringBeforeLast(".")
        return nameWithoutExt
            .replace("_", " ")
            .replace("-", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { "Checklist Importado" }
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    private fun inferCategory(title: String, raw: RawDocumentContent): String {
        val text = (title + " " + raw.paragraphs.take(5).joinToString(" ") { it.text }).lowercase()
        return when {
            text.contains("veículo") || text.contains("veiculo") || text.contains("caminhão") || text.contains("frota") || text.contains("comboio") -> "Veículos e Frotas"
            text.contains("incêndio") || text.contains("incendio") || text.contains("extintor") || text.contains("emergência") -> "Combate a Incêndio"
            text.contains("nr-") || text.contains("nr ") || text.contains("segurança") || text.contains("seguranca") || text.contains("epi") || text.contains("epc") -> "Segurança do Trabalho"
            text.contains("elétrica") || text.contains("eletrica") || text.contains("painel") || text.contains("subestação") -> "Instalações Elétricas"
            text.contains("obra") || text.contains("construção") || text.contains("canteiro") || text.contains("andaime") -> "Construção Civil"
            text.contains("manutenção") || text.contains("manutencao") || text.contains("preventiva") -> "Manutenção Geral"
            text.contains("qualidade") || text.contains("5s") || text.contains("auditoria") || text.contains("iso") -> "Qualidade e Auditoria"
            text.contains("meio ambiente") || text.contains("ambiental") || text.contains("resíduos") -> "Meio Ambiente"
            else -> "Segurança do Trabalho"
        }
    }
}
