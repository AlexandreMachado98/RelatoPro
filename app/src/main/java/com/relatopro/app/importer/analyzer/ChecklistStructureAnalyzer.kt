package com.relatopro.app.importer.analyzer

import com.relatopro.app.importer.model.*
import com.relatopro.app.importer.parser.RawDocumentContent
import com.relatopro.app.importer.parser.RawParagraph
import com.relatopro.app.importer.parser.RawTable
import java.util.UUID

object ChecklistStructureAnalyzer {

    // Regex for Section / Group Headers (e.g. "1. DOCUMENTAÇÃO", "SEÇÃO 2: EQUIPAMENTOS", "MÓDULO I - VEÍCULO", "A — INSTALAÇÕES")
    private val SECTION_HEADER_REGEX = Regex(
        """^(?:(?:SEÇÃO|SECAO|GRUPO|MODULO|MÓDULO|BLOCO|PARTE|ETAPA|ITEM)\s*(?:[0-9IVXLCDM]+|[A-Z])[:\-–.]?\s+)?([0-9IVXLCDM]+[.\-–\s]+[A-ZÀ-Ú\s\-_/()]{3,}|[A-ZÀ-Ú\s\-_/()]{4,})$""",
        RegexOption.IGNORE_CASE
    )

    // Regex for Multi-level numbering
    private val MULTI_LEVEL_NUMBER_REGEX = Regex(
        """^((?:ITEM\s*)?([0-9]{1,3}(?:\.[0-9]{1,3})+|[0-9]{1,3}\.|[A-Z]\.(?:[0-9]{1,3})*|[a-z]\)|[0-9]{1,3}\s*[-–]))\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )

    // Keywords for status columns
    private val CONFORME_KEYWORDS = setOf("C", "CONFORME", "SIM", "OK", "BOM", "ADEQUADO", "ATENDE", "COMPLIANT", "TRUE", "S", "1")
    private val NAO_CONFORME_KEYWORDS = setOf("NC", "NÃO CONFORME", "NAO CONFORME", "NÃO", "NAO", "NOK", "RUIM", "IRREGULAR", "NÃO ATENDE", "NON-COMPLIANT", "FALSE", "N")
    private val PARCIAL_KEYWORDS = setOf("P", "PARCIAL", "PARCIALMENTE CONFORME", "PARCIALMENTE", "RESSALVA", "REGULAR", "PARTIAL")
    private val NA_KEYWORDS = setOf("NA", "N/A", "NÃO APLICÁVEL", "NAO APLICAVEL", "NÃO SE APLICA", "EXCLUÍDO", "NOT APPLICABLE")
    private val OBS_KEYWORDS = setOf("OBS", "OBSERVAÇÃO", "OBSERVACOES", "OBSERVAÇÕES", "COMENTÁRIO", "RESSALVAS", "AÇÃO", "NOTAS", "EVIDÊNCIA")
    private val PHOTO_KEYWORDS = setOf("FOTO", "FOTOS", "FOTOGRAFIA", "ANEXO", "EVIDÊNCIA FOTOGRÁFICA", "IMAGEM")

    // Interrogative / Verification prefixes in Portuguese
    private val INSPECTION_VERB_PREFIXES = listOf(
        "verificar", "inspecionar", "avaliar", "conferir", "checar", "analisar",
        "identificar", "testar", "garantir", "assegurar", "constatar", "observar",
        "examinar", "revisar", "validar"
    )

    private val QUESTION_STARTER_PREFIXES = listOf(
        "o ", "a ", "os ", "as ", "existe ", "existem ", "possui ", "possuem ",
        "está ", "estão ", "são ", "é ", "há ", "dispõe ", "dispõem ", "apresenta ",
        "apresentam ", "foram ", "foi ", "todos ", "todas ", "nenhum "
    )

    /**
     * Backward-compatible entrypoint for raw document content
     */
    fun analyze(
        raw: RawDocumentContent,
        format: ImportFormat,
        fileName: String
    ): ParsedChecklist {
        val richDoc = RichDocumentContent(
            suggestedTitle = raw.suggestedTitle,
            suggestedSubtitle = raw.suggestedSubtitle,
            paragraphs = raw.paragraphs.map { p ->
                RichParagraph(
                    text = p.text,
                    style = ElementStyle(
                        isBold = p.isHeading,
                        isHeadingStyle = p.isHeading,
                        headingLevel = p.headingLevel
                    ),
                    isListItem = p.isListItem,
                    listNumber = p.listNumber,
                    location = SourceLocation(rawText = p.text)
                )
            },
            tables = raw.tables.map { t ->
                RichTable(
                    title = t.title,
                    headers = t.headers,
                    rows = t.rows.mapIndexed { rIdx, r ->
                        RichRow(
                            rowIndex = rIdx,
                            cells = r.cells.mapIndexed { cIdx, c ->
                                RichCell(
                                    text = c.text,
                                    columnIndex = cIdx,
                                    rowIndex = rIdx,
                                    isHeader = c.isHeader
                                )
                            }
                        )
                    }
                )
            },
            rawTextLines = raw.rawTextLines,
            alerts = raw.warnings.map {
                ImportValidationAlert(
                    title = "Aviso de Importação",
                    description = it,
                    severity = AlertSeverity.WARNING
                )
            },
            ocrUsed = raw.ocrUsed
        )

        return analyzeRich(richDoc, format, fileName)
    }

    /**
     * Advanced Rich Document Structural Analyzer
     */
    fun analyzeRich(
        raw: RichDocumentContent,
        format: ImportFormat,
        fileName: String
    ): ParsedChecklist {
        val alerts = mutableListOf<ImportValidationAlert>()
        alerts.addAll(raw.alerts)

        // 1. Determine Title & Subtitle & Category
        var title = raw.suggestedTitle.trim()
        if (title.isBlank() || isMetadataNoise(title)) {
            title = sanitizeFileNameToTitle(fileName)
        }
        val category = inferCategory(title, raw)
        val description = raw.suggestedSubtitle.trim()

        // Hierarchical structure: Section Name -> Map of Category Name -> List of ParsedItems
        val hierarchyMap = linkedMapOf<String, LinkedHashMap<String, MutableList<ParsedItem>>>()
        var currentSection = "1. Itens de Verificação"
        var currentCategory = "Geral"

        // 2. Process Rich Tables
        if (raw.tables.isNotEmpty()) {
            for (table in raw.tables) {
                if (table.title.isNotBlank() && !isMetadataNoise(table.title)) {
                    val (level, name) = classifyHeaderLevel(table.title)
                    if (level == HierarchyLevel.SECTION) {
                        currentSection = cleanName(name)
                        currentCategory = "Geral"
                    } else if (level == HierarchyLevel.CATEGORY) {
                        currentCategory = cleanName(name)
                    }
                }

                val colMapping = analyzeTableColumns(table.headers)

                for (row in table.rows) {
                    if (row.cells.isEmpty()) continue

                    val nonBlankCells = row.cells.map { it.text.trim() }.filter { it.isNotBlank() }
                    if (nonBlankCells.isEmpty()) continue

                    // Check if entire row is a section break banner
                    if (row.isSectionBreakRow || (nonBlankCells.size == 1 && nonBlankCells[0].length in 3..90)) {
                        val bannerText = nonBlankCells[0]
                        if (!isMetadataNoise(bannerText)) {
                            val (level, name) = classifyHeaderLevel(bannerText)
                            if (level == HierarchyLevel.SECTION) {
                                currentSection = cleanName(name)
                                currentCategory = "Geral"
                            } else {
                                currentCategory = cleanName(name)
                            }
                            continue
                        }
                    }

                    // Check if this row is an administrative noise row (CNPJ, Signature, Total, Date)
                    if (isRowNoise(nonBlankCells)) {
                        continue
                    }

                    // Extract question text
                    val questionCell = if (colMapping.questionColIdx in row.cells.indices) {
                        row.cells[colMapping.questionColIdx].text.trim()
                    } else {
                        nonBlankCells.maxByOrNull { it.length } ?: ""
                    }

                    if (questionCell.isBlank() || isMetadataNoise(questionCell)) continue

                    // Check if questionCell is actually a section/category title inside the table
                    val (headerLvl, headerName) = classifyHeaderLevel(questionCell)
                    if (headerLvl == HierarchyLevel.SECTION && nonBlankCells.size <= 2) {
                        currentSection = cleanName(headerName)
                        currentCategory = "Geral"
                        continue
                    } else if (headerLvl == HierarchyLevel.CATEGORY && nonBlankCells.size <= 2) {
                        currentCategory = cleanName(headerName)
                        continue
                    }

                    // Extract numbering prefix
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

                    // Extract Pre-filled Answer from table row
                    val extractedAnswer = extractPreFilledAnswerFromRow(row, colMapping)
                    if (extractedAnswer?.hasConflict == true) {
                        alerts.add(
                            ImportValidationAlert(
                                elementLabel = cleanLabel.take(50),
                                title = "Conflito de Resposta Detectado",
                                description = "O item '$cleanLabel' possui múltiplas marcações conflitantes no documento original (${extractedAnswer.conflictingCodes.joinToString(", ")}).",
                                severity = AlertSeverity.CRITICAL,
                                suggestedAction = "Selecione o status correto na prévia de importação."
                            )
                        )
                    }

                    // Calculate Confidence Score
                    val confidence = calculateItemConfidence(
                        label = cleanLabel,
                        numberPrefix = numberPrefix,
                        hasTableContext = true,
                        isQuestionFormat = cleanLabel.endsWith("?") || isQuestionSentence(cleanLabel)
                    )

                    val item = ParsedItem(
                        id = UUID.randomUUID().toString(),
                        sectionName = currentSection,
                        categoryName = currentCategory.takeIf { it != "Geral" },
                        hierarchyPath = if (currentCategory != "Geral") "$currentSection > $currentCategory" else currentSection,
                        numberPrefix = numberPrefix,
                        label = if (cleanLabel.isNotBlank()) cleanLabel else questionCell,
                        type = "C_NC_NA",
                        allowedStatuses = listOf("C", "PARCIAL", "NC", "NA"),
                        allowObservation = true,
                        allowPhoto = true,
                        allowAttachment = false,
                        isRequired = true,
                        requireObservationOnNC = true,
                        requirePhotoOnNC = colMapping.photoColIdx != -1,
                        preFilledAnswer = extractedAnswer,
                        confidenceRating = confidence.rating,
                        confidenceScore = confidence.score,
                        classificationReason = confidence.reason,
                        sourceLocation = SourceLocation(
                            sheetName = table.location.sheetName,
                            pageNumber = table.location.pageNumber,
                            rowNumber = row.rowIndex,
                            rawText = questionCell
                        )
                    )

                    val categoryMap = hierarchyMap.getOrPut(currentSection) { linkedMapOf() }
                    val itemList = categoryMap.getOrPut(currentCategory) { mutableListOf() }
                    itemList.add(item)
                }
            }
        }

        // 3. Process Rich Paragraphs and Raw Text Lines (if any)
        if (raw.paragraphs.isNotEmpty() || raw.rawTextLines.isNotEmpty()) {
            val paragraphsToProcess = if (raw.paragraphs.isNotEmpty()) {
                raw.paragraphs
            } else {
                raw.rawTextLines.map {
                    RichParagraph(
                        text = it,
                        style = ElementStyle(),
                        location = SourceLocation(rawText = it)
                    )
                }
            }

            for (para in paragraphsToProcess) {
                val text = para.text.trim()
                if (text.isBlank() || isMetadataNoise(text)) continue

                val (level, name) = classifyHeaderLevel(text, para.style)
                when (level) {
                    HierarchyLevel.SECTION -> {
                        currentSection = cleanName(name)
                        currentCategory = "Geral"
                        continue
                    }
                    HierarchyLevel.CATEGORY -> {
                        currentCategory = cleanName(name)
                        continue
                    }
                    HierarchyLevel.METADATA_NOISE, HierarchyLevel.TABLE_HEADER -> {
                        continue
                    }
                    else -> {
                        // Candidate for Item
                        val (prefix, label) = extractNumberPrefix(text) ?: (null to text)
                        val isQuestionLike = para.isListItem || prefix != null || text.endsWith("?") || isQuestionSentence(text)

                        if (isQuestionLike && label.length >= 3) {
                            val confidence = calculateItemConfidence(
                                label = label,
                                numberPrefix = prefix ?: para.listNumber,
                                hasTableContext = false,
                                isQuestionFormat = text.endsWith("?") || isQuestionSentence(text)
                            )

                            val item = ParsedItem(
                                id = UUID.randomUUID().toString(),
                                sectionName = currentSection,
                                categoryName = currentCategory.takeIf { it != "Geral" },
                                hierarchyPath = if (currentCategory != "Geral") "$currentSection > $currentCategory" else currentSection,
                                numberPrefix = prefix ?: para.listNumber,
                                label = label,
                                type = "C_NC_NA",
                                allowedStatuses = listOf("C", "PARCIAL", "NC", "NA"),
                                allowObservation = true,
                                allowPhoto = true,
                                allowAttachment = false,
                                isRequired = true,
                                requireObservationOnNC = true,
                                confidenceRating = confidence.rating,
                                confidenceScore = confidence.score,
                                classificationReason = confidence.reason,
                                sourceLocation = para.location
                            )

                            val categoryMap = hierarchyMap.getOrPut(currentSection) { linkedMapOf() }
                            val itemList = categoryMap.getOrPut(currentCategory) { mutableListOf() }
                            itemList.add(item)
                        }
                    }
                }
            }
        }

        // 4. Fallback if empty: line-by-line tolerant extraction
        if (hierarchyMap.values.sumOf { catMap -> catMap.values.sumOf { it.size } } == 0 && raw.rawTextLines.isNotEmpty()) {
            currentSection = "Itens da Inspeção"
            currentCategory = "Geral"
            for (line in raw.rawTextLines) {
                val trimmed = line.trim()
                if (trimmed.length > 4 && !isMetadataNoise(trimmed)) {
                    val (prefix, label) = extractNumberPrefix(trimmed) ?: (null to trimmed)
                    val item = ParsedItem(
                        id = UUID.randomUUID().toString(),
                        sectionName = currentSection,
                        numberPrefix = prefix,
                        label = label,
                        type = "C_NC_NA",
                        allowedStatuses = listOf("C", "PARCIAL", "NC", "NA"),
                        allowObservation = true,
                        allowPhoto = true,
                        isRequired = true,
                        confidenceRating = ConfidenceRating.MEDIUM,
                        confidenceScore = 0.70f,
                        classificationReason = "Extraído a partir de fluxo de recuperação de texto contínuo"
                    )
                    val categoryMap = hierarchyMap.getOrPut(currentSection) { linkedMapOf() }
                    val itemList = categoryMap.getOrPut(currentCategory) { mutableListOf() }
                    itemList.add(item)
                }
            }
            if (hierarchyMap.isNotEmpty()) {
                alerts.add(
                    ImportValidationAlert(
                        title = "Estrutura Reconstruída",
                        description = "O layout nativo de tabelas não pôde ser determinado com precisão. O checklist foi montado a partir do texto sequencial.",
                        severity = AlertSeverity.WARNING
                    )
                )
            }
        }

        // 5. Build ParsedSections and check for duplicates
        val allLabelsSeen = mutableMapOf<String, Int>()
        val parsedSections = hierarchyMap.entries.mapIndexed { sIdx, sectionEntry ->
            val secName = sectionEntry.key
            val categories = mutableListOf<ParsedCategory>()
            val directItems = mutableListOf<ParsedItem>()

            sectionEntry.value.forEach { (catName, items) ->
                // Check duplicates
                items.forEach { item ->
                    val clean = item.label.lowercase().trim()
                    allLabelsSeen[clean] = (allLabelsSeen[clean] ?: 0) + 1
                }

                if (catName == "Geral") {
                    directItems.addAll(items)
                } else {
                    categories.add(
                        ParsedCategory(
                            id = UUID.randomUUID().toString(),
                            name = catName,
                            level = 2,
                            orderIndex = categories.size,
                            items = items
                        )
                    )
                }
            }

            ParsedSection(
                id = UUID.randomUUID().toString(),
                name = secName,
                orderIndex = sIdx,
                categories = categories,
                items = directItems
            )
        }.filter { it.items.isNotEmpty() || it.categories.any { cat -> cat.items.isNotEmpty() } }

        // Alert on duplicates
        val duplicates = allLabelsSeen.filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            alerts.add(
                ImportValidationAlert(
                    title = "Possíveis Itens Duplicados",
                    description = "Foram encontrados ${duplicates.size} itens com textos idênticos em seções diferentes.",
                    severity = AlertSeverity.INFO,
                    suggestedAction = "Verifique na prévia se as repetições são intencionais (ex: em etapas diferentes da inspeção)."
                )
            )
        }

        val totalItems = parsedSections.sumOf { s -> s.items.size + s.categories.sumOf { c -> c.items.size } }
        val overallConfidence = if (totalItems > 0) {
            val totalScore = parsedSections.sumOf { s ->
                s.items.sumOf { it.confidenceScore.toDouble() } + s.categories.sumOf { c -> c.items.sumOf { it.confidenceScore.toDouble() } }
            }
            (totalScore / totalItems).toFloat()
        } else 0.5f

        val warningsList = alerts.map { "${it.title}: ${it.description}" }

        return ParsedChecklist(
            title = title,
            subtitle = description,
            category = category,
            description = description,
            sections = parsedSections,
            availableStatuses = CustomStatusConfig.DEFAULT_STATUSES,
            sourceFormat = format,
            fileName = fileName,
            warnings = warningsList,
            validationAlerts = alerts,
            totalConfidenceScore = overallConfidence
        )
    }

    // ==========================================
    // CLASSIFICATION & HEURISTICS
    // ==========================================

    private data class ClassifiedLevel(val level: HierarchyLevel, val cleanName: String)

    private fun classifyHeaderLevel(text: String, style: ElementStyle = ElementStyle()): ClassifiedLevel {
        val trimmed = text.trim()
        if (isMetadataNoise(trimmed)) return ClassifiedLevel(HierarchyLevel.METADATA_NOISE, trimmed)

        // Heading styles from Word / OpenXML
        if (style.isHeadingStyle) {
            return when (style.headingLevel) {
                1 -> ClassifiedLevel(HierarchyLevel.SECTION, trimmed)
                2 -> ClassifiedLevel(HierarchyLevel.CATEGORY, trimmed)
                3 -> ClassifiedLevel(HierarchyLevel.SUBCATEGORY, trimmed)
                else -> ClassifiedLevel(HierarchyLevel.SECTION, trimmed)
            }
        }

        // Multi-level number patterns (e.g. "1.", "1.1", "1.1.1")
        val match = MULTI_LEVEL_NUMBER_REGEX.find(trimmed)
        if (match != null) {
            val numberStr = match.groupValues[2].trim().removeSuffix(".")
            val rest = match.groupValues[3].trim()
            val dotCount = numberStr.count { it == '.' }

            when (dotCount) {
                0 -> {
                    // "1." or "A." -> Section if uppercase or short, else Category
                    if (rest.all { it.isUpperCase() || it.isWhitespace() || it in ".-_/:()" } || rest.length <= 40) {
                        return ClassifiedLevel(HierarchyLevel.SECTION, trimmed)
                    }
                }
                1 -> {
                    // "1.1" -> Category
                    if (!rest.endsWith("?") && !isQuestionSentence(rest)) {
                        return ClassifiedLevel(HierarchyLevel.CATEGORY, trimmed)
                    }
                }
                2 -> {
                    // "1.1.1" -> Subcategory or Item
                    if (!rest.endsWith("?") && !isQuestionSentence(rest) && rest.length <= 40) {
                        return ClassifiedLevel(HierarchyLevel.SUBCATEGORY, trimmed)
                    }
                }
            }
        }

        // Regex for uppercase section header
        if (SECTION_HEADER_REGEX.matches(trimmed) && trimmed.length in 3..90 && !trimmed.endsWith("?")) {
            return ClassifiedLevel(HierarchyLevel.SECTION, trimmed)
        }

        // Uppercase short strings
        if (trimmed.length in 4..60 && trimmed.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-_/:()&" } && !trimmed.endsWith("?")) {
            return ClassifiedLevel(HierarchyLevel.SECTION, trimmed)
        }

        return ClassifiedLevel(HierarchyLevel.ITEM, trimmed)
    }

    private data class ItemConfidenceResult(
        val rating: ConfidenceRating,
        val score: Float,
        val reason: String
    )

    private fun calculateItemConfidence(
        label: String,
        numberPrefix: String?,
        hasTableContext: Boolean,
        isQuestionFormat: Boolean
    ): ItemConfidenceResult {
        var score = 0.50f
        val reasons = mutableListOf<String>()

        if (isQuestionFormat) {
            score += 0.25f
            reasons.add("Estrutura interrogativa detectada")
        }

        if (isQuestionSentence(label)) {
            score += 0.15f
            reasons.add("Verbo ou condição de inspeção identificado")
        }

        if (numberPrefix != null) {
            score += 0.10f
            reasons.add("Numeração de item reconhecida ($numberPrefix)")
        }

        if (hasTableContext) {
            score += 0.10f
            reasons.add("Localizado em tabela de inspeção")
        }

        val clampedScore = score.coerceIn(0.10f, 0.99f)
        val rating = when {
            clampedScore >= 0.90f -> ConfidenceRating.HIGH
            clampedScore >= 0.65f -> ConfidenceRating.MEDIUM
            else -> ConfidenceRating.LOW
        }

        val reasonText = if (reasons.isNotEmpty()) reasons.joinToString("; ") else "Identificado por padrão textual"
        return ItemConfidenceResult(rating, clampedScore, reasonText)
    }

    private fun isQuestionSentence(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.endsWith("?")) return true

        if (INSPECTION_VERB_PREFIXES.any { lower.startsWith(it) }) return true
        if (QUESTION_STARTER_PREFIXES.any { lower.startsWith(it) }) return true

        return lower.contains("está conforme") ||
                lower.contains("em boas condições") ||
                lower.contains("dentro da validade") ||
                lower.contains("sem vazamento") ||
                lower.contains("devidamente sinalizado") ||
                lower.contains("proteção adequada")
    }

    private fun isMetadataNoise(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.length <= 1) return true

        // Administrative noise
        if (lower.contains("cnpj") || lower.contains("inscrição estadual") || lower.contains("endereço:") || lower.contains("telefone:")) return true
        if (lower.startsWith("página ") || lower.startsWith("pagina ") || lower.matches(Regex("""^\d+\s*/\s*\d+$"""))) return true
        if (lower.startsWith("data:") || lower.startsWith("hora:") || lower.startsWith("local:") || lower.startsWith("unidade:")) return true
        if (lower.startsWith("assinatura") || lower.startsWith("visto do") || lower.startsWith("responsável técnico")) return true
        if (lower.startsWith("instruções:") || lower.startsWith("instrucoes:") || lower.startsWith("nota:")) return true

        // Table headers that might appear alone
        if (lower in listOf("c", "nc", "na", "p", "parcial", "conforme", "não conforme", "não aplicável", "observação", "observações", "foto", "anexo")) return true

        return false
    }

    private fun isRowNoise(cells: List<String>): Boolean {
        val combined = cells.joinToString(" ").lowercase()
        return combined.contains("total de itens") ||
                combined.contains("assinatura do") ||
                combined.contains("todos os direitos reservados") ||
                combined.contains("responsável pela inspeção")
    }

    private data class TableColumnMapping(
        val numberColIdx: Int = -1,
        val questionColIdx: Int = 0,
        val conformeColIdx: Int = -1,
        val naoConformeColIdx: Int = -1,
        val parcialColIdx: Int = -1,
        val naColIdx: Int = -1,
        val obsColIdx: Int = -1,
        val photoColIdx: Int = -1
    )

    private fun analyzeTableColumns(headers: List<String>): TableColumnMapping {
        var numIdx = -1
        var questionIdx = -1
        var cIdx = -1
        var ncIdx = -1
        var pIdx = -1
        var naIdx = -1
        var obsIdx = -1
        var photoIdx = -1

        headers.forEachIndexed { index, header ->
            val upper = header.trim().uppercase()
            when {
                upper in listOf("Nº", "NO", "N.", "#", "ITEM", "ORDEM", "COD", "CÓDIGO") -> numIdx = index
                upper in CONFORME_KEYWORDS -> cIdx = index
                upper in NAO_CONFORME_KEYWORDS -> ncIdx = index
                upper in PARCIAL_KEYWORDS -> pIdx = index
                upper in NA_KEYWORDS -> naIdx = index
                upper in OBS_KEYWORDS || upper.contains("OBS") || upper.contains("RESSALVA") -> obsIdx = index
                upper in PHOTO_KEYWORDS || upper.contains("FOTO") || upper.contains("EVIDÊNCIA") -> photoIdx = index
                upper.contains("DESCRIÇÃO") || upper.contains("DESCRICAO") || upper.contains("PERGUNTA") ||
                        upper.contains("REQUISITO") || upper.contains("VERIFICAÇÃO") || upper.contains("ITEM") -> {
                    if (questionIdx == -1) questionIdx = index
                }
            }
        }

        if (questionIdx == -1) {
            val candidates = headers.indices.filter {
                it != numIdx && it != cIdx && it != ncIdx && it != pIdx && it != naIdx && it != obsIdx && it != photoIdx
            }
            questionIdx = candidates.firstOrNull() ?: 0
        }

        return TableColumnMapping(
            numberColIdx = numIdx,
            questionColIdx = questionIdx,
            conformeColIdx = cIdx,
            naoConformeColIdx = ncIdx,
            parcialColIdx = pIdx,
            naColIdx = naIdx,
            obsColIdx = obsIdx,
            photoColIdx = photoIdx
        )
    }

    private fun extractPreFilledAnswerFromRow(
        row: RichRow,
        mapping: TableColumnMapping
    ): ExtractedAnswer? {
        val detectedMarks = mutableListOf<Pair<String, String>>() // Status to RawMark

        fun checkCell(colIdx: Int, code: String, name: String) {
            if (colIdx in row.cells.indices) {
                val mark = row.cells[colIdx].text.trim().uppercase()
                if (isPositiveMark(mark)) {
                    detectedMarks.add(code to mark)
                }
            }
        }

        checkCell(mapping.conformeColIdx, "C", "Conforme")
        checkCell(mapping.naoConformeColIdx, "NC", "Não Conforme")
        checkCell(mapping.parcialColIdx, "PARCIAL", "Parcial")
        checkCell(mapping.naColIdx, "NA", "Não Aplicável")

        if (detectedMarks.isEmpty()) return null

        val hasConflict = detectedMarks.size > 1
        val first = detectedMarks.first()

        return ExtractedAnswer(
            statusShortCode = first.first,
            statusName = when (first.first) {
                "C" -> "Conforme"
                "NC" -> "Não Conforme"
                "PARCIAL" -> "Parcial"
                "NA" -> "Não Aplicável"
                else -> first.first
            },
            rawMarking = first.second,
            hasConflict = hasConflict,
            conflictingCodes = detectedMarks.map { it.first }
        )
    }

    private fun isPositiveMark(mark: String): Boolean {
        if (mark.isBlank()) return false
        val upper = mark.uppercase()
        return upper in listOf("X", "✓", "SIM", "OK", "1", "S", "SIM", "V", "TRUE", "•", "*")
    }

    private fun cleanName(text: String): String {
        return text.trim()
            .replace(Regex("""\s+"""), " ")
            .take(100)
    }

    private fun extractNumberPrefix(text: String): Pair<String, String>? {
        val match = MULTI_LEVEL_NUMBER_REGEX.find(text.trim()) ?: return null
        val prefix = match.groupValues[1].trim()
        val rest = match.groupValues[3].trim()
        return prefix to rest
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

    private fun inferCategory(title: String, raw: RichDocumentContent): String {
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
